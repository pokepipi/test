import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/login', name: 'Login', component: () => import('../views/Login.vue'), meta: { public: true } },
  { path: '/', name: 'Home', component: () => import('../views/Home.vue'), meta: { public: true } },
  { path: '/item/:id', name: 'ItemDetail', component: () => import('../views/ItemDetail.vue'), meta: { public: true } },
  { path: '/publish', name: 'Publish', component: () => import('../views/Publish.vue'), meta: { title: '发布物品' } },
  { path: '/publish/edit/:id', name: 'PublishEdit', component: () => import('../views/Publish.vue'), meta: { title: '编辑物品' } },
  { path: '/my/items', name: 'MyItems', component: () => import('../views/MyItems.vue'), meta: { title: '我的物品' } },
  { path: '/my/orders', name: 'MyOrders', component: () => import('../views/MyOrders.vue'), meta: { title: '我的订单' } },
  { path: '/chat', name: 'Chat', component: () => import('../views/Chat.vue'), meta: { title: '消息' } },
  { path: '/profile', name: 'Profile', component: () => import('../views/Profile.vue'), meta: { title: '个人中心' } },
  {
    path: '/admin',
    component: () => import('../views/admin/AdminLayout.vue'),
    meta: { admin: true },
    children: [
      { path: '', redirect: '/admin/dashboard' },
      { path: 'dashboard', name: 'AdminDashboard', component: () => import('../views/admin/Dashboard.vue'), meta: { title: '数据统计' } },
      { path: 'audit', name: 'AdminAudit', component: () => import('../views/admin/Audit.vue'), meta: { title: '物品审核' } },
      { path: 'users', name: 'AdminUsers', component: () => import('../views/admin/Users.vue'), meta: { title: '用户管理' } },
      { path: 'items', name: 'AdminItems', component: () => import('../views/admin/Items.vue'), meta: { title: '物品管理' } },
      { path: 'orders', name: 'AdminOrders', component: () => import('../views/admin/Orders.vue'), meta: { title: '订单管理' } },
      { path: 'categories', name: 'AdminCategories', component: () => import('../views/admin/Categories.vue'), meta: { title: '分类管理' } },
      { path: 'words', name: 'AdminWords', component: () => import('../views/admin/SensitiveWords.vue'), meta: { title: '敏感词管理' } }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach(to => {
  const token = localStorage.getItem('token')
  const userInfo = JSON.parse(localStorage.getItem('userInfo') || 'null')
  if (!to.meta.public && !token) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.meta.admin && userInfo?.role !== 'ADMIN') {
    return { path: '/' }
  }
  document.title = (to.meta.title ? to.meta.title + ' - ' : '') + '校园二手交易平台'
  return true
})

export default router
