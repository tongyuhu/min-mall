<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { api, query } from '../shared/api';
import type { Entry, Page, Product } from '../shared/types';
import { money, toFen } from '../shared/format';
import { useTask } from '../shared/useTask';
import Feedback from '../shared/Feedback.vue';
import Pager from './Pager.vue';
interface EditSku {
  skuId: string | null;
  specificationValueIds: string[];
  price: string;
}
const rows = ref<Product[]>([]),
  total = ref(0),
  page = ref(1),
  categories = ref<Entry[]>([]),
  brands = ref<Entry[]>([]),
  values = ref<Entry[]>([]),
  specs = ref<Entry[]>([]);
const dialog = ref(false),
  form = reactive({
    spuId: '',
    title: '',
    categoryId: '',
    brandId: '',
    images: '',
    skus: [] as EditSku[],
  });
const disabledSkus = ref<EditSku[]>([]);
const { busy, error, notice, run } = useTask();
async function refresh() {
  const data = await api<Page<Product>>('admin', '/products' + query({ page: page.value }));
  rows.value = data.items;
  total.value = data.total;
}
const load = () => run(refresh);
onMounted(() =>
  run(async () => {
    [categories.value, brands.value, values.value, specs.value] = await Promise.all(
      ['categories', 'brands', 'specification-values', 'specifications'].map((k) =>
        api<Entry[]>('admin', `/catalog/${k}`),
      ),
    );
    await refresh();
  }),
);
function create() {
  disabledSkus.value = [];
  Object.assign(form, {
    spuId: '',
    title: '',
    categoryId: '',
    brandId: '',
    images: '',
    skus: [{ skuId: null, specificationValueIds: [], price: '0.00' }],
  });
  dialog.value = true;
}
function edit(row: Product) {
  void run(async () => {
    const result = await api<{
      product: Product;
      skus: {
        skuId: string;
        specificationValueIds: string[];
        priceFen: number;
        enabled: boolean;
      }[];
    }>('admin', `/products/${row.id}/editor`);
    Object.assign(form, {
      spuId: row.id,
      title: result.product.title,
      categoryId: result.product.categoryId,
      brandId: result.product.brandId || '',
      images: result.product.imageUrls.join('\n'),
      skus: result.skus
        .filter((s) => s.enabled)
        .map((s) => ({ ...s, price: (s.priceFen / 100).toFixed(2) })),
    });
    disabledSkus.value = result.skus
      .filter((s) => !s.enabled)
      .map((s) => ({ ...s, price: (s.priceFen / 100).toFixed(2) }));
    dialog.value = true;
  });
}
function save() {
  void run(async () => {
    await api('admin', '/products', {
      method: 'POST',
      body: {
        spuId: form.spuId || null,
        title: form.title,
        categoryId: form.categoryId,
        brandId: form.brandId || null,
        imageUrls: form.images
          .split('\n')
          .map((s) => s.trim())
          .filter(Boolean),
        skus: form.skus.map((s) => ({
          skuId:
            s.skuId ||
            disabledSkus.value.find(
              (old) =>
                [...old.specificationValueIds].sort().join(',') ===
                [...s.specificationValueIds].sort().join(','),
            )?.skuId ||
            null,
          specificationValueIds: s.specificationValueIds,
          priceFen: toFen(s.price),
        })),
      },
    });
    dialog.value = false;
    await refresh();
  }, '商品已保存；新商品需要设置库存并上架后才能购买');
}
function sale(row: Product) {
  if (confirm(`确定${row.onSale ? '下架' : '上架'}“${row.title}”？`))
    void run(async () => {
      await api('admin', `/products/${row.id}/sale`, {
        method: 'PUT',
        body: { onSale: !row.onSale },
      });
      await refresh();
    }, '商品状态已更新');
}
</script>
<template>
  <div class="admin-toolbar">
    <h1>商品管理</h1>
    <button class="button secondary small" :disabled="busy" @click="load">刷新</button
    ><button class="button small" :disabled="busy" @click="create">新增商品</button>
  </div>
  <Feedback :busy="busy" :error="!dialog ? error : ''" :notice="notice" />
  <section class="panel">
    <el-table :data="rows" empty-text="还没有商品，先新增一件好物"
      ><el-table-column prop="id" label="编号" width="90" /><el-table-column
        prop="title"
        label="商品名称"
        min-width="180"
      /><el-table-column label="价格" width="120"
        ><template #default="{ row }">{{
          money(Math.min(...row.skus.filter((s: any) => s.enabled).map((s: any) => s.priceFen)))
        }}</template></el-table-column
      ><el-table-column label="状态" width="100"
        ><template #default="{ row }"
          ><el-tag :type="row.onSale ? 'success' : 'info'">{{
            row.onSale ? '已上架' : '未上架'
          }}</el-tag></template
        ></el-table-column
      ><el-table-column label="操作" width="185"
        ><template #default="{ row }"
          ><el-button link :disabled="busy" @click="edit(row)">编辑</el-button
          ><el-button link :disabled="busy" @click="sale(row)">{{
            row.onSale ? '下架' : '上架'
          }}</el-button
          ><RouterLink :to="`/admin/stock?productId=${row.id}`">库存</RouterLink></template
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
  <el-dialog
    v-model="dialog"
    :title="form.spuId ? '编辑商品' : '新增商品'"
    width="760px"
    :close-on-click-modal="false"
    ><Feedback :busy="busy" :error="error" />
    <form class="form-stack" @submit.prevent="save">
      <div class="form-grid">
        <label class="field full"
          ><span>商品名称</span><input v-model="form.title" required maxlength="200" /></label
        ><label class="field"
          ><span>分类</span
          ><select v-model="form.categoryId" required aria-label="分类">
            <option value="">请选择</option>
            <option v-for="c in categories" :key="c.id" :value="c.id">{{ c.name }}</option>
          </select></label
        ><label class="field"
          ><span>品牌</span
          ><select v-model="form.brandId" aria-label="品牌">
            <option value="">无品牌</option>
            <option v-for="b in brands" :key="b.id" :value="b.id">{{ b.name }}</option>
          </select></label
        ><label class="field full"
          ><span>商品图片地址（每行一个 HTTP(S) 地址，最多 10 张）</span
          ><textarea v-model="form.images" placeholder="https://example.com/product.jpg" />
        </label>
      </div>
      <h3>SKU 与价格</h3>
      <p class="inline-note">
        已有 SKU 的规格组合不可更改；删除一行会停用该 SKU。添加规格需先到分类与规格页面创建。
      </p>
      <div v-for="(sku, index) in form.skus" :key="index" class="sku-editor">
        <div class="form-grid">
          <label class="field"
            ><span>规格组合</span
            ><el-select
              v-model="sku.specificationValueIds"
              multiple
              :disabled="!!sku.skuId"
              placeholder="不选即默认规格"
              ><el-option
                v-for="v in values"
                :key="v.id"
                :label="`${specs.find((s) => s.id === v.parentId)?.name || ''}：${v.name}`"
                :value="v.id" /></el-select></label
          ><label class="field"
            ><span>售价（元）</span
            ><input v-model="sku.price" required type="number" min="0" max="1000000" step="0.01"
          /></label>
        </div>
        <button
          type="button"
          class="text-button"
          :disabled="form.skus.length <= 1"
          @click="form.skus.splice(index, 1)"
        >
          移除此 SKU
        </button>
      </div>
      <div
        v-if="disabledSkus.some((s) => !form.skus.some((active) => active.skuId === s.skuId))"
        class="panel"
      >
        <h4>恢复已停用规格</h4>
        <p class="inline-note">恢复会沿用原 SKU 和库存，不新建重复规格。</p>
        <button
          v-for="s in disabledSkus.filter(
            (s) => !form.skus.some((active) => active.skuId === s.skuId),
          )"
          :key="s.skuId!"
          type="button"
          class="button secondary small"
          @click="form.skus.push({ ...s, specificationValueIds: [...s.specificationValueIds] })"
        >
          恢复
          {{
            s.specificationValueIds
              .map((id) => values.find((v) => v.id === id)?.name || id)
              .join(' / ') || '默认规格'
          }}
        </button>
      </div>
      <button
        type="button"
        class="button secondary small"
        @click="form.skus.push({ skuId: null, specificationValueIds: [], price: '0.00' })"
      >
        添加 SKU</button
      ><button class="button" :disabled="busy">保存商品</button>
    </form></el-dialog
  >
</template>
