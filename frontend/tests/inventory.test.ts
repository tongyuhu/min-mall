import { beforeEach, expect, it, vi } from 'vitest';
import { readAdjustment, saveAdjustment, finishAdjustment } from '../src/shared/inventory';
const data = new Map<string, string>();
vi.stubGlobal('sessionStorage', {
  getItem: (k: string) => data.get(k) ?? null,
  setItem: (k: string, v: string) => data.set(k, v),
  removeItem: (k: string) => data.delete(k),
});
beforeEach(() => data.clear());
it('库存结果不明时按管理员恢复完整原请求，确认成功后才开启新调整', () => {
  const original = { skuId: '9007199254740993', delta: 10, key: 'stable-key' };
  saveAdjustment('admin-1', original);
  expect(readAdjustment('admin-1')).toEqual(original);
  expect(readAdjustment('admin-2')).toBeNull();
  finishAdjustment('admin-1');
  expect(readAdjustment('admin-1')).toBeNull();
});
