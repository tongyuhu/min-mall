export const money = (fen: number | null | undefined) =>
  new Intl.NumberFormat('zh-CN', { style: 'currency', currency: 'CNY' }).format((fen ?? 0) / 100);
export function toFen(value: string | number): number {
  const text = String(value).trim();
  if (!/^\d+(\.\d{1,2})?$/.test(text)) throw new Error('金额须为非负数，最多两位小数');
  const [whole, decimal = ''] = text.split('.');
  const fen = Number(whole) * 100 + Number(decimal.padEnd(2, '0'));
  if (!Number.isSafeInteger(fen)) throw new Error('金额超出允许范围');
  return fen;
}
export const dateTime = (value?: string | null) =>
  value
    ? new Intl.DateTimeFormat('zh-CN', {
        timeZone: 'Asia/Shanghai',
        dateStyle: 'medium',
        timeStyle: 'short',
        hour12: false,
      }).format(new Date(value))
    : '—';
export const today = () =>
  new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Shanghai' }).format(new Date());
export const labels: Record<string, string> = {
  PENDING_PAYMENT: '待付款',
  PAID: '待发货',
  SHIPPED: '待收货',
  COMPLETED: '已完成',
  CLOSED: '已关闭',
  REFUNDED: '已退款',
  PENDING: '处理中',
  SUCCEEDED: '已成功',
  FAILED: '未成功',
  UNKNOWN: '结果确认中',
  AVAILABLE: '可使用',
  RESERVED: '订单占用',
  USED: '已使用',
  EXPIRED: '已过期',
  WAIT_REVIEW: '待审核',
  WAIT_RETURN: '待寄回',
  WAIT_INSPECTION: '待验收',
  REFUNDING: '退款中',
  REJECTED: '已拒绝',
  WITHDRAWN: '已撤销',
  REFUND_ONLY: '仅退款',
  RETURN_REFUND: '退货退款',
  CREATING: '创建中',
  ACCEPTED: '已受理',
  CREATED: '已揽收',
  IN_TRANSIT: '运输中',
  DELIVERED: '已送达',
  MANUAL: '待人工处理',
  READY: '等待执行',
  RUNNING: '执行中',
  WAITING: '等待结果',
  DONE: '已完成',
};
export const statusText = (value: string) => labels[value] ?? value;
