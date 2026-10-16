<template>
  <div class="page-container" v-if="item">
    <el-breadcrumb separator="/" style="margin-bottom: 12px">
      <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
      <el-breadcrumb-item>物品详情</el-breadcrumb-item>
    </el-breadcrumb>

    <div class="card detail-card">
      <div class="gallery">
        <el-carousel v-if="images.length" height="400px" :interval="4000" arrow="always">
          <el-carousel-item v-for="img in images" :key="img">
            <el-image :src="img" fit="contain" style="width: 100%; height: 100%"
                      :preview-src-list="images" :initial-index="images.indexOf(img)" />
          </el-carousel-item>
        </el-carousel>
        <div v-else class="no-img">暂无图片</div>
      </div>

      <div class="info-panel">
        <h2 class="title">{{ item.title }}</h2>
        <div class="price-row">
          <span class="price-text big">￥{{ formatPrice(item.price) }}</span>
          <el-tag v-if="item.status === 3" type="danger">已售出</el-tag>
          <el-tag v-else type="success">在售</el-tag>
        </div>
        <el-descriptions :column="1" border size="small" class="desc">
          <el-descriptions-item label="分类">{{ item.categoryName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="卖家">
            <span class="seller">
              <el-avatar :size="20" :src="item.sellerAvatar || ''">{{ (item.sellerName || '?').slice(0, 1) }}</el-avatar>
              {{ item.sellerName || '未知' }}
            </span>
          </el-descriptions-item>
          <el-descriptions-item label="发布时间">{{ formatTime(item.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="浏览量">{{ item.viewCount }}</el-descriptions-item>
        </el-descriptions>
        <div class="actions">
          <el-button type="primary" :icon="ChatDotRound" @click="contactSeller">联系卖家</el-button>
          <el-button type="danger" :icon="ShoppingCart" :disabled="item.status !== 1" @click="openOrder">
            立即购买
          </el-button>
        </div>
      </div>
    </div>

    <div class="card desc-card">
      <h3>物品描述</h3>
      <p class="content" style="white-space: pre-wrap">{{ item.description || '暂无描述' }}</p>
    </div>

    <!-- 下单弹窗 (支持议价后价格) -->
    <el-dialog v-model="orderDialog" title="确认下单" width="480px">
      <el-form :model="orderForm" label-width="90px">
        <el-form-item label="成交价(元)">
          <el-input-number v-model="orderForm.price" :min="0.01" :precision="2" :step="5" style="width: 100%" />
        </el-form-item>
        <el-form-item label="收货人">
          <el-input v-model="orderForm.contactName" placeholder="请输入收货人姓名" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="orderForm.contactPhone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="收货地址">
          <el-input v-model="orderForm.address" type="textarea" :rows="2" placeholder="请输入收货地址" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="orderForm.remark" placeholder="选填" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="orderDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitOrder">提交订单</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ChatDotRound, ShoppingCart } from '@element-plus/icons-vue'
import { getItemDetail, createOrder } from '../api'
import { useUserStore } from '../store/user'
import { formatPrice, formatTime } from '../utils/format'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const item = ref(null)
const images = computed(() =>
  item.value?.images ? String(item.value.images).split(',').filter(Boolean) : []
)

const orderDialog = ref(false)
const submitting = ref(false)
const orderForm = reactive({
  price: 0,
  contactName: '',
  contactPhone: '',
  address: '',
  remark: ''
})

async function load() {
  item.value = await getItemDetail(route.params.id)
}

function requireLogin() {
  if (!userStore.isLogin) {
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return false
  }
  return true
}

function contactSeller() {
  if (!requireLogin()) return
  router.push({ path: '/chat', query: { to: item.value.userId, itemId: item.value.id } })
}

function openOrder() {
  if (!requireLogin()) return
  orderForm.price = Number(item.value.price)
  orderDialog.value = true
}

async function submitOrder() {
  if (!orderForm.contactName || !orderForm.contactPhone || !orderForm.address) {
    ElMessage.warning('请填写完整的收货信息')
    return
  }
  submitting.value = true
  try {
    await createOrder({
      itemId: item.value.id,
      price: orderForm.price,
      contactName: orderForm.contactName,
      contactPhone: orderForm.contactPhone,
      address: orderForm.address,
      remark: orderForm.remark
    })
    ElMessage.success('下单成功，请尽快支付')
    orderDialog.value = false
    router.push('/my/orders')
  } catch (e) { /* ignore */ }
  submitting.value = false
}

onMounted(load)
</script>

<style scoped>
.detail-card {
  display: flex;
  gap: 24px;
  padding: 20px;
  flex-wrap: wrap;
}
.gallery { flex: 1 1 420px; min-width: 320px; }
.no-img {
  height: 400px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #c0c4cc;
  background: #f2f3f5;
  border-radius: 8px;
}
.info-panel { flex: 1 1 360px; min-width: 300px; }
.title { font-size: 22px; margin-bottom: 12px; }
.price-row { margin-bottom: 16px; display: flex; align-items: center; gap: 12px; }
.price-text.big { font-size: 28px; }
.desc { margin-bottom: 16px; }
.seller { display: inline-flex; align-items: center; gap: 6px; }
.actions { margin-top: 8px; }
.desc-card { margin-top: 16px; padding: 20px; }
.desc-card h3 { margin-bottom: 12px; }
.content { line-height: 1.8; color: #606266; }
</style>
