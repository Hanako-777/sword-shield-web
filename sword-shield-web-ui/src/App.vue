<template>
  <div class="app-container">
    <header class="app-header">
      <div class="header-content">
        <div class="logo" @click="$router.push('/')">
          <span class="shield-icon">🛡️</span>
          <div class="logo-block">
            <span class="logo-text">西湖论剑</span>
            <span class="logo-sub">数字安全大会</span>
          </div>
        </div>
        <button class="mobile-menu-btn" @click="drawer = true" aria-label="打开菜单">☰</button>
        <nav class="nav-menu">
          <router-link to="/" class="nav-link">首页</router-link>
          <router-link to="/activities" class="nav-link">活动</router-link>
          <router-link to="/articles" class="nav-link">资讯</router-link>
          <router-link to="/exhibitors" class="nav-link">展商</router-link>
          <router-link to="/achievements" class="nav-link">成果</router-link>
          <router-link to="/materials" class="nav-link">资料</router-link>
          <router-link to="/payment" class="nav-link">购票</router-link>
          <router-link to="/media" class="nav-link">媒体</router-link>
          <router-link to="/statistics" class="nav-link">统计</router-link>
          <router-link to="/submit" class="nav-link">投稿</router-link>
          <router-link to="/checkin" class="nav-link">签到</router-link>
        </nav>
        <div class="user-actions">
          <el-button 
            size="small" 
            @click="$router.push('/admin/login')" 
            class="btn-admin"
            :icon="Lock"
          >
            管理后台
          </el-button>
          <template v-if="isLoggedIn">
            <router-link to="/profile" class="btn-profile">
              <el-avatar :size="32" :src="userAvatar || undefined">{{ username?.charAt(0) || 'U' }}</el-avatar>
              <span class="username">{{ username }}</span>
            </router-link>
            <el-button size="small" @click="logout" text>退出</el-button>
          </template>
          <template v-else>
            <el-button type="primary" size="small" @click="$router.push('/login')">登录</el-button>
            <el-button size="small" class="btn-register" @click="$router.push('/register')" plain>注册</el-button>
          </template>
        </div>
      </div>
    </header>
    <main class="main-content">
      <router-view />
    </main>
    <AiAssistant />
    <footer class="app-footer">
      <p>&copy; 2026 西湖论剑·数字安全大会 · 数字安全活动平台</p>
    </footer>

    <el-drawer v-model="drawer" direction="ltr" size="72%" :with-header="false">
      <div class="drawer-brand">
        <span class="shield-icon">🛡️</span>
        <div>
          <div class="drawer-title">西湖论剑</div>
          <div class="drawer-sub">数字安全大会</div>
        </div>
      </div>
      <nav class="drawer-nav">
        <router-link v-for="item in navItems" :key="item.path" :to="item.path" @click="drawer = false">
          {{ item.label }}
        </router-link>
      </nav>
      <el-button v-if="!isLoggedIn" type="primary" class="drawer-login" @click="drawer = false; $router.push('/login')">
        登录 / 注册
      </el-button>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Lock } from '@element-plus/icons-vue'
import AiAssistant from './components/AiAssistant.vue'

const router = useRouter()
const route = useRoute()
const drawer = ref(false)
const navItems = [
  { path: '/', label: '首页' },
  { path: '/activities', label: '活动' },
  { path: '/articles', label: '资讯' },
  { path: '/exhibitors', label: '展商' },
  { path: '/achievements', label: '成果' },
  { path: '/materials', label: '资料' },
  { path: '/payment', label: '购票' },
  { path: '/media', label: '媒体' },
  { path: '/statistics', label: '统计' },
  { path: '/submit', label: '投稿' },
  { path: '/checkin', label: '签到' }
]
const username = ref(localStorage.getItem('username') || '')
const userAvatar = ref(localStorage.getItem('userAvatar') || '')
const token = ref(localStorage.getItem('token') || '')

const isLoggedIn = computed(() => !!token.value)

// Watch for route changes to update user info
watch(() => route.path, () => {
  username.value = localStorage.getItem('username') || ''
  userAvatar.value = localStorage.getItem('userAvatar') || ''
  token.value = localStorage.getItem('token') || ''
})

// Listen to custom login event
if (typeof window !== 'undefined') {
  window.addEventListener('user-login', () => {
    username.value = localStorage.getItem('username') || ''
    userAvatar.value = localStorage.getItem('userAvatar') || ''
    token.value = localStorage.getItem('token') || ''
  })
}

// Listen to storage events from other tabs/windows
if (typeof window !== 'undefined') {
  window.addEventListener('storage', (e) => {
    if (e.key === 'username' || e.key === 'userAvatar' || e.key === 'token') {
      username.value = localStorage.getItem('username') || ''
      userAvatar.value = localStorage.getItem('userAvatar') || ''
      token.value = localStorage.getItem('token') || ''
    }
  })
}

function logout() {
  localStorage.removeItem('token')
  localStorage.removeItem('userId')
  localStorage.removeItem('username')
  localStorage.removeItem('userAvatar')
  username.value = ''
  userAvatar.value = ''
  token.value = ''
  router.push('/login')
}

onMounted(() => {
  // Refresh user info on mount
  username.value = localStorage.getItem('username') || ''
  userAvatar.value = localStorage.getItem('userAvatar') || ''
  token.value = localStorage.getItem('token') || ''
})
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
  background: linear-gradient(135deg, #000000 0%, #ff0050 50%, #00f2ea 100%);
  min-height: 100vh;
  overflow-x: hidden;
}

.app-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.app-header {
  background: rgba(255, 255, 255, 0.98);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  position: sticky;
  top: 0;
  z-index: 1000;
}

.header-content {
  max-width: 1400px;
  margin: 0 auto;
  padding: 16px 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: nowrap;
}

.logo {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  transition: transform 0.2s;
}

.logo:hover {
  transform: scale(1.05);
}

.shield-icon {
  font-size: 32px;
}

.logo-text {
  font-size: 22px;
  font-weight: bold;
  background: linear-gradient(135deg, #ff0050 0%, #00f2ea 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  letter-spacing: 1px;
}

.logo-block {
  display: flex;
  flex-direction: column;
  line-height: 1.15;
}

.logo-sub {
  font-size: 11px;
  letter-spacing: 3px;
  color: #888;
}

.nav-menu {
  display: flex;
  gap: 16px;
  flex: 1;
  justify-content: center;
  align-items: center;
  flex-wrap: nowrap;
  overflow-x: auto;
  white-space: nowrap;
}

.nav-link {
  text-decoration: none;
  color: #333;
  font-weight: 500;
  padding: 8px 12px;
  border-radius: 8px;
  transition: all 0.3s;
  white-space: nowrap;
  flex-shrink: 0;
}

.nav-link:hover {
  background: #f5f5f5;
  color: #ff0050;
}

.nav-link.router-link-active {
  background: linear-gradient(135deg, #ff0050 0%, #00f2ea 100%);
  color: white;
}

.user-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.btn-profile {
  display: flex;
  align-items: center;
  gap: 8px;
  text-decoration: none;
  color: #333;
  padding: 6px 12px;
  border-radius: 20px;
  transition: background 0.3s;
}

.btn-profile:hover {
  background: #f5f5f5;
}

.username {
  font-size: 14px;
  font-weight: 500;
}

.btn-admin {
  background: linear-gradient(135deg, #ff0050 0%, #00f2ea 100%);
  color: white !important;
  border: none;
  font-weight: 500;
}

.btn-admin span {
  color: #fff !important;
}

.mobile-menu-btn {
  display: none;
  border: none;
  background: #f5f5f5;
  border-radius: 8px;
  font-size: 22px;
  line-height: 1;
  padding: 6px 10px;
  cursor: pointer;
}

.btn-admin:hover {
  opacity: 0.9;
  background: linear-gradient(135deg, #ff0050 0%, #00f2ea 100%);
  color: white;
}

.main-content {
  flex: 1;
  padding: 32px 16px;
}

.logo-text,
.logo-sub {
  white-space: nowrap;
}

.drawer-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 4px 20px;
  border-bottom: 1px solid #eee;
  margin-bottom: 12px;
}
.drawer-title {
  font-size: 20px;
  font-weight: 700;
  background: linear-gradient(135deg, #ff0050, #00f2ea);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}
.drawer-sub {
  font-size: 11px;
  color: #999;
  letter-spacing: 2px;
}
.drawer-nav {
  display: flex;
  flex-direction: column;
}
.drawer-nav a {
  padding: 14px 8px;
  color: #333;
  text-decoration: none;
  font-size: 15px;
  border-radius: 8px;
}
.drawer-nav a:hover,
.drawer-nav a.router-link-active {
  background: #fff0f5;
  color: #ff0050;
}
.drawer-login {
  width: 100%;
  margin-top: 16px;
}

@media (max-width: 900px) {
  .header-content {
    padding: 10px 12px;
    gap: 8px;
  }
  .nav-menu {
    display: none;
  }
  .mobile-menu-btn {
    display: inline-flex;
    align-items: center;
    justify-content: center;
  }
  .logo-sub {
    display: none;
  }
  .logo-text {
    font-size: 19px;
  }
  .shield-icon {
    font-size: 26px;
  }
  .user-actions .btn-admin,
  .user-actions .username {
    display: none !important;
  }
  /* 未登录时只保留“登录”，注册入口在登录页与抽屉菜单中 */
  .user-actions .btn-register {
    display: none !important;
  }
  .user-actions {
    gap: 6px;
    flex-shrink: 0;
  }
  .logo {
    flex-shrink: 0;
    gap: 6px;
  }
  .main-content {
    padding: 16px 10px;
  }
}

.app-footer {
  background: rgba(255, 255, 255, 0.95);
  text-align: center;
  padding: 24px;
  color: #666;
  font-size: 14px;
}
</style>
