<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import { api, ApiError } from '../shared/api';
import {
  readAdjustment,
  saveAdjustment,
  finishAdjustment,
  type PendingAdjustment,
} from '../shared/inventory';
import type { Product } from '../shared/types';
import { useTask } from '../shared/useTask';
import { adminIdentity } from './identity';
import Feedback from '../shared/Feedback.vue';
const route = useRoute(),
  product = ref<Product>(),
  skuId = ref(''),
  delta = ref(1),
  stock = ref<{ available: number; reserved: number }>();
const { busy, error, notice, run } = useTask();
const adminId = adminIdentity.profile.value!.id;
let attempt: PendingAdjustment | undefined;
const uncertain = ref(false);
async function refresh() {
  stock.value = await api('admin', `/stock/${skuId.value}`);
}
function load() {
  stock.value = undefined;
  return run(refresh);
}
onMounted(() =>
  run(async () => {
    const pending = readAdjustment(adminId);
    if (pending) {
      attempt = pending;
      uncertain.value = true;
      skuId.value = pending.skuId;
      delta.value = pending.delta;
      await refresh();
      return;
    }
    if (route.query.productId && adminIdentity.can('catalog:write')) {
      product.value = await api<Product>('admin', `/products/${route.query.productId}`);
      skuId.value = product.value.skus.find((s) => s.enabled)?.id || '';
      if (skuId.value) await refresh();
    }
  }),
);
function adjust() {
  if (!confirm(`确定调整 SKU ${skuId.value} 库存 ${delta.value > 0 ? '+' : ''}${delta.value} 件？`))
    return;
  void run(async () => {
    attempt ??= { skuId: skuId.value, delta: delta.value, key: crypto.randomUUID() };
    saveAdjustment(adminId, attempt);
    uncertain.value = true;
    try {
      await api('admin', `/stock/${attempt.skuId}/adjustments`, {
        method: 'POST',
        headers: { 'Idempotency-Key': attempt.key },
        body: { delta: attempt.delta },
      });
      finishAdjustment(adminId);
      attempt = undefined;
      uncertain.value = false;
      await refresh();
    } catch (e) {
      if (e instanceof ApiError && e.status >= 400 && e.status < 500 && e.status !== 401) {
        finishAdjustment(adminId);
        attempt = undefined;
        uncertain.value = false;
      }
      throw e;
    }
  }, '库存调整成功');
}
</script>
<template>
  <h1>库存管理</h1>
  <p class="admin-title-note">库存调整以 SKU 为单位，负数表示减少可售库存。</p>
  <Feedback :busy="busy" :error="error" :notice="notice" />
  <p v-if="uncertain" class="feedback error">
    上次库存调整结果尚未确认，已恢复原 SKU 和数量。请重试确认；系统沿用原幂等键，不会重复调整。
  </p>
  <section class="panel">
    <h3 v-if="product">{{ product.title }}</h3>
    <form class="admin-toolbar" @submit.prevent="load">
      <label class="field"
        ><span>SKU 编号</span
        ><select
          v-if="product"
          v-model="skuId"
          :disabled="busy || uncertain"
          @change="stock = undefined"
        >
          <option v-for="s in product.skus" :key="s.id" :value="s.id">
            {{ s.id }} · {{ s.specs }}{{ s.enabled ? '' : '（停用）' }}
          </option></select
        ><input
          v-else
          v-model="skuId"
          required
          pattern="[0-9]+"
          :disabled="busy || uncertain"
          @input="stock = undefined" /></label
      ><button class="button small" :disabled="busy">查询库存</button>
    </form>
    <template v-if="stock"
      ><div class="metric-grid">
        <div class="metric-card">
          <span>可售库存</span><strong>{{ stock.available }}</strong>
        </div>
        <div class="metric-card">
          <span>订单预占</span><strong>{{ stock.reserved }}</strong>
        </div>
      </div>
      <form class="admin-toolbar" @submit.prevent="adjust">
        <label class="field"
          ><span>调整数量</span
          ><input
            v-model.number="delta"
            type="number"
            required
            step="1"
            :disabled="busy || uncertain" /></label
        ><button class="button" :disabled="busy || !Number.isInteger(delta) || delta === 0">
          确认调整
        </button>
      </form></template
    >
  </section>
</template>
