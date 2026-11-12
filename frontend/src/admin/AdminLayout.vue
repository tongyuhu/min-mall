<script setup lang="ts">
import { getCurrentInstance } from 'vue';
import { useRouter } from 'vue-router';
import ElementPlus from 'element-plus';
import zhCn from 'element-plus/es/locale/lang/zh-cn';
import 'element-plus/dist/index.css';
import {
  LayoutDashboard,
  Package,
  Layers,
  Warehouse,
  ShoppingBag,
  RotateCcw,
  Ticket,
  Users,
  Shield,
  ListChecks,
  ClipboardList,
  Leaf,
} from 'lucide-vue-next';
import { api } from '../shared/api';
import { session } from '../shared/session';
import { adminIdentity } from './identity';
getCurrentInstance()!.appContext.app.use(ElementPlus, { locale: zhCn });
const router = useRouter();
const links = [
  { path: '/admin', title: '经营概览', icon: LayoutDashboard, permissions: [] },
  { path: '/admin/products', title: '商品管理', icon: Package, permissions: ['catalog:write'] },
  { path: '/admin/catalog', title: '分类与规格', icon: Layers, permissions: ['catalog:write'] },
  { path: '/admin/stock', title: '库存管理', icon: Warehouse, permissions: ['stock:adjust'] },
  { path: '/admin/orders', title: '订单管理', icon: ShoppingBag, permissions: ['order:read'] },
  {
    path: '/admin/after-sales',
    title: '售后管理',
    icon: RotateCcw,
    permissions: ['aftersale:review'],
  },
  { path: '/admin/coupons', title: '优惠券管理', icon: Ticket, permissions: ['catalog:write'] },
  { path: '/admin/members', title: '会员管理', icon: Users, permissions: ['member:write'] },
  { path: '/admin/access', title: '账号与权限', icon: Shield, permissions: ['rbac:write'] },
  {
    path: '/admin/tasks',
    title: '后台任务',
    icon: ListChecks,
    permissions: ['order:read', 'task:retry'],
  },
  { path: '/admin/audits', title: '操作审计', icon: ClipboardList, permissions: ['audit:read'] },
];
async function logout() {
  try {
    await api('admin', '/auth/logout', { method: 'POST' });
  } finally {
    session.clear('admin');
    adminIdentity.clear();
    await router.push('/admin/login');
  }
}
</script>
<template>
  <div class="admin-shell">
    <aside class="admin-sidebar">
      <RouterLink to="/admin" class="brand"><Leaf :size="24" /><span>小满工作台</span></RouterLink>
      <div class="nav-caption">自营商城 / 商家管理</div>
      <nav aria-label="后台导航">
        <template v-for="link in links" :key="link.path"
          ><RouterLink
            v-if="!link.permissions.length || link.permissions.some(adminIdentity.can)"
            :to="link.path"
            :title="link.title"
            ><component :is="link.icon" :size="18" /><span>{{ link.title }}</span></RouterLink
          ></template
        >
      </nav>
    </aside>
    <div class="admin-main">
      <header class="admin-header">
        <span>商家工作台 <span> / 自营商城</span></span>
        <div class="actions" style="margin: 0">
          <RouterLink to="/">访问商城 ↗</RouterLink
          ><RouterLink to="/admin/password">{{ adminIdentity.profile.value?.username }}</RouterLink
          ><button class="text-button" @click="logout">退出</button>
        </div>
      </header>
      <main class="admin-content"><RouterView :key="$route.path" /></main>
    </div>
  </div>
</template>
