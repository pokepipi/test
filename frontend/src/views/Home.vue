<template>
  <div class="page-container">
    <!-- 搜索栏 -->
    <div class="card search-bar">
      <el-input v-model="query.keyword" placeholder="搜索物品关键字" clearable style="width: 240px"
                @keyup.enter="load(1)" @clear="load(1)" />
      <el-cascader v-model="query.categoryId" :options="categoryOptions" placeholder="全部分类" clearable
                   :props="cascaderProps" style="width: 200px" @change="load(1)" />
      <el-input v-model.number="query.minPrice" placeholder="最低价" type="number" style="width: 100px" />
      <span class="muted">-</span>
      <el-input v-model.number="query.maxPrice" placeholder="最高价" type="number" style="width: 100px" />
      <el-select v-model="query.sort" style="width: 140px" @change="load(1)">
        <el-option label="最新发布" value="new" />
        <el-option label="价格从低到高" value="price_asc" />
        <el-option label="价格从高到低" value="price_desc" />
        <el-option label="最多浏览" value="hot" />
      </el-select>
      <el-button type="primary" @click="load(1)">搜索</el-button>
    </div>

    <!-- 物品网格 -->
    <el-row :gutter="16" class="grid">
      <el-col v-for="item in list" :key="item.id" :xs="12" :sm="8" :md="6" class="grid-col">
        <ItemCard :item="item" />
      </el-col>
    </el-row>
    <el-empty v-if="!loading && list.length === 0" description="没有找到相关物品" />

    <div class="pager" v-if="total > 0">
      <el-pagination background layout="prev, pager, next, total" :total="total" :page-size="query.size"
                     :current-page="query.page" @current-change="load" />
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import ItemCard from '../components/ItemCard.vue'
import { getItemPage, getCategoryTree } from '../api'

const query = reactive({
  keyword: '',
  categoryId: null,
  minPrice: null,
  maxPrice: null,
  sort: 'new',
  page: 1,
  size: 12
})

const list = ref([])
const total = ref(0)
const loading = ref(false)
const categoryOptions = ref([])
const cascaderProps = { checkStrictly: true, emitPath: false, value: 'id', label: 'name' }

function toOptions(list) {
  return list.map(c => ({
    id: c.id,
    name: c.name,
    children: c.children && c.children.length ? toOptions(c.children) : undefined
  }))
}

async function loadCategories() {
  const tree = await getCategoryTree()
  categoryOptions.value = toOptions(tree)
}

async function load(page) {
  query.page = page || 1
  loading.value = true
  try {
    const params = {
      page: query.page,
      size: query.size,
      keyword: query.keyword || undefined,
      categoryId: query.categoryId || undefined,
      minPrice: query.minPrice || undefined,
      maxPrice: query.maxPrice || undefined,
      sort: query.sort
    }
    const res = await getItemPage(params)
    list.value = res.records
    total.value = Number(res.total)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadCategories()
  load(1)
})
</script>

<style scoped>
.search-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}
.grid { margin: 0 -8px; }
.grid-col { margin-bottom: 16px; }
</style>
