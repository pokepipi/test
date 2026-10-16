<template>
  <div class="app">
    <!-- 顶部导航 (管理端使用独立布局, 不显示) -->
    <header class="top-bar" v-if="!$route.meta.admin">
      <div class="top-inner">
        <router-link to="/" class="logo">
          <el-icon :size="22"><ShoppingBag /></el-icon>
          <span>校园二手交易平台</span>
        </router-link>

        <nav class="nav">
          <router-link to="/">首页</router-link>
          <router-link to="/publish">发布物品</router-link>
          <router-link to="/my/items">我的物品</router-link>
          <router-link to="/my/orders">我的订单</router-link>
          <router-link to="/chat" class="nav-chat">
            消息
            <el-badge :value="unread" :hidden="unread === 0" class="unread-badge" />
          </router-link>
        </nav>

        <div class="user-area">
          <template v-if="userStore.isLogin">
            <router-link v-if="userStore.isAdmin" to="/admin/dashboard" class="admin-link">
              <el-icon><DataAnalysis /></el-icon> 管理后台
            </router-link>
            <el-dropdown @command="onCommand">
              <span class="user-name">
                <el-avatar :size="28" :src="userStore.userInfo?.avatar || ''">
                  {{ (userStore.userInfo?.nickname || '?').slice(0, 1) }}
                </el-avatar>
                {{ userStore.userInfo?.nickname }}
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                  <el-dropdown-item command="myItems">我的物品</el-dropdown-item>
                  <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template v-else>
            <router-link to="/login" class="login-btn">登录 / 注册</router-link>
          </template>
        </div>
      </div>
    </header>

    <router-view />
  </div>
</template>

<script setup>
import { onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from './store/user'
import { getUnread } from './api'

const router = useRouter()
const userStore = useUserStore()
const unread = ref(0)

let timer = null

async function loadUnread() {
  if (!userStore.isLogin) return
  try {
    unread.value = await getUnread()
  } catch (e) { /* ignore */ }
}

function onCommand(cmd) {
  if (cmd === 'logout') {
    userStore.logout()
    ElMessage.success('已退出登录')
    router.push('/')
  } else if (cmd === 'profile') {
    router.push('/profile')
  } else if (cmd === 'myItems') {
    router.push('/my/items')
  }
}

onMounted(() => {
  loadUnread()
  timer = setInterval(loadUnread, 30000)
})

onUnmounted(() => clearInterval(timer))
</script>

<style scoped>
.top-bar {
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  position: sticky;
  top: 0;
  z-index: 100;
}
.top-inner {
  max-width: 1200px;
  margin: 0 auto;
  height: 60px;
  display: flex;
  align-items: center;
  gap: 32px;
  padding: 0 16px;
}
.logo {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 18px;
  font-weight: 700;
  color: #409eff;
}
.nav {
  display: flex;
  gap: 24px;
  flex: 1;
}
.nav a {
  color: #606266;
  font-size: 15px;
  line-height: 60px;
  border-bottom: 2px solid transparent;
}
.nav a.router-link-exact-active,
.nav a:hover {
  color: #409eff;
  border-bottom-color: #409eff;
}
.nav-chat { position: relative; }
.unread-badge { position: absolute; top: 8px; margin-left: 2px; }
.user-area { display: flex; align-items: center; gap: 16px; }
.admin-link { color: #e6a23c; font-size: 14px; }
.user-name {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  font-size: 14px;
  color: #303133;
}
.login-btn {
  background: #409eff;
  color: #fff !important;
  padding: 6px 16px;
  border-radius: 4px;
  font-size: 14px;
}
</style>
