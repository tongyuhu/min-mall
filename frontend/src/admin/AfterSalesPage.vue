<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { api, query } from '../shared/api';
import type { AfterSale, Page } from '../shared/types';
import { dateTime, statusText } from '../shared/format';
import { useTask } from '../shared/useTask';
import Feedback from '../shared/Feedback.vue';
import Pager from './Pager.vue';
const rows = ref<AfterSale[]>([]),
  page = ref(1),
  total = ref(0),
  status = ref('');
const { busy, error, notice, run } = useTask();
async function refresh() {
  const result = await api<Page<AfterSale>>(
    'admin',
    '/after-sales' + query({ page: page.value, status: status.value }),
  );
  rows.value = result.items;
  total.value = result.total;
}
const load = () => run(refresh);
onMounted(load);
function review(row: AfterSale, approved: boolean) {
  const reason = approved ? '审核通过' : prompt('请填写拒绝原因');
  if (
    reason === null ||
    !reason?.trim() ||
    (approved && !confirm('确认同意此售后申请？仅退款申请会开始异步退款。'))
  )
    return;
  void run(async () => {
    await api('admin', `/after-sales/${row.afterSaleNo}/review`, {
      method: 'POST',
      body: { approved, reason },
    });
    await refresh();
  }, '审核结果已保存');
}
function accept(row: AfterSale) {
  if (confirm('确认已收到退货并验收通过？通过后将开始整单退款。'))
    void run(async () => {
      await api('admin', `/after-sales/${row.afterSaleNo}/accept-return`, { method: 'POST' });
      await refresh();
    }, '验收已确认，退款正在处理');
}
</script>
<template>
  <div class="admin-toolbar">
    <h1>售后管理</h1>
    <select
      v-model="status"
      aria-label="售后状态"
      :disabled="busy"
      @change="
        page = 1;
        load();
      "
    >
      <option value="">全部状态</option>
      <option
        v-for="s in [
          'WAIT_REVIEW',
          'WAIT_RETURN',
          'WAIT_INSPECTION',
          'REFUNDING',
          'COMPLETED',
          'REJECTED',
          'WITHDRAWN',
        ]"
        :key="s"
        :value="s"
      >
        {{ statusText(s) }}
      </option></select
    ><button class="button secondary small" :disabled="busy" @click="load">刷新</button>
  </div>
  <Feedback :busy="busy" :error="error" :notice="notice" />
  <section v-for="r in rows" :key="r.id" class="panel">
    <div class="order-top">
      <strong class="mono">{{ r.afterSaleNo }}</strong><span>{{ statusText(r.type) }}</span
      ><span class="badge">{{ statusText(r.status) }}</span>
    </div>
    <p class="inline-note">订单 {{ r.orderNo }} · {{ dateTime(r.createdAt) }}</p>
    <p>申请原因：{{ r.reason }}</p>
    <p v-if="r.returnTrackingNo">退货物流：{{ r.returnCarrier }} / {{ r.returnTrackingNo }}</p>
    <p v-if="r.refundNo" class="inline-note">退款单：{{ r.refundNo }}</p>
    <div class="actions">
      <template v-if="r.status === 'WAIT_REVIEW'"
        ><button class="button small" :disabled="busy" @click="review(r, true)">同意申请</button
        ><button class="button secondary small" :disabled="busy" @click="review(r, false)">
          拒绝申请
        </button></template
      ><button
        v-if="r.status === 'WAIT_INSPECTION'"
        class="button small"
        :disabled="busy"
        @click="accept(r)"
      >
        确认验收
      </button>
    </div>
  </section>
  <div v-if="!rows.length && !busy" class="empty">暂无售后申请</div>
  <Pager
    :page="page"
    :total="total"
    :busy="busy"
    @change="
      page = $event;
      load();
    "
  />
</template>
