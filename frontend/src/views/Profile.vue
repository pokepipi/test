<template>
  <div class="page-container">
    <el-row :gutter="16">
      <!-- 基本资料 -->
      <el-col :xs="24" :md="12">
        <div class="card">
          <h3 class="sec-title">基本资料</h3>
          <el-form :model="profile" label-width="90px" style="max-width: 420px">
            <el-form-item label="头像">
              <div class="avatar-wrap">
                <el-avatar :size="72" :src="profile.avatar || ''">{{ (profile.nickname || '?').slice(0, 1) }}</el-avatar>
                <el-button size="small" @click="avatarInput.click()">更换头像</el-button>
                <input ref="avatarInput" type="file" accept="image/*" style="display: none" @change="uploadAvatar" />
              </div>
            </el-form-item>
            <el-form-item label="用户名">
              <el-input :model-value="userStore.userInfo?.username" disabled />
            </el-form-item>
            <el-form-item label="昵称">
              <el-input v-model="profile.nickname" maxlength="20" />
            </el-form-item>
            <el-form-item label="手机号">
              <el-input v-model="profile.phone" />
            </el-form-item>
            <el-form-item label="邮箱">
              <el-input v-model="profile.email" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="saving" @click="saveProfile">保存</el-button>
            </el-form-item>
          </el-form>
        </div>
      </el-col>

      <!-- 修改密码 -->
      <el-col :xs="24" :md="12">
        <div class="card">
          <h3 class="sec-title">修改密码</h3>
          <el-form :model="pwdForm" :rules="pwdRules" ref="pwdRef" label-width="90px" style="max-width: 420px">
            <el-form-item label="原密码" prop="oldPassword">
              <el-input v-model="pwdForm.oldPassword" type="password" show-password />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input v-model="pwdForm.newPassword" type="password" show-password />
            </el-form-item>
            <el-form-item label="确认密码" prop="confirm">
              <el-input v-model="pwdForm.confirm" type="password" show-password />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="savingPwd" @click="savePassword">修改密码</el-button>
            </el-form-item>
          </el-form>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { updateProfile, changePassword, uploadImage } from '../api'
import { useUserStore } from '../store/user'

const router = useRouter()
const userStore = useUserStore()

const profile = reactive({ nickname: '', phone: '', email: '', avatar: '' })
const saving = ref(false)

const pwdForm = reactive({ oldPassword: '', newPassword: '', confirm: '' })
const pwdRef = ref(null)
const savingPwd = ref(false)
const avatarInput = ref(null)

const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '长度 6-20 位', trigger: 'blur' }
  ],
  confirm: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (rule, value, cb) =>
        value === pwdForm.newPassword ? cb() : cb(new Error('两次密码不一致')),
      trigger: 'blur'
    }
  ]
}

onMounted(() => {
  const u = userStore.userInfo || {}
  profile.nickname = u.nickname || ''
  profile.phone = u.phone || ''
  profile.email = u.email || ''
  profile.avatar = u.avatar || ''
})

async function uploadAvatar(e) {
  const file = e.target.files[0]
  e.target.value = ''
  if (!file) return
  profile.avatar = await uploadImage(file)
  ElMessage.success('头像已上传，请点击保存')
}

async function saveProfile() {
  saving.value = true
  try {
    await updateProfile(profile)
    await userStore.fetchMe()
    ElMessage.success('保存成功')
  } catch (err) { /* ignore */ }
  saving.value = false
}

async function savePassword() {
  await pwdRef.value.validate()
  savingPwd.value = true
  try {
    await changePassword({ oldPassword: pwdForm.oldPassword, newPassword: pwdForm.newPassword })
    ElMessage.success('密码修改成功，请重新登录')
    pwdForm.oldPassword = ''
    pwdForm.newPassword = ''
    pwdForm.confirm = ''
    userStore.logout()
    router.push('/login')
  } catch (err) { /* ignore */ }
  savingPwd.value = false
}
</script>

<style scoped>
.sec-title { margin-bottom: 16px; }
.avatar-wrap { display: flex; align-items: center; gap: 12px; }
</style>
