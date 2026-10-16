<template>
  <div class="page-container">
    <div class="card">
      <div class="head">
        <h2>我的物品</h2>
        <el-button type="primary" :icon="Plus" @click="$router.push('/publish')">发布物品</el-button>
      </div>

      <el-tabs v-model="status" @tab-change="load(1)">
        <el-tab-pane label="全部" name="-1" />
        <el-tab-pane label="待审核" name="0" />
        <el-tab-pane label="在售" name="1" />
        <el-tab-pane label="已下架" name="2" />
        <el-tab-pane label="已售出" name="3" />
        <el-tab-pane label="已驳回" name="4" />
      </el-tabs>

      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column label="物品" min-width="260">
          <template #default="{ row }">
            <div class="item-cell">
              <el-image v-if="row.images" :src="String(row.images).split(',')[0]" fit="cover"
                        style="width: 60px; height: 60px; border-radius: 6px" />
              <div v-else class="thumb">无图</div>
              <span>{{ row.title }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="分类" prop="categoryName" width="110" />
        <el-table-column label="价格" width="100">
          <template #default="{ row }"><span class="price-text">￥{{ formatPrice(row.price) }}</span></template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusMap[row.status]?.type || 'info'">{{ statusMap[row.status]?.text }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="浏览量" prop="viewCount" width="90" />
        <el-table-column label="发布时间" width="160">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="$router.push('/publish/edit/' + row.id)">编辑</el-button>
            <el-button v-if="row.status === 2" size="small" type="success" @click="changeStatus(row, 1)">上架</el-button>
            <el-button v-if="row.status === 1" size="small" type="warning" @click="changeStatus(row, 2)">下架</el-button>
            <el-button size="small" type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager" v-if="total > 0">
        <el-pagination background layout="prev, pager, next, total" :total="total" :page-size="query.size"
                       :current-page="query.page" @current-change="load" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getMyItems, changeItemStatus, deleteItem } from '../api'
import { formatPrice, formatTime } from '../utils/format'

const router = useRouter()
const status = ref('-1')
const list = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ page: 1, size: 10 })

const statusMap = {
  0: { text: '待审核', type: 'warning' },
  1: { text: '在售', type: 'success' },
  2: { text: '已下架', type: 'info' },
  3: { text: '已售出', type: 'danger' },
  4: { text: '已驳回', type: 'danger' }
}

async function load(page) {
  query.page = page || 1
  loading.value = true
  try {
    const params = { page: query.page, size: query.size }
    if (status.value !== '-1') params.status = Number(status.value)
    const res = await getMyItems(params)
    list.value = res.records
    total.value = Number(res.total)
  } finally {
    loading.value = false
  }
}

async function changeStatus(row, target) {
  await changeItemStatus(row.id, target)
  ElMessage.success(target === 1 ? '已上架' : '已下架')
  load(query.page)
}

async function remove(row) {
  await ElMessageBox.confirm(`确定删除「${row.title}」吗？`, '提示', { type: 'warning' })
  await deleteItem(row.id)
  ElMessage.success('已删除')
  load(1)
}

onMounted(() => load(1))
</script>

<style scoped>
.head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.item-cell { display: flex; align-items: center; gap: 10px; }
.thumb {
  width: 60px; height: 60px; border-radius: 6px; background: #f2f3f5;
  display: flex; align-items: center; justify-content: center; color: #c0c4cc; font-size: 12px;
}
</style>
