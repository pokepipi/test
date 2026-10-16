<template>
  <div class="image-upload">
    <div class="preview-list">
      <div v-for="(url, i) in modelValue" :key="url" class="preview-item">
        <el-image :src="url" fit="cover" class="preview-img" />
        <el-icon class="remove" @click="remove(i)"><Close /></el-icon>
      </div>
      <div v-if="modelValue.length < limit" class="upload-box" @click="choose">
        <el-icon :size="24"><Plus /></el-icon>
        <span>上传图片</span>
      </div>
    </div>
    <input ref="fileInput" type="file" accept="image/*" multiple style="display: none" @change="onChange" />
    <div class="muted tip">支持 jpg/png/gif, 单张不超过 10MB, 上传自动压缩; 最多 {{ limit }} 张</div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { uploadImage } from '../api'

const props = defineProps({
  modelValue: { type: Array, default: () => [] },
  limit: { type: Number, default: 6 }
})
const emit = defineEmits(['update:modelValue'])

const fileInput = ref(null)
const uploading = ref(false)

function choose() {
  if (uploading.value) return
  fileInput.value.click()
}

async function onChange(e) {
  const files = Array.from(e.target.files || [])
  e.target.value = ''
  if (props.modelValue.length + files.length > props.limit) {
    ElMessage.warning(`最多上传 ${props.limit} 张图片`)
    return
  }
  uploading.value = true
  try {
    for (const file of files) {
      const url = await uploadImage(file)
      emit('update:modelValue', [...props.modelValue, url])
    }
  } catch (err) { /* 错误已由拦截器提示 */ }
  uploading.value = false
}

function remove(i) {
  const list = [...props.modelValue]
  list.splice(i, 1)
  emit('update:modelValue', list)
}
</script>

<style scoped>
.preview-list { display: flex; flex-wrap: wrap; gap: 8px; }
.preview-item {
  position: relative;
  width: 100px;
  height: 100px;
  border-radius: 6px;
  overflow: hidden;
  border: 1px solid #e4e7ed;
}
.preview-img { width: 100%; height: 100%; }
.remove {
  position: absolute;
  top: 2px;
  right: 2px;
  background: rgba(0, 0, 0, 0.5);
  color: #fff;
  border-radius: 50%;
  padding: 2px;
  cursor: pointer;
}
.upload-box {
  width: 100px;
  height: 100px;
  border: 1px dashed #c0c4cc;
  border-radius: 6px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  color: #909399;
  cursor: pointer;
}
.upload-box:hover { border-color: #409eff; color: #409eff; }
.tip { margin-top: 6px; }
</style>
