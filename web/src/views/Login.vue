<template>
  <div style="display:flex; align-items:center; justify-content:center; height:100vh; background:#eef1f6">
    <el-card style="width:380px">
      <h2 style="text-align:center; margin-bottom:20px">AI 智能测试平台</h2>
      <el-form :model="form" @keyup.enter="doLogin">
        <el-form-item>
          <el-input v-model="form.username" placeholder="用户名" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" type="password" placeholder="密码" show-password />
        </el-form-item>
        <el-button type="primary" style="width:100%" :loading="loading" @click="doLogin">登 录</el-button>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import request from '../api/request'

const router = useRouter()
const form = reactive({ username: '', password: '' })
const loading = ref(false)

async function doLogin() {
  if (!form.username || !form.password) return
  loading.value = true
  try {
    const data = await request.post('/auth/login', form)
    localStorage.setItem('token', data.token)
    router.push('/')
  } finally {
    loading.value = false
  }
}
</script>
