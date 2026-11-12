import { ref } from 'vue';
import { ApiError } from './api';
export function useTask() {
  const busy = ref(false);
  const error = ref('');
  const notice = ref('');
  async function run(action: () => Promise<unknown>, success = '') {
    if (busy.value) return;
    busy.value = true;
    error.value = '';
    notice.value = '';
    try {
      await action();
      notice.value = success;
    } catch (e) {
      error.value =
        e instanceof Error
          ? e.message + (e instanceof ApiError && e.traceId ? `（追踪号：${e.traceId}）` : '')
          : '操作失败，请重试';
    } finally {
      busy.value = false;
    }
  }
  return { busy, error, notice, run };
}
