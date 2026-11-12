import { createRouter, createWebHistory } from 'vue-router';
import { session } from './shared/session';
import { adminIdentity } from './admin/identity';

export const router = createRouter({
  history: createWebHistory(),
  scrollBehavior: () => ({ top: 0 }),
  routes: [
    {
      path: '/',
      component: () => import('./storefront/StoreLayout.vue'),
      children: [
        { path: '', component: () => import('./storefront/CatalogPage.vue') },
        { path: 'products', component: () => import('./storefront/CatalogPage.vue') },
        { path: 'products/:id', component: () => import('./storefront/ProductPage.vue') },
        { path: 'login', component: () => import('./storefront/LoginPage.vue') },
        { path: 'register', component: () => import('./storefront/LoginPage.vue') },
        {
          path: 'cart',
          component: () => import('./storefront/CartPage.vue'),
          meta: { buyer: true },
        },
        {
          path: 'checkout',
          component: () => import('./storefront/CheckoutPage.vue'),
          meta: { buyer: true },
        },
        {
          path: 'orders',
          component: () => import('./storefront/OrdersPage.vue'),
          meta: { buyer: true },
        },
        {
          path: 'orders/:no',
          component: () => import('./storefront/OrderPage.vue'),
          meta: { buyer: true },
        },
        {
          path: 'after-sales',
          component: () => import('./storefront/AfterSalesPage.vue'),
          meta: { buyer: true },
        },
        {
          path: 'account/:tab?',
          component: () => import('./storefront/AccountPage.vue'),
          meta: { buyer: true },
        },
        {
          path: 'coupons',
          component: () => import('./storefront/CouponsPage.vue'),
          meta: { buyer: true },
        },
      ],
    },
    { path: '/admin/login', component: () => import('./storefront/LoginPage.vue') },
    {
      path: '/admin',
      component: () => import('./admin/AdminLayout.vue'),
      meta: { admin: true },
      children: [
        { path: '', component: () => import('./admin/DashboardPage.vue') },
        {
          path: 'products',
          component: () => import('./admin/ProductsPage.vue'),
          meta: { permission: 'catalog:write' },
        },
        {
          path: 'catalog',
          component: () => import('./admin/TaxonomyPage.vue'),
          meta: { permission: 'catalog:write' },
        },
        {
          path: 'stock',
          component: () => import('./admin/StockPage.vue'),
          meta: { permission: 'stock:adjust' },
        },
        {
          path: 'orders',
          component: () => import('./admin/OrdersPage.vue'),
          meta: { permission: 'order:read' },
        },
        {
          path: 'orders/:no',
          component: () => import('./admin/OrderPage.vue'),
          meta: { permission: 'order:read' },
        },
        {
          path: 'after-sales',
          component: () => import('./admin/AfterSalesPage.vue'),
          meta: { permission: 'aftersale:review' },
        },
        {
          path: 'coupons',
          component: () => import('./admin/CouponsPage.vue'),
          meta: { permission: 'catalog:write' },
        },
        {
          path: 'members',
          component: () => import('./admin/MembersPage.vue'),
          meta: { permission: 'member:write' },
        },
        {
          path: 'access',
          component: () => import('./admin/AccessPage.vue'),
          meta: { permission: 'rbac:write' },
        },
        {
          path: 'tasks',
          component: () => import('./admin/SystemPage.vue'),
          meta: { permission: ['order:read', 'task:retry'] },
        },
        {
          path: 'audits',
          component: () => import('./admin/SystemPage.vue'),
          meta: { permission: 'audit:read' },
        },
        { path: 'password', component: () => import('./admin/PasswordPage.vue') },
        { path: 'forbidden', component: () => import('./shared/NotFound.vue') },
      ],
    },
    { path: '/:pathMatch(.*)*', component: () => import('./shared/NotFound.vue') },
  ],
});
router.beforeEach(async (to) => {
  document.title = `${to.path.startsWith('/admin') ? '商家工作台' : '小满生活'} · Mini Mall`;
  if (to.meta.buyer && !session.get('buyer'))
    return { path: '/login', query: { redirect: to.fullPath } };
  if (to.meta.admin) {
    if (!session.get('admin')) return { path: '/admin/login', query: { redirect: to.fullPath } };
    try {
      await adminIdentity.load();
      const permission = to.meta.permission as string | string[] | undefined;
      if (
        permission &&
        !(Array.isArray(permission) ? permission : [permission]).some(adminIdentity.can)
      )
        return '/admin/forbidden';
    } catch {
      return { path: '/admin/login', query: { redirect: to.fullPath } };
    }
  }
});
