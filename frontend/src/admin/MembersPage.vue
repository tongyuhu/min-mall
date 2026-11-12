<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { api, query } from '../shared/api';
import type { Member, Page } from '../shared/types';
import { useTask } from '../shared/useTask';
import Feedback from '../shared/Feedback.vue';
import Pager from './Pager.vue';
const rows = ref<Member[]>([]),
  page = ref(1),
  total = ref(0);
const { busy, error, notice, run } = useTask();
async function refresh() {
  const data = await api<Page<Member>>('admin', '/members' + query({ page: page.value }));
  rows.value = data.items;
  total.value = data.total;
}
const load = () => run(refresh);
onMounted(load);
function toggle(row: Member) {
  if (confirm(`确定${row.enabled ? '禁用' : '启用'}会员“${row.username}”？原有登录会话会失效。`))
    void run(async () => {
      await api('admin', `/members/${row.id}/enabled`, {
        method: 'PUT',
        body: { enabled: !row.enabled },
      });
      await refresh();
    }, '会员状态已更新');
}
</script>
<template>
  <div class="admin-toolbar">
    <h1>会员管理</h1>
    <button class="button secondary small" :disabled="busy" @click="load">刷新</button>
  </div>
  <Feedback :busy="busy" :error="error" :notice="notice" />
  <section class="panel">
    <el-table :data="rows" empty-text="暂无会员"
      ><el-table-column prop="id" label="编号" width="100" /><el-table-column
        prop="username"
        label="用户名"
        min-width="180"
      /><el-table-column prop="nickname" label="昵称" min-width="140" /><el-table-column
        label="状态"
        width="110"
        ><template #default="{ row }"
          ><el-tag :type="row.enabled ? 'success' : 'danger'">{{
            row.enabled ? '正常' : '禁用'
          }}</el-tag></template
        ></el-table-column
      ><el-table-column label="操作" width="120"
        ><template #default="{ row }"
          ><el-button
            link
            :type="row.enabled ? 'danger' : 'primary'"
            :disabled="busy"
            @click="toggle(row)"
            >{{ row.enabled ? '禁用会员' : '启用会员' }}</el-button
          ></template
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
