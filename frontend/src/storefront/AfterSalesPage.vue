<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { api } from '../shared/api';
import type { AfterSale } from '../shared/types';
import { dateTime, statusText } from '../shared/format';
import { useTask } from '../shared/useTask';
import Feedback from '../shared/Feedback.vue';
const rows = ref<AfterSale[]>([]),
  forms = reactive<Record<string, { carrier: string; trackingNo: string }>>({});
const { busy, error, notice, run } = useTask();
async function refresh() {
  rows.value = await api<AfterSale[]>('buyer', '/after-sales');
  rows.value.forEach((r) => {
    forms[r.afterSaleNo] ??= { carrier: '', trackingNo: '' };
  });
}
const load = () => run(refresh);
onMounted(load);
function returned(no: string) {
  void run(async () => {
    await api('buyer', `/after-sales/${no}/return`, { method: 'POST', body: forms[no] });
    await refresh();
  }, '退货物流已提交');
}
function withdraw(no: string) {
  if (confirm('确定撤销售后申请？'))
    void run(async () => {
      await api('buyer', `/after-sales/${no}/withdraw`, { method: 'POST' });
      await refresh();
    }, '售后申请已撤销');
}
</script>
<template>
  <div class="container page">
    <div class="section-heading">
      <div>
        <span class="eyebrow">售后服务 · 用心相伴</span>
        <h1>售后记录</h1>
      </div>
      <button class="button secondary small" :disabled="busy" @click="load">刷新进度</button>
    </div>
    <Feedback :busy="busy" :error="error" :notice="notice" />
    <div v-if="!rows.length && !busy" class="empty">暂无售后申请，可在订单详情中申请。</div>
    <article v-for="r in rows" :key="r.afterSaleNo" class="panel">
      <div class="order-top">
        <span class="mono">{{ r.afterSaleNo }}</span><span>{{ statusText(r.type) }}</span
        ><span class="badge">{{ statusText(r.status) }}</span>
      </div>
      <p class="inline-note">
        订单 <RouterLink :to="`/orders/${r.orderNo}`">{{ r.orderNo }} →</RouterLink><br />申请时间
        {{ dateTime(r.createdAt) }}
      </p>
      <p>{{ r.reason }}</p>
      <p v-if="r.returnTrackingNo" class="inline-note">
        退货物流：{{ r.returnCarrier }} / {{ r.returnTrackingNo }}
      </p>
      <form
        v-if="r.status === 'WAIT_RETURN'"
        class="form-grid"
        @submit.prevent="returned(r.afterSaleNo)"
      >
        <label class="field"
          ><span>退货承运商</span
          ><input v-model="forms[r.afterSaleNo]!.carrier" required maxlength="64" /></label
        ><label class="field"
          ><span>退货运单号</span
          ><input v-model="forms[r.afterSaleNo]!.trackingNo" required maxlength="100" /></label
        ><button class="button" :disabled="busy">提交退货物流</button>
      </form>
      <div class="actions">
        <button
          v-if="['WAIT_REVIEW', 'WAIT_RETURN', 'WAIT_INSPECTION'].includes(r.status)"
          class="button secondary small"
          :disabled="busy"
          @click="withdraw(r.afterSaleNo)"
        >
          撤销申请</button
        ><RouterLink v-if="r.refundNo" :to="`/orders/${r.orderNo}`" class="button secondary small"
          >查看退款记录</RouterLink
        >
      </div>
    </article>
  </div>
</template>
