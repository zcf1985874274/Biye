<template>
  <div class="forgot-password-container">
    <div class="forgot-password-box">
      <h2>找回密码</h2>
      
      <!-- 步骤1: 输入用户名 -->
      <div v-if="currentStep === 1" class="step-form">
        <div class="form-item">
          <input
            v-model="forgotForm.username"
            placeholder="请输入用户名"
            type="text"
            @keyup.enter="checkUsername"
          />
        </div>
        <div class="form-item">
          <button @click="checkUsername" :disabled="loading">
            {{ loading ? '查询中...' : '下一步' }}
          </button>
        </div>
        <div class="form-links">
          <router-link to="/login">返回登录</router-link>
        </div>
      </div>
      
      <!-- 步骤2: 验证手机号 -->
      <div v-if="currentStep === 2" class="step-form">
        <div class="phone-display">
          <p>您的手机号: {{ maskedPhone }}</p>
        </div>
        <div class="form-item">
          <input
            v-model="forgotForm.fullPhone"
            placeholder="请输入完整的手机号"
            type="text"
            @keyup.enter="verifyPhone"
          />
        </div>
        <div class="form-item">
          <button @click="verifyPhone" :disabled="loading">
            {{ loading ? '验证中...' : '验证手机号' }}
          </button>
        </div>
        <div class="form-item">
          <button @click="resetToStep1" class="secondary-btn">
            返回上一步
          </button>
        </div>
      </div>
      
      <!-- 步骤3: 重置密码 -->
      <div v-if="currentStep === 3" class="step-form">
        <div class="form-item">
          <input
            v-model="forgotForm.newPassword"
            placeholder="请输入新密码"
            type="password"
          />
        </div>
        <div class="form-item">
          <input
            v-model="forgotForm.confirmPassword"
            placeholder="请确认新密码"
            type="password"
            @keyup.enter="resetPassword"
          />
        </div>
        <div class="form-item">
          <button @click="resetPassword" :disabled="loading">
            {{ loading ? '重置中...' : '重置密码' }}
          </button>
        </div>
        <div class="form-item">
          <button @click="resetToStep1" class="secondary-btn">
            返回上一步
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import * as userApi from '@/api/user'

export default {
  name: 'ForgotPassword',
  data() {
    return {
      currentStep: 1, // 当前步骤：1-输入用户名，2-验证手机号，3-重置密码
      loading: false,
      forgotForm: {
        username: '',
        fullPhone: '',
        newPassword: '',
        confirmPassword: ''
      },
      maskedPhone: '', // 脱敏显示的手机号
      userData: null // 存储用户数据
    }
  },
  methods: {
    // 步骤1：检查用户名是否存在
    async checkUsername() {
      if (!this.forgotForm.username) {
        this.$message.error('请输入用户名')
        return
      }
      
      this.loading = true
      try {
        const response = await userApi.checkUsername(this.forgotForm.username)
        if (response.code === 200) {
          this.userData = response.data
          // 对手机号进行脱敏处理：显示前3位和后2位，中间用*代替
          const phone = this.userData.phone
          if (phone && phone.length >= 5) {
            this.maskedPhone = phone.substring(0, 3) + '****' + phone.substring(phone.length - 2)
          } else {
            this.maskedPhone = phone
          }
          this.currentStep = 2
        } else {
          this.$message.error(response.message || '用户名不存在')
        }
      } catch (error) {
        this.$message.error('查询失败，请稍后重试')
      } finally {
        this.loading = false
      }
    },
    
    // 步骤2：验证手机号
    async verifyPhone() {
      if (!this.forgotForm.fullPhone) {
        this.$message.error('请输入完整的手机号')
        return
      }
      
      this.loading = true
      try {
        const response = await userApi.verifyPhone({
          username: this.forgotForm.username,
          phone: this.forgotForm.fullPhone
        })
        
        if (response.code === 200) {
          this.currentStep = 3
        } else {
          this.$message.error(response.message || '手机号验证失败')
        }
      } catch (error) {
        this.$message.error('手机号验证失败，请稍后重试')
      } finally {
        this.loading = false
      }
    },
    
    // 步骤3：重置密码
    async resetPassword() {
      if (!this.forgotForm.newPassword) {
        this.$message.error('请输入新密码')
        return
      }
      
      if (this.forgotForm.newPassword.length < 6) {
        this.$message.error('密码长度不能少于6位')
        return
      }
      
      if (this.forgotForm.newPassword !== this.forgotForm.confirmPassword) {
        this.$message.error('两次输入的密码不一致')
        return
      }
      
      this.loading = true
      try {
        const response = await userApi.resetPassword({
          username: this.forgotForm.username,
          newPassword: this.forgotForm.newPassword
        })
        
        if (response.code === 200) {
          this.$message.success('密码重置成功，请使用新密码登录')
          this.$router.push('/login')
        } else {
          this.$message.error(response.message || '密码重置失败')
        }
      } catch (error) {
        this.$message.error('密码重置失败，请稍后重试')
      } finally {
        this.loading = false
      }
    },
    
    // 返回第一步
    resetToStep1() {
      this.currentStep = 1
      this.forgotForm = {
        username: '',
        fullPhone: '',
        newPassword: '',
        confirmPassword: ''
      }
      this.maskedPhone = ''
      this.userData = null
    }
  }
}
</script>

<style scoped>
.forgot-password-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background-color: #f5f5f5;
}

.forgot-password-box {
  width: 400px;
  padding: 40px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

h2 {
  text-align: center;
  margin-bottom: 30px;
  color: #333;
}

.step-form {
  display: flex;
  flex-direction: column;
}

.form-item {
  margin-bottom: 20px;
}

.form-item input {
  width: 100%;
  height: 40px;
  padding: 0 15px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  font-size: 14px;
  transition: border-color 0.3s;
}

.form-item input:focus {
  border-color: #409eff;
  outline: none;
}

.form-item button {
  width: 100%;
  height: 40px;
  background-color: #409eff;
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
  transition: background-color 0.3s;
}

.form-item button:hover {
  background-color: #66b1ff;
}

.form-item button:disabled {
  background-color: #a0cfff;
  cursor: not-allowed;
}

.secondary-btn {
  background-color: #909399 !important;
}

.secondary-btn:hover {
  background-color: #a6a9ad !important;
}

.phone-display {
  background-color: #f8f9fa;
  padding: 15px;
  border-radius: 4px;
  margin-bottom: 20px;
  text-align: center;
}

.phone-display p {
  margin: 0;
  font-size: 16px;
  color: #333;
}

.form-links {
  text-align: center;
  margin-top: 20px;
}

.form-links a {
  color: #409eff;
  text-decoration: none;
  font-size: 14px;
}

.form-links a:hover {
  text-decoration: underline;
}
</style>