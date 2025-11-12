<template>
  <div class="settings-container">
    <h2>账号设置</h2>
    
    <!-- 用户信息展示区域 -->
    <div class="user-info-card">
      <h3>当前账号信息</h3>
      <div class="info-item">
        <span class="label">用户名：</span>
        <span class="value">{{ userInfo.username || '未设置' }}</span>
      </div>
      <div class="info-item">
        <span class="label">手机号：</span>
        <span class="value">{{ userInfo.phone || '未设置' }}</span>
      </div>
      <div class="info-item">
        <span class="label">邮箱：</span>
        <span class="value">{{ userInfo.email || '未设置' }}</span>
      </div>
      <button @click="showEditDialog" class="edit-btn">修改信息</button>
    </div>

    <!-- 修改信息弹窗 -->
    <div v-if="showDialog" class="dialog-overlay">
      <div class="dialog-content">
        <h3>修改账号信息</h3>
        <form @submit.prevent="handleSubmit">
          <div class="form-group">
            <label for="phone">手机号</label>
            <input
              type="text"
              id="phone"
              v-model="form.phone"
              :placeholder="userInfo.phone || '请输入手机号'"
            />
          </div>
          <div class="form-group">
            <label for="email">邮箱</label>
            <input
              type="email"
              id="email"
              v-model="form.email"
              :placeholder="userInfo.email || '请输入邮箱'"
            />
          </div>
          <div class="form-group">
            <label for="password">新密码</label>
            <input
              type="password"
              id="password"
              v-model="form.password"
              placeholder="请输入新密码（留空则不修改）"
            />
          </div>
          <div class="dialog-buttons">
            <button type="button" @click="closeDialog" class="cancel-btn">取消</button>
            <button type="submit" class="submit-btn">保存</button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script>
import { mapGetters } from 'vuex';
import { updateSelfInfo, getUserInfo } from '@/api/user';

export default {
  name: 'Settings',
  data() {
    return {
      userInfo: {
        username: '',
        phone: '',
        email: ''
      },
      form: {
        phone: '',
        email: '',
        password: ''
      },
      showDialog: false,
      loading: false
    };
  },
  computed: {
    ...mapGetters('user', ['username'])
  },
  mounted() {
    this.loadUserInfo();
  },
  methods: {
    // 加载用户信息
    async loadUserInfo() {
      try {
        const response = await getUserInfo(this.username);
        if (response.code === 200) {
          this.userInfo = response.data;
        } else {
          this.$message.error('获取用户信息失败');
        }
      } catch (error) {
        this.$message.error('请求失败，请重试');
      }
    },
    
    // 显示修改弹窗
    showEditDialog() {
      // 将当前用户信息填充到表单中
      this.form.phone = this.userInfo.phone || '';
      this.form.email = this.userInfo.email || '';
      this.form.password = '';
      this.showDialog = true;
    },
    
    // 关闭弹窗
    closeDialog() {
      this.showDialog = false;
      this.form.phone = '';
      this.form.email = '';
      this.form.password = '';
    },
    
    async handleLogout() {
      try {
        // 先调用后端退出API
        await this.$store.dispatch('user/logout')
        
        // 清除前端认证信息
        this.$store.commit('user/CLEAR_AUTH')
        this.$store.commit('user/RESET_STATE')
        
        this.$message.success('退出登录成功')
        this.$router.push('/login')
        
      } catch (error) {
        console.error('退出失败:', error)
        // 如果API调用失败，仍然清除前端状态
        this.$store.commit('user/CLEAR_AUTH')
        this.$store.commit('user/RESET_STATE')
        
        this.$message.error('退出异常，已清除本地登录状态')
        this.$router.push('/login')
      }
    },
    
    // 提交修改
    async handleSubmit() {
      try {
        this.loading = true;
        
        // 构建更新数据，如果密码为空则不更新密码
        const updateData = {
          username: this.username,
          phone: this.form.phone,
          email: this.form.email
        };
        
        const hasPasswordChanged = this.form.password.trim() !== '';
        
        // 只有当密码不为空时才更新密码
        if (hasPasswordChanged) {
          updateData.password = this.form.password;
        }
        
        const response = await updateSelfInfo(updateData);
        if (response.code === 200) {
          this.$message.success('修改成功！');
          
          if (hasPasswordChanged) {
            // 如果修改了密码，则调用退出登录清除用户数据
            this.$message.info('密码已修改，请重新登录');
            await this.handleLogout();
          } else {
            // 重新加载用户信息
            await this.loadUserInfo();
            // 关闭弹窗
            this.closeDialog();
          }
        } else {
          this.$message.error(response.message || '修改失败');
        }
      } catch (error) {
        this.$message.error('请求失败，请重试');
      } finally {
        this.loading = false;
      }
    }
  }
};
</script>

<style scoped>
.settings-container {
  max-width: 600px;
  margin: 0 auto;
  padding: 20px;
}

h2 {
  text-align: center;
  margin-bottom: 30px;
}

/* 用户信息卡片样式 */
.user-info-card {
  background: #f8f9fa;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 30px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.user-info-card h3 {
  margin-top: 0;
  margin-bottom: 20px;
  color: #333;
  border-bottom: 1px solid #e9ecef;
  padding-bottom: 10px;
}

.info-item {
  display: flex;
  margin-bottom: 15px;
  align-items: center;
}

.label {
  font-weight: bold;
  color: #666;
  min-width: 80px;
}

.value {
  color: #333;
  flex: 1;
}

.edit-btn {
  background-color: #409eff;
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  margin-top: 10px;
}

.edit-btn:hover {
  background-color: #66b1ff;
}

/* 弹窗样式 */
.dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.dialog-content {
  background: white;
  border-radius: 8px;
  padding: 30px;
  width: 90%;
  max-width: 500px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.dialog-content h3 {
  margin-top: 0;
  margin-bottom: 20px;
  text-align: center;
}

.form-group {
  margin-bottom: 20px;
}

label {
  display: block;
  margin-bottom: 5px;
  font-weight: bold;
  color: #333;
}

input {
  width: 100%;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  box-sizing: border-box;
}

input:focus {
  outline: none;
  border-color: #409eff;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
}

.dialog-buttons {
  display: flex;
  gap: 10px;
  margin-top: 20px;
}

.cancel-btn {
  flex: 1;
  padding: 10px;
  background-color: #6c757d;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.cancel-btn:hover {
  background-color: #5a6268;
}

.submit-btn {
  flex: 1;
  padding: 10px;
  background-color: #409eff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.submit-btn:hover {
  background-color: #66b1ff;
}

.submit-btn:disabled {
  background-color: #a0cfff;
  cursor: not-allowed;
}
</style>