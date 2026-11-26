<template>
  <div class="admin-container">
    <el-container>
      <el-aside width="250px" class="admin-sidebar">
        <div class="sidebar-header">
          <div class="sidebar-brand-text">
            <h3>西湖论剑</h3>
            <p>管理后台</p>
          </div>
        </div>
        <el-menu
          :default-active="activeMenu"
          class="sidebar-menu"
          @select="handleMenuSelect"
        >
          <el-menu-item index="dashboard">
            <Icon name="home" :size="18" style="margin-right: 8px" />
            <span>仪表盘</span>
          </el-menu-item>
          <el-menu-item index="activities">
            <Icon name="calendar" :size="18" style="margin-right: 8px" />
            <span>活动管理</span>
          </el-menu-item>
          <el-menu-item index="articles">
            <Icon name="book-open" :size="18" style="margin-right: 8px" />
            <span>资讯管理</span>
          </el-menu-item>
          <el-menu-item index="submissions">
            <Icon name="edit" :size="18" style="margin-right: 8px" />
            <span>在线投稿</span>
          </el-menu-item>
          <el-menu-item index="media">
            <Icon name="image" :size="18" style="margin-right: 8px" />
            <span>媒体管理</span>
          </el-menu-item>
          <el-menu-item index="materials">
            <Icon name="file-text" :size="18" style="margin-right: 8px" />
            <span>资料管理</span>
          </el-menu-item>
          <el-menu-item index="logs">
            <Icon name="ticket" :size="18" style="margin-right: 8px" />
            <span>系统日志</span>
          </el-menu-item>
        </el-menu>
        <div class="sidebar-footer">
          <div class="admin-info">
            <div class="info-icon">
              <Icon name="user" :size="16" />
            </div>
            <span class="info-text">系统管理员</span>
          </div>
          <el-button link type="danger" @click="logout" class="logout-btn">
            <Icon name="log-out" :size="16" style="margin-right: 4px" />
            <span>退出登录</span>
          </el-button>
        </div>
      </el-aside>
      
      <el-container>
        <el-header class="admin-header">
          <h2>{{ pageTitle }}</h2>
        </el-header>
        
        <el-main class="admin-main">
          <router-view />
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script setup lang="ts">
/**
 * 后台管理布局页（Dashboard Shell）。
 *
 * - 提供左侧菜单 + 顶部标题区域，并通过子路由渲染各管理模块页面。
 * - 登出时清理管理员 localStorage 并跳转回 `/admin/login`。
 */
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import Icon from '../../components/ui/Icon.vue'

const router = useRouter()
const route = useRoute()

const adminUsername = ref(localStorage.getItem('adminUsername') || '')
const adminNickname = ref(localStorage.getItem('adminNickname') || '')

const activeMenu = computed(() => {
  if (route.path.includes('/admin/dashboard/activities')) return 'activities'
  if (route.path.includes('/admin/dashboard/articles')) return 'articles'
  if (route.path.includes('/admin/dashboard/submissions')) return 'submissions'
  if (route.path.includes('/admin/dashboard/media')) return 'media'
  if (route.path.includes('/admin/dashboard/materials')) return 'materials'
  if (route.path.includes('/admin/dashboard/logs')) return 'logs'
  return 'dashboard'
})

const pageTitle = computed(() => {
  if (route.path.includes('/admin/dashboard/activities')) return '活动管理'
  if (route.path.includes('/admin/dashboard/articles')) return '资讯管理'
  if (route.path.includes('/admin/dashboard/submissions')) return '在线投稿'
  if (route.path.includes('/admin/dashboard/media')) return '媒体资产管理'
  if (route.path.includes('/admin/dashboard/materials')) return '资料管理'
  if (route.path.includes('/admin/dashboard/logs')) return '系统日志与审计'
  return '仪表盘'
})

function handleMenuSelect(index: string) {
  if (index === 'dashboard') {
    router.push('/admin/dashboard')
  } else if (index === 'activities') {
    router.push('/admin/dashboard/activities')
  } else if (index === 'articles') {
    router.push('/admin/dashboard/articles')
  } else if (index === 'submissions') {
    router.push('/admin/dashboard/submissions')
  } else if (index === 'media') {
    router.push('/admin/dashboard/media')
  } else if (index === 'materials') {
    router.push('/admin/dashboard/materials')
  } else if (index === 'logs') {
    router.push('/admin/dashboard/logs')
  }
}

async function logout() {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    localStorage.removeItem('adminToken')
    localStorage.removeItem('adminId')
    localStorage.removeItem('adminUsername')
    localStorage.removeItem('adminNickname')
    
    ElMessage.success('已退出登录')
    router.push('/admin/login')
  } catch {
    // User cancelled
  }
}
</script>

<style scoped>
.admin-container {
  min-height: 100vh;
  height: 100vh;
  background: #f6f8fa;
  overflow: hidden;
}

.admin-container :deep(.el-container) {
  height: 100vh;
}

.admin-sidebar {
  background: #ffffff;
  color: #152f49;
  display: flex;
  flex-direction: column;
  border-right: 1px solid #e2e8ee;
}

.sidebar-header {
  display: flex;
  align-items: center;
  padding: 18px 20px 14px;
  border-bottom: 1px solid #e8edf2;
}

.sidebar-brand-text h3 {
  margin: 0 0 2px 0;
  font-size: 17px;
  font-weight: 700;
  color: #0b2545;
}

.sidebar-brand-text p {
  margin: 0;
  font-size: 12px;
  color: #506273;
}

.sidebar-menu {
  flex: 1;
  border: none;
  background: transparent;
  padding: 10px 0 8px;
}

.sidebar-menu :deep(.el-menu-item) {
  color: #334155;
  margin: 3px 10px;
  border-radius: 6px;
  height: 40px;
  line-height: 40px;
}

.sidebar-menu :deep(.el-menu-item:hover) {
  background-color: #f1f5f9;
  color: #0b2545;
}

.sidebar-menu :deep(.el-menu-item.is-active) {
  background: #0b2545;
  color: #ffffff;
}

.sidebar-menu :deep(.el-menu-item .el-icon) {
  font-size: 16px;
}

.sidebar-footer {
  margin-top: auto;
  padding: 12px 14px 16px;
  border-top: 1px solid #e8edf2;
  background: #f8fafc;
}

.admin-info {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 8px;
  margin-bottom: 6px;
  font-size: 13px;
  color: #334155;
}

.info-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  color: #506273;
}

.info-text {
  flex: 1;
  font-weight: 500;
}

.logout-btn {
  width: 100%;
  color: #b91c1c;
  font-size: 13px;
  padding: 8px 10px;
  display: flex;
  align-items: center;
  gap: 8px;
  justify-content: flex-start;
  border-radius: 6px;
}

.logout-btn:hover {
  background: rgba(185, 28, 28, 0.08);
}

.admin-header {
  background: #ffffff;
  display: flex;
  align-items: center;
  padding: 0 28px;
  border-bottom: 1px solid #e2e8ee;
}

.admin-header h2 {
  margin: 0;
  font-size: 17px;
  font-weight: 600;
  color: #0b2545;
}

.admin-main {
  padding: 20px 24px 22px;
  overflow-y: auto;
  height: calc(100vh - 60px);
  background: transparent;
}
</style>
