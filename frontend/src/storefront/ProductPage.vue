<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ShoppingBag } from 'lucide-vue-next';
import { api } from '../shared/api';
import type { Product } from '../shared/types';
import { money } from '../shared/format';
import { session } from '../shared/session';
import { useTask } from '../shared/useTask';
import ProductArt from '../shared/ProductArt.vue';
import Feedback from '../shared/Feedback.vue';
const route = useRoute(),
  router = useRouter();
const product = ref<Product>(),
  skuId = ref(''),
  quantity = ref(1),
  imageIndex = ref(0);
const { busy, error, notice, run } = useTask();
const sku = computed(() => product.value?.skus.find((s) => s.id === skuId.value));
const load = () =>
  run(async () => {
    product.value = await api<Product>('buyer', `/products/${route.params.id}`);
    skuId.value = product.value.skus.find((s) => s.enabled)?.id ?? '';
  });
onMounted(load);
function add() {
  if (!session.get('buyer')) {
    void router.push({ path: '/login', query: { redirect: route.fullPath } });
    return;
  }
  void run(
    () =>
      api('buyer', '/cart', {
        method: 'POST',
        body: { skuId: skuId.value, quantity: quantity.value },
      }),
    '已加入购物车',
  );
}
</script>
<template>
  <div class="container page">
    <div class="breadcrumbs">
      <RouterLink to="/">首页</RouterLink> / <RouterLink to="/products">全部好物</RouterLink> /
      商品详情
    </div>
    <Feedback :busy="busy" :error="error" :notice="notice" /><button
      v-if="error && !product"
      class="button"
      @click="load"
    >
      重试
    </button>
    <div v-if="product" class="product-detail">
      <div>
        <ProductArt :src="product.imageUrls[imageIndex]" :title="product.title" />
        <div v-if="product.imageUrls.length > 1" class="actions">
          <button
            v-for="(image, index) in product.imageUrls"
            :key="image"
            class="text-button"
            @click="imageIndex = index"
          >
            图片 {{ index + 1 }}
          </button>
        </div>
      </div>
      <div class="product-copy">
        <span class="eyebrow">MINI MALL / 日常精选</span>
        <h1>{{ product.title }}</h1>
        <p class="muted">单商家自营 · 认真对待每一件好物</p>
        <div class="price">{{ money(sku?.priceFen) }}</div>
        <h4>选择规格</h4>
        <div class="spec-options">
          <button
            v-for="s in product.skus.filter((s) => s.enabled)"
            :key="s.id"
            :class="{ active: skuId === s.id }"
            @click="skuId = s.id"
          >
            {{ s.specs }}
          </button>
        </div>
        <label class="field" style="margin-top: 24px"
          ><span>购买数量</span
          ><input v-model.number="quantity" class="quantity" type="number" min="1" max="99"
        /></label>
        <div class="actions">
          <button
            class="button"
            :disabled="busy || !sku || !Number.isInteger(quantity) || quantity < 1 || quantity > 99"
            @click="add"
          >
            <ShoppingBag :size="18" />加入购物车</button
          ><RouterLink class="button secondary" to="/cart">查看购物车</RouterLink>
        </div>
        <div class="detail-note">
          商品库存与优惠以提交订单时的校验结果为准。<br />支持整单售后，已发货商品需寄回后退款。<br />本地学习环境，支付通过模拟渠道完成。
        </div>
      </div>
    </div>
  </div>
</template>
