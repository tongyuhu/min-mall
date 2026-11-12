<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { api, query } from '../shared/api';
import type { Order, Page } from '../shared/types';
import { dateTime, money, statusText } from '../shared/format';
import { useTask } from '../shared/useTask';
import Feedback from '../shared/Feedback.vue';
const orders = ref<Order[]>([]),
  page = ref(1),
  total = ref(0);
const { busy, error, run } = useTask();
function load() {
  return run(async () => {
    const data = await api<Page<Order>>('buyer', '/orders' + query({ page: page.value, size: 10 }));
    orders.value = data.items;
    total.value = data.total;
  });
}
onMounted(load);
</script>
<template>
  <div class="container page">
    <div class="section-heading">
      <div>
        <span class="eyebrow">每份订单 · 用心照顾</span>
        <h1>我的订单</h1>
      </div>
      <button class="button secondary small" :disabled="busy" @click="load">刷新订单</button>
    </div>
    <Feedback :busy="busy" :error="error" />
    <div v-if="!orders.length && !busy" class="empty">
      <h3>还没有订单</h3>
      <RouterLink to="/products" class="button">发现好物</RouterLink>
    </div>
    <article v-for="o in orders" :key="o.orderNo" class="panel">
      <div class="order-top">
        <span class="order-number">{{ o.orderNo }}</span><span class="muted">{{
          dateTime(o.createdAt)
        }}</span
        ><span class="badge">{{ statusText(o.status) }}</span>
      </div>
      <div v-for="i in o.items" :key="i.skuId" class="list-row">
        <div class="grow">
          <h3>{{ i.title }}</h3>
          <p>{{ i.specs }} × {{ i.quantity }}</p>
        </div>
        <strong>{{ money(i.payableFen) }}</strong>
      </div>
      <div class="order-footer">
        <span
          >实付 <strong>{{ money(o.payableFen) }}</strong></span
        ><RouterLink :to="`/orders/${o.orderNo}`" class="button small">{{
          o.status === 'PENDING_PAYMENT' ? '查看并付款' : '查看详情'
        }}</RouterLink>
      </div>
    </article>
    <div v-if="total > 10" class="pagination">
      <button
        :disabled="page <= 1 || busy"
        @click="
          page--;
          load();
        "
      >
        上一页</button
      ><span>{{ page }} / {{ Math.ceil(total / 10) }}</span
      ><button
        :disabled="page * 10 >= total || busy"
        @click="
          page++;
          load();
        "
      >
        下一页
      </button>
    </div>
  </div>
</template>
