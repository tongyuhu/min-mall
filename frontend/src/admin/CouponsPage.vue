<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { api } from '../shared/api';
import type { Coupon } from '../shared/types';
import { dateTime, money, toFen } from '../shared/format';
import { useTask } from '../shared/useTask';
import Feedback from '../shared/Feedback.vue';
const rows = ref<Coupon[]>([]),
  dialog = ref(false);
const blank = () => ({
  id: '',
  name: '',
  threshold: '30.00',
  discount: '5.00',
  total: 100,
  validFrom: '',
  validUntil: '',
});
const form = reactive(blank()),
  { busy, error, notice, run } = useTask();
async function refresh() {
  rows.value = await api<Coupon[]>('admin', '/coupons');
}
const load = () => run(refresh);
onMounted(load);
function localDate(value: string) {
  const date = new Date(value);
  return new Date(date.getTime() - date.getTimezoneOffset() * 60000).toISOString().slice(0, 16);
}
function edit(row: Coupon) {
  Object.assign(form, {
    id: row.id,
    name: row.name,
    threshold: (row.thresholdFen / 100).toFixed(2),
    discount: (row.discountFen / 100).toFixed(2),
    total: row.total,
    validFrom: localDate(row.validFrom),
    validUntil: localDate(row.validUntil),
  });
  dialog.value = true;
}
function save() {
  void run(async () => {
    await api('admin', `/coupons${form.id ? '/' + form.id : ''}`, {
      method: form.id ? 'PUT' : 'POST',
      body: {
        name: form.name,
        thresholdFen: toFen(form.threshold),
        discountFen: toFen(form.discount),
        total: form.total,
        validFrom: new Date(form.validFrom).toISOString(),
        validUntil: new Date(form.validUntil).toISOString(),
      },
    });
    dialog.value = false;
    await refresh();
  }, '优惠券已保存');
}
function toggle(row: Coupon) {
  if (confirm(`确定${row.enabled ? '停用' : '启用'}此优惠券？`))
    void run(async () => {
      await api('admin', `/coupons/${row.id}/enabled`, {
        method: 'PUT',
        body: { enabled: !row.enabled },
      });
      await refresh();
    }, '状态已更新');
}
</script>
<template>
  <div class="admin-toolbar">
    <h1>优惠券管理</h1>
    <button class="button secondary small" :disabled="busy" @click="load">刷新</button
    ><button
      class="button small"
      @click="
        Object.assign(form, blank());
        dialog = true;
      "
    >
      创建优惠券
    </button>
  </div>
  <Feedback :busy="busy" :error="!dialog ? error : ''" :notice="notice" />
  <section class="panel">
    <el-table :data="rows" empty-text="暂无优惠券"
      ><el-table-column prop="name" label="名称" min-width="160" /><el-table-column
        label="优惠规则"
        min-width="170"
        ><template #default="{ row }"
          >满{{ money(row.thresholdFen) }}减{{ money(row.discountFen) }}</template
        ></el-table-column
      ><el-table-column label="已领 / 总量" width="115"
        ><template #default="{ row }">{{ row.issued }} / {{ row.total }}</template></el-table-column
      ><el-table-column label="有效期" min-width="180"
        ><template #default="{ row }"
          >{{ dateTime(row.validFrom) }}<br />{{ dateTime(row.validUntil) }}</template
        ></el-table-column
      ><el-table-column label="状态" width="85"
        ><template #default="{ row }">{{
          row.enabled ? '启用' : '停用'
        }}</template></el-table-column
      ><el-table-column label="操作" width="140"
        ><template #default="{ row }"
          ><el-button link :disabled="busy || row.issued > 0" @click="edit(row)">编辑</el-button
          ><el-button link :disabled="busy" @click="toggle(row)">{{
            row.enabled ? '停用' : '启用'
          }}</el-button></template
        ></el-table-column
      ></el-table
    >
  </section>
  <el-dialog v-model="dialog" title="优惠券规则" width="600px" :close-on-click-modal="false"
    ><Feedback :busy="busy" :error="error" />
    <form class="form-grid" @submit.prevent="save">
      <label class="field full"
        ><span>优惠券名称</span><input v-model="form.name" required maxlength="100" /></label
      ><label class="field"
        ><span>使用门槛（元）</span
        ><input v-model="form.threshold" type="number" min="0" step="0.01" required /></label
      ><label class="field"
        ><span>优惠金额（元）</span
        ><input v-model="form.discount" type="number" min="0.01" step="0.01" required /></label
      ><label class="field full"
        ><span>发放总量</span
        ><input v-model.number="form.total" type="number" min="1" step="1" required /></label
      ><label class="field"
        ><span>开始时间（本地时区）</span
        ><input v-model="form.validFrom" type="datetime-local" required /></label
      ><label class="field"
        ><span>结束时间（本地时区）</span
        ><input v-model="form.validUntil" type="datetime-local" :min="form.validFrom" required
      /></label>
      <p class="inline-note field full">优惠券一旦被领取，规则不可修改。</p>
      <button class="button" :disabled="busy">保存优惠券</button>
    </form></el-dialog
  >
</template>
