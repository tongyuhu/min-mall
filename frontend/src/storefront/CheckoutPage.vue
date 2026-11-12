<script setup lang="ts">
import { onMounted, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { api, ApiError } from '../shared/api';
import type { AddressView, Checkout, Coupon, Member, Order, Quote } from '../shared/types';
import { money } from '../shared/format';
import {
  orderAttempt,
  completeOrderAttempt,
  pendingOrder,
  savePendingOrder,
  type PendingOrder,
} from '../shared/checkout';
import { useTask } from '../shared/useTask';
import Feedback from '../shared/Feedback.vue';
const router = useRouter(),
  addresses = ref<AddressView[]>([]),
  coupons = ref<Coupon[]>([]),
  addressId = ref(''),
  couponId = ref('');
const lines = ref<Checkout['lines']>([]),
  quote = ref<Quote>(),
  buyerId = ref(''),
  uncertain = ref(false);
const { busy, error, run } = useTask();
let submitted: PendingOrder | undefined;
const command = (): Checkout => ({
  addressId: addressId.value,
  couponId: couponId.value || null,
  lines: lines.value,
});
watch(
  [addressId, couponId],
  () => {
    if (!uncertain.value) {
      quote.value = undefined;
      submitted = undefined;
    }
  },
  { flush: 'sync' },
);
onMounted(() =>
  run(async () => {
    try {
      lines.value = JSON.parse(sessionStorage.getItem('mini-mall:checkout') || '[]');
    } catch {
      lines.value = [];
    }
    const [a, c, m] = await Promise.all([
      api<AddressView[]>('buyer', '/addresses'),
      api<Coupon[]>('buyer', '/coupons/mine'),
      api<Member>('buyer', '/me'),
    ]);
    addresses.value = a;
    coupons.value = c.filter((c) => c.status === 'AVAILABLE');
    buyerId.value = m.id;
    const pending = pendingOrder(m.id);
    if (pending) {
      uncertain.value = true;
      submitted = pending;
      addressId.value = pending.body.checkout.addressId;
      couponId.value = pending.body.checkout.couponId || '';
      lines.value = pending.body.checkout.lines;
      quote.value = pending.quote;
    } else addressId.value = (a.find((a) => a.address.defaultAddress) ?? a[0])?.id ?? '';
  }),
);
function preview() {
  void run(async () => {
    quote.value = await api<Quote>('buyer', '/checkout', { method: 'POST', body: command() });
    submitted = undefined;
  });
}
function place() {
  void run(async () => {
    if (!quote.value) return;
    submitted ??= {
      body: { checkout: command(), quoteFingerprint: quote.value.fingerprint, fromCart: true },
      key: orderAttempt(buyerId.value, quote.value.fingerprint),
      quote: quote.value,
    };
    savePendingOrder(buyerId.value, submitted);
    uncertain.value = true;
    try {
      const order = await api<Order>('buyer', '/orders', {
        method: 'POST',
        headers: { 'Idempotency-Key': submitted.key },
        body: submitted.body,
      });
      uncertain.value = false;
      completeOrderAttempt(buyerId.value);
      sessionStorage.removeItem('mini-mall:checkout');
      await router.replace(`/orders/${order.orderNo}`);
    } catch (e) {
      if (e instanceof ApiError && e.status >= 400 && e.status < 500 && e.status !== 401) {
        completeOrderAttempt(buyerId.value);
        quote.value = undefined;
        submitted = undefined;
        uncertain.value = false;
      } else if (e instanceof ApiError && (e.status === 0 || e.status >= 500))
        uncertain.value = true;
      throw e;
    }
  });
}
</script>
<template>
  <div class="container page">
    <span class="eyebrow">确认好物 · 安心下单</span>
    <h1>确认订单</h1>
    <Feedback :busy="busy" :error="error" />
    <div v-if="!lines.length" class="empty">
      <h3>还没有选择结算商品</h3>
      <RouterLink to="/cart" class="button">返回购物车</RouterLink>
    </div>
    <div v-else class="two-columns">
      <div>
        <section class="panel">
          <h3>收货信息</h3>
          <div v-if="!addresses.length" class="inline-note">
            请先添加收货地址。<RouterLink to="/account/addresses">去添加 →</RouterLink>
          </div>
          <label v-for="a in addresses" :key="a.id" class="address-card check-label"
            ><input
              v-model="addressId"
              type="radio"
              :value="a.id"
              :disabled="busy || uncertain"
              name="address"
            /><span
              ><strong>{{ a.address.receiver }} · {{ a.address.phone }}</strong
              ><br />{{ a.address.province }} {{ a.address.city }} {{ a.address.district }}
              {{ a.address.detail }}</span
            ></label
          ><RouterLink class="text-button" to="/account/addresses">管理收货地址</RouterLink>
        </section>
        <section class="panel">
          <h3>优惠券</h3>
          <select v-model="couponId" aria-label="使用优惠券" :disabled="busy || uncertain">
            <option value="">不使用优惠券</option>
            <option v-for="c in coupons" :key="c.id" :value="c.id">
              {{ c.name }} · 满{{ money(c.thresholdFen) }}减{{ money(c.discountFen) }}
            </option>
          </select>
        </section>
        <section v-if="quote" class="panel">
          <h3>商品清单</h3>
          <div v-for="s in quote.skus" :key="s.skuId" class="list-row">
            <div class="grow">
              <h3>{{ s.title }}</h3>
              <p>{{ s.specs }} × {{ lines.find((l) => l.skuId === s.skuId)?.quantity }}</p>
            </div>
            <strong>{{ money(s.priceFen) }}</strong>
          </div>
        </section>
      </div>
      <aside class="panel summary">
        <h3>金额明细</h3>
        <template v-if="quote"
          ><div class="price-row"><span>商品金额</span>{{ money(quote.goodsFen) }}</div>
          <div class="price-row"><span>优惠</span>−{{ money(quote.discountFen) }}</div>
          <div class="price-row"><span>运费</span>{{ money(quote.freightFen) }}</div>
          <div class="price-row total">
            <span>应付</span><strong>{{ money(quote.payableFen) }}</strong>
          </div></template
        >
        <p v-else class="inline-note">先确认地址和优惠券，再计算最终应付金额。</p>
        <p v-if="uncertain" class="feedback error">
          提交结果尚未确认。请保持当前选择，重试会沿用同一订单请求，也可先查看我的订单。
        </p>
        <div class="actions">
          <button
            v-if="!quote"
            class="button wide-button"
            :disabled="!addressId || busy"
            @click="preview"
          >
            确认金额</button
          ><button v-else class="button wide-button" :disabled="busy" @click="place">
            {{ uncertain ? '重试提交订单' : '提交订单' }}
          </button>
        </div>
        <p class="inline-note">金额或库存变化时需要重新确认。下单后请在 30 分钟内完成支付。</p>
      </aside>
    </div>
  </div>
</template>
