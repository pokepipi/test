<template>
  <div class="card">
    <div class="toolbar">
      <h3>分类管理</h3>
      <el-button type="primary" :icon="Plus" @click="openAdd(null)">新增一级分类</el-button>
    </div>

    <el-table :data="tree" row-key="id" v-loading="loading" stripe
              :tree-props="{ children: 'children' }" default-expand-all>
      <el-table-column prop="name" label="分类名称" min-width="200" />
      <el-table-column prop="sort" label="排序" width="90" />
      <el-table-column label="创建时间" width="170">
        <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button size="small" @click="openAdd(row)">添加子分类</el-button>
          <el-button size="small" type="primary" plain @click="openEdit(row)">编辑</el-button>
          <el-button size="small" type="danger" plain @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialog" :title="form.id ? '编辑分类' : '新增分类'" width="420px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="父分类">
          <el-cascader v-model="form.parentId" :options="parentOptions" :props="{ checkStrictly: true, emitPath: false, value: 'id', label: 'name' }"
                       placeholder="不选为一级分类" clearable style="width: 100%" :disabled="!!form.id" />
        </el-form-item>
        <el-form-item label="名称">
          <el-input v-model="form.name" placeholder="分类名称" maxlength="50" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" style="width: 160px" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getCategoryTree, addCategory, updateCategory, deleteCategory } from '../../api'
import { formatTime } from '../../utils/format'

const tree = ref([])
const loading = ref(false)
const dialog = ref(false)
const saving = ref(false)

const form = reactive({ id: null, name: '', parentId: null, sort: 0 })

const parentOptions = computed(() => toOptions(tree.value))

function toOptions(list) {
  return list.map(c => ({
    id: c.id,
    name: c.name,
    children: c.children && c.children.length ? toOptions(c.children) : undefined
  }))
}

async function load() {
  loading.value = true
  try {
    tree.value = await getCategoryTree()
  } finally {
    loading.value = false
  }
}

function openAdd(parent) {
  form.id = null
  form.name = ''
  form.sort = 0
  form.parentId = parent ? parent.id : null
  dialog.value = true
}

function openEdit(row) {
  form.id = row.id
  form.name = row.name
  form.sort = row.sort
  form.parentId = row.parentId || null
  dialog.value = true
}

async function save() {
  if (!form.name.trim()) {
    ElMessage.warning('请输入分类名称')
    return
  }
  saving.value = true
  try {
    if (form.id) {
      await updateCategory(form.id, { name: form.name, sort: form.sort })
    } else {
      await addCategory({ name: form.name, parentId: form.parentId || 0, sort: form.sort })
    }
    ElMessage.success('保存成功')
    dialog.value = false
    load()
  } catch (e) { /* ignore */ }
  saving.value = false
}

async function remove(row) {
  await ElMessageBox.confirm(`确认删除分类「${row.name}」？`, '提示', { type: 'warning' })
  await deleteCategory(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>

<style scoped>
.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
</style>
