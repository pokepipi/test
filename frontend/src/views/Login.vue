<template>
  <div class="login-page">
    <div class="login-card card">
      <div class="login-logo">
        <el-icon :size="30" color="#409eff"><ShoppingBag /></el-icon>
        <h2>校园二手交易平台</h2>
      </div>
      <el-tabs v-model="tab" stretch>
        <el-tab-pane label="登录" name="login">
          <el-form :model="loginForm" :rules="loginRules" label-width="0">
            <el-form-item prop="username">
              <el-input v-model="loginForm.username" placeholder="用户名" :prefix-icon="User" />
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="loginForm.password" type="password" placeholder="密码" show-password
                        :prefix-icon="Lock" @keyup.enter="doLogin" />
            </el-form-item>
            <el-button type="primary" style="width: 100%" :loading="loading" @click="doLogin">登 录</el-button>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="注册" name="register">
          <el-form :model="registerForm" :rules="registerRules" label-width="0">
            <el-form-item prop="username">
              <el-input v-model="registerForm.username" placeholder="用户名(4-20位)" :prefix-icon="User" />
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="registerForm.password" type="password" placeholder="密码(6-20位)" show-password :prefix-icon="Lock" />
            </el-form-item>
            <el-form-item>
              <el-input v-model="registerForm.nickname" placeholder="昵称" :prefix-icon="Postcard" />
            </el-form-item>
            <el-form-item>
              <el-input v-model="registerForm.phone" placeholder="手机号(选填)" :prefix-icon="Iphone" />
            </el-form-item>
            <el-button type="primary" style="width: 100%" :loading="loading" @click="doRegister">注 册</el-button>
          </el-form>
        </el-tab-pane>
      </el-tabs>
      <div class="muted tip">
        演示账号：admin / admin123（管理员）；student / 123456（学生）
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Postcard, Iphone } from '@element-plus/icons-vue'
import { useUserStore } from '../store/user'
import { register } from '../api'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const tab = ref('login')
const loading = ref(false)

const loginForm = reactive({ username: '', password: '' })
const registerForm = reactive({ username: '', password: '', nickname: '', phone: '' })

const loginRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}
const registerRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 4, max: 20, message: '长度 4-20 位', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '长度 6-20 位', trigger: 'blur' }
  ],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }]
}

async function doLogin() {
  loading.value = true
  try {
    await userStore.login(loginForm)
    ElMessage.success('登录成功')
    router.push(route.query.redirect || '/')
  } catch (e) { /* 错误已统一提示 */ }
  loading.value = false
}

async function doRegister() {
  loading.value = true
  try {
    await register(registerForm)
    ElMessage.success('注册成功，请登录')
    tab.value = 'login'
  } catch (e) { /* ignore */ }
  loading.value = false
}
</script>

<style scoped>
.login-page {
  min-height: calc(100vh - 60px);
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #eaf3ff 0%, #f5f7fa 100%);
}
.login-card { width: 420px; padding: 32px; }
.login-logo { display: flex; align-items: center; gap: 8px; justify-content: center; margin-bottom: 8px; }
.login-logo h2 { font-size: 20px; }
.tip { margin-top: 16px; text-align: center; }
</style>
