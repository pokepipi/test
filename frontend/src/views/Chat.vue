<template>
  <div class="page-container">
    <div class="card chat-card">
      <!-- 会话列表 -->
      <div class="conv-list">
        <div class="conv-head">消息列表</div>
        <div v-if="conversations.length === 0" class="muted empty">暂无会话，去物品详情页联系卖家吧</div>
        <div v-for="conv in conversations" :key="convKey(conv)" class="conv-item"
             :class="{ active: isActive(conv) }" @click="select(conv)">
          <el-avatar :size="40" :src="conv.otherAvatar || ''">{{ conv.otherName.slice(0, 1) }}</el-avatar>
          <div class="conv-main">
            <div class="conv-top">
              <span class="conv-name">{{ conv.otherName }}</span>
              <span class="muted time">{{ formatTime(conv.lastTime) }}</span>
            </div>
            <div class="conv-sub">
              <span class="conv-last">{{ conv.lastMessage }}</span>
              <el-badge v-if="conv.unread > 0" :value="conv.unread" />
            </div>
            <div v-if="conv.itemTitle" class="conv-item-title">📦 {{ conv.itemTitle }}</div>
          </div>
        </div>
      </div>

      <!-- 聊天窗口 -->
      <div class="chat-window">
        <template v-if="current">
          <div class="chat-head">
            <span class="chat-name">{{ current.otherName }}</span>
            <el-tag v-if="current.itemTitle" size="small" type="warning" class="item-tag">
              议价物品：{{ current.itemTitle }}
            </el-tag>
          </div>
          <div class="msg-list" ref="msgListRef">
            <div v-for="msg in messages" :key="msg.id" class="msg-row"
                 :class="{ mine: msg.fromUserId === userStore.userInfo?.id }">
              <el-avatar :size="32" :src="msg.fromUserId === userStore.userInfo?.id
                ? userStore.userInfo?.avatar || '' : current.otherAvatar || ''">
                {{ (msg.fromUserId === userStore.userInfo?.id ? userStore.userInfo?.nickname : current.otherName).slice(0, 1) }}
              </el-avatar>
              <div class="bubble-wrap">
                <div class="bubble">{{ msg.content }}</div>
                <div class="muted msg-time">{{ formatTime(msg.createTime) }}</div>
              </div>
            </div>
          </div>
          <div class="chat-input">
            <el-input v-model="draft" type="textarea" :rows="2" placeholder="输入消息，可与卖家协商价格…"
                      @keydown.enter.exact.prevent="send" />
            <div class="send-row">
              <span class="muted">提示：聊天内容会进行敏感词过滤</span>
              <el-button type="primary" :loading="sending" @click="send">发送</el-button>
            </div>
          </div>
        </template>
        <div v-else class="chat-placeholder">选择一个会话开始聊天</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { nextTick, onMounted, onUnmounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getConversations, getMessages, sendMessage } from '../api'
import { useUserStore } from '../store/user'
import { formatTime } from '../utils/format'

const route = useRoute()
const userStore = useUserStore()

const conversations = ref([])
const current = ref(null)
const messages = ref([])
const draft = ref('')
const sending = ref(false)
const msgListRef = ref(null)
let ws = null

const convKey = conv => `${conv.otherId}-${conv.itemId || ''}`
const isActive = conv =>
  current.value && current.value.otherId === conv.otherId && current.value.itemId === conv.itemId

async function loadConversations() {
  conversations.value = await getConversations()
  // 路由带参时自动选中
  if (route.query.to && !current.value) {
    const queryItemId = route.query.itemId ? Number(route.query.itemId) : null
    const found = conversations.value.find(
      c => c.otherId === Number(route.query.to) && (c.itemId || null) === queryItemId
    )
    if (found) select(found)
  }
}

async function loadMessages() {
  const res = await getMessages({
    otherId: current.value.otherId,
    itemId: current.value.itemId || undefined,
    page: 1,
    size: 100
  })
  messages.value = res.records
  scrollBottom()
}

async function select(conv) {
  current.value = { ...conv }
  await loadMessages()
  // 会话未读数清零
  const idx = conversations.value.findIndex(c => convKey(c) === convKey(conv))
  if (idx >= 0) conversations.value[idx].unread = 0
}

async function send() {
  const content = draft.value.trim()
  if (!content) return
  sending.value = true
  try {
    await sendMessage({
      toUserId: current.value.otherId,
      itemId: current.value.itemId || null,
      content
    })
    draft.value = ''
    await loadMessages()
    await loadConversations()
    if (current.value) {
      const found = conversations.value.find(c => convKey(c) === convKey(current.value))
      if (found) current.value = { ...current.value, otherName: found.otherName, otherAvatar: found.otherAvatar, itemTitle: found.itemTitle }
    }
  } catch (e) { /* ignore */ }
  sending.value = false
}

function scrollBottom() {
  nextTick(() => {
    if (msgListRef.value) msgListRef.value.scrollTop = msgListRef.value.scrollHeight
  })
}

function connectWs() {
  const token = localStorage.getItem('token')
  if (!token) return
  const proto = location.protocol === 'https:' ? 'wss://' : 'ws://'
  ws = new WebSocket(`${proto}${location.host}/ws/chat?token=${token}`)
  ws.onmessage = e => {
    try {
      const data = JSON.parse(e.data)
      if (data.type !== 'new_message') return
      // 当前会话直接追加
      if (current.value && data.fromUserId === current.value.otherId
          && (data.itemId || null) === (current.value.itemId || null)) {
        messages.value.push(data)
        scrollBottom()
      } else {
        loadConversations()
      }
    } catch (err) { /* ignore */ }
  }
  ws.onclose = () => { ws = null }
}

onMounted(() => {
  loadConversations()
  connectWs()
})

onUnmounted(() => {
  if (ws) ws.close()
})
</script>

<style scoped>
.chat-card { display: flex; height: calc(100vh - 120px); min-height: 480px; padding: 0; overflow: hidden; }
.conv-list {
  width: 300px;
  border-right: 1px solid #ebeef5;
  overflow-y: auto;
}
.conv-head { padding: 14px 16px; font-weight: 700; border-bottom: 1px solid #ebeef5; }
.empty { padding: 24px; text-align: center; }
.conv-item {
  display: flex; gap: 10px; padding: 12px 16px; cursor: pointer;
  border-bottom: 1px solid #f5f7fa;
}
.conv-item:hover { background: #f5f7fa; }
.conv-item.active { background: #ecf5ff; }
.conv-main { flex: 1; min-width: 0; }
.conv-top { display: flex; justify-content: space-between; align-items: center; }
.conv-name { font-weight: 600; font-size: 14px; }
.conv-sub { display: flex; justify-content: space-between; align-items: center; margin-top: 4px; }
.conv-last {
  font-size: 13px; color: #909399; overflow: hidden;
  text-overflow: ellipsis; white-space: nowrap;
}
.conv-item-title { margin-top: 4px; font-size: 12px; color: #e6a23c; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.chat-window { flex: 1; display: flex; flex-direction: column; min-width: 0; }
.chat-head {
  padding: 12px 16px; border-bottom: 1px solid #ebeef5;
  display: flex; align-items: center; gap: 10px; font-weight: 600;
}
.item-tag { max-width: 300px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.msg-list { flex: 1; overflow-y: auto; padding: 16px; background: #f9fafc; }
.msg-row { display: flex; gap: 8px; margin-bottom: 14px; }
.msg-row.mine { flex-direction: row-reverse; }
.bubble-wrap { max-width: 65%; }
.bubble {
  background: #fff; border-radius: 8px; padding: 8px 12px; font-size: 14px;
  word-break: break-word; box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}
.msg-row.mine .bubble { background: #409eff; color: #fff; }
.msg-time { margin-top: 4px; font-size: 12px; }
.chat-input { padding: 12px 16px; border-top: 1px solid #ebeef5; }
.send-row { display: flex; justify-content: space-between; align-items: center; margin-top: 8px; }
.chat-placeholder {
  flex: 1; display: flex; align-items: center; justify-content: center; color: #c0c4cc;
}
</style>
