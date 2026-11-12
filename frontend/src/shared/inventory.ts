export interface PendingAdjustment {
  skuId: string;
  delta: number;
  key: string;
}
export function readAdjustment(admin: string): PendingAdjustment | null {
  return JSON.parse(sessionStorage.getItem(`mini-mall:stock-attempt:${admin}`) || 'null');
}
export function saveAdjustment(admin: string, attempt: PendingAdjustment) {
  sessionStorage.setItem(`mini-mall:stock-attempt:${admin}`, JSON.stringify(attempt));
}
export function finishAdjustment(admin: string) {
  sessionStorage.removeItem(`mini-mall:stock-attempt:${admin}`);
}
