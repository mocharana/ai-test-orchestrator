<template>
  <!-- 登录页独立展示；其余页面套统一布局（侧栏菜单 + 主内容） -->
  <router-view v-if="$route.path === '/login'" />
  <el-container v-else style="height: 100vh">
    <el-aside width="200px" style="background: #001529">
      <div style="color:#fff; text-align:center; padding:20px 0; font-weight:bold">AI 智能测试平台</div>
      <el-menu router :default-active="$route.path" background-color="#001529" text-color="#ffffffa6" active-text-color="#fff" style="border-right:none">
        <el-menu-item index="/report">测试报告</el-menu-item>
        <el-menu-item index="/cases">接口用例</el-menu-item>
        <el-menu-item index="/ui-suite">UI 用例</el-menu-item>
        <el-menu-item index="/executions">执行记录</el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header style="background:#fff; display:flex; align-items:center; justify-content:flex-end; gap:12px">
        <el-button link type="danger" @click="logout">退出登录</el-button>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
function logout() {
  localStorage.removeItem('token')
  location.href = '/login'
}
</script>
