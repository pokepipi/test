<template>
  <div class="card">
    <div class="toolbar">
      <el-input v-model="keyword" placeholder="搜索用户名/昵称" clearable style="width: 240px"
                @keyup.enter="load(1)" @clear="load(1)" />
      <el-button type="primary" @click="load(1)">搜索</el-button>
    </div>

    <el-table :data="list" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column label="用户" min-width="180">
        <template #default="{ row }">
          <div class="user-cell">
            <el-avatar :size="32" :src="row.avatar || ''">{{ (row.nickname || '?').slice(0, 1) }}</el-avatar>
            <div>
              <div>{{ row.nickname }}</div>
              <div class="muted">{{ row.username }}</div>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="角色" width="100">
        <template #default="{ row }">
          <el-tag :type="row.role === 'ADMIN' ? 'danger' : 'primary'">
            {{ row.role === 'ADMIN' ? '管理员' : '学生' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="phone" label="手机号" width="130" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">
            {{ row.status === 1 ? '正常' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="注册时间" width="160">
        <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <template v-if="row.role !== 'ADMIN'">
            <el-button v-if="row.status === 1" size="small" type="danger" plain @click="toggle(row, 0)">禁用</el-button>
            <el-button v-else size="small" type="success" plain @click="toggle(row, 1)">启用</el-button>
          </template>
          <span v-else class="muted">-</span>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager" v-if="total > 0">
      <el-pagination background layout="prev, pager, next, total" :total="total" :page-size="query.size"
                     :current-page="query.page" @current-change="load" />
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAdminUsers, updateUserStatus } from '../../api'
import { formatTime } from '../../utils/format'

const keyword = ref('')
const list = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ page: 1, size: 10 })

async function load(page) {
  query.page = page || 1
  loading.value = true
  try {
    const res = await getAdminUsers({
      page: query.page,
      size: query.size,
      keyword: keyword.value || undefined
    })
    list.value = res.records
    total.value = Number(res.total)
  } finally {
    loading.value = false
  }
}

async function toggle(row, status) {
  await ElMessageBox.confirm(
    `确认${status === 1 ? '启用' : '禁用'}用户「${row.nickname}」？`,
    '提示', { type: 'warning' }
  )
  await updateUserStatus(row.id, status)
  ElMessage.success('操作成功')
  load(query.page)
}

onMounted(() => load(1))
</script>

<style scoped>
.toolbar { display: flex; gap: 10px; margin-bottom: 12px; }
.user-cell { display: flex; align-items: center; gap: 8px; }
</style>
