<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import { api, query } from '../shared/api';
import type { Page } from '../shared/types';
import { dateTime, statusText } from '../shared/format';
import { adminIdentity } from './identity';
import { useTask } from '../shared/useTask';
import Feedback from '../shared/Feedback.vue';
import Pager from './Pager.vue';
interface SystemRow {
  id: string;
  type?: string;
  businessKey?: string;
  status?: string;
  attempts?: number;
  nextRunAt?: string;
  lastError?: string;
  actorId?: string;
  operation?: string;
  target?: string;
  outcome?: string;
  httpStatus?: number;
  traceId?: string;
  at?: string;
}
const route = useRoute(),
  audit = computed(() => route.path.endsWith('/audits')),
  rows = ref<SystemRow[]>([]),
  page = ref(1),
  total = ref(0);
const { busy, error, notice, run } = useTask();
async function refresh() {
  if (audit.value) {
    const result = await api<Page<SystemRow>>('admin', '/audits' + query({ page: page.value }));
    rows.value = result.items;
    total.value = result.total;
  } else rows.value = await api<SystemRow[]>('admin', '/tasks');
}
const load = () => run(refresh);
onMounted(load);
function retry(row: SystemRow) {
  if (confirm('确认重试这个人工处理任务？系统将再次查询或提交原业务请求。'))
    void run(async () => {
      await api('admin', `/tasks/${row.id}/retry`, { method: 'POST' });
      await refresh();
    }, '任务已重新排队');
}
</script>
<template>
  <div class="admin-toolbar">
    <h1>{{ audit ? '操作审计' : '后台任务' }}</h1>
    <button class="button secondary small" :disabled="busy" @click="load">刷新</button>
  </div>
  <p class="admin-title-note">
    {{
      audit
        ? '记录管理端操作结果，使用追踪号辅助定位问题。'
        : '查看等待中的异步任务；仅人工处理状态允许手动重试，最多展示 200 条。'
    }}
  </p>
  <Feedback :busy="busy" :error="error" :notice="notice" />
  <section class="panel">
    <el-table v-if="audit" :data="rows" empty-text="暂无操作记录"
      ><el-table-column prop="actorId" label="操作者" width="90" /><el-table-column
        prop="operation"
        label="操作"
        min-width="170"
      /><el-table-column
        prop="target"
        label="目标"
        min-width="180"
        show-overflow-tooltip
      /><el-table-column prop="httpStatus" label="状态码" width="85" /><el-table-column
        prop="outcome"
        label="结果"
        width="100"
      /><el-table-column
        prop="traceId"
        label="追踪号"
        min-width="160"
        show-overflow-tooltip
      /><el-table-column label="时间" min-width="170"
        ><template #default="{ row }">{{ dateTime(row.at) }}</template></el-table-column
      ></el-table
    ><el-table v-else :data="rows" empty-text="暂无等待中的任务"
      ><el-table-column prop="type" label="任务类型" min-width="160" /><el-table-column
        prop="businessKey"
        label="业务号"
        min-width="190"
        show-overflow-tooltip
      /><el-table-column label="状态" width="110"
        ><template #default="{ row }">{{ statusText(row.status) }}</template></el-table-column
      ><el-table-column prop="attempts" label="尝试次数" width="85" /><el-table-column
        prop="lastError"
        label="最近错误"
        min-width="200"
        show-overflow-tooltip
      /><el-table-column label="下次执行" min-width="175"
        ><template #default="{ row }">{{ dateTime(row.nextRunAt) }}</template></el-table-column
      ><el-table-column v-if="adminIdentity.can('task:retry')" label="操作" width="90"
        ><template #default="{ row }"
          ><el-button link :disabled="busy || row.status !== 'MANUAL'" @click="retry(row)"
            >重试</el-button
          ></template
        ></el-table-column
      ></el-table
    ><Pager
      v-if="audit"
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
