<template>
  <div class="page-container">
    <div class="card">
      <div class="head">
        <h2>我的订单</h2>
        <el-radio-group v-model="role">
          <el-radio-button value="buyer">我买到的</el-radio-button>
          <el-radio-button value="seller">我卖出的</el-radio-button>
        </el-radio-group>
      </div>

      <el-tabs v-model="status" @tab-change="load(1)">
        <el-tab-pane label="全部" name="-1" />
        <el-tab-pane label="待付款" name="0" />
        <el-tab-pane label="待发货" name="1" />
        <el-tab-pane label="待收货" name="2" />
        <el-tab-pane label="已完成" name="3" />
        <el-tab-pane label="已取消" name="4" />
      </el-tabs>

      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column label="订单号" prop="orderNo" width="180" />
        <el-table-column label="物品" min-width="220">
          <template #default="{ row }">
            <div class="item-cell">
              <el-image v-if="row.itemImage" :src="row.itemImage" fit="cover"
                        style="width: 50px; height: 50px; border-radius: 6px" />
              <div v-else class="thumb">无图</div>
              <span>{{ row.itemTitle }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="金额" width="110">
          <template #default="{ row }"><span class="price-text">￥{{ formatPrice(row.price) }}</span></template>
        </el-table-column>
        <el-table-column :label="role === 'buyer' ? '卖家' : '买家'" width="100">
          <template #default="{ row }">{{ role === 'buyer' ? row.sellerName : row.buyerName }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusMap[row.status]?.type || 'info'">{{ statusMap[row.status]?.text }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="下单时间" width="160">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="showDetail(row)">详情</el-button>
            <el-button v-if="role === 'buyer' && row.status === 0" size="small" type="primary" @click="pay(row)">去支付</el-button>
            <el-button v-if="role === 'buyer' && row.status === 0" size="small" type="danger" plain @click="cancel(row)">取消</el-button>
            <el-button v-if="role === 'seller' && row.status === 1" size="small" type="warning" @click="ship(row)">发货</el-button>
            <el-button v-if="role === 'buyer' && row.status === 2" size="small" type="success" @click="finish(row)">确认收货</el-button>
            <el-button v-if="role === 'seller' && row.status === 1" size="small" type="danger" plain @click="cancel(row)">取消</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager" v-if="total > 0">
        <el-pagination background layout="prev, pager, next, total" :total="total" :page-size="query.size"
                       :current-page="query.page" @current-change="load" />
      </div>
    </div>

    <!-- 订单详情 -->
    <el-dialog v-model="detailDialog" title="订单详情" width="520px">
      <el-descriptions v-if="current" :column="1" border>
        <el-descriptions-item label="订单号">{{ current.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="物品">{{ current.itemTitle }}</el-descriptions-item>
        <el-descriptions-item label="成交价">￥{{ formatPrice(current.price) }}</el-descriptions-item>
        <el-descriptions-item label="收货人">{{ current.contactName }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ current.contactPhone }}</el-descriptions-item>
        <el-descriptions-item label="收货地址">{{ current.address }}</el-descriptions-item>
        <el-descriptions-item label="备注">{{ current.remark || '-' }}</el-descriptions-item>
        <el-descriptions-item label="下单时间">{{ formatTime(current.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="支付时间">{{ formatTime(current.payTime) || '-' }}</el-descriptions-item>
        <el-descriptions-item label="发货时间">{{ formatTime(current.shipTime) || '-' }}</el-descriptions-item>
        <el-descriptions-item label="完成时间">{{ formatTime(current.finishTime) || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getOrderPage, payOrder, cancelOrder, shipOrder, finishOrder, getOrderDetail } from '../api'
import { formatPrice, formatTime } from '../utils/format'

const role = ref('buyer')
const status = ref('-1')
const list = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ page: 1, size: 10 })

const detailDialog = ref(false)
const current = ref(null)

const statusMap = {
  0: { text: '待付款', type: 'warning' },
  1: { text: '待发货', type: 'primary' },
  2: { text: '待收货', type: 'primary' },
  3: { text: '已完成', type: 'success' },
  4: { text: '已取消', type: 'info' }
}

watch(role, () => {
  status.value = '-1'
  load(1)
})

async function load(page) {
  query.page = page || 1
  loading.value = true
  try {
    const params = { role: role.value, page: query.page, size: query.size }
    if (status.value !== '-1') params.status = Number(status.value)
    const res = await getOrderPage(params)
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

async function pay(row) {
  await ElMessageBox.confirm('确认支付该订单？（模拟支付）', '支付确认', { type: 'info' })
  await payOrder(row.id)
  ElMessage.success('支付成功')
  load(query.page)
}

async function cancel(row) {
  await ElMessageBox.confirm('确认取消该订单？', '提示', { type: 'warning' })
  await cancelOrder(row.id)
  ElMessage.success('已取消')
  load(query.page)
}

async function ship(row) {
  await ElMessageBox.confirm('确认已发货？', '提示', { type: 'warning' })
  await shipOrder(row.id)
  ElMessage.success('已发货')
  load(query.page)
}

async function finish(row) {
  await ElMessageBox.confirm('确认已收到物品？', '提示', { type: 'warning' })
  await finishOrder(row.id)
  ElMessage.success('交易完成')
  load(query.page)
}

onMounted(() => load(1))
</script>

<style scoped>
.head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.item-cell { display: flex; align-items: center; gap: 10px; }
.thumb {
  width: 50px; height: 50px; border-radius: 6px; background: #f2f3f5;
  display: flex; align-items: center; justify-content: center; color: #c0c4cc; font-size: 12px;
}
</style>
