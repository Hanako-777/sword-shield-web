<template>
  <div class="profile-container">
    <el-card class="profile-card">
      <div class="profile-header">
        <div class="avatar-section">
          <el-avatar :size="100" :src="form.avatarUrl || undefined" class="large-avatar">
            {{ form.nickname?.charAt(0) || 'U' }}
          </el-avatar>
          <label class="upload-label">
            <input type="file" @change="onFile" accept="image/*" hidden ref="fileInput" />
            <el-button type="primary" size="small" :loading="uploading" @click="triggerUpload">
              <Icon name="camera" :size="14" style="margin-right: 4px" />
              {{ uploading ? '上传中...' : '更换头像' }}
            </el-button>
          </label>
        </div>
        <h2 class="profile-title">个人信息</h2>
      </div>

      <el-divider />

      <el-form :model="form" label-width="100px" class="profile-form">
        <el-form-item label="用户名">
          <el-input 
            v-model="form.username" 
            disabled
            size="large"
          />
        </el-form-item>
        
        <el-form-item label="昵称">
          <el-input 
            v-model="form.nickname" 
            placeholder="请输入昵称"
            size="large"
          />
        </el-form-item>
        
        <el-form-item label="性别">
          <el-radio-group v-model="form.gender" size="large">
            <el-radio label="男">男</el-radio>
            <el-radio label="女">女</el-radio>
            <el-radio label="保密">保密</el-radio>
          </el-radio-group>
        </el-form-item>
        
        <el-form-item label="手机号">
          <el-input 
            v-model="form.phone" 
            placeholder="请输入手机号"
            size="large"
            maxlength="11"
          />
        </el-form-item>
        
        <el-form-item label="邮箱">
          <el-input 
            v-model="form.email" 
            placeholder="请输入邮箱"
            size="large"
            type="email"
          />
        </el-form-item>
        
        <el-form-item>
          <el-button 
            type="primary" 
            @click="save" 
            :loading="saving"
            size="large"
            class="save-button"
          >
            保存修改
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
/**
 * 用户资料页。
 *
 * - 展示并编辑个人资料（昵称、性别、联系方式、头像等）。
 * - 支持头像上传：上传文件 → 生成预签名 URL → 保存为 avatarUrl。
 *
 * 依赖接口（部分）：
 * - GET `/user/profile`、PUT `/user/profile`
 * - POST `/file/upload`、GET `/file/presigned`
 */
import { reactive, ref, onMounted } from 'vue'
import Icon from '../components/ui/Icon.vue'
import api from '../api'
import { ElMessage } from 'element-plus'

const form = reactive<any>({ 
  id: null, 
  username: '', 
  nickname: '', 
  gender: '保密', 
  phone: '', 
  email: '', 
  avatarUrl: '' 
})
const saving = ref(false)
const uploading = ref(false)
const fileInput = ref<HTMLInputElement | null>(null)

function triggerUpload() {
  fileInput.value?.click()
}

async function load() {
  const userId = localStorage.getItem('userId')
  if (!userId) return
  
  try {
    const { data } = await api.get('/user/profile', { params: { id: userId } })
    if (data.code === 0) {
      Object.assign(form, data.data)
      // Update localStorage with latest info
      if (data.data.nickname) {
        localStorage.setItem('username', data.data.nickname)
      }
      if (data.data.avatarUrl) {
        localStorage.setItem('userAvatar', data.data.avatarUrl)
      }
    }
  } catch (error) {
    ElMessage.error('加载用户信息失败')
  }
}

async function save() {
  if (!form.nickname?.trim()) {
    ElMessage.warning('请输入昵称')
    return
  }
  
  // Validate phone number if provided
  if (form.phone && !/^1[3-9]\d{9}$/.test(form.phone)) {
    ElMessage.warning('请输入正确的手机号码')
    return
  }
  
  // Validate email if provided
  if (form.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) {
    ElMessage.warning('请输入正确的邮箱地址')
    return
  }
  
  saving.value = true
  try {
    const payload = {
      id: form.id,
      nickname: form.nickname,
      gender: form.gender,
      phone: form.phone,
      email: form.email,
      avatarUrl: form.avatarUrl
    }
    const { data } = await api.put('/user/profile', payload)
    if (data.code === 0) {
      ElMessage.success('保存成功')
      // Update localStorage
      localStorage.setItem('username', form.nickname)
      if (form.avatarUrl) {
        localStorage.setItem('userAvatar', form.avatarUrl)
      }
      // Dispatch custom event to notify App.vue
      window.dispatchEvent(new Event('user-login'))
    } else {
      ElMessage.error(data.message || '保存失败')
    }
  } catch (error) {
    ElMessage.error('保存失败，请稍后重试')
  } finally {
    saving.value = false
  }
}

async function onFile(e: Event) {
  const files = (e.target as HTMLInputElement).files
  if (!files || files.length === 0) return
  
  const file = files[0]
  
  // Validate file size (max 5MB)
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.warning('图片大小不能超过5MB')
    return
  }
  
  // Validate file type
  if (!file.type.startsWith('image/')) {
    ElMessage.warning('请选择图片文件')
    return
  }
  
  uploading.value = true
  try {
    const fd = new FormData()
    fd.append('file', file)
    
    const { data } = await api.post('/file/upload', fd, { 
      headers: { 'Content-Type': 'multipart/form-data' } 
    })
    
    if (data.code === 0) {
      const pres = await api.get('/file/presigned', { 
        params: { objectName: data.data.objectName } 
      })
      form.avatarUrl = pres.data.data
      ElMessage.success('头像上传成功，请点击保存修改')
    } else {
      ElMessage.error(data.message || '上传失败')
    }
  } catch (error: any) {
    console.error('头像上传失败:', error)
    const msg = error.response?.data?.message || error.message || '上传失败，请稍后重试'
    ElMessage.error(msg)
  } finally {
    uploading.value = false
    // Reset file input
    if (fileInput.value) {
      fileInput.value.value = ''
    }
  }
}

onMounted(load)
</script>

<style scoped>
.profile-container {
  max-width: 800px;
  margin: 0 auto;
}

.profile-card {
  border-radius: 8px;
  border: 1px solid #d8e0e5;
  box-shadow: none;
}

.profile-header {
  text-align: center;
  margin-bottom: 20px;
}

.avatar-section {
  margin-bottom: 16px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;
}

.large-avatar {
  background: #0b2545;
  font-size: 36px;
  font-weight: 600;
  color: #ffffff;
}

.upload-label {
  cursor: pointer;
}

.profile-title {
  font-size: 22px;
  color: #0b2545;
  margin: 0;
  font-weight: 700;
}

.profile-form {
  margin-top: 24px;
}

.profile-form :deep(.el-form-item__label) {
  font-weight: 500;
  color: #506273;
}

.profile-form :deep(.el-input__inner) {
  border-radius: 6px;
}

.profile-form :deep(.el-radio-group) {
  display: flex;
  gap: 24px;
}

.save-button {
  width: 100%;
  background: #0b2545;
  border-color: #0b2545;
  font-size: 15px;
  font-weight: 600;
  margin-top: 8px;
}

.save-button:hover {
  background: #183a63;
  border-color: #183a63;
}
</style>
