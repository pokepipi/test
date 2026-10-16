<template>
  <el-container class="admin-layout">
    <el-aside width="210px" class="aside">
      <div class="logo">
        <el-icon :size="20"><ShoppingBag /></el-icon>
        <span>校园二手管理后台</span>
      </div>
      <el-menu :default-active="$route.path" router background-color="#001529"
               text-color="#b8c2cc" active-text-color="#ffffff">
        <el-menu-item index="/admin/dashboard"><el-icon><DataAnalysis /></el-icon><span>数据统计</span></el-menu-item>
        <el-menu-item index="/admin/audit"><el-icon><DocumentChecked /></el-icon><span>物品审核</span></el-menu-item>
        <el-menu-item index="/admin/users"><el-icon><User /></el-icon><span>用户管理</span></el-menu-item>
        <el-menu-item index="/admin/items"><el-icon><Goods /></el-icon><span>物品管理</span></el-menu-item>
        <el-menu-item index="/admin/orders"><el-icon><List /></el-icon><span>订单管理</span></el-menu-item>
        <el-menu-item index="/admin/categories"><el-icon><Menu /></el-icon><span>分类管理</span></el-menu-item>
        <el-menu-item index="/admin/words"><el-icon><Warning /></el-icon><span>敏感词管理</span></el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <span class="page-title">{{ $route.meta.title }}</span>
        <div class="header-actions">
          <el-button link @click="$router.push('/')">返回前台</el-button>
          <el-button link type="danger" @click="logout">退出登录</el-button>
        </div>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useUserStore } from '../../store/user'

const router = useRouter()
const userStore = useUserStore()

function logout() {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.admin-layout { height: 100vh; }
.aside { background: #001529; }
.logo {
  height: 60px; display: flex; align-items: center; justify-content: center; gap: 6px;
  color: #fff; font-weight: 700; font-size: 15px;
}
.aside :deep(.el-menu) { border-right: none; }
.header {
  background: #fff; display: flex; align-items: center; justify-content: space-between;
  border-bottom: 1px solid #e4e7ed;
}
.page-title { font-size: 16px; font-weight: 600; }
.main { background: #f5f7fa; }
</style>
