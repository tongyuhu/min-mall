<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { api } from '../shared/api';
import type { Address, AddressView, Member } from '../shared/types';
import { session } from '../shared/session';
import { useTask } from '../shared/useTask';
import Feedback from '../shared/Feedback.vue';
const route = useRoute(),
  router = useRouter(),
  tab = computed(() => route.params.tab || 'profile');
const { busy, error, notice, run } = useTask();
const member = ref<Member>(),
  addresses = ref<AddressView[]>([]),
  editing = ref('');
const emptyAddress = (): Address => ({
  receiver: '',
  phone: '',
  province: '',
  city: '',
  district: '',
  detail: '',
  defaultAddress: false,
});
const address = reactive(emptyAddress()),
  nickname = ref(''),
  oldPassword = ref(''),
  newPassword = ref('');
async function refresh() {
  [member.value, addresses.value] = await Promise.all([
    api<Member>('buyer', '/me'),
    api<AddressView[]>('buyer', '/addresses'),
  ]);
  nickname.value = member.value.nickname;
}
const load = () => run(refresh);
onMounted(load);
function edit(value: AddressView) {
  editing.value = value.id;
  Object.assign(address, value.address);
}
function saveAddress() {
  void run(async () => {
    await api('buyer', `/addresses${editing.value ? '/' + editing.value : ''}`, {
      method: editing.value ? 'PUT' : 'POST',
      body: address,
    });
    editing.value = '';
    Object.assign(address, emptyAddress());
    await refresh();
  }, '地址已保存');
}
function remove(id: string) {
  if (confirm('确定删除这个收货地址？'))
    void run(async () => {
      await api('buyer', `/addresses/${id}`, { method: 'DELETE' });
      await refresh();
    }, '地址已删除');
}
function profile() {
  void run(async () => {
    await api('buyer', '/me', { method: 'PATCH', body: { nickname: nickname.value } });
    await refresh();
  }, '资料已更新');
}
function password() {
  void run(async () => {
    await api('buyer', '/me/password', {
      method: 'PUT',
      body: { oldPassword: oldPassword.value, newPassword: newPassword.value },
    });
    session.clear('buyer');
    await router.push('/login');
  });
}
function logout() {
  void run(async () => {
    await api('buyer', '/auth/logout', { method: 'POST' });
    session.clear('buyer');
    await router.push('/');
  });
}
</script>
<template>
  <div class="container page">
    <span class="eyebrow">我的生活 · 我的喜欢</span>
    <h1>我的小满</h1>
    <nav class="account-nav">
      <RouterLink to="/account">个人资料</RouterLink
      ><RouterLink to="/account/addresses">收货地址</RouterLink
      ><RouterLink to="/account/password">修改密码</RouterLink
      ><RouterLink to="/orders">我的订单</RouterLink
      ><RouterLink to="/after-sales">售后记录</RouterLink>
    </nav>
    <Feedback :busy="busy" :error="error" :notice="notice" /><button
      v-if="error"
      class="button secondary"
      @click="load"
    >
      重新加载
    </button>
    <div v-if="tab === 'addresses'" class="two-columns">
      <div>
        <div v-for="a in addresses" :key="a.id" class="panel address-card">
          <strong>{{ a.address.receiver }} · {{ a.address.phone }}</strong>
          <span v-if="a.address.defaultAddress" class="badge">默认地址</span>
          <p>
            {{ a.address.province }} {{ a.address.city }} {{ a.address.district }}
            {{ a.address.detail }}
          </p>
          <button class="text-button" @click="edit(a)">编辑</button
          ><button class="text-button" :disabled="busy" @click="remove(a.id)">删除</button>
        </div>
        <div v-if="!addresses.length" class="empty">还没有收货地址，先添加一个吧。</div>
      </div>
      <form class="panel form-stack" @submit.prevent="saveAddress">
        <h3>{{ editing ? '编辑收货地址' : '新增收货地址' }}</h3>
        <label class="field"
          ><span>收货人</span><input v-model="address.receiver" required maxlength="50" /></label
        ><label class="field"
          ><span>手机号码</span
          ><input v-model="address.phone" required type="tel" maxlength="32" /></label
        ><label class="field"
          ><span>省份</span><input v-model="address.province" required maxlength="50" /></label
        ><label class="field"
          ><span>城市</span><input v-model="address.city" required maxlength="50" /></label
        ><label class="field"
          ><span>区县</span><input v-model="address.district" required maxlength="50" /></label
        ><label class="field"
          ><span>详细地址</span><input v-model="address.detail" required maxlength="200" /></label
        ><label class="check-label"
          ><input v-model="address.defaultAddress" type="checkbox" />设为默认地址</label
        ><button class="button" :disabled="busy">保存地址</button
        ><button
          v-if="editing"
          type="button"
          class="text-button"
          @click="
            editing = '';
            Object.assign(address, emptyAddress());
          "
        >
          取消编辑
        </button>
      </form>
    </div>
    <form
      v-else-if="tab === 'password'"
      class="panel form-stack"
      style="max-width: 520px"
      @submit.prevent="password"
    >
      <h3>修改登录密码</h3>
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
      <p class="inline-note">修改后需要重新登录，原有会话将失效。</p>
      <button class="button" :disabled="busy">更新密码</button>
    </form>
    <form v-else class="panel form-stack" style="max-width: 520px" @submit.prevent="profile">
      <h3>个人资料</h3>
      <label class="field"><span>用户名</span><input :value="member?.username" disabled /></label
      ><label class="field"
        ><span>昵称</span><input v-model="nickname" required maxlength="50"
      /></label>
      <div class="actions">
        <button class="button" :disabled="busy">保存资料</button
        ><button type="button" class="button secondary" :disabled="busy" @click="logout">
          退出登录
        </button>
      </div>
    </form>
  </div>
</template>
