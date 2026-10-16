<template>
  <div class="page-container">
    <div class="card form-card">
      <h2 class="form-title">{{ isEdit ? '编辑物品' : '发布物品' }}</h2>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px" style="max-width: 720px">
        <el-form-item label="物品标题" prop="title">
          <el-input v-model="form.title" placeholder="例如：九成新高数教材，附笔记" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="分类" prop="categoryId">
          <el-cascader v-model="form.categoryId" :options="categoryOptions" :props="cascaderProps"
                       placeholder="请选择分类" style="width: 100%" clearable />
        </el-form-item>
        <el-form-item label="价格(元)" prop="price">
          <el-input-number v-model="form.price" :min="0.01" :precision="2" :step="5" style="width: 200px" />
        </el-form-item>
        <el-form-item label="物品图片">
          <ImageUpload v-model="form.images" :limit="6" />
        </el-form-item>
        <el-form-item label="详细描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="6" maxlength="2000" show-word-limit
                    placeholder="描述物品成色、入手渠道、使用情况、交易方式等" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="submit">{{ isEdit ? '保存修改' : '发布' }}</el-button>
          <el-button @click="$router.back()">取消</el-button>
        </el-form-item>
      </el-form>
      <el-alert v-if="!isEdit" type="info" :closable="false" show-icon
                title="发布后将进入人工审核，审核通过才会在首页展示；如含敏感词将被重点审核。" />
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import ImageUpload from '../components/ImageUpload.vue'
import { getCategoryTree, publishItem, updateItem, getMyItems } from '../api'

const route = useRoute()
const router = useRouter()

const isEdit = computed(() => !!route.params.id)
const formRef = ref(null)
const submitting = ref(false)
const categoryOptions = ref([])
const cascaderProps = { checkStrictly: true, emitPath: false, value: 'id', label: 'name' }

const form = reactive({
  title: '',
  categoryId: null,
  price: 0.01,
  description: '',
  images: []
})

const rules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  price: [{ required: true, message: '请输入价格', trigger: 'blur' }]
}

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

async function loadForEdit() {
  if (!isEdit.value) return
  const res = await getMyItems({ page: 1, size: 100 })
  const target = res.records.find(i => i.id === Number(route.params.id))
  if (!target) {
    ElMessage.error('物品不存在')
    router.replace('/my/items')
    return
  }
  form.title = target.title
  form.categoryId = target.categoryId
  form.price = Number(target.price)
  form.description = target.description || ''
  form.images = target.images ? String(target.images).split(',').filter(Boolean) : []
}

async function submit() {
  await formRef.value.validate()
  submitting.value = true
  try {
    const data = {
      categoryId: form.categoryId,
      title: form.title,
      description: form.description,
      price: form.price,
      images: form.images.join(',')
    }
    if (isEdit.value) {
      await updateItem(route.params.id, data)
      ElMessage.success('修改成功，已重新提交审核')
    } else {
      await publishItem(data)
      ElMessage.success('发布成功，等待管理员审核')
    }
    router.push('/my/items')
  } catch (e) { /* ignore */ }
  submitting.value = false
}

onMounted(() => {
  loadCategories()
  loadForEdit()
})
</script>

<style scoped>
.form-card { max-width: 860px; margin: 0 auto; padding: 24px; }
.form-title { margin-bottom: 20px; font-size: 20px; }
</style>
