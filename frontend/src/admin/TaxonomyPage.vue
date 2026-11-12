<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { api } from '../shared/api';
import type { Entry } from '../shared/types';
import { parentChoices } from '../shared/taxonomy';
import { useTask } from '../shared/useTask';
import Feedback from '../shared/Feedback.vue';
const kind = ref('categories'),
  rows = ref<Entry[]>([]),
  parents = ref<Entry[]>([]),
  form = reactive({ id: '', parentId: '', name: '' });
const kinds: Record<string, string> = {
  categories: '商品分类',
  brands: '品牌',
  specifications: '规格名称',
  'specification-values': '规格值',
};
const hasParent = computed(() => ['categories', 'specification-values'].includes(kind.value));
const { busy, error, notice, run } = useTask();
async function refresh() {
  rows.value = await api<Entry[]>('admin', `/catalog/${kind.value}`);
  parents.value = hasParent.value
    ? await api<Entry[]>(
        'admin',
        `/catalog/${kind.value === 'categories' ? 'categories' : 'specifications'}`,
      )
    : [];
}
const load = () => run(refresh);
watch(kind, () => {
  Object.assign(form, { id: '', parentId: '', name: '' });
  void load();
});
onMounted(load);
function save() {
  void run(async () => {
    await api('admin', `/catalog/${kind.value}`, {
      method: 'POST',
      body: {
        id: form.id || null,
        parentId: hasParent.value ? form.parentId || null : null,
        name: form.name,
      },
    });
    Object.assign(form, { id: '', parentId: '', name: '' });
    await refresh();
  }, '已保存');
}
function remove(row: Entry) {
  if (confirm(`确定停用“${row.name}”？已被引用的资料可能无法停用。`))
    void run(async () => {
      await api('admin', `/catalog/${kind.value}/${row.id}`, { method: 'DELETE' });
      await refresh();
    }, '已停用');
}
</script>
<template>
  <div class="admin-toolbar">
    <h1>分类与规格</h1>
    <button class="button secondary small" :disabled="busy" @click="load">刷新</button>
  </div>
  <div class="category-tabs">
    <button
      v-for="(name, key) in kinds"
      :key="key"
      :class="{ active: kind === key }"
      :disabled="busy"
      @click="kind = key"
    >
      {{ name }}
    </button>
  </div>
  <Feedback :busy="busy" :error="error" :notice="notice" />
  <div class="two-columns">
    <section class="panel">
      <el-table :data="rows" empty-text="暂无资料"
        ><el-table-column prop="id" label="编号" width="85" /><el-table-column
          prop="name"
          :label="kinds[kind]"
        /><el-table-column v-if="hasParent" label="所属"
          ><template #default="{ row }">{{
            parents.find((p) => p.id === row.parentId)?.name || '顶级分类'
          }}</template></el-table-column
        ><el-table-column label="操作" width="145"
          ><template #default="{ row }"
            ><el-button
              link
              :disabled="busy"
              @click="Object.assign(form, { ...row, parentId: row.parentId || '' })"
              >编辑</el-button
            ><el-button link type="danger" :disabled="busy" @click="remove(row)"
              >停用</el-button
            ></template
          ></el-table-column
        ></el-table
      >
    </section>
    <form class="panel form-stack" @submit.prevent="save">
      <h3>{{ form.id ? '编辑' : '新增' }}{{ kinds[kind] }}</h3>
      <label class="field"
        ><span>名称</span><input v-model="form.name" required maxlength="100" /></label
      ><label v-if="hasParent" class="field"
        ><span>{{ kind === 'categories' ? '上级分类' : '所属规格' }}</span
        ><select v-model="form.parentId" :required="kind === 'specification-values'">
          <option value="">{{ kind === 'categories' ? '顶级分类' : '请选择规格' }}</option>
          <option v-for="p in parentChoices(kind, form.id, parents)" :key="p.id" :value="p.id">
            {{ p.name }}
          </option>
        </select></label
      ><button class="button" :disabled="busy">保存资料</button
      ><button
        v-if="form.id"
        type="button"
        class="text-button"
        @click="Object.assign(form, { id: '', parentId: '', name: '' })"
      >
        取消编辑
      </button>
    </form>
  </div>
</template>
