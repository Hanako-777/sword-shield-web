/**
 * 前端应用入口。
 *
 * - 初始化 Vue App
 * - 注册 Router
 * - 引入并注册 Element Plus 与全量图标（便于页面直接使用）
 */
import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import ElementPlus from 'element-plus'
import '@fontsource-variable/inter'
import '@fontsource/noto-sans-sc/400.css'
import '@fontsource/noto-sans-sc/500.css'
import '@fontsource/noto-sans-sc/600.css'
import 'element-plus/dist/index.css'
// 设计规范与全局样式：必须在 Element Plus 样式之后引入，才能覆盖其主题变量
import './styles/tokens.css'
import './styles/base.css'

const app = createApp(App)

app.use(router)
app.use(ElementPlus)
app.mount('#app')
