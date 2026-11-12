<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { api } from '../shared/api';
import { session } from '../shared/session';
import { useTask } from '../shared/useTask';
import Feedback from '../shared/Feedback.vue';
const oldPassword = ref(''),
  newPassword = ref(''),
  router = useRouter();
const { busy, error, run } = useTask();
function save() {
  void run(async () => {
    await api('admin', '/auth/password', {
      method: 'PUT',
      body: { oldPassword: oldPassword.value, newPassword: newPassword.value },
    });
    session.clear('admin');
    await router.push('/admin/login');
  });
}
</script>
<template>
  <h1>修改登录密码</h1>
  <Feedback :busy="busy" :error="error" />
  <form class="panel form-stack" style="max-width: 520px" @submit.prevent="save">
    <label class="field"
      ><span>原密码</span
      ><input
        v-model="oldPassword"
        type="password"
        required
        autocomplete="current-password" /></label
    ><label class="field"
      ><span>新密码</span
      ><input
        v-model="newPassword"
        type="password"
        required
        minlength="8"
        maxlength="72"
        autocomplete="new-password"
    /></label>
    <p class="inline-note">修改密码后需要重新登录。</p>
    <button class="button" :disabled="busy">更新密码</button>
  </form>
</template>
