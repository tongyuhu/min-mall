<script setup lang="ts">
import { computed, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { api } from '../shared/api';
import { session, type Session } from '../shared/session';
import { useTask } from '../shared/useTask';
import Feedback from '../shared/Feedback.vue';
const route = useRoute(),
  router = useRouter();
const admin = computed(() => route.path.startsWith('/admin'));
const register = computed(() => route.path === '/register');
const username = ref(''),
  password = ref('');
const { busy, error, run } = useTask();
async function submit() {
  await run(async () => {
    const scope = admin.value ? 'admin' : 'buyer';
    const body = { username: username.value, password: password.value };
    if (register.value) await api(scope, '/auth/register', { method: 'POST', body });
    session.save(scope, await api<Session>(scope, '/auth/login', { method: 'POST', body }));
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '';
    const safe =
      redirect.startsWith('/') &&
      !redirect.startsWith('//') &&
      (admin.value
        ? redirect.startsWith('/admin') && redirect !== '/admin/login'
        : !redirect.startsWith('/admin'));
    await router.push(safe ? redirect : admin.value ? '/admin' : '/');
  });
}
</script>
<template>
  <div class="login-page">
    <div class="login-intro">
      <RouterLink to="/" class="eyebrow">小满生活 / MINI MALL</RouterLink>
      <h2>
        {{ admin ? '让经营井井有条，' : '每一份小小喜欢，' }}<br />{{
          admin ? '让好物走进日常。' : '都值得认真对待。'
        }}
      </h2>
      <p>{{ admin ? '商品、订单与服务，从这里开始。' : '欢迎在这里，找到属于你的日常好物。' }}</p>
    </div>
    <form class="login-form form-stack" @submit.prevent="submit">
      <h1>{{ admin ? '登录商家工作台' : register ? '开启你的小满生活' : '欢迎回到小满' }}</h1>
      <p v-if="route.query.expired" class="inline-note">登录已失效，请重新登录后继续。</p>
      <Feedback :busy="busy" :error="error" /><label class="field"
        ><span>用户名</span
        ><input
          v-model="username"
          required
          minlength="3"
          maxlength="64"
          autocomplete="username"
          placeholder="请输入用户名" /></label
      ><label class="field"
        ><span>密码</span
        ><input
          v-model="password"
          required
          type="password"
          minlength="8"
          maxlength="72"
          :autocomplete="register ? 'new-password' : 'current-password'"
          placeholder="请输入密码，至少 8 位" /></label
      ><button class="button" :disabled="busy">
        {{ admin ? '登录管理后台' : register ? '注册并登录' : '登录' }}
      </button>
      <div class="login-foot">
        <RouterLink
          v-if="!admin"
          :to="{ path: register ? '/login' : '/register', query: route.query }"
          >{{ register ? '已有账号？去登录' : '还没有账号？立即注册' }}</RouterLink><RouterLink
          to="/"
          >返回商城</RouterLink
        >
      </div>
    </form>
  </div>
</template>
