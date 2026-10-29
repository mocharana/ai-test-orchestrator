import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
// 主题覆盖必须在 Element Plus 样式之后引入，才能盖住默认色值
import './styles/theme.css'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import App from './App.vue'
import router from './router'

// 统一域名：127.0.0.1 与 localhost 是不同源，localStorage 登录态不互通，
// 双地址交替打开会造成"时好时坏"的 401——统一归到 localhost（保留原路径）
if (location.hostname === '127.0.0.1') {
  location.replace('http://localhost:5173' + location.pathname + location.search)
}

const app = createApp(App)
app.use(ElementPlus, { locale: zhCn })
app.use(router)
app.mount('#app')
