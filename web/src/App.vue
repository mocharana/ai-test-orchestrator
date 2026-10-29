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
      <el-button link type="danger" class="logout" @click="logout">退出登录</el-button>
    </el-header>
    <el-main>
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup>
function logout() {
  localStorage.removeItem('token')
  location.href = '/login'
}
</script>

<style scoped>
/* 顶部导航：深色底 + 水平菜单。
   配色走 Element Plus 的 CSS 变量（--el-menu-*），不用已标记弃用的 background-color/text-color props */
.top-nav {
  display: flex;
  align-items: center;
  gap: 28px;
  padding: 0 24px;
  background: #001529;
}
.brand {
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  white-space: nowrap;
}
.nav-menu {
  flex: 1;
  border-bottom: none;
  --el-menu-bg-color: transparent;
  --el-menu-text-color: #ffffffa6;
  --el-menu-active-color: #ffffff;
  --el-menu-hover-bg-color: rgba(255, 255, 255, 0.08);
  --el-menu-horizontal-height: 56px;
}
/* 深色底上去掉菜单自带横线，只保留激活项下划线 */
.nav-menu :deep(.el-menu-item) {
  border-bottom: none;
}
.nav-menu :deep(.el-menu-item.is-active) {
  border-bottom: 2px solid var(--el-color-primary);
}
.logout {
  color: #ff7875;
}
</style>
