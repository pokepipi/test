<template>
  <div class="card">
    <div class="toolbar">
      <h3>敏感词管理（违规内容审核词典）</h3>
    </div>

    <div class="add-row">
      <el-input v-model="newWord" placeholder="输入要添加的敏感词" style="width: 260px" maxlength="50"
                @keyup.enter="add" />
      <el-button type="primary" @click="add">添加</el-button>
    </div>

    <el-table :data="list" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="word" label="敏感词" min-width="200" />
      <el-table-column label="添加时间" width="170">
        <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="100">
        <template #default="{ row }">
          <el-button size="small" type="danger" plain @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 内容检测 -->
    <div class="check-box">
      <h4>内容检测测试</h4>
      <el-input v-model="testText" type="textarea" :rows="3" placeholder="输入一段文字，检测是否包含敏感词" />
      <el-button type="primary" style="margin-top: 10px" @click="check">检测</el-button>
      <div v-if="checkResult !== null" style="margin-top: 10px">
        <el-tag v-if="checkResult.length" type="danger">命中敏感词：{{ checkResult.join('、') }}</el-tag>
        <el-tag v-else type="success">未发现敏感词</el-tag>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getSensitiveWords, addSensitiveWord, deleteSensitiveWord, checkSensitiveWord } from '../../api'
import { formatTime } from '../../utils/format'

const list = ref([])
const loading = ref(false)
const newWord = ref('')
const testText = ref('')
const checkResult = ref(null)

async function load() {
  loading.value = true
  try {
    list.value = await getSensitiveWords()
  } finally {
    loading.value = false
  }
}

async function add() {
  if (!newWord.value.trim()) {
    ElMessage.warning('请输入敏感词')
    return
  }
  await addSensitiveWord(newWord.value.trim())
  ElMessage.success('已添加')
  newWord.value = ''
  load()
}

async function remove(row) {
  await ElMessageBox.confirm(`确认删除敏感词「${row.word}」？`, '提示', { type: 'warning' })
  await deleteSensitiveWord(row.id)
  ElMessage.success('已删除')
  load()
}

async function check() {
  if (!testText.value) {
    ElMessage.warning('请输入检测文本')
    return
  }
  checkResult.value = await checkSensitiveWord(testText.value)
}

onMounted(load)
</script>

<style scoped>
.toolbar { margin-bottom: 12px; }
.add-row { display: flex; gap: 10px; margin-bottom: 14px; }
.check-box { margin-top: 20px; padding-top: 16px; border-top: 1px solid #ebeef5; }
.check-box h4 { margin-bottom: 10px; }
</style>
