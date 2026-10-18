import { createRouter, createWebHistory } from 'vue-router'
import Login from '../views/Login.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', component: Login },
    { path: '/', redirect: '/api-case' },
    { path: '/api-case', component: () => import('../views/ApiCase.vue') },
    { path: '/executions', component: () => import('../views/Executions.vue') },
    { path: '/ui-suite', component: () => import('../views/UiSuite.vue') }
  ]
})

// 路由守卫：未登录（无 token）一律踢回登录页
router.beforeEach((to) => {
  const token = localStorage.getItem('token')
  if (to.path !== '/login' && !token) return '/login'
})

export default router
