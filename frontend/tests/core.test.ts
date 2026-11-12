import { beforeEach, describe, expect, it, vi } from 'vitest';
import { api, ApiError } from '../src/shared/api';
import { money, toFen } from '../src/shared/format';
import { session } from '../src/shared/session';
import {
  orderAttempt,
  completeOrderAttempt,
  savePendingOrder,
  pendingOrder,
} from '../src/shared/checkout';

const values = new Map<string, string>();
vi.stubGlobal('sessionStorage', {
  getItem: (key: string) => values.get(key) ?? null,
  setItem: (key: string, value: string) => values.set(key, value),
  removeItem: (key: string) => values.delete(key),
});
beforeEach(() => {
  values.clear();
  vi.restoreAllMocks();
});
describe('请求契约', () => {
  it('直接读取 DTO，接受空响应和异步受理', async () => {
    vi.stubGlobal(
      'fetch',
      vi
        .fn()
        .mockResolvedValueOnce(new Response('{"id":"9007199254740993"}'))
        .mockResolvedValueOnce(new Response(null, { status: 204 }))
        .mockResolvedValueOnce(new Response('{"status":"PENDING"}', { status: 202 })),
    );
    expect(await api('buyer', '/products')).toEqual({ id: '9007199254740993' });
    expect(await api('buyer', '/logout', { method: 'POST' })).toBeUndefined();
    expect(await api('buyer', '/payments', { method: 'POST' })).toEqual({ status: 'PENDING' });
  });
  it('401 只清理发生过期的身份，并保留可诊断错误', async () => {
    session.save('buyer', { token: 'buyer-token', expiresAt: '2099-01-01' });
    session.save('admin', { token: 'admin-token', expiresAt: '2099-01-01' });
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response('{"message":"请重新登录","traceId":"trace-1","code":"UNAUTHENTICATED"}', {
          status: 401,
        }),
      ),
    );
    await expect(api('buyer', '/me')).rejects.toMatchObject({ status: 401, traceId: 'trace-1' });
    expect(session.get('buyer')).toBeNull();
    expect(session.get('admin')?.token).toBe('admin-token');
  });
  it('403 不注销，发送所属身份的 Bearer', async () => {
    session.save('admin', { token: 'admin-token', expiresAt: '2099-01-01' });
    const fetcher = vi
      .fn()
      .mockResolvedValue(new Response('{"message":"没有权限"}', { status: 403 }));
    vi.stubGlobal('fetch', fetcher);
    await expect(api('admin', '/roles')).rejects.toBeInstanceOf(ApiError);
    expect(fetcher.mock.calls[0]![1].headers.get('Authorization')).toBe('Bearer admin-token');
    expect(session.get('admin')).not.toBeNull();
  });
});
it('整数分与人民币展示，拒绝精度或负数输入', () => {
  expect(toFen('19.90')).toBe(1990);
  expect(toFen('0.01')).toBe(1);
  expect(() => toFen('0.001')).toThrow();
  expect(() => toFen('-1')).toThrow();
  expect(money(1990)).toContain('19.90');
});
it('同一结算重试保留幂等键，新的结算才换键', () => {
  const first = orderAttempt('buyer-1', 'fingerprint-1');
  expect(orderAttempt('buyer-1', 'fingerprint-1')).toBe(first);
  expect(orderAttempt('buyer-1', 'fingerprint-2')).not.toBe(first);
  expect(orderAttempt('buyer-2', 'fingerprint-1')).not.toBe(first);
});

it('已成功订单结束后，相同商品的再次购买使用新键', () => {
  const first = orderAttempt('buyer-1', 'same-fingerprint');
  completeOrderAttempt('buyer-1');
  expect(orderAttempt('buyer-1', 'same-fingerprint')).not.toBe(first);
});

it('未知结果完整持久化，刷新后保留原地址优惠券和报价，身份隔离', () => {
  const attempt = {
    key: orderAttempt('buyer-1', 'quoted'),
    body: {
      checkout: {
        addressId: 'non-default',
        couponId: 'coupon-1',
        lines: [{ skuId: 'sku-1', quantity: 2 }],
      },
      quoteFingerprint: 'quoted',
      fromCart: true,
    },
    quote: {
      skus: [],
      goodsFen: 3980,
      discountFen: 500,
      freightFen: 0,
      payableFen: 3480,
      fingerprint: 'quoted',
    },
  };
  savePendingOrder('buyer-1', attempt);
  expect(pendingOrder('buyer-1')).toEqual(attempt);
  expect(pendingOrder('buyer-2')).toBeNull();
  completeOrderAttempt('buyer-1');
  expect(pendingOrder('buyer-1')).toBeNull();
});
