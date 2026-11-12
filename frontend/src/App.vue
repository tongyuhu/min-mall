<script setup lang="ts">
import { onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
const router = useRouter();
function expired(event: Event) {
  const scope = (event as CustomEvent).detail;
  const current = router.currentRoute.value;
  if ((scope === 'admin') !== current.path.startsWith('/admin')) return;
  void router.push({
    path: scope === 'admin' ? '/admin/login' : '/login',
    query: { redirect: current.fullPath, expired: '1' },
  });
}
onMounted(() => window.addEventListener('session-expired', expired));
onUnmounted(() => window.removeEventListener('session-expired', expired));
</script>
<template><RouterView /></template>
