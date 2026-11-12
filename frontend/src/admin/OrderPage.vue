<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import { api } from '../shared/api';
import type { Order, Shipment } from '../shared/types';
import { dateTime, money, statusText } from '../shared/format';
import { adminIdentity } from './identity';
import { useTask } from '../shared/useTask';
import Feedback from '../shared/Feedback.vue';
const no = String(useRoute().params.no),
  order = ref<Order>(),
  shipment = ref<Shipment | null>(null),
  carrier = ref(''),
  trackingNo = ref('');
const { busy, error, notice, run } = useTask();
async function refresh() {
  order.value = await api<Order>('admin', `/orders/${no}`);
  if (adminIdentity.can('shipment:write'))
    shipment.value = await api<Shipment | null>('admin', `/orders/${no}/shipment`);
}
const load = () => run(refresh);
onMounted(load);
function ship() {
  void run(async () => {
    await api('admin', `/orders/${no}/shipment`, {
      method: 'POST',
      body: { carrier: carrier.value, trackingNo: trackingNo.value },
    });
    await refresh();
  }, '发货已受理');
}
function sync() {
  void run(
    () => api('admin', `/shipments/${shipment.value!.shipmentNo}/sync`, { method: 'POST' }),
    '物流同步已受理，稍后刷新查看',
  );
}
function delivered() {
  if (confirm('在本地模拟渠道中将此包裹标记为送达？'))
    void run(
      () =>
        api('admin', `/simulation/shipments/${shipment.value!.shipmentNo}/delivered`, {
          method: 'POST',
        }),
      '模拟送达已提交，请稍后刷新',
    );
}
</script>
<template>
  <div class="admin-toolbar">
    <h1>订单详情</h1>
    <RouterLink to="/admin/orders">返回列表</RouterLink
    ><button class="button secondary small" :disabled="busy" @click="load">刷新</button>
  </div>
  <Feedback :busy="busy" :error="error" :notice="notice" /><template v-if="order"
    ><section class="panel">
      <div class="order-top">
        <strong class="mono">{{ no }}</strong><span class="badge">{{
          statusText(order.status)
        }}</span>
      </div>
      <p class="inline-note">
        下单 {{ dateTime(order.createdAt) }} · 付款 {{ dateTime(order.paidAt) }}
      </p>
      <el-table :data="order.items"
        ><el-table-column prop="title" label="商品" min-width="160" /><el-table-column
          prop="specs"
          label="规格"
        /><el-table-column prop="quantity" label="数量" width="80" /><el-table-column label="金额"
          ><template #default="{ row }">{{ money(row.payableFen) }}</template></el-table-column
        ></el-table
      >
      <div class="order-footer">
        商品 {{ money(order.goodsFen) }} · 优惠 {{ money(order.discountFen) }} · 运费
        {{ money(order.freightFen) }} <strong>应付 {{ money(order.payableFen) }}</strong>
      </div>
    </section>
    <div class="split-panels">
      <section class="panel">
        <h3>收货信息</h3>
        <p>{{ order.address.receiver }} · {{ order.address.phone }}</p>
        <p class="inline-note">
          {{ order.address.province }} {{ order.address.city }} {{ order.address.district }}
          {{ order.address.detail }}
        </p>
        <p v-if="order.activeAfterSaleNo" class="feedback error">
          此订单正在售后处理：{{ order.activeAfterSaleNo }}
        </p>
      </section>
      <section v-if="adminIdentity.can('shipment:write')" class="panel">
        <h3>发货与物流</h3>
        <form
          v-if="order.status === 'PAID' && !order.activeAfterSaleNo && !shipment"
          class="form-stack"
          @submit.prevent="ship"
        >
          <label class="field"
            ><span>承运商</span><input v-model="carrier" required maxlength="64" /></label
          ><label class="field"
            ><span>运单号</span><input v-model="trackingNo" required maxlength="100" /></label
          ><button class="button" :disabled="busy">确认发货</button>
        </form>
        <template v-else-if="shipment"
          ><p>{{ shipment.carrier }} · {{ shipment.trackingNo }}</p>
          <p>物流状态：{{ statusText(shipment.status) }}</p>
          <div class="actions">
            <button class="button small" :disabled="busy" @click="sync">同步物流</button
            ><button
              class="button secondary small"
              :disabled="busy || shipment.status === 'CREATING'"
              @click="delivered"
            >
              本地模拟送达
            </button>
          </div>
          <p class="inline-note">模拟功能仅在本地或测试环境可用。</p></template
        >
        <p v-else class="inline-note">当前订单状态不允许发货。</p>
      </section>
    </div></template
  >
</template>
