<template>
  <div class="item-card" @click="$router.push('/item/' + item.id)">
    <div class="cover">
      <el-image v-if="images.length" :src="images[0]" fit="cover" class="cover-img" lazy />
      <div v-else class="no-img">暂无图片</div>
      <el-tag v-if="item.status !== undefined && item.status !== 1" size="small" class="status-tag"
              :type="statusType">{{ statusText }}</el-tag>
    </div>
    <div class="info">
      <div class="title">{{ item.title }}</div>
      <div class="meta">
        <span class="price-text">￥{{ formatPrice(item.price) }}</span>
        <span class="muted">{{ item.viewCount || 0 }} 浏览</span>
      </div>
      <div class="seller">
        <span>{{ item.sellerName || '未知卖家' }}</span>
        <span class="muted">{{ item.categoryName || '' }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { formatPrice } from '../utils/format'

const props = defineProps({ item: { type: Object, required: true } })

const images = computed(() =>
  props.item.images ? String(props.item.images).split(',').filter(Boolean) : []
)

const statusMap = {
  0: { text: '待审核', type: 'warning' },
  1: { text: '在售', type: 'success' },
  2: { text: '已下架', type: 'info' },
  3: { text: '已售出', type: 'danger' },
  4: { text: '已驳回', type: 'danger' }
}
const statusText = computed(() => statusMap[props.item.status]?.text || '')
const statusType = computed(() => statusMap[props.item.status]?.type || 'info')
</script>

<style scoped>
.item-card {
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}
.item-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.12);
}
.cover { position: relative; height: 180px; }
.cover-img { width: 100%; height: 100%; }
.no-img {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #c0c4cc;
  background: #f2f3f5;
}
.status-tag { position: absolute; top: 8px; left: 8px; }
.info { padding: 10px 12px 14px; }
.title {
  font-size: 14px;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
}
.seller {
  display: flex;
  justify-content: space-between;
  margin-top: 6px;
  font-size: 12px;
  color: #909399;
}
</style>
