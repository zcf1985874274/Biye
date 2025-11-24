<template>
  <div class="login-container">
    <div class="login-box">
      <h2>棋牌室系统 - 管理员登录</h2>
      <div class="login-form">
        <div class="form-item">
          <input
            v-model="loginForm.username"
            placeholder="管理员账号"
            type="text"
            @keyup.enter="handleLogin"
          />
        </div>
        <div class="form-item">
          <input
            v-model="loginForm.password"
            placeholder="密码"
            type="password"
            @keyup.enter="handleLogin"
          />
        </div>
        
        <div class="form-item captcha-item">
          <input
            v-model="loginForm.captcha"
            placeholder="验证码"
            type="text"
            class="captcha-input"
            @keyup.enter="handleLogin"
          />
          <canvas
            ref="captchaCanvas"
            class="captcha-canvas"
            @click="generateCaptcha"
            title="点击刷新验证码"
          ></canvas>
        </div>

        <div class="form-item">
          <button @click="handleLogin" :disabled="loading">
            {{ loading ? '登录中...' : '管理员登录' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'AdminLogin',
  data() {
    return {
      loginForm: {
        username: '',
        password: '',
        storeId: 1,
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
      return this.loginForm.captcha.toLowerCase() === this.captchaText.toLowerCase()
    },
    
    handleLogin() {
      if (!this.loginForm.username || !this.loginForm.password) {
        this.$message.error('请输入管理员账号和密码');
        return;
      }
      
      if (!this.loginForm.captcha) {
        this.$message.error('请输入验证码');
        return;
      }
      
      if (!this.validateCaptcha()) {
        this.$message.error('验证码错误');
        this.generateCaptcha(); // 重新生成验证码
        this.loginForm.captcha = ''; // 清空验证码输入
        return;
      }

      this.loading = true;
      this.$store
        .dispatch('admin/adminLogin', this.loginForm)
        .then((response) => {
          this.$message.success('管理员登录成功');
          
          // 登录成功后，adminInfo已经在store中设置好了，包含role信息
          console.log('登录成功，当前adminInfo:', this.$store.getters.adminInfo);
          
          // 跳转到管理员首页
          this.$router.push('/adminhome');
        })
        .catch(error => {
          let errorMessage = error.message || '管理员登录失败';
          if (error.response) {
            // 从后端响应中获取错误信息
            errorMessage = error.response.data?.message || errorMessage;
          }
          this.$message.error(errorMessage);
          this.generateCaptcha(); // 登录失败后重新生成验证码
          this.loginForm.captcha = ''; // 清空验证码输入
        })
        .finally(() => {
          this.loading = false;
        })
    }
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background-color: #f5f5f5;
}

.login-box {
  width: 400px;
  padding: 40px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.login-box h2 {
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
