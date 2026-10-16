<template>
  <div class="card">
    <div class="toolbar">
      <el-input v-model="keyword" placeholder="搜索物品标题" clearable style="width: 220px"
                @keyup.enter="load(1)" @clear="load(1)" />
      <el-select v-model="status" placeholder="全部状态" clearable style="width: 140px" @change="load(1)">
        <el-option v-for="(label, key) in statusMap" :key="key" :label="label" :value="Number(key)" />
      </el-select>
      <el-button type="primary" @click="load(1)">搜索</el-button>
    </div>

    <el-table :data="list" v-loading="loading" stripe>
      <el-table-column label="物品" min-width="240">
        <template #default="{ row }">
          <div class="item-cell">
            <el-image v-if="row.images" :src="String(row.images).split(',')[0]" fit="cover"
                      style="width: 56px; height: 56px; border-radius: 6px" />
            <div v-else class="thumb">无图</div>
            <div>
              <div>{{ row.title }}</div>
              <div class="muted">{{ row.sellerName }}</div>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="分类" prop="categoryName" width="100" />
      <el-table-column label="价格" width="100">
        <template #default="{ row }"><span class="price-text">￥{{ formatPrice(row.price) }}</span></template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusType[row.status] || 'info'">{{ statusMap[row.status] }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="浏览量" prop="viewCount" width="90" />
      <el-table-column label="发布时间" width="160">
        <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.status === 1" size="small" type="warning" plain @click="toggle(row, 2)">下架</el-button>
          <el-button v-if="row.status === 2" size="small" type="success" plain @click="toggle(row, 1)">恢复上架</el-button>
          <el-button v-if="row.status === 0" size="small" type="success" plain @click="toggle(row, 1)">直接通过</el-button>
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
import { getAdminItems, updateAdminItemStatus } from '../../api'
import { formatPrice, formatTime } from '../../utils/format'

const keyword = ref('')
const status = ref(null)
const list = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ page: 1, size: 10 })

const statusMap = { 0: '待审核', 1: '在售', 2: '已下架', 3: '已售出', 4: '已驳回' }
const statusType = { 0: 'warning', 1: 'success', 2: 'info', 3: 'danger', 4: 'danger' }

async function load(page) {
  query.page = page || 1
  loading.value = true
  try {
    const res = await getAdminItems({
      page: query.page,
      size: query.size,
      keyword: keyword.value || undefined,
      status: status.value ?? undefined
    })
    list.value = res.records
    total.value = Number(res.total)
  } finally {
    loading.value = false
  }
}

async function toggle(row, target) {
  const label = { 1: '恢复上架', 2: '下架' }[target]
  await ElMessageBox.confirm(`确认${label}「${row.title}」？`, '提示', { type: 'warning' })
  await updateAdminItemStatus(row.id, target)
  ElMessage.success('操作成功')
  load(query.page)
}

onMounted(() => load(1))
</script>

<style scoped>
.toolbar { display: flex; gap: 10px; margin-bottom: 12px; }
.item-cell { display: flex; align-items: center; gap: 10px; }
.thumb {
  width: 56px; height: 56px; border-radius: 6px; background: #f2f3f5;
  display: flex; align-items: center; justify-content: center; color: #c0c4cc; font-size: 12px;
}
</style>
