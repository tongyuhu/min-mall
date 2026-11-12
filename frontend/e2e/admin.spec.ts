import { test, expect } from '@playwright/test';

test('后台页面、商品创建编辑、库存与上下架真实联调', async ({ page }) => {
  test.skip(!process.env.MALL_ADMIN_PASSWORD, '需要本地管理员密码');
  const errors: string[] = [];
  page.on('pageerror', (e) => errors.push(e.message));
  page.on('dialog', (dialog) => dialog.accept());
  await page.goto('/admin/login');
  await page.getByLabel('用户名', { exact: true }).fill('admin');
  await page.getByLabel('密码', { exact: true }).fill(process.env.MALL_ADMIN_PASSWORD!);
  await page.getByRole('button', { name: '登录管理后台' }).click();
  await expect(page).toHaveURL('/admin');
  for (const [path, heading] of [
    ['catalog', '分类与规格'],
    ['coupons', '优惠券管理'],
    ['members', '会员管理'],
    ['access', '账号与权限'],
    ['tasks', '后台任务'],
    ['audits', '操作审计'],
  ]) {
    await page.goto(`/admin/${path}`);
    await expect(page.getByRole('heading', { name: heading, exact: true })).toBeVisible();
    await expect(page.locator('.loading')).toHaveCount(0);
    await expect(page.getByRole('alert')).toHaveCount(0);
  }
  const name = `浏览器验收商品-${Date.now()}`;
  await page.goto('/admin/products');
  await page.getByRole('button', { name: '新增商品' }).click();
  await page.getByLabel('商品名称', { exact: true }).fill(name);
  await page.getByLabel('分类', { exact: true }).selectOption({ index: 1 });
  await page.getByLabel('售价（元）').fill('39.90');
  await page.getByRole('button', { name: '保存商品' }).click();
  await expect(page.getByRole('dialog')).toHaveCount(0);
  const row = page.getByRole('row').filter({ hasText: name });
  await expect(row).toBeVisible();
  await row.getByRole('button', { name: '编辑', exact: true }).click();
  await expect(page.getByLabel('售价（元）')).toHaveValue('39.90');
  await page.getByLabel('售价（元）').fill('35.80');
  await page.getByRole('button', { name: '保存商品' }).click();
  await expect(row).toContainText('35.80');
  await row.getByRole('link', { name: '库存', exact: true }).click();
  await page.getByLabel('调整数量').fill('10');
  await page.getByRole('button', { name: '确认调整' }).click();
  await expect(page.getByText('库存调整成功', { exact: true })).toBeVisible();
  await page.goto('/admin/products');
  await row.getByRole('button', { name: '上架', exact: true }).click();
  await expect(row).toContainText('已上架');
  await page.goto('/products');
  await page.getByLabel('搜索商品').fill(name);
  await page.getByRole('button', { name: '搜索', exact: true }).click();
  await expect(page.locator('.product-card').filter({ hasText: name })).toBeVisible();
  // 验收商品保留为草稿，避免影响用户日常浏览。
  await page.goto('/admin/products');
  await row.getByRole('button', { name: '下架', exact: true }).click();
  await expect(row).toContainText('未上架');
  expect(errors).toEqual([]);
});

test('普通库存角色隐藏权限菜单并拒绝直接访问商品页', async ({ page }) => {
  await page.route('**/admin/v1/auth/me', (route) =>
    route.fulfill({ json: { id: 'stock-only', username: '仓管', permissions: ['stock:adjust'] } }),
  );
  await page.goto('/admin/login');
  await page.evaluate(() =>
    sessionStorage.setItem(
      'mini-mall:admin',
      JSON.stringify({ token: 'test', expiresAt: '2099-01-01' }),
    ),
  );
  await page.goto('/admin');
  await expect(page.getByRole('link', { name: '库存管理', exact: true })).toBeVisible();
  await expect(page.getByRole('link', { name: '商品管理', exact: true })).toHaveCount(0);
  await page.goto('/admin/products');
  await expect(page).toHaveURL('/admin/forbidden');
});
