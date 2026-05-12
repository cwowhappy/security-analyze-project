import { createApp } from 'vue'
import { createPinia } from 'pinia'
import router from './router'
import './style.css'
import App from './App.vue'
import { useAuthStore } from './stores/modules/auth'

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.mount('#app')

// 初始化认证状态
const authStore = useAuthStore()
authStore.initAuth()
