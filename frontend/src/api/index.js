import request from '../utils/request'

// ===== 认证 =====
export const login = data => request.post('/api/auth/login', data)
export const register = data => request.post('/api/auth/register', data)
export const getMe = () => request.get('/api/auth/me')

// ===== 用户 =====
export const updateProfile = data => request.put('/api/user/profile', data)
export const changePassword = data => request.put('/api/user/password', data)

// ===== 上传 =====
export const uploadImage = file => {
  const form = new FormData()
  form.append('file', file)
  return request.post('/api/upload/image', form, { headers: { 'Content-Type': 'multipart/form-data' } })
}

// ===== 分类 =====
export const getCategoryTree = () => request.get('/api/category/tree')

// ===== 物品 =====
export const getItemPage = params => request.get('/api/item/page', { params })
export const getItemDetail = id => request.get(`/api/item/${id}`)
export const publishItem = data => request.post('/api/item', data)
export const updateItem = (id, data) => request.put(`/api/item/${id}`, data)
export const changeItemStatus = (id, status) => request.put(`/api/item/${id}/status`, null, { params: { status } })
export const deleteItem = id => request.delete(`/api/item/${id}`)
export const getMyItems = params => request.get('/api/item/my', { params })

// ===== 聊天 =====
export const getConversations = () => request.get('/api/chat/conversations')
export const getMessages = params => request.get('/api/chat/messages', { params })
export const sendMessage = data => request.post('/api/chat/send', data)
export const getUnread = () => request.get('/api/chat/unread')

// ===== 订单 =====
export const createOrder = data => request.post('/api/order/create', data)
export const getOrderPage = params => request.get('/api/order/page', { params })
export const getOrderDetail = id => request.get(`/api/order/${id}`)
export const payOrder = id => request.post(`/api/order/${id}/pay`)
export const cancelOrder = id => request.post(`/api/order/${id}/cancel`)
export const shipOrder = id => request.post(`/api/order/${id}/ship`)
export const finishOrder = id => request.post(`/api/order/${id}/finish`)

// ===== 管理端: 统计 =====
export const getStatsOverview = () => request.get('/api/admin/stats/overview')
export const getStatsTrend = days => request.get('/api/admin/stats/trend', { params: { days } })
export const getStatsCategory = () => request.get('/api/admin/stats/category')
export const getStatsTopItems = limit => request.get('/api/admin/stats/top-items', { params: { limit } })
export const getStatsStatus = () => request.get('/api/admin/stats/status')
export const getStatsUserGrowth = days => request.get('/api/admin/stats/user-growth', { params: { days } })

// ===== 管理端: 审核 =====
export const getAuditPage = params => request.get('/api/admin/audit/page', { params })
export const auditItem = (id, data) => request.post(`/api/admin/audit/${id}`, data)

// ===== 管理端: 用户 =====
export const getAdminUsers = params => request.get('/api/admin/user/page', { params })
export const updateUserStatus = (id, status) => request.put(`/api/admin/user/${id}/status`, null, { params: { status } })

// ===== 管理端: 物品 =====
export const getAdminItems = params => request.get('/api/admin/item/page', { params })
export const updateAdminItemStatus = (id, status) => request.put(`/api/admin/item/${id}/status`, null, { params: { status } })

// ===== 管理端: 订单 =====
export const getAdminOrders = params => request.get('/api/admin/order/page', { params })

// ===== 管理端: 分类 =====
export const addCategory = data => request.post('/api/admin/category', data)
export const updateCategory = (id, data) => request.put(`/api/admin/category/${id}`, data)
export const deleteCategory = id => request.delete(`/api/admin/category/${id}`)

// ===== 管理端: 敏感词 =====
export const getSensitiveWords = () => request.get('/api/admin/sensitive-word/list')
export const addSensitiveWord = word => request.post('/api/admin/sensitive-word', { word })
export const deleteSensitiveWord = id => request.delete(`/api/admin/sensitive-word/${id}`)
export const checkSensitiveWord = text => request.get('/api/admin/sensitive-word/check', { params: { text } })
