<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import { ArrowRight, Search, PackageCheck, Truck, ShieldCheck } from 'lucide-vue-next';
import { api, query } from '../shared/api';
import type { Entry, Page, Product } from '../shared/types';
import { money } from '../shared/format';
import { useTask } from '../shared/useTask';
import Feedback from '../shared/Feedback.vue';
import ProductArt from '../shared/ProductArt.vue';
const route = useRoute();
const isHome = computed(() => route.path === '/');
const { busy, error, run } = useTask();
const categories = ref<Entry[]>([]),
  brands = ref<Entry[]>([]);
const products = ref<Product[]>([]),
  total = ref(0),
  page = ref(1);
const keyword = ref(''),
  categoryId = ref(''),
  brandId = ref(''),
  sort = ref('id');
async function load() {
  await run(async () => {
    const data = await api<Page<Product>>(
      'buyer',
      '/products' +
        query({
          keyword: keyword.value,
          categoryId: categoryId.value,
          brandId: brandId.value,
          sort: sort.value,
          page: page.value,
          size: 12,
        }),
    );
    products.value = data.items;
    total.value = data.total;
  });
}
function filter() {
  page.value = 1;
  void load();
}
onMounted(async () => {
  await run(async () => {
    [categories.value, brands.value] = await Promise.all([
      api<Entry[]>('buyer', '/categories'),
      api<Entry[]>('buyer', '/brands'),
    ]);
  });
  await load();
});
</script>
<template>
  <section v-if="isHome" class="hero container">
    <div class="hero-copy">
      <span class="eyebrow">为日常，认真挑选</span>
      <h1>把日子，<br />过成喜欢的样子。</h1>
      <p>从一件用得顺手的好物开始，<br />发现平凡日常里的小小满足。</p>
      <RouterLink class="button" to="/products">探索全部好物 <ArrowRight :size="17" /></RouterLink>
      <div class="hero-caption"><span>01 / 日常精选</span><i /></div>
    </div>
    <div class="hero-art">
      <div class="hero-orbit" />
      <ProductArt title="日常精选" /><span class="hero-stamp"
        >少一点，<br /><strong>刚刚好。</strong></span
      >
    </div>
  </section>
  <div v-if="isHome" class="container service-strip">
    <span><PackageCheck /> 自营好物，认真挑选</span><span><Truck /> 订单物流，随时可查</span
    ><span><ShieldCheck /> 完整售后，安心购物</span>
  </div>
  <section class="container catalog-section">
    <div class="section-heading">
      <div>
        <span class="eyebrow">日常精选 · 每一件都用心</span>
        <h1 v-if="!isHome">发现日常好物</h1>
        <h2 v-else>刚刚好的日常精选</h2>
      </div>
      <span class="muted">共 {{ total }} 件好物</span>
    </div>
    <form class="catalog-filters" @submit.prevent="filter">
      <div class="search-field">
        <Search :size="18" /><input
          v-model="keyword"
          :disabled="busy"
          aria-label="搜索商品"
          placeholder="搜索你想找的好物"
        /><button class="text-button" type="submit" :disabled="busy">搜索</button>
      </div>
      <select v-model="brandId" aria-label="品牌筛选" :disabled="busy" @change="filter">
        <option value="">全部品牌</option>
        <option v-for="b in brands" :key="b.id" :value="b.id">{{ b.name }}</option></select
      ><select v-model="sort" aria-label="排序" :disabled="busy" @change="filter">
        <option value="id">最新上架</option>
        <option value="price">价格从低到高</option>
      </select>
    </form>
    <div class="category-tabs">
      <button
        :class="{ active: !categoryId }"
        :disabled="busy"
        @click="
          categoryId = '';
          filter();
        "
      >
        全部好物</button
      ><button
        v-for="c in categories"
        :key="c.id"
        :class="{ active: categoryId === c.id }"
        :disabled="busy"
        @click="
          categoryId = c.id;
          filter();
        "
      >
        {{ c.name }}
      </button>
    </div>
    <Feedback :busy="busy" :error="error" /><button
      v-if="error"
      class="button secondary"
      @click="load"
    >
      重新加载
    </button>
    <div v-if="!busy && !error && !products.length" class="empty">
      <h3>暂时没有找到这件好物</h3>
      <p>换个关键词或分类试试。</p>
    </div>
    <div class="product-grid">
      <RouterLink
        v-for="(p, index) in products"
        :key="p.id"
        :to="`/products/${p.id}`"
        class="product-card"
        ><ProductArt :src="p.imageUrls[0]" :title="p.title" :variant="index" />
        <div class="product-meta">
          <small>{{ categories.find((c) => c.id === p.categoryId)?.name || '生活精选' }}</small>
          <h3>{{ p.title }}</h3>
          <div>
            <strong>{{
              money(Math.min(...p.skus.filter((s) => s.enabled).map((s) => s.priceFen)))
            }}</strong><span>{{
              p.skus.filter((s) => s.enabled).length > 1 ? '多款可选' : '日常好物'
            }}</span
            ><ArrowRight :size="18" />
          </div></div
      ></RouterLink>
    </div>
    <div v-if="total > 12" class="pagination">
      <button
        :disabled="page <= 1 || busy"
        @click="
          page--;
          load();
        "
      >
        上一页</button
      ><span>{{ page }} / {{ Math.ceil(total / 12) }}</span
      ><button
        :disabled="page * 12 >= total || busy"
        @click="
          page++;
          load();
        "
      >
        下一页
      </button>
    </div>
  </section>
  <section v-if="isHome" class="container editorial">
    <div>
      <span class="eyebrow">生活里的小小满足</span>
      <h2>生活不必很满，<br />喜欢就刚刚好。</h2>
    </div>
    <p>
      我们相信，好物的意义，<br />是陪你度过一个又一个普通的日子。<br /><RouterLink to="/products"
        >去发现你的那一件 <ArrowRight :size="16"
      /></RouterLink>
    </p>
  </section>
</template>
