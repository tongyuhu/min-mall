<script setup lang="ts">
import { ShoppingBag, UserRound, ArrowUpRight, Leaf } from 'lucide-vue-next';
</script>
<template>
  <div class="store-shell">
    <div class="top-note">认真挑选，慢慢生活 <span>单商家自营 · 用心做好每一份订单</span></div>
    <header class="store-header container">
      <RouterLink to="/" class="brand"
        ><span class="brand-mark"><Leaf :size="22" /></span
        ><span>小满生活<small>MINI MALL</small></span></RouterLink
      >
      <nav aria-label="商城导航">
        <RouterLink to="/" exact-active-class="active">首页</RouterLink
        ><RouterLink to="/products">全部好物</RouterLink
        ><RouterLink to="/coupons">领券中心</RouterLink
        ><RouterLink to="/orders">我的订单</RouterLink>
      </nav>
      <div class="header-actions">
        <RouterLink to="/cart" aria-label="购物车"><ShoppingBag :size="21" /></RouterLink
        ><RouterLink to="/account" aria-label="个人中心"><UserRound :size="21" /></RouterLink>
      </div>
    </header>
    <main><RouterView :key="$route.path" /></main>
    <footer class="store-footer">
      <div class="container footer-grid">
        <div>
          <div class="brand">小满生活 <Leaf :size="22" /></div>
          <p>给日常一点恰到好处的美好。</p>
          <small>Mini Mall · 全链路商城学习项目</small>
        </div>
        <div>
          <h4>购物与服务</h4>
          <RouterLink to="/products">发现好物</RouterLink
          ><RouterLink to="/orders">订单与物流</RouterLink
          ><RouterLink to="/after-sales">售后服务</RouterLink>
        </div>
        <div>
          <h4>我的小满</h4>
          <RouterLink to="/account/addresses">收货地址</RouterLink
          ><RouterLink to="/coupons">我的优惠券</RouterLink
          ><RouterLink to="/admin">商家工作台 <ArrowUpRight :size="13" /></RouterLink>
        </div>
      </div>
      <div class="container footer-bottom">
        本地学习环境 · 支付与物流采用模拟服务，不产生真实资金交易。
      </div>
    </footer>
  </div>
</template>
