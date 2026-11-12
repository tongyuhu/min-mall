import { session, type Scope } from './session';

export class ApiError extends Error {
  constructor(
    public status: number,
    message: string,
    public traceId = '',
    public code = '',
  ) {
    super(message);
  }
}
type Options = Omit<RequestInit, 'body'> & { body?: unknown };
export async function api<T = void>(scope: Scope, path: string, options: Options = {}): Promise<T> {
  const headers = new Headers(options.headers);
  const auth = session.get(scope);
  if (auth) headers.set('Authorization', `Bearer ${auth.token}`);
  if (options.body !== undefined) headers.set('Content-Type', 'application/json');
  let response: Response;
  try {
    response = await fetch(`${scope === 'admin' ? '/admin/v1' : '/api/v1'}${path}`, {
      ...options,
      headers,
      body: options.body === undefined ? undefined : JSON.stringify(options.body),
      signal: options.signal ?? AbortSignal.timeout(20000),
    });
  } catch (error) {
    if (options.signal?.aborted) throw error;
    throw new ApiError(0, '网络连接失败，请检查服务后重试；已提交的交易请先查看订单状态。');
  }
  const text = await response.text();
  let data: any;
  try {
    data = text ? JSON.parse(text) : undefined;
  } catch {
    throw new ApiError(response.status, '服务响应无法解析，请稍后重试');
  }
  if (!response.ok) {
    if (response.status === 401) {
      session.clear(scope);
      if (typeof window !== 'undefined')
        window.dispatchEvent(new CustomEvent('session-expired', { detail: scope }));
    }
    throw new ApiError(
      response.status,
      data?.message || `请求失败（${response.status}）`,
      data?.traceId,
      data?.code,
    );
  }
  return data as T;
}
export function query(values: Record<string, unknown>): string {
  const params = new URLSearchParams();
  Object.entries(values).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') params.set(key, String(value));
  });
  return `?${params}`;
}
