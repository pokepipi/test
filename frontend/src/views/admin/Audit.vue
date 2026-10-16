<template>
  <div class="card">
    <div class="toolbar">
      <el-radio-group v-model="status" @change="load(1)">
        <el-radio-button value="-1">全部</el-radio-button>
        <el-radio-button value="0">待审核</el-radio-button>
        <el-radio-button value="4">已驳回</el-radio-button>
      </el-radio-group>
      <el-button type="primary" :icon="Refresh" circle @click="load(1)" />
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
              <div class="muted" style="max-width: 260px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap">
                {{ row.description }}
              </div>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="卖家" prop="sellerName" width="100" />
      <el-table-column label="价格" width="100">
        <template #default="{ row }"><span class="price-text">￥{{ formatPrice(row.price) }}</span></template>
      </el-table-column>
      <el-table-column label="敏感词" width="110">
        <template #default="{ row }">
          <el-tag v-if="row.sensitiveHit === 1" type="danger">命中敏感词</el-tag>
          <el-tag v-else type="success">正常</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="发布时间" width="160">
        <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.status === 0" size="small" type="success" @click="approve(row)">通过</el-button>
          <el-button v-if="row.status === 0" size="small" type="danger" @click="openReject(row)">驳回</el-button>
          <el-button v-if="row.status === 4" size="small" type="primary" plain @click="approve(row)">恢复上架</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager" v-if="total > 0">
      <el-pagination background layout="prev, pager, next, total" :total="total" :page-size="query.size"
                     :current-page="query.page" @current-change="load" />
    </div>

    <!-- 驳回原因 -->
    <el-dialog v-model="rejectDialog" title="驳回物品" width="420px">
      <el-input v-model="rejectReason" type="textarea" :rows="3" placeholder="请输入驳回原因（将展示给卖家）" />
      <template #footer>
        <el-button @click="rejectDialog = false">取消</el-button>
        <el-button type="danger" @click="reject">确认驳回</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { getAuditPage, auditItem } from '../../api'
import { formatPrice, formatTime } from '../../utils/format'

const status = ref('0')
const list = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ page: 1, size: 10 })

const rejectDialog = ref(false)
const rejectReason = ref('')
let rejectTarget = null

async function load(page) {
  query.page = page || 1
  loading.value = true
  try {
    const params = { page: query.page, size: query.size }
    if (status.value !== '-1') params.status = Number(status.value)
    const res = await getAuditPage(params)
    list.value = res.records
    total.value = Number(res.total)
  } finally {
    loading.value = false
  }
}

async function approve(row) {
  await ElMessageBox.confirm(`确认通过「${row.title}」的审核？`, '审核', { type: 'success' })
  await auditItem(row.id, { action: 'approve' })
  ElMessage.success('已通过')
  load(query.page)
}

function openReject(row) {
  rejectTarget = row
  rejectReason.value = ''
  rejectDialog.value = true
}

async function reject() {
  if (!rejectReason.value.trim()) {
    ElMessage.warning('请输入驳回原因')
    return
  }
  await auditItem(rejectTarget.id, { action: 'reject', reason: rejectReason.value.trim() })
  ElMessage.success('已驳回')
  rejectDialog.value = false
  load(query.page)
}

onMounted(() => load(1))
</script>

<style scoped>
.toolbar { display: flex; justify-content: space-between; margin-bottom: 12px; }
.item-cell { display: flex; align-items: center; gap: 10px; }
.thumb {
  width: 56px; height: 56px; border-radius: 6px; background: #f2f3f5;
  display: flex; align-items: center; justify-content: center; color: #c0c4cc; font-size: 12px;
}
</style>
