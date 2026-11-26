<template>
  <div class="auth-page">
    <div class="auth-card">
      <div class="auth-header">
        <div class="auth-top-row">
          <h1 class="auth-title">登录西湖论剑</h1>
          <router-link class="auth-switch" to="/admin/login">管理后台 →</router-link>
        </div>
        <p class="auth-subtitle">用于报名活动、下载资料和查看个人信息。</p>
      </div>
      <el-form :model="form" @submit.prevent="login" class="auth-form">
        <el-form-item>
          <el-input
            v-model="form.username"
            placeholder="请输入用户名"
            size="large"
            prefix-icon="User"
          />
        </el-form-item>
        <el-form-item>
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            size="large"
            prefix-icon="Lock"
            show-password
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            @click="login"
            :loading="loading"
            size="large"
            class="auth-button"
          >
            登录
          </el-button>
        </el-form-item>
        <div class="auth-footer">
          <span>还没有账号？</span>
          <el-button link @click="$router.push('/register')" type="primary">
            去注册
          </el-button>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * 用户登录页。
 *
 * - 调用后端登录接口换取 JWT，并写入 localStorage：`token/userId/username/userAvatar`。
 * - 登录成功后根据 query `redirect` 跳回原页面。
 *
 * 依赖接口：
 * - POST `/user/login`
 * - GET `/user/profile`（用于补齐头像/昵称）
 */
import { reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import api from '../api'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()
const form = reactive({ username: '', password: '' })
const loading = ref(false)

async function login() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  
  loading.value = true
  try {
    const { data } = await api.post('/user/login', form)
    if (data.code === 0) {
      localStorage.setItem('token', data.data.token)
      localStorage.setItem('userId', data.data.userId)
      localStorage.setItem('username', form.username)
      
      // Fetch user profile to get avatar and other info
      try {
        const profileRes = await api.get('/user/profile', {
          params: { id: data.data.userId }
        })
        if (profileRes.data.code === 0 && profileRes.data.data) {
          const user = profileRes.data.data
          if (user.avatarUrl) {
            localStorage.setItem('userAvatar', user.avatarUrl)
          }
          if (user.nickname) {
            localStorage.setItem('username', user.nickname)
          }
        }
      } catch (error) {
        console.warn('Failed to fetch user profile:', error)
      }
      
      // Dispatch custom event to notify App.vue
      window.dispatchEvent(new Event('user-login'))
      
      ElMessage.success('登录成功')
      const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
      router.push(redirect)
    } else {
      ElMessage.error(data.message || '登录失败')
    }
  } catch (error) {
    ElMessage.error('登录失败，请稍后重试')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px 16px;
  background: #f6f8fa;
}

.auth-card {
  width: 100%;
  max-width: 440px;
  padding: 32px 32px 28px;
  border-radius: 8px;
  background: #ffffff;
  border: 1px solid #d8e0e5;
  box-shadow: 0 4px 16px rgba(11, 37, 69, 0.06);
}

.auth-header {
  margin-bottom: 24px;
}

.auth-top-row {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
}

.auth-switch {
  font-size: 13px;
  color: #506273;
  text-decoration: none;
  white-space: nowrap;
}

.auth-switch:hover {
  color: #0b2545;
  text-decoration: underline;
}

.auth-title {
  font-size: 22px;
  font-weight: 700;
  color: #0b2545;
}

.auth-subtitle {
  margin-top: 6px;
  font-size: 13px;
  color: #506273;
}

.auth-form {
  margin-top: 8px;
}

.auth-button {
  width: 100%;
  background: #0b2545;
  border-color: #0b2545;
  font-size: 15px;
  font-weight: 600;
}

.auth-button:hover {
  background: #183a63;
  border-color: #183a63;
}

.auth-footer {
  text-align: center;
  margin-top: 8px;
  color: #506273;
  font-size: 13px;
}
</style>
