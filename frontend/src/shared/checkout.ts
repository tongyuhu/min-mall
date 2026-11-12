import type { Checkout, Quote } from './types';

export interface PendingOrder {
  key: string;
  body: { checkout: Checkout; quoteFingerprint: string; fromCart: boolean };
  quote: Quote;
}

export function pendingOrder(buyer: string): PendingOrder | null {
  return JSON.parse(sessionStorage.getItem(`mini-mall:pending-order:${buyer}`) || 'null');
}

export function savePendingOrder(buyer: string, attempt: PendingOrder) {
  sessionStorage.setItem(`mini-mall:pending-order:${buyer}`, JSON.stringify(attempt));
}

export function completeOrderAttempt(buyer: string) {
  sessionStorage.removeItem(`mini-mall:attempt:${buyer}`);
  sessionStorage.removeItem(`mini-mall:pending-order:${buyer}`);
}

// 同次提交重试使用原键；确认成功后结束本次提交，再买相同商品也生成新键。
export function orderAttempt(buyer: string, fingerprint: string): string {
  const name = `mini-mall:attempt:${buyer}`;
  const previous = JSON.parse(sessionStorage.getItem(name) || 'null');
  if (previous?.fingerprint === fingerprint) return previous.key;
  const key = crypto.randomUUID();
  sessionStorage.setItem(name, JSON.stringify({ fingerprint, key }));
  return key;
}
