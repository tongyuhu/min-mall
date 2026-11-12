<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { api, query } from '../shared/api';
import type { Order, Page } from '../shared/types';
import { dateTime, money, statusText } from '../shared/format';
import { useTask } from '../shared/useTask';
import Feedback from '../shared/Feedback.vue';
import Pager from './Pager.vue';
const rows = ref<Order[]>([]),
  total = ref(0),
  page = ref(1);
const filters = reactive({ orderNo: '', memberId: '', status: '', from: '', until: '' });
const { busy, error, run } = useTask();
function load() {
  return run(async () => {
    const data = await api<Page<Order>>(
      'admin',
      '/orders' +
        query({
          ...filters,
          from: filters.from ? new Date(filters.from).toISOString() : '',
          until: filters.until ? new Date(filters.until).toISOString() : '',
          page: page.value,
        }),
    );
    rows.value = data.items;
    total.value = data.total;
  });
}
onMounted(load);
</script>
<template>
  <h1>订单管理</h1>
  <p class="admin-title-note">查看订单、收货信息，安排整单发货。</p>
  <form
    class="admin-toolbar"
    @submit.prevent="
      page = 1;
      load();
    "
  >
    <input v-model="filters.orderNo" aria-label="订单号筛选" placeholder="订单号" /><input
      v-model="filters.memberId"
      aria-label="会员编号"
      placeholder="会员编号"
      pattern="[0-9]*"
    /><select v-model="filters.status" aria-label="订单状态">
      <option value="">全部状态</option>
      <option
        v-for="s in ['PENDING_PAYMENT', 'PAID', 'SHIPPED', 'COMPLETED', 'CLOSED', 'REFUNDED']"
        :key="s"
        :value="s"
      >
        {{ statusText(s) }}
      </option></select
    ><button class="button small" :disabled="busy">查询</button>
  </form>
  <Feedback :busy="busy" :error="error" />
  <section class="panel">
    <el-table :data="rows" empty-text="暂无符合条件的订单"
      ><el-table-column prop="orderNo" label="订单号" min-width="230" /><el-table-column
        label="收货人"
        min-width="110"
        ><template #default="{ row }">{{ row.address.receiver }}</template></el-table-column
      ><el-table-column label="金额" width="110"
        ><template #default="{ row }">{{ money(row.payableFen) }}</template></el-table-column
      ><el-table-column label="状态" width="110"
        ><template #default="{ row }"
          ><el-tag>{{ statusText(row.status) }}</el-tag></template
        ></el-table-column
      ><el-table-column label="下单时间" min-width="175"
        ><template #default="{ row }">{{ dateTime(row.createdAt) }}</template></el-table-column
      ><el-table-column label="操作" width="100"
        ><template #default="{ row }"
          ><RouterLink :to="`/admin/orders/${row.orderNo}`">查看详情 →</RouterLink></template
        ></el-table-column
      ></el-table
    ><Pager
      :page="page"
      :total="total"
      :busy="busy"
      @change="
        page = $event;
        load();
      "
    />
  </section>
</template>
