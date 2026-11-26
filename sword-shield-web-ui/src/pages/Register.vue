<template>
  <div class="auth-page">
    <div class="auth-card">
      <div class="auth-header">
        <h1 class="auth-title">注册账号</h1>
        <p class="auth-subtitle">创建西湖论剑账号以报名活动、下载大会资料并参与互动。</p>
      </div>
      <el-form :model="form" @submit.prevent="register" class="auth-form">
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
          <el-input
            v-model="form.confirmPassword"
            type="password"
            placeholder="请再次输入密码"
            size="large"
            prefix-icon="Lock"
            show-password
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            @click="register"
            :loading="loading"
            size="large"
            class="auth-button"
          >
            注册
          </el-button>
        </el-form-item>
        <div class="auth-footer">
          <span>已经有账号？</span>
          <el-button link @click="$router.push('/login')" type="primary">
            去登录
          </el-button>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * 用户注册页。
 *
 * - 提交用户名/密码/确认密码完成注册，成功后跳转到登录页。
 *
 * 依赖接口：
 * - POST `/user/register`
 */
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import api from '../api'
import { ElMessage } from 'element-plus'

const router = useRouter()
const form = reactive({ username: '', password: '', confirmPassword: '' })
const loading = ref(false)

async function register() {
  if (!form.username || !form.password || !form.confirmPassword) {
    ElMessage.warning('请填写所有字段')
    return
  }
  
  if (form.password !== form.confirmPassword) {
    ElMessage.warning('两次密码输入不一致')
    return
  }
  
  loading.value = true
  try {
    const { data } = await api.post('/user/register', form)
    if (data.code === 0) {
      ElMessage.success('注册成功，请登录')
      router.push('/login')
    } else {
      ElMessage.error(data.message || '注册失败')
    }
  } catch (error) {
    ElMessage.error('注册失败，请稍后重试')
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
