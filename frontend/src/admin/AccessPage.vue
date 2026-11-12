<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { api } from '../shared/api';
import type { AdminAccount, Role } from '../shared/types';
import { useTask } from '../shared/useTask';
import { adminIdentity } from './identity';
import Feedback from '../shared/Feedback.vue';
const accounts = ref<AdminAccount[]>([]),
  roles = ref<Role[]>([]),
  dialog = ref(false);
const form = reactive({ id: '', username: '', password: '', roleIds: [] as string[] });
const roleEdit = ref<Role>(),
  roleDialog = ref(false);
const permissions: Record<string, string> = {
  'catalog:write': '商品和优惠券管理',
  'stock:adjust': '库存调整',
  'order:read': '订单查询',
  'shipment:write': '订单发货',
  'aftersale:review': '售后审核',
  'member:write': '会员管理',
  'rbac:write': '账号与权限管理',
  'report:read': '统计查看',
  'task:retry': '任务重试',
  'audit:read': '审计查看',
};
const { busy, error, notice, run } = useTask();
async function refresh() {
  [accounts.value, roles.value] = await Promise.all([
    api<AdminAccount[]>('admin', '/administrators'),
    api<Role[]>('admin', '/roles'),
  ]);
  await adminIdentity.load();
}
const load = () => run(refresh);
onMounted(load);
function create() {
  Object.assign(form, { id: '', username: '', password: '', roleIds: [] });
  dialog.value = true;
}
function edit(row: AdminAccount) {
  Object.assign(form, {
    id: row.id,
    username: row.username,
    password: '',
    roleIds: [...row.roleIds],
  });
  dialog.value = true;
}
function save() {
  void run(async () => {
    await api('admin', form.id ? `/administrators/${form.id}/roles` : '/administrators', {
      method: form.id ? 'PUT' : 'POST',
      body: form.id
        ? { roleIds: form.roleIds }
        : { username: form.username, password: form.password, roleIds: form.roleIds },
    });
    dialog.value = false;
    await refresh();
  }, '管理员已保存');
}
function toggle(row: AdminAccount) {
  if (confirm(`确定${row.enabled ? '禁用' : '启用'}管理员 ${row.username}？`))
    void run(async () => {
      await api('admin', `/administrators/${row.id}/enabled`, {
        method: 'PUT',
        body: { enabled: !row.enabled },
      });
      await refresh();
    }, '账号状态已更新');
}
const resetId = ref(''),
  resetPassword = ref(''),
  passwordDialog = ref(false);
function reset() {
  void run(async () => {
    await api('admin', `/administrators/${resetId.value}/password`, {
      method: 'PUT',
      body: { password: resetPassword.value },
    });
    passwordDialog.value = false;
    resetPassword.value = '';
  }, '密码已重置，原有会话已失效');
}
function editRole(role: Role) {
  roleEdit.value = { ...role, permissions: [...role.permissions] };
  roleDialog.value = true;
}
function saveRole() {
  void run(async () => {
    await api('admin', `/roles/${roleEdit.value!.id}/permissions`, {
      method: 'PUT',
      body: { permissions: roleEdit.value!.permissions },
    });
    roleDialog.value = false;
    await refresh();
  }, '角色权限已更新');
}
</script>
<template>
  <div class="admin-toolbar">
    <h1>账号与权限</h1>
    <button class="button secondary small" :disabled="busy" @click="load">刷新</button
    ><button class="button small" :disabled="busy" @click="create">创建管理员</button>
  </div>
  <Feedback
    :busy="busy"
    :error="!dialog && !roleDialog && !passwordDialog ? error : ''"
    :notice="notice"
  />
  <section class="panel">
    <h3>管理员账号</h3>
    <el-table :data="accounts" empty-text="暂无管理员"
      ><el-table-column prop="username" label="用户名" min-width="150" /><el-table-column
        label="角色"
        min-width="170"
        ><template #default="{ row }">{{
          row.roleIds.map((id: string) => roles.find((r) => r.id === id)?.name || id).join('、')
        }}</template></el-table-column
      ><el-table-column label="状态" width="100"
        ><template #default="{ row }">{{
          row.enabled ? '正常' : '已禁用'
        }}</template></el-table-column
      ><el-table-column label="操作" min-width="225"
        ><template #default="{ row }"
          ><el-button link :disabled="busy" @click="edit(row)">设置角色</el-button
          ><el-button
            link
            :disabled="busy"
            @click="
              resetId = row.id;
              resetPassword = '';
              passwordDialog = true;
            "
            >重置密码</el-button
          ><el-button link :disabled="busy" @click="toggle(row)">{{
            row.enabled ? '禁用' : '启用'
          }}</el-button></template
        ></el-table-column
      ></el-table
    >
    <p class="inline-note">账号列表最多显示 100 项；最后一个有效超级管理员不能被禁用或移除角色。</p>
  </section>
  <section class="panel">
    <h3>角色权限</h3>
    <el-table :data="roles"
      ><el-table-column prop="name" label="角色" width="160" /><el-table-column label="权限"
        ><template #default="{ row }">{{
          row.permissions.map((p: string) => permissions[p] || p).join('、')
        }}</template></el-table-column
      ><el-table-column label="操作" width="100"
        ><template #default="{ row }"
          ><el-button link :disabled="busy || row.id === '1'" @click="editRole(row)"
            >编辑权限</el-button
          ></template
        ></el-table-column
      ></el-table
    >
  </section>
  <el-dialog
    v-model="dialog"
    :title="form.id ? '设置管理员角色' : '创建管理员'"
    width="520px"
    :close-on-click-modal="false"
    ><Feedback :busy="busy" :error="error" />
    <form class="form-stack" @submit.prevent="save">
      <label class="field"
        ><span>用户名</span
        ><input
          v-model="form.username"
          :disabled="!!form.id"
          required
          minlength="3"
          maxlength="64" /></label
      ><label v-if="!form.id" class="field"
        ><span>初始密码</span
        ><input
          v-model="form.password"
          type="password"
          required
          minlength="8"
          maxlength="72"
          autocomplete="new-password" /></label
      ><label v-for="r in roles" :key="r.id" class="check-label"
        ><input v-model="form.roleIds" type="checkbox" :value="r.id" />{{ r.name }}</label
      ><button class="button" :disabled="busy || !form.roleIds.length">保存管理员</button>
    </form></el-dialog
  >
  <el-dialog v-model="roleDialog" title="角色权限" width="560px" :close-on-click-modal="false"
    ><Feedback :busy="busy" :error="error" />
    <form v-if="roleEdit" class="form-stack" @submit.prevent="saveRole">
      <h3>{{ roleEdit.name }}</h3>
      <div class="permission-grid">
        <label v-for="(name, code) in permissions" :key="code" class="check-label"
          ><input v-model="roleEdit.permissions" type="checkbox" :value="code" />{{ name }}</label
        >
      </div>
      <button class="button" :disabled="busy">保存权限</button>
    </form></el-dialog
  >
  <el-dialog v-model="passwordDialog" title="重置管理员密码" width="450px"
    ><Feedback :busy="busy" :error="error" />
    <form class="form-stack" @submit.prevent="reset">
      <label class="field"
        ><span>新密码</span
        ><input
          v-model="resetPassword"
          type="password"
          required
          minlength="8"
          maxlength="72"
          autocomplete="new-password" /></label
      ><button class="button" :disabled="busy">确认重置</button>
    </form></el-dialog
  >
</template>
