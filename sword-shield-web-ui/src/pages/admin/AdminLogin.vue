<template>
  <div class="auth-page admin-auth-page">
    <div class="auth-card admin-auth-card">
      <div class="auth-header">
        <div class="auth-top-row">
          <h1 class="auth-title">管理后台登录</h1>
          <router-link class="auth-switch" to="/login">返回前台 →</router-link>
        </div>
        <p class="auth-subtitle">仅限大会工作人员使用，用于管理活动、媒体与数据。</p>
      </div>
      <el-form :model="form" @submit.prevent="login" class="auth-form">
        <el-form-item>
          <el-input 
            v-model="form.username" 
            placeholder="管理员用户名"
            size="large"
            prefix-icon="User"
          />
        </el-form-item>
        <el-form-item>
          <el-input 
            v-model="form.password" 
            type="password" 
            placeholder="管理员密码"
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
            class="auth-button admin-auth-button"
          >
            登录后台
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * 管理员登录页。
 *
 * - 调用后端管理员登录接口换取 JWT，并写入 localStorage：`adminToken/adminId/adminUsername/adminNickname`。
 * - 登录成功后根据 query `redirect` 跳转到后台目标页面（默认 `/admin/dashboard`）。
 *
 * 依赖接口：
 * - POST `/admin/auth/login`
 */
import { reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import adminApi from '../../adminApi'
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
    const { data } = await adminApi.post('/admin/auth/login', form)
    if (data.code === 0) {
      localStorage.setItem('adminToken', data.data.token)
      localStorage.setItem('adminId', data.data.adminId)
      localStorage.setItem('adminUsername', data.data.username)
      localStorage.setItem('adminNickname', data.data.nickname)
      ElMessage.success('登录成功')
      const redirect = (route.query.redirect as string) || '/admin/dashboard'
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
.auth-page.admin-auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px 16px;
  background: #f6f8fa;
}

.auth-card.admin-auth-card {
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

.admin-auth-button {
  width: 100%;
  background: #0b2545;
  border-color: #0b2545;
  font-size: 15px;
  font-weight: 600;
}

.admin-auth-button:hover {
  background: #183a63;
  border-color: #183a63;
}
</style>
