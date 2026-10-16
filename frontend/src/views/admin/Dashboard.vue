<template>
  <div>
    <!-- 核心指标卡片 -->
    <el-row :gutter="16" class="stat-row">
      <el-col v-for="card in statCards" :key="card.label" :xs="12" :sm="8" :md="6" :lg="3">
        <div class="stat-card">
          <div class="stat-value" :style="{ color: card.color }">{{ card.value }}</div>
          <div class="stat-label">{{ card.label }}</div>
        </div>
      </el-col>
    </el-row>

    <!-- 图表区 -->
    <el-row :gutter="16">
      <el-col :xs="24" :md="14">
        <div class="card chart-card">
          <div class="chart-title">近 7 日成交趋势</div>
          <div ref="trendRef" class="chart" />
        </div>
      </el-col>
      <el-col :xs="24" :md="10">
        <div class="card chart-card">
          <div class="chart-title">订单状态分布</div>
          <div ref="statusRef" class="chart" />
        </div>
      </el-col>
      <el-col :xs="24" :md="12">
        <div class="card chart-card">
          <div class="chart-title">分类成交金额分布</div>
          <div ref="categoryRef" class="chart" />
        </div>
      </el-col>
      <el-col :xs="24" :md="12">
        <div class="card chart-card">
          <div class="chart-title">热销物品 TOP10</div>
          <div ref="topRef" class="chart" />
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import * as echarts from 'echarts'
import { getStatsOverview, getStatsTrend, getStatsCategory, getStatsTopItems, getStatsStatus } from '../../api'

const overview = ref({})
const trendRef = ref(null)
const statusRef = ref(null)
const categoryRef = ref(null)
const topRef = ref(null)
let charts = []

const statCards = computed(() => [
  { label: '注册用户', value: overview.value.userCount ?? '-', color: '#409eff' },
  { label: '物品总数', value: overview.value.itemCount ?? '-', color: '#67c23a' },
  { label: '在售物品', value: overview.value.onSaleCount ?? '-', color: '#e6a23c' },
  { label: '待审核', value: overview.value.pendingAuditCount ?? '-', color: '#f56c6c' },
  { label: '订单总数', value: overview.value.orderCount ?? '-', color: '#909399' },
  { label: '累计成交额(元)', value: overview.value.finishedAmount ?? '-', color: '#f56c6c' },
  { label: '总成交额(元)', value: overview.value.totalAmount ?? '-', color: '#409eff' },
  { label: '总浏览量', value: overview.value.totalViews ?? '-', color: '#67c23a' }
])

const statusMap = { 0: '待付款', 1: '待发货', 2: '待收货', 3: '已完成', 4: '已取消' }

function createChart(el) {
  const chart = echarts.init(el)
  charts.push(chart)
  return chart
}

async function loadAll() {
  overview.value = await getStatsOverview()

  // 成交趋势
  const trend = await getStatsTrend(7)
  createChart(trendRef.value).setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['成交金额', '订单数'] },
    grid: { left: 50, right: 20, top: 40, bottom: 30 },
    xAxis: { type: 'category', data: trend.map(d => d.date) },
    yAxis: { type: 'value' },
    series: [
      { name: '成交金额', type: 'line', smooth: true, areaStyle: {}, data: trend.map(d => Number(d.amount)) },
      { name: '订单数', type: 'bar', barWidth: 16, data: trend.map(d => d.count) }
    ]
  })

  // 订单状态分布
  const status = await getStatsStatus()
  createChart(statusRef.value).setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie', radius: ['40%', '65%'],
      data: status.map(d => ({ name: statusMap[d.status] || '未知', value: d.count }))
    }]
  })

  // 分类成交
  const category = await getStatsCategory()
  createChart(categoryRef.value).setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie', radius: '60%', roseType: 'radius',
      data: category.map(d => ({ name: d.name, value: Number(d.amount) }))
    }]
  })

  // 热销排行
  const top = await getStatsTopItems(10)
  const names = top.map(d => (d.title || '').slice(0, 12))
  createChart(topRef.value).setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 130, right: 30, top: 20, bottom: 30 },
    xAxis: { type: 'value' },
    yAxis: { type: 'category', data: names.reverse() },
    series: [{ name: '成交金额', type: 'bar', barWidth: 14, data: top.map(d => Number(d.amount)).reverse() }]
  })
}

function onResize() {
  charts.forEach(c => c.resize())
}

onMounted(() => {
  loadAll()
  window.addEventListener('resize', onResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', onResize)
  charts.forEach(c => c.dispose())
  charts = []
})
</script>

<style scoped>
.stat-row { margin-bottom: 16px; }
.stat-card {
  background: #fff; border-radius: 8px; padding: 14px; text-align: center;
  margin-bottom: 16px; box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}
.stat-value { font-size: 20px; font-weight: 700; }
.stat-label { margin-top: 4px; font-size: 13px; color: #909399; }
.chart-card { margin-bottom: 16px; }
.chart-title { font-weight: 600; margin-bottom: 8px; }
.chart { height: 320px; }
</style>
