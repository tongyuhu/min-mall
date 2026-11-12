<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { api } from '../shared/api';
import type { Coupon } from '../shared/types';
import { dateTime, money, statusText } from '../shared/format';
import { useTask } from '../shared/useTask';
import Feedback from '../shared/Feedback.vue';
const templates = ref<Coupon[]>([]),
  mine = ref<Coupon[]>([]);
const { busy, error, notice, run } = useTask();
async function refresh() {
  [templates.value, mine.value] = await Promise.all([
    api<Coupon[]>('buyer', '/coupons'),
    api<Coupon[]>('buyer', '/coupons/mine'),
  ]);
}
const load = () => run(refresh);
onMounted(load);
function claim(id: string) {
  void run(async () => {
    await api('buyer', `/coupons/${id}/claim`, { method: 'POST' });
    await refresh();
  }, '优惠券已放入你的账户');
}
</script>
<template>
  <div class="container page">
    <span class="eyebrow">一点心意 · 一份优惠</span>
    <h1>领券中心</h1>
    <Feedback :busy="busy" :error="error" :notice="notice" /><button
      v-if="error"
      class="button secondary"
      @click="load"
    >
      重新加载
    </button>
    <div class="coupon-grid">
      <article v-for="c in templates" :key="c.id" class="coupon-card">
        <strong class="discount">{{ money(c.discountFen) }}</strong>
        <h3>{{ c.name }}</h3>
        <p>满 {{ money(c.thresholdFen) }} 可用 · 剩余 {{ c.remaining }} 张</p>
        <p>{{ dateTime(c.validFrom) }} 至 {{ dateTime(c.validUntil) }}</p>
        <button
          class="button small"
          :disabled="busy || !c.remaining || mine.some((m) => m.templateId === c.id)"
          @click="claim(c.id)"
        >
          {{ mine.some((m) => m.templateId === c.id) ? '已领取' : '立即领取' }}
        </button>
      </article>
    </div>
    <div v-if="!templates.length && !busy" class="empty">暂无可领取的优惠券</div>
    <h2 style="margin-top: 40px">我的优惠券</h2>
    <div class="coupon-grid">
      <article v-for="c in mine" :key="c.id" class="coupon-card">
        <span class="badge">{{ statusText(c.status!) }}</span>
        <h3>{{ c.name }}</h3>
        <strong class="discount">{{ money(c.discountFen) }}</strong>
        <p>满 {{ money(c.thresholdFen) }} 可用</p>
        <p>有效至 {{ dateTime(c.validUntil) }}</p>
      </article>
    </div>
    <div v-if="!mine.length" class="empty">还没有优惠券</div>
  </div>
</template>
