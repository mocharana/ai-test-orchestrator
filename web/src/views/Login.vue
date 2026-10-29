<template>
  <div class="login-page">
    <!-- 背景光晕层：铺满整屏（含右半区）—— 右侧玻璃必须有光影可模糊，才有"透出来"的效果 -->
    <div class="bg" aria-hidden="true">
      <span class="orb orb-cyan"></span>
      <span class="orb orb-violet"></span>
      <span class="orb orb-blue"></span>
      <span class="stream stream-1"></span>
      <span class="stream stream-2"></span>
      <span class="streak streak-1"></span>
      <span class="streak streak-2"></span>
      <span class="grid-lines"></span>
    </div>

    <!-- 左半：品牌区（与右侧表单同样垂直+水平居中） -->
    <section class="panel-left">
      <div class="brand">
        <div class="brand-kicker">AI TEST ORCHESTRATOR</div>
        <h1 class="brand-title">AI 智能测试平台</h1>
        <p class="brand-sub">用例即数据 · 混合引擎 · AI 归因</p>
      </div>
      <div class="brand-foot">Spring Boot 4 · Vue 3 · Spring AI</div>
    </section>

    <!-- 右半：全高毛玻璃面板（登录 / 注册两态共用） -->
    <section class="panel-right">
      <div class="form-box">
        <!-- ========== 登录态 ========== -->
        <template v-if="mode === 'login'">
          <h2 class="form-title">登录</h2>
          <p class="form-sub">使用平台账号进入控制台</p>
          <div class="form-line"></div>

          <el-form :model="form" @keyup.enter="doLogin">
            <el-form-item>
              <el-input v-model="form.username" placeholder="用户名" size="large" />
            </el-form-item>
            <el-form-item>
              <el-input v-model="form.password" type="password" placeholder="密码" show-password size="large" />
            </el-form-item>
            <el-button class="login-btn" :loading="loading" @click="doLogin">登 录</el-button>
          </el-form>

          <div class="switch-hint">
            新用户，<a class="switch-link" @click="toRegister">点此注册</a>
          </div>
        </template>

        <!-- ========== 注册态 ========== -->
        <template v-else>
          <h2 class="form-title">注册</h2>
          <p class="form-sub">创建一个平台账号</p>
          <div class="form-line"></div>

          <el-form :model="regForm" @keyup.enter="doRegister">
            <el-form-item>
              <el-input v-model="regForm.username" placeholder="用户名（3~20 位）" size="large" />
            </el-form-item>
            <el-form-item>
              <el-input v-model="regForm.password" type="password" placeholder="密码（6~64 位）" show-password size="large" />
            </el-form-item>
            <el-form-item>
              <el-input v-model="regForm.confirm" type="password" placeholder="确认密码" show-password size="large" />
            </el-form-item>
            <el-button class="login-btn" :loading="regLoading" @click="doRegister">注 册</el-button>
          </el-form>

          <div class="switch-hint">
            已有账号，<a class="switch-link" @click="toLogin">返回登录</a>
          </div>
        </template>
      </div>
    </section>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '../api/request'

const router = useRouter()

/** 面板两态：login / register 共用同一块玻璃，不弹窗，视觉统一 */
const mode = ref('login')

const form = reactive({ username: '', password: '' })
const regForm = reactive({ username: '', password: '', confirm: '' })
const loading = ref(false)
const regLoading = ref(false)

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

/** 切到注册：带上已输入的用户名，少打一遍 */
function toRegister() {
  regForm.username = form.username
  regForm.password = ''
  regForm.confirm = ''
  mode.value = 'register'
}

function toLogin() {
  form.password = ''
  mode.value = 'login'
}

async function doRegister() {
  if (!regForm.username || !regForm.password) {
    ElMessage.warning('请填写用户名和密码')
    return
  }
  if (regForm.password !== regForm.confirm) {
    ElMessage.warning('两次输入的密码不一致')
    return
  }
  regLoading.value = true
  try {
    await request.post('/auth/register', {
      username: regForm.username,
      password: regForm.password
    })
    ElMessage.success('注册成功，请登录')
    form.username = regForm.username
    form.password = ''
    mode.value = 'login'
  } catch (e) {
    // 业务错误（如 1002 用户名已存在）已由 request 拦截器统一提示，这里只保证按钮复位
  } finally {
    regLoading.value = false
  }
}
</script>

<style scoped>
/* ===================== 页面骨架：全屏 50/50 ===================== */
.login-page {
  position: fixed;
  inset: 0;
  display: grid;
  grid-template-columns: 1fr 1fr;
  overflow: hidden;
  background: #05070d;
  font-family: "Inter", "HarmonyOS Sans SC", "PingFang SC", "Microsoft YaHei", system-ui, sans-serif;
  -webkit-font-smoothing: antialiased;
}

/* ===================== 背景光晕层（纯 CSS，零图片） ===================== */
.bg {
  position: absolute;
  inset: 0;
  z-index: 0;
  overflow: hidden;
}

/* 三团主光：青 / 紫 / 蓝。径向渐变 + 大半径模糊 = 柔光团，只动 transform/opacity，走 GPU */
.orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(90px);
  will-change: transform, opacity;
}
.orb-cyan {
  width: 640px;
  height: 640px;
  left: -10%;
  top: 4%;
  background: radial-gradient(circle, rgba(34, 211, 238, 0.9) 0%, rgba(34, 211, 238, 0) 68%);
  animation: drift-a 26s ease-in-out infinite alternate, breathe 9s ease-in-out infinite;
}
.orb-violet {
  width: 560px;
  height: 560px;
  right: 4%;
  top: -8%;
  background: radial-gradient(circle, rgba(168, 85, 247, 0.8) 0%, rgba(168, 85, 247, 0) 68%);
  animation: drift-b 31s ease-in-out infinite alternate, breathe 11s ease-in-out infinite;
}
.orb-blue {
  width: 720px;
  height: 720px;
  left: 14%;
  bottom: -24%;
  background: radial-gradient(circle, rgba(59, 130, 246, 0.75) 0%, rgba(59, 130, 246, 0) 70%);
  animation: drift-c 34s ease-in-out infinite alternate, breathe 13s ease-in-out infinite;
}

/* 两道流线型光带：细长椭圆横向扫过，制造"速度"方向感 */
.stream {
  position: absolute;
  width: 78%;
  height: 180px;
  filter: blur(46px);
  will-change: transform, opacity;
}
.stream-1 {
  left: -18%;
  top: 46%;
  background: linear-gradient(90deg, rgba(34, 211, 238, 0), rgba(45, 212, 191, 0.55), rgba(34, 211, 238, 0));
  animation: sweep 14s linear infinite;
}
.stream-2 {
  left: -30%;
  top: 72%;
  height: 120px;
  background: linear-gradient(90deg, rgba(99, 102, 241, 0), rgba(139, 92, 246, 0.42), rgba(99, 102, 241, 0));
  animation: sweep 19s linear infinite 3s;
}

/* 1px 极细光丝：快速掠过，强化速度线 */
.streak {
  position: absolute;
  left: 0;
  width: 34%;
  height: 1px;
  opacity: 0;
  background: linear-gradient(90deg, rgba(34, 211, 238, 0), rgba(103, 232, 249, 0.95), rgba(34, 211, 238, 0));
  animation: shoot 9s ease-in-out infinite;
}
.streak-1 {
  top: 31%;
}
.streak-2 {
  top: 63%;
  animation-duration: 13s;
  animation-delay: 2.4s;
}

/* 细网格：给暗底一点结构感，用径向遮罩只在左侧中心可见 */
.grid-lines {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.035) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.035) 1px, transparent 1px);
  background-size: 72px 72px;
  -webkit-mask-image: radial-gradient(circle at 32% 38%, #000 0%, transparent 72%);
  mask-image: radial-gradient(circle at 32% 38%, #000 0%, transparent 72%);
}

/* ===================== 动画 ===================== */
@keyframes drift-a {
  from { transform: translate3d(0, 0, 0) scale(1); }
  to   { transform: translate3d(90px, 60px, 0) scale(1.12); }
}
@keyframes drift-b {
  from { transform: translate3d(0, 0, 0) scale(1.06); }
  to   { transform: translate3d(-80px, 70px, 0) scale(0.95); }
}
@keyframes drift-c {
  from { transform: translate3d(0, 0, 0) scale(0.96); }
  to   { transform: translate3d(70px, -70px, 0) scale(1.1); }
}
@keyframes breathe {
  0%, 100% { opacity: 0.42; }
  50%      { opacity: 0.72; }
}
@keyframes sweep {
  0%   { transform: translateX(-10%) rotate(-16deg); opacity: 0; }
  12%  { opacity: 1; }
  88%  { opacity: 0.8; }
  100% { transform: translateX(70%) rotate(-16deg); opacity: 0; }
}
@keyframes shoot {
  0%   { transform: translateX(-30%) scaleX(0.4); opacity: 0; }
  20%  { opacity: 0.9; }
  80%  { opacity: 0.5; }
  100% { transform: translateX(320%) scaleX(1); opacity: 0; }
}

/* ===================== 左侧品牌区（与右侧表单对称居中） ===================== */
.panel-left {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 56px;
  text-align: center;
}
.brand-kicker {
  font-size: 11px;
  letter-spacing: 0.34em;
  color: rgba(94, 234, 212, 0.9);
}
.brand-title {
  margin: 20px 0 16px;
  font-size: 46px;
  font-weight: 200;
  line-height: 1.16;
  letter-spacing: 0.02em;
  color: #f4f9ff;
}
.brand-sub {
  margin: 0;
  font-size: 14px;
  letter-spacing: 0.16em;
  color: rgba(214, 232, 255, 0.5);
}
/* 技术栈署名固定在左下，不参与居中——避免把品牌块整体顶偏 */
.brand-foot {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 56px;
  text-align: center;
  font-size: 11px;
  letter-spacing: 0.22em;
  color: rgba(214, 232, 255, 0.28);
}
.brand-foot::before {
  content: '';
  display: block;
  width: 132px;
  height: 1px;
  margin: 0 auto 16px;
  background: linear-gradient(90deg, rgba(34, 211, 238, 0), rgba(34, 211, 238, 0.7), rgba(34, 211, 238, 0));
}

/* ===================== 右侧毛玻璃面板 ===================== */
.panel-right {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px;
  /* 两层叠加：斜向高光 + 半透明底 —— 高光让"玻璃"有反光面，不只是半透明色块 */
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.07) 0%, rgba(255, 255, 255, 0) 44%),
    rgba(255, 255, 255, 0.04);
  backdrop-filter: blur(28px) saturate(150%);
  -webkit-backdrop-filter: blur(28px) saturate(150%);
  border-left: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow: inset 1px 0 0 rgba(255, 255, 255, 0.06);
}
/* 右上角反光：模拟光线打在玻璃边缘 */
.panel-right::before {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
  background: radial-gradient(120% 62% at 100% 0%, rgba(255, 255, 255, 0.09), transparent 62%);
}
.form-box {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 360px;
}
.form-title {
  margin: 0 0 8px;
  font-size: 26px;
  font-weight: 300;
  letter-spacing: 0.1em;
  color: #f2f7ff;
}
.form-sub {
  margin: 0;
  font-size: 13px;
  letter-spacing: 0.06em;
  color: rgba(230, 241, 255, 0.42);
}
.form-line {
  width: 44px;
  height: 1px;
  margin: 22px 0 30px;
  background: linear-gradient(90deg, #22d3ee, rgba(99, 102, 241, 0.2));
}

/* ===================== 表单控件（Element Plus 暗色玻璃化） ===================== */
:deep(.el-form-item) {
  margin-bottom: 18px;
}
:deep(.el-input__wrapper) {
  padding: 2px 16px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.05);
  box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.12) inset;
  transition: box-shadow 0.25s ease, background 0.25s ease;
}
:deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.24) inset;
}
:deep(.el-input__wrapper.is-focus) {
  background: rgba(34, 211, 238, 0.07);
  box-shadow: 0 0 0 1px rgba(34, 211, 238, 0.7) inset, 0 0 20px rgba(34, 211, 238, 0.18);
}
:deep(.el-input__inner) {
  height: 42px;
  color: #e6f1ff;
  caret-color: #22d3ee;
}
:deep(.el-input__inner::placeholder) {
  color: rgba(230, 241, 255, 0.45);
  letter-spacing: 0.04em;
}
:deep(.el-input__password) {
  color: rgba(230, 241, 255, 0.4);
}

/* 主按钮：青→靛蓝渐变，唯一的强引导元素 */
:deep(.el-button.login-btn) {
  width: 100%;
  height: 46px;
  margin-top: 6px;
  border: none;
  border-radius: 10px;
  background-color: transparent;
  background-image: linear-gradient(100deg, #22d3ee 0%, #6366f1 100%);
  color: #04131b;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 0.24em;
  box-shadow: 0 10px 26px rgba(34, 211, 238, 0.22);
  transition: transform 0.2s ease, box-shadow 0.2s ease, filter 0.2s ease;
}
:deep(.el-button.login-btn:hover) {
  transform: translateY(-1px);
  filter: brightness(1.08);
  box-shadow: 0 14px 32px rgba(34, 211, 238, 0.3);
}
:deep(.el-button.login-btn:active) {
  transform: translateY(0);
}
:deep(.el-button.login-btn.is-loading) {
  opacity: 0.85;
}

/* 登录/注册切换提示：按钮右下角 */
.switch-hint {
  margin-top: 14px;
  text-align: right;
  font-size: 13px;
  letter-spacing: 0.04em;
  color: rgba(230, 241, 255, 0.4);
}
.switch-link {
  color: #22d3ee;
  cursor: pointer;
  transition: color 0.2s ease, text-shadow 0.2s ease;
}
.switch-link:hover {
  color: #67e8f9;
  text-shadow: 0 0 12px rgba(34, 211, 238, 0.6);
}

/* ===================== 窄屏兜底：单列，只留玻璃面板 ===================== */
@media (max-width: 900px) {
  .login-page {
    grid-template-columns: 1fr;
  }
  .panel-left {
    display: none;
  }
  .panel-right {
    border-left: none;
    background: rgba(5, 7, 13, 0.5);
  }
}
</style>
