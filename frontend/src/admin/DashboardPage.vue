<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { api, query } from '../shared/api';
import { today, money } from '../shared/format';
import { useTask } from '../shared/useTask';
import { adminIdentity } from './identity';
import Feedback from '../shared/Feedback.vue';
const from = ref(today()),
  until = ref(today()),
  statistics = ref<Record<string, number>>();
const { busy, error, run } = useTask();
const cards = [
  { key: 'createdOrders', name: '新增订单', amount: false },
  { key: 'receivedFen', name: '支付收入', amount: true },
  { key: 'refundedFen', name: '退款金额', amount: true },
  { key: 'netReceivedFen', name: '净收款', amount: true },
];
function load() {
  if (adminIdentity.can('report:read'))
    return run(async () => {
      statistics.value = (
        await api<{ statistics: Record<string, number> }>(
          'admin',
          '/dashboard' + query({ from: from.value, until: until.value }),
        )
      ).statistics;
    });
}
onMounted(load);
</script>
<template>
  <div class="admin-toolbar">
    <div>
      <h1>经营概览</h1>
      <p class="admin-title-note">
        欢迎回来，{{ adminIdentity.profile.value?.username }}。今天也认真经营每一份好物。
      </p>
    </div>
  </div>
  <template v-if="adminIdentity.can('report:read')"
    ><form class="admin-toolbar" @submit.prevent="load">
      <label class="field"><span>开始日期</span><input v-model="from" type="date" required /></label
      ><label class="field"
        ><span>结束日期</span><input v-model="until" type="date" :min="from" required /></label
      ><button class="button small" :disabled="busy">查看统计</button>
    </form>
    <Feedback :busy="busy" :error="error" />
    <div class="metric-grid">
      <div v-for="c in cards" :key="c.key" class="metric-card">
        <span>{{ c.name }}</span><strong>{{
          statistics ? (c.amount ? money(statistics[c.key]) : statistics[c.key]) : '—'
        }}</strong>
      </div>
    </div>
    <div class="split-panels">
      <section class="panel">
        <h3>待办事项</h3>
        <div class="price-row">
          <span>待发货订单</span><strong>{{ statistics?.waitingShipment ?? '—' }}</strong>
        </div>
        <div class="price-row">
          <span>待审核售后</span><strong>{{ statistics?.waitingReview ?? '—' }}</strong>
        </div>
        <div class="price-row">
          <span>需人工处理任务</span><strong>{{ statistics?.manualTasks ?? '—' }}</strong>
        </div>
        <p class="inline-note">待办数量反映当前状态；收款数据按所选北京时间日期范围统计。</p>
      </section>
      <section class="panel">
        <h3>经营提醒</h3>
        <p class="inline-note">
          发货前核对订单和收货地址。退款通过审核后异步处理，可在售后列表追踪进度。
        </p>
        <p class="inline-note">本地学习环境使用模拟支付与物流，不会产生真实资金交易。</p>
        <div class="actions">
          <RouterLink v-if="adminIdentity.can('order:read')" class="button small" to="/admin/orders"
            >处理订单</RouterLink
          ><RouterLink
            v-if="adminIdentity.can('aftersale:review')"
            class="button secondary small"
            to="/admin/after-sales"
            >处理售后</RouterLink
          >
        </div>
      </section>
    </div></template
  >
  <section v-else class="panel">
    <h3>你的工作台已就绪</h3>
    <p>请从左侧菜单进入有权限的业务模块。</p>
    <p class="inline-note">当前账号没有经营统计查看权限。</p>
  </section>
</template>
