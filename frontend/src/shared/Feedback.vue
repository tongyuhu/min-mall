<script setup lang="ts">
defineProps<{ busy?: boolean; error?: string; notice?: string }>();
</script>
<template>
  <div v-if="error" role="alert" class="feedback error">{{ error }}</div>
  <div v-if="notice" role="status" class="feedback success">{{ notice }}</div>
  <div v-if="busy" role="status" class="loading"><span class="spinner" /> 正在处理，请稍候…</div>
</template>
