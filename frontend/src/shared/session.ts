export type Scope = 'buyer' | 'admin';
export interface Session {
  token: string;
  expiresAt: string;
}
const key = (scope: Scope) => `mini-mall:${scope}`;
export const session = {
  get(scope: Scope): Session | null {
    try {
      const value = JSON.parse(sessionStorage.getItem(key(scope)) || 'null') as Session | null;
      if (!value?.token || Date.parse(value.expiresAt) <= Date.now()) return null;
      return value;
    } catch {
      return null;
    }
  },
  save(scope: Scope, value: Session) {
    sessionStorage.setItem(key(scope), JSON.stringify(value));
  },
  clear(scope: Scope) {
    sessionStorage.removeItem(key(scope));
  },
};
