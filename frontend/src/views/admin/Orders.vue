<template>
  <div class="card">
    <div class="toolbar">
      <el-select v-model="status" placeholder="全部状态" clearable style="width: 140px" @change="load(1)">
        <el-option v-for="(label, key) in statusMap" :key="key" :label="label" :value="Number(key)" />
      </el-select>
      <el-input v-model="keyword" placeholder="搜索订单号/收货人" clearable style="width: 220px"
                @keyup.enter="load(1)" @clear="load(1)" />
      <el-button type="primary" @click="load(1)">搜索</el-button>
    </div>

    <el-table :data="list" v-loading="loading" stripe>
      <el-table-column prop="orderNo" label="订单号" width="190" />
      <el-table-column label="物品" min-width="200">
        <template #default="{ row }">
          <div class="item-cell">
            <el-image v-if="row.itemImage" :src="row.itemImage" fit="cover"
                      style="width: 48px; height: 48px; border-radius: 6px" />
            <div v-else class="thumb">无图</div>
            <span>{{ row.itemTitle }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="买家" prop="buyerName" width="100" />
      <el-table-column label="卖家" prop="sellerName" width="100" />
      <el-table-column label="金额" width="100">
        <template #default="{ row }"><span class="price-text">￥{{ formatPrice(row.price) }}</span></template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusType[row.status] || 'info'">{{ statusMap[row.status] }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="下单时间" width="160">
        <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="showDetail(row)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager" v-if="total > 0">
      <el-pagination background layout="prev, pager, next, total" :total="total" :page-size="query.size"
                     :current-page="query.page" @current-change="load" />
    </div>

    <el-dialog v-model="detailDialog" title="订单详情" width="520px">
      <el-descriptions v-if="current" :column="1" border>
        <el-descriptions-item label="订单号">{{ current.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="物品">{{ current.itemTitle }}</el-descriptions-item>
        <el-descriptions-item label="买家">{{ current.buyerName }}</el-descriptions-item>
        <el-descriptions-item label="卖家">{{ current.sellerName }}</el-descriptions-item>
        <el-descriptions-item label="成交价">￥{{ formatPrice(current.price) }}</el-descriptions-item>
        <el-descriptions-item label="收货人">{{ current.contactName }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ current.contactPhone }}</el-descriptions-item>
        <el-descriptions-item label="收货地址">{{ current.address }}</el-descriptions-item>
        <el-descriptions-item label="备注">{{ current.remark || '-' }}</el-descriptions-item>
        <el-descriptions-item label="支付时间">{{ formatTime(current.payTime) || '-' }}</el-descriptions-item>
        <el-descriptions-item label="发货时间">{{ formatTime(current.shipTime) || '-' }}</el-descriptions-item>
        <el-descriptions-item label="完成时间">{{ formatTime(current.finishTime) || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { getAdminOrders, getOrderDetail } from '../../api'
import { formatPrice, formatTime } from '../../utils/format'

const status = ref(null)
const keyword = ref('')
const list = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ page: 1, size: 10 })

const detailDialog = ref(false)
const current = ref(null)

const statusMap = { 0: '待付款', 1: '待发货', 2: '待收货', 3: '已完成', 4: '已取消' }
const statusType = { 0: 'warning', 1: 'primary', 2: 'primary', 3: 'success', 4: 'info' }

async function load(page) {
  query.page = page || 1
  loading.value = true
  try {
    const res = await getAdminOrders({
      page: query.page,
      size: query.size,
      status: status.value ?? undefined,
      keyword: keyword.value || undefined
    })
    list.value = res.records
    total.value = Number(res.total)
  } finally {
    loading.value = false
  }
}

async function showDetail(row) {
  current.value = await getOrderDetail(row.id)
  detailDialog.value = true
}

onMounted(() => load(1))
</script>

<style scoped>
.toolbar { display: flex; gap: 10px; margin-bottom: 12px; }
.item-cell { display: flex; align-items: center; gap: 10px; }
.thumb {
  width: 48px; height: 48px; border-radius: 6px; background: #f2f3f5;
  display: flex; align-items: center; justify-content: center; color: #c0c4cc; font-size: 12px;
}
</style>
