<template>
  <!-- 登录页独立展示；其余页面套统一布局（顶部导航 + 主内容） -->
  <router-view v-if="$route.path === '/login'" />
  <el-container v-else direction="vertical" style="height: 100vh">
    <el-header class="top-nav" height="56px">
      <div class="brand">AI 智能测试平台</div>
      <el-menu
        class="nav-menu"
        mode="horizontal"
        router
        :default-active="$route.path"
        :ellipsis="false"
      >
        <el-menu-item index="/report">测试报告</el-menu-item>
        <el-menu-item index="/cases">接口用例</el-menu-item>
        <el-menu-item index="/ui-suite">UI 用例</el-menu-item>
        <el-menu-item index="/executions">执行记录</el-menu-item>
      </el-menu>
      <div class="nav-user">
        <span v-if="username" class="user-name">{{ username }}</span>
        <el-button link class="logout" @click="logout">退出登录</el-button>
      </div>
    </el-header>
    <el-main class="app-main">
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup>
import { ref } from 'vue'

/**
 * 当前用户：优先读登录时写入的 username；
 * 老登录态没这个键时，从 JWT payload 的 sub 兜底解析（payload 是 base64url，需补 padding）。
 */
const username = ref(resolveUser())

function resolveUser() {
  const saved = localStorage.getItem('username')
  if (saved) return saved
  try {
    const payload = (localStorage.getItem('token') || '').split('.')[1]
    if (!payload) return ''
    const b64 = payload.replace(/-/g, '+').replace(/_/g, '/')
    const pad = b64.length % 4 === 0 ? '' : '='.repeat(4 - (b64.length % 4))
    return JSON.parse(atob(b64 + pad)).sub || ''
  } catch (e) {
    return ''
  }
}

function logout() {
  localStorage.removeItem('token')
  localStorage.removeItem('username')
  location.href = '/login'
}
</script>

<style scoped>
/* ===================== 顶部导航：深色半透明毛玻璃 ===================== */
.top-nav {
  position: relative;
  z-index: 10;
  display: flex;
  align-items: center;
  gap: 28px;
  padding: 0 24px;
  /* 深色底 + 背景模糊：背后是浅色页面，靠较高黑度压出"暗黑"质感，同时保留透光 */
  background: rgba(8, 12, 24, 0.86);
  backdrop-filter: blur(18px) saturate(160%);
  -webkit-backdrop-filter: blur(18px) saturate(160%);
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  box-shadow: 0 8px 24px rgba(10, 20, 40, 0.16), inset 0 1px 0 rgba(255, 255, 255, 0.06);
}
.brand {
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 0.02em;
  white-space: nowrap;
}
.nav-menu {
  flex: 1;
  border-bottom: none;
  background: transparent;
  --el-menu-bg-color: transparent;
  --el-menu-text-color: rgba(255, 255, 255, 0.62);
  --el-menu-active-color: #ffffff;
  --el-menu-hover-bg-color: rgba(255, 255, 255, 0.06);
  --el-menu-horizontal-height: 56px;
}
.nav-menu :deep(.el-menu-item) {
  position: relative;
  border-bottom: none !important;
  font-size: 14px;
  letter-spacing: 0.02em;
  transition: color 0.2s ease;
}
.nav-menu :deep(.el-menu-item:hover) {
  color: #fff;
}

/* 激活项：青色发光下划线（渐变两端收窄 + 外发光） */
.nav-menu :deep(.el-menu-item.is-active)::after {
  content: '';
  position: absolute;
  left: 50%;
  bottom: 4px;
  width: 72%;
  height: 2px;
  transform: translateX(-50%);
  border-radius: 2px;
  background: linear-gradient(90deg, rgba(34, 211, 238, 0), #3ee6f7 50%, rgba(34, 211, 238, 0));
  box-shadow: 0 0 16px rgba(34, 211, 238, 0.95), 0 0 4px rgba(34, 211, 238, 0.9);
}

/* ===================== 用户区：当前用户 + 退出 ===================== */
.nav-user {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-shrink: 0;
}
.user-name {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 13.5px;
  letter-spacing: 0.02em;
  color: rgba(255, 255, 255, 0.75);
}
/* 在线状态点：青色微光，与导航激活下划线同色系 */
.user-name::before {
  content: '';
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #3ee6f7;
  box-shadow: 0 0 10px rgba(34, 211, 238, 0.85);
}

/* 退出登录：柔和珊瑚红 */
.logout {
  color: #ff9d93;
  font-size: 14px;
  letter-spacing: 0.02em;
  transition: color 0.2s ease, text-shadow 0.2s ease;
}
.logout:hover {
  color: #ffb6ad;
  text-shadow: 0 0 12px rgba(255, 140, 130, 0.45);
}

/* ===================== 主内容区：明亮浅蓝紫渐变 ===================== */
.app-main {
  padding: 22px 26px;
  background:
    radial-gradient(1100px 520px at 8% -10%, rgba(182, 205, 234, 0.42), transparent 62%),
    radial-gradient(920px 480px at 100% 0%, rgba(211, 198, 240, 0.34), transparent 58%),
    linear-gradient(165deg, #f7f9fd 0%, #eef2f9 52%, #f6f1fb 100%);
}
</style>
