<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { api } from '../shared/api';
import type { CartItem, Checkout } from '../shared/types';
import { money } from '../shared/format';
import { useTask } from '../shared/useTask';
import Feedback from '../shared/Feedback.vue';
const items = ref<CartItem[]>([]),
  router = useRouter();
const { busy, error, run } = useTask();
const selected = computed(() => items.value.filter((i) => i.selected && i.valid));
const sum = computed(() => selected.value.reduce((s, i) => s + i.quantity * i.priceFen, 0));
async function refresh() {
  items.value = await api<CartItem[]>('buyer', '/cart');
}
const load = () => run(refresh);
onMounted(load);
function update(item: CartItem) {
  void run(async () => {
    try {
      await api('buyer', `/cart/${item.id}`, {
        method: 'PUT',
        body: { quantity: item.quantity, selected: item.selected },
      });
    } finally {
      await refresh();
    }
  });
}
function remove(item: CartItem) {
  if (confirm('确定从购物车移除这件商品？'))
    void run(async () => {
      await api('buyer', `/cart/${item.id}`, { method: 'DELETE' });
      await refresh();
    });
}
function checkout() {
  void run(async () => {
    const lines = await api<Checkout['lines']>('buyer', '/cart/selected', {
      method: 'POST',
      body: { itemIds: selected.value.map((i) => i.id) },
    });
    sessionStorage.setItem('mini-mall:checkout', JSON.stringify(lines));
    await router.push('/checkout');
  });
}
</script>
<template>
  <div class="container page">
    <span class="eyebrow">你的日常好物</span>
    <h1>购物车</h1>
    <Feedback :busy="busy" :error="error" /><button
      v-if="error"
      class="button secondary"
      @click="load"
    >
      刷新购物车
    </button>
    <div v-if="!items.length && !busy" class="empty">
      <h3>购物车里还没有好物</h3>
      <RouterLink to="/products" class="button">去逛逛</RouterLink>
    </div>
    <div v-else class="two-columns">
      <div class="panel">
        <div v-for="item in items" :key="item.id" class="list-row">
          <input
            v-model="item.selected"
            type="checkbox"
            :aria-label="`选择${item.title}`"
            :disabled="!item.valid || busy"
            @change="update(item)"
          />
          <div class="grow">
            <h3>{{ item.title }}</h3>
            <p>{{ item.specs }}</p>
            <small v-if="!item.valid">商品已失效，请移除后重新选择</small>
          </div>
          <strong>{{ money(item.priceFen) }}</strong><input
            v-model.number="item.quantity"
            class="quantity"
            type="number"
            min="1"
            max="99"
            :aria-label="`${item.title}数量`"
            :disabled="busy"
            @change="update(item)"
          /><button class="text-button" :disabled="busy" @click="remove(item)">移除</button>
        </div>
      </div>
      <aside class="panel summary">
        <h3>订单小计</h3>
        <div class="price-row">
          <span>已选商品</span><span>{{ selected.reduce((s, i) => s + i.quantity, 0) }} 件</span>
        </div>
        <div class="price-row total">
          <span>商品合计</span><strong>{{ money(sum) }}</strong>
        </div>
        <p class="inline-note">优惠和运费将在结算时计算。</p>
        <button class="button wide-button" :disabled="!selected.length || busy" @click="checkout">
          去结算
        </button>
      </aside>
    </div>
  </div>
</template>
