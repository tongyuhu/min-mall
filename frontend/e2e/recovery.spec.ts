import { test, expect } from '@playwright/test';

const address = {
  receiver: '测试买家',
  phone: '13800000000',
  province: '浙江',
  city: '杭州',
  district: '西湖',
  detail: '原地址',
  defaultAddress: true,
};

test('下单响应丢失后刷新恢复原请求，成功后相同商品能再次购买', async ({ page }) => {
  const requests: { key: string; body: any }[] = [];
  const quote = {
    skus: [{ skuId: 'sku-1', title: '回归商品', specs: '默认规格', priceFen: 1990 }],
    goodsFen: 1990,
    discountFen: 0,
    freightFen: 0,
    payableFen: 1990,
    fingerprint: 'same-quote',
  };
  const order = {
    orderNo: 'Oregression',
    status: 'PAID',
    goodsFen: 1990,
    discountFen: 0,
    freightFen: 0,
    payableFen: 1990,
    items: [],
    address,
    createdAt: new Date().toISOString(),
    activeAfterSaleNo: null,
  };
  await page.route('**/api/v1/**', async (route) => {
    const url = new URL(route.request().url()),
      path = url.pathname;
    let body: any = [];
    if (path.endsWith('/me')) body = { id: 'recovery-buyer', username: 'buyer', nickname: '买家' };
    if (path.endsWith('/addresses'))
      body = [
        { id: 'address-1', address },
        { id: 'address-2', address: { ...address, detail: '非默认地址', defaultAddress: false } },
      ];
    if (path.endsWith('/checkout')) body = quote;
    if (path === '/api/v1/orders' && route.request().method() === 'POST') {
      requests.push({
        key: route.request().headers()['idempotency-key']!,
        body: route.request().postDataJSON(),
      });
      if (requests.length === 1) {
        await route.fulfill({ status: 503, json: { message: '模拟：响应丢失，结果未知' } });
        return;
      }
      body = { ...order, orderNo: requests.length === 2 ? 'Oregression' : 'Osecond' };
    }
    if (/\/orders\/O/.test(path) && !path.endsWith('/refunds')) body = order;
    await route.fulfill({ json: body });
  });
  await page.goto('/login');
  await page.evaluate(() => {
    sessionStorage.setItem(
      'mini-mall:buyer',
      JSON.stringify({ token: 'test', expiresAt: '2099-01-01' }),
    );
    sessionStorage.setItem('mini-mall:checkout', JSON.stringify([{ skuId: 'sku-1', quantity: 1 }]));
  });
  await page.goto('/checkout');
  await page.getByRole('radio').nth(1).check();
  await page.getByRole('button', { name: '确认金额' }).click();
  await page.getByRole('button', { name: '提交订单', exact: true }).click();
  await expect(page.getByRole('button', { name: '重试提交订单' })).toBeVisible();
  await page.reload();
  await expect(page.getByRole('radio').nth(1)).toBeChecked();
  await expect(page.getByRole('radio').first()).toBeDisabled();
  await page.getByRole('button', { name: '重试提交订单' }).click();
  await expect(page).toHaveURL('/orders/Oregression');
  expect(requests[1]).toEqual(requests[0]);
  await page.evaluate(() =>
    sessionStorage.setItem('mini-mall:checkout', JSON.stringify([{ skuId: 'sku-1', quantity: 1 }])),
  );
  await page.goto('/checkout');
  await page.getByRole('radio').nth(1).check();
  await page.getByRole('button', { name: '确认金额' }).click();
  await page.getByRole('button', { name: '提交订单', exact: true }).click();
  await expect(page).toHaveURL('/orders/Osecond');
  expect(requests[2]!.key).not.toEqual(requests[1]!.key);
});

test('慢查询期间禁止切换筛选，避免显示条件与响应不一致', async ({ page }) => {
  let release: (() => void) | undefined;
  await page.route('**/api/v1/**', async (route) => {
    const url = new URL(route.request().url());
    if (url.pathname.endsWith('/categories')) {
      await route.fulfill({
        json: [
          { id: '1', name: '分类甲' },
          { id: '2', name: '分类乙' },
        ],
      });
      return;
    }
    if (url.pathname.endsWith('/brands')) {
      await route.fulfill({ json: [] });
      return;
    }
    if (url.searchParams.get('categoryId') === '1')
      await new Promise<void>((resolve) => {
        release = resolve;
      });
    await route.fulfill({ json: { items: [], total: 0, page: 1, size: 12 } });
  });
  await page.goto('/products');
  await page.getByRole('button', { name: '分类甲', exact: true }).click();
  await expect(page.getByRole('button', { name: '分类乙', exact: true })).toBeDisabled();
  await expect(page.getByLabel('排序')).toBeDisabled();
  await expect.poll(() => !!release).toBe(true);
  release!();
  await expect(page.getByRole('button', { name: '分类乙', exact: true })).toBeEnabled();
});

test('库存调整结果不明时刷新保留原 SKU、数量与幂等键', async ({ page }) => {
  const requests: { key: string; body: any; path: string }[] = [];
  await page.route('**/admin/v1/**', async (route) => {
    const path = new URL(route.request().url()).pathname;
    if (path.endsWith('/auth/me')) {
      await route.fulfill({
        json: { id: 'inventory-admin', username: '仓管', permissions: ['stock:adjust'] },
      });
      return;
    }
    if (path.endsWith('/adjustments')) {
      requests.push({
        key: route.request().headers()['idempotency-key']!,
        body: route.request().postDataJSON(),
        path,
      });
      await route.fulfill(
        requests.length === 1
          ? { status: 503, json: { message: '模拟响应丢失' } }
          : { status: 204 },
      );
      return;
    }
    await route.fulfill({ json: { available: 10, reserved: 0 } });
  });
  await page.goto('/admin/login');
  await page.evaluate(() =>
    sessionStorage.setItem(
      'mini-mall:admin',
      JSON.stringify({ token: 'test', expiresAt: '2099-01-01' }),
    ),
  );
  await page.goto('/admin/stock');
  await page.getByLabel('SKU 编号').fill('123');
  await page.getByRole('button', { name: '查询库存' }).click();
  await page.getByLabel('调整数量').fill('10');
  page.on('dialog', (dialog) => dialog.accept());
  await page.getByRole('button', { name: '确认调整' }).click();
  await expect(page.getByRole('alert')).toContainText('模拟响应丢失');
  await page.reload();
  await expect(page.getByLabel('SKU 编号')).toHaveValue('123');
  await expect(page.getByLabel('调整数量')).toHaveValue('10');
  await expect(page.getByLabel('SKU 编号')).toBeDisabled();
  await page.getByRole('button', { name: '确认调整' }).click();
  await expect(page.getByText('库存调整成功', { exact: true })).toBeVisible();
  expect(requests[1]).toEqual(requests[0]);
  await expect(page.getByLabel('SKU 编号')).toBeEnabled();
});
