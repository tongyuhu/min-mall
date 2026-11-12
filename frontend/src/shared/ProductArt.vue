<script setup lang="ts">
import { ref, watch } from 'vue';
const props = defineProps<{ src?: string; title?: string; variant?: number }>();
const failed = ref(false);
watch(
  () => props.src,
  () => {
    failed.value = false;
  },
);
</script>
<template>
  <div class="product-art" :class="`art-${(variant ?? 0) % 3}`">
    <img
      v-if="src && !failed"
      :src="src"
      :alt="title || '商品图片'"
      loading="lazy"
      referrerpolicy="no-referrer"
      @error="failed = true"
    />
    <svg v-else viewBox="0 0 400 320" role="img" :aria-label="`${title || '商品'} · 暂无实拍图片`">
      <ellipse cx="211" cy="270" rx="104" ry="17" fill="#000" opacity=".08" />
      <g transform="rotate(-13 200 160)">
        <rect x="117" y="49" width="172" height="215" rx="8" fill="#183d30" />
        <path d="M129 50h147v205H129z" fill="#e7dfc8" />
        <path d="M132 50h145v198H132z" fill="#c1ccaa" />
        <rect x="132" y="50" width="12" height="198" fill="#91a886" />
        <path d="M172 145h66m-66 9h42" stroke="#688064" stroke-width="2" />
        <text x="172" y="133" fill="#39583e" font-size="11" letter-spacing="3">日常手记</text>
        <path d="M251 50v220l-9-7-9 7V50" fill="#c7804d" />
      </g>
      <path d="M301 212q25-42 33-84q-36 8-33 84" fill="#648568" />
      <path d="M305 196q-24-40-40-40q-1 35 40 40" fill="#92a88a" />
    </svg>
    <span v-if="!src || failed" class="art-note">好物示意 · 以商品信息为准</span>
  </div>
</template>
