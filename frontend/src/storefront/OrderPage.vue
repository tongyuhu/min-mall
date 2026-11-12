<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import { api } from '../shared/api';
import type { Order, Payment, Track, Refund } from '../shared/types';
import { dateTime, money, statusText } from '../shared/format';
import { useTask } from '../shared/useTask';
import Feedback from '../shared/Feedback.vue';
const no = String(useRoute().params.no);
const order = ref<Order>(),
  payment = ref<Payment>(),
  tracks = ref<Track[]>([]),
  refunds = ref<Refund[]>([]),
  reason = ref('');
const { busy, error, notice, run } = useTask();
let alive = true,
  timer: ReturnType<typeof setTimeout> | undefined;
async function refresh() {
  const current = await api<Order>('buyer', `/orders/${no}`);
  if (!alive) return;
  order.value = current;
  refunds.value = await api<Refund[]>('buyer', `/orders/${no}/refunds`);
  if (['SHIPPED', 'COMPLETED', 'REFUNDED'].includes(current.status))
    tracks.value = await api<Track[]>('buyer', `/orders/${no}/tracks`);
  if (payment.value) {
    payment.value = await api<Payment>('buyer', `/payments/${payment.value.paymentNo}`);
    if (payment.value.status === 'SUCCEEDED') notice.value = '支付成功，订单已更新';
  }
}
async function poll() {
  try {
    if (!busy.value) await refresh();
  } catch (e) {
    if (alive) error.value = e instanceof Error ? e.message : '状态更新失败，请手动刷新';
  }
  if (
    alive &&
    (order.value?.status === 'PENDING_PAYMENT' ||
      order.value?.closeRequested ||
      refunds.value.some((r) => !['SUCCEEDED', 'FAILED'].includes(r.status)))
  )
    timer = setTimeout(poll, 3000);
}
function schedule() {
  clearTimeout(timer);
  if (alive) timer = setTimeout(poll, 2000);
}
function load() {
  return run(async () => {
    await refresh();
    schedule();
  });
}
onMounted(load);
onUnmounted(() => {
  alive = false;
  clearTimeout(timer);
});
function pay() {
  void run(async () => {
    payment.value = await api<Payment>('buyer', '/payments', {
      method: 'POST',
      body: { orderNo: no },
    });
    schedule();
  });
}
function simulate(result: string) {
  void run(async () => {
    await api('buyer', `/simulation/payments/${payment.value!.paymentNo}/${result}`, {
      method: 'POST',
    });
    await refresh();
    schedule();
  }, '模拟请求已受理，正在确认实际支付结果…');
}
function cancel() {
  if (confirm('确定取消这个订单？支付结果确认后会更新订单状态。'))
    void run(async () => {
      await api('buyer', `/orders/${no}/cancel`, { method: 'POST' });
      await refresh();
      schedule();
    }, '取消申请已受理，请等待状态更新');
}
function receive() {
  if (confirm('确认已经收到全部商品？'))
    void run(async () => {
      await api('buyer', `/orders/${no}/receipt`, { method: 'POST' });
      await refresh();
    }, '已确认收货');
}
function afterSale() {
  void run(async () => {
    await api('buyer', '/after-sales', {
      method: 'POST',
      body: { orderNo: no, reason: reason.value },
    });
    await refresh();
  }, '售后申请已提交，可前往售后记录查看进度');
}
</script>
<template>
  <div class="container page">
    <div class="breadcrumbs"><RouterLink to="/orders">我的订单</RouterLink> / 订单详情</div>
    <div class="section-heading">
      <h1>订单详情</h1>
      <button class="button secondary small" :disabled="busy" @click="load">刷新状态</button>
    </div>
    <Feedback :busy="busy" :error="error" :notice="notice" /><template v-if="order"
      ><section class="panel">
        <div class="order-top">
          <span class="order-number">{{ no }}</span><span class="badge">{{
            statusText(order.status)
          }}</span>
        </div>
        <p class="inline-note">
          下单时间 {{ dateTime(order.createdAt)
          }}<template v-if="order.status === 'PENDING_PAYMENT'">
            · 支付截止 {{ dateTime(order.expiresAt) }}</template
          >
        </p>
        <div class="actions">
          <button
            v-if="order.status === 'PENDING_PAYMENT' && !order.closeRequested"
            class="button"
            :disabled="busy"
            @click="pay"
          >
            去付款</button
          ><button
            v-if="order.status === 'PENDING_PAYMENT' && !order.closeRequested"
            class="button secondary"
            :disabled="busy"
            @click="cancel"
          >
            取消订单</button
          ><span
            v-if="order.closeRequested && order.status === 'PENDING_PAYMENT'"
            class="inline-note"
            >取消处理中，正在确认支付结果…</span
          ><button
            v-if="order.status === 'SHIPPED' && !order.activeAfterSaleNo"
            class="button"
            :disabled="busy"
            @click="receive"
          >
            确认收货</button
          ><RouterLink v-if="order.activeAfterSaleNo" to="/after-sales" class="button secondary"
            >查看售后进度</RouterLink
          >
        </div>
      </section>
      <section
        v-if="payment && order.status === 'PENDING_PAYMENT' && !order.closeRequested"
        class="panel"
      >
        <h3>本地模拟收银台</h3>
        <p>
          支付金额 <strong>{{ money(payment.amountFen) }}</strong> ·
          {{ statusText(payment.status) }}
        </p>
        <p class="inline-note">
          此操作不产生真实扣款。模拟渠道发送异步通知，实际支付结果以后台确认状态为准。
        </p>
        <div v-if="payment.payParameters.mode === 'LOCAL_SIMULATION'" class="actions">
          <button
            class="button"
            :disabled="busy || payment.status === 'SUCCEEDED' || payment.status === 'CLOSED'"
            @click="simulate('success')"
          >
            模拟支付成功</button
          ><button
            class="button secondary"
            :disabled="busy || payment.status === 'SUCCEEDED' || payment.status === 'CLOSED'"
            @click="simulate('failure')"
          >
            模拟支付失败
          </button>
        </div>
        <p v-else class="inline-note">支付渠道准备中，请稍候；页面将自动刷新。</p>
      </section>
      <div class="two-columns">
        <div>
          <section class="panel">
            <h3>商品清单</h3>
            <div v-for="item in order.items" :key="item.skuId" class="list-row">
              <div class="grow">
                <h3>{{ item.title }}</h3>
                <p>{{ item.specs }} × {{ item.quantity }}</p>
              </div>
              <strong>{{ money(item.payableFen) }}</strong>
            </div>
          </section>
          <section class="panel">
            <h3>收货信息</h3>
            <p>{{ order.address.receiver }} · {{ order.address.phone }}</p>
            <p class="inline-note">
              {{ order.address.province }} {{ order.address.city }} {{ order.address.district }}
              {{ order.address.detail }}
            </p>
          </section>
          <section v-if="['SHIPPED', 'COMPLETED', 'REFUNDED'].includes(order.status)" class="panel">
            <h3>物流轨迹</h3>
            <ul class="timeline">
              <li v-for="t in tracks" :key="t.eventId">
                <strong>{{ statusText(t.status) }}</strong> · {{ t.description
                }}<small>{{ dateTime(t.eventTime) }}</small>
              </li>
            </ul>
            <p v-if="!tracks.length" class="inline-note">暂无轨迹，物流受理后会逐步更新。</p>
          </section>
          <section v-if="refunds.length" class="panel">
            <h3>退款记录</h3>
            <div v-for="r in refunds" :key="r.refundNo" class="list-row">
              <div class="grow">
                <p class="mono">{{ r.refundNo }}</p>
                <span class="badge">{{ statusText(r.status) }}</span>
                <p v-if="r.refundedAt">{{ dateTime(r.refundedAt) }}</p>
              </div>
              <strong>{{ money(r.amountFen) }}</strong>
            </div>
          </section>
        </div>
        <aside>
          <section class="panel">
            <h3>订单金额</h3>
            <div class="price-row"><span>商品金额</span>{{ money(order.goodsFen) }}</div>
            <div class="price-row"><span>优惠金额</span>−{{ money(order.discountFen) }}</div>
            <div class="price-row"><span>运费</span>{{ money(order.freightFen) }}</div>
            <div class="price-row total">
              <span>应付金额</span><strong>{{ money(order.payableFen) }}</strong>
            </div>
          </section>
          <form
            v-if="
              ['PAID', 'SHIPPED', 'COMPLETED'].includes(order.status) && !order.activeAfterSaleNo
            "
            class="panel form-stack"
            @submit.prevent="afterSale"
          >
            <h3>售后服务</h3>
            <p class="inline-note">支持整单退款；已发货需寄回商品。收货后 7 天内可以申请。</p>
            <label class="field"
              ><span>售后原因</span
              ><textarea
                v-model="reason"
                required
                maxlength="500"
                placeholder="请描述你的问题"
              /></label
            ><button class="button secondary" :disabled="busy">申请售后</button>
          </form>
        </aside>
      </div>
    </template>
  </div>
</template>
