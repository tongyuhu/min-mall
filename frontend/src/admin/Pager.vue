<script setup lang="ts">
defineProps<{ page: number; total: number; busy?: boolean; size?: number }>();
defineEmits<{ change: [page: number] }>();
</script>
<template>
  <div class="pagination">
    <button :disabled="page <= 1 || busy" @click="$emit('change', page - 1)">上一页</button
    ><span>第 {{ page }} 页 · 共 {{ total }} 条</span
    ><button :disabled="page * (size || 20) >= total || busy" @click="$emit('change', page + 1)">
      下一页
    </button>
  </div>
</template>
