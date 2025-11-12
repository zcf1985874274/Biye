<template>
  <div class="register-container">
    <div class="register-box">
      <h2>用户注册</h2>
      <div class="register-form">
        <div class="form-item">
          <input
            v-model="registerForm.username"
            placeholder="用户名"
            type="text"
          />
        </div>
        <div class="form-item">
          <input
            v-model="registerForm.password"
            placeholder="密码"
            type="password"
          />
        </div>
        <div class="form-item">
          <input
            v-model="registerForm.confirmPassword"
            placeholder="确认密码"
            type="password"
          />
        </div>
        <div class="form-item">
          <input
            v-model="registerForm.email"
            placeholder="电子邮箱"
            type="email"
          />
        </div>
        <div class="form-item">
          <input
            v-model="registerForm.phone"
            placeholder="手机号码"
            type="text"
          />
        </div>
        <div class="form-item captcha-item">
          <input
            v-model="registerForm.captcha"
            placeholder="验证码"
            type="text"
            class="captcha-input"
            @keyup.enter="handleRegister"
          />
          <canvas
            ref="captchaCanvas"
            class="captcha-canvas"
            @click="generateCaptcha"
            title="点击刷新验证码"
          ></canvas>
        </div>
        <div class="form-item">
          <button @click="handleRegister" :disabled="loading">
            {{ loading ? '注册中...' : '注册' }}
          </button>
        </div>
        <div class="form-links">
          <router-link to="/login">已有账号？立即登录</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { register } from '@/api/user'

export default {
  // eslint-disable-next-line vue/multi-word-component-names
  name: 'Register',
  data() {
    return {
      registerForm: {
        username: '',
        password: '',
        confirmPassword: '',
        email: '',
        phone: '',
        captcha: ''
      },
      loading: false,
      captchaText: '',
      captchaWidth: 120,
      captchaHeight: 40
    }
  },
  mounted() {
    this.generateCaptcha()
  },
  methods: {
    // 生成随机验证码
    generateCaptcha() {
      const chars = 'ABCDEFGHJKMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789'
      let captcha = ''
      for (let i = 0; i < 4; i++) {
        captcha += chars.charAt(Math.floor(Math.random() * chars.length))
      }
      this.captchaText = captcha
      this.drawCaptcha()
    },
    
    // 绘制验证码图片
    drawCaptcha() {
      const canvas = this.$refs.captchaCanvas
      const ctx = canvas.getContext('2d')
      
      canvas.width = this.captchaWidth
      canvas.height = this.captchaHeight
      
      // 清空画布
      ctx.clearRect(0, 0, this.captchaWidth, this.captchaHeight)
      
      // 设置背景色
      ctx.fillStyle = '#f0f0f0'
      ctx.fillRect(0, 0, this.captchaWidth, this.captchaHeight)
      
      // 绘制干扰线
      for (let i = 0; i < 5; i++) {
        ctx.strokeStyle = this.getRandomColor()
        ctx.beginPath()
        ctx.moveTo(Math.random() * this.captchaWidth, Math.random() * this.captchaHeight)
        ctx.lineTo(Math.random() * this.captchaWidth, Math.random() * this.captchaHeight)
        ctx.stroke()
      }
      
      // 绘制验证码文字
      for (let i = 0; i < this.captchaText.length; i++) {
        ctx.fillStyle = this.getRandomColor()
        ctx.font = `${20 + Math.random() * 10}px Arial`
        ctx.textBaseline = 'middle'
        
        const x = 15 + i * 25 + Math.random() * 10
        const y = this.captchaHeight / 2 + Math.random() * 10 - 5
        const angle = (Math.random() - 0.5) * 0.5
        
        ctx.save()
        ctx.translate(x, y)
        ctx.rotate(angle)
        ctx.fillText(this.captchaText[i], 0, 0)
        ctx.restore()
      }
      
      // 绘制干扰点
      for (let i = 0; i < 50; i++) {
        ctx.fillStyle = this.getRandomColor()
        ctx.beginPath()
        ctx.arc(Math.random() * this.captchaWidth, Math.random() * this.captchaHeight, 1, 0, 2 * Math.PI)
        ctx.fill()
      }
    },
    
    // 获取随机颜色
    getRandomColor() {
      const r = Math.floor(Math.random() * 256)
      const g = Math.floor(Math.random() * 256)
      const b = Math.floor(Math.random() * 256)
      return `rgb(${r},${g},${b})`
    },
    
    // 验证验证码
    validateCaptcha() {
      return this.registerForm.captcha.toLowerCase() === this.captchaText.toLowerCase()
    },
    
    handleRegister() {
      // 表单验证
      if (!this.registerForm.username) {
        this.$message.error('请输入用户名')
        return
      }
      if (!this.registerForm.password) {
        this.$message.error('请输入密码')
        return
      }
      if (this.registerForm.password !== this.registerForm.confirmPassword) {
        this.$message.error('两次输入的密码不一致')
        return
      }
      if (!this.validateEmail(this.registerForm.email)) {
        this.$message.error('请输入有效的电子邮箱')
        return
      }
      if (!this.validatePhone(this.registerForm.phone)) {
        console.log(this.registerForm.phone)
        this.$message.error('请输入有效的手机号码')
        return
      }
      
      if (!this.registerForm.captcha) {
        this.$message.error('请输入验证码')
        return
      }
      
      if (!this.validateCaptcha()) {
        this.$message.error('验证码错误')
        this.generateCaptcha() // 重新生成验证码
        this.registerForm.captcha = '' // 清空验证码输入
        return
      }

      this.loading = true
      register({
        username: this.registerForm.username,
        password: this.registerForm.password,
        email: this.registerForm.email,
        phone: this.registerForm.phone
      })
        .then(response => {
          this.$message.success('注册成功，请登录')
          this.$router.push('/login')
        })
        .catch(error => {
          this.$message.error(error.message || '注册失败')
          this.generateCaptcha() // 注册失败后重新生成验证码
          this.registerForm.captcha = '' // 清空验证码输入
        })
        .finally(() => {
          this.loading = false
        })
    },
    validateEmail(email) {
      const re = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
      return re.test(email)
    },
    validatePhone(phone) {
      const re = /^1[3-9]\d{9}$/; // 匹配中国手机号
      return re.test(phone);
    }
  }
}
</script>

<style scoped>
.register-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background-color: #f5f5f5;
}

.register-box {
  width: 400px;
  padding: 40px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.register-box h2 {
  text-align: center;
  margin-bottom: 30px;
  color: #333;
}

.form-item {
  margin-bottom: 20px;
}

.form-item input {
  width: 100%;
  padding: 12px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  font-size: 14px;
  transition: border-color 0.2s;
}

.form-item input:focus {
  border-color: #409eff;
  outline: none;
}

.form-item button {
  width: 100%;
  padding: 12px;
  background-color: #409eff;
  border: none;
  border-radius: 4px;
  color: white;
  font-size: 14px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.form-item button:hover {
  background-color: #66b1ff;
}

.form-item button:disabled {
  background-color: #a0cfff;
  cursor: not-allowed;
}

.form-links {
  text-align: center;
  font-size: 14px;
}

.form-links a {
  color: #409eff;
  text-decoration: none;
}

.form-links a:hover {
  color: #66b1ff;
}

.captcha-item {
  display: flex;
  align-items: center;
  gap: 10px;
}

.captcha-input {
  flex: 1;
  width: auto !important;
}

.captcha-canvas {
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  cursor: pointer;
  background: #f0f0f0;
  transition: border-color 0.2s;
}

.captcha-canvas:hover {
  border-color: #409eff;
}
</style>
