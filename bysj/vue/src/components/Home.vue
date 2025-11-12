<template>
  <div class="home-container">
    <header class="header">
      <h1>24小时自助棋牌室系统</h1>
      <div class="user-info">
        <span>欢迎，{{ username }}</span>
        <div class="dropdown" @mouseenter="showDropdown = true" @mouseleave="showDropdown = false">
          <span class="dropdown-toggle" >个人中心</span>
          <div class="dropdown-menu" v-show="showDropdown">
            <button class="dropdown-btn" @click="$router.push('/settings')">账号设置</button>
            <button class="dropdown-btn" @click="$router.push('/orders')">订单记录</button>
            <button class="dropdown-btn" @click="handleLogout">退出</button>
          </div>
        </div>
      </div>
    </header>
    <main class="main-content">
      <div class="welcome-message">
        <h2>欢迎使用24小时自助棋牌室系统</h2>
        <p>您已成功登录系统，可以开始使用各项功能。</p>
      </div>
      
      <room-list @book-room="handleBookRoom" />
    </main>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import RoomList from './RoomList.vue'

export default {
  name: 'Home',
  components: {
    RoomList
  },
  data() {
    return {
      showDropdown: false
    };
  },
  computed: {
    ...mapGetters('user', ['username'])
  },
  created() {
    // 获取用户信息
    this.$store.dispatch('user/getUserInfo').catch(error => {
      this.$message.error('获取用户信息失败，请重新登录')
      this.handleLogout()
    })
  },
  methods: {
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
    
    // 测试方法：仅重置状态
    testResetState() {
      this.$store.commit('user/RESET_STATE')
      this.$message.success('已重置用户状态')
      console.log('当前状态:', this.$store.state.user)
    },
    handleBookRoom(room) {
      // 这里可以添加预订逻辑，比如跳转到预订页面或显示预订对话框
      console.log('预订房间:', room)
      this.$message.success(`已预订房间: ${room.roomName}`)
    }
  }
}
</script>

<style scoped>
.home-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.header {
  background-color: #409eff;
  color: white;
  padding: 15px 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header h1 {
  margin: 0;
  font-size: 20px;
}

.user-info {
  display: flex;
  align-items: center;
}

.user-info span {
  margin-right: 15px;
}

.user-info button {
  background-color: transparent;
  border: 1px solid white;
  color: white;
  padding: 5px 10px;
  border-radius: 4px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.user-info button:hover {
  background-color: rgba(255, 255, 255, 0.2);
}

.dropdown {
  position: relative;
  display: inline-block;
}

.dropdown-menu {
  position: absolute;
  top: 100%;
  right: 0;
  background-color: #409eff;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  z-index: 1000;
  padding: 5px 0;
}

.dropdown-menu button {
  width: 100%;
  text-align: left;
  padding: 5px 15px;
  background: none;
  border: none;
  color: white;
  cursor: pointer;
}

.dropdown-menu button:hover {
  background-color: rgba(255, 255, 255, 0.2);
}

.dropdown-btn {
  min-width: 80px;
  padding: 8px 12px;
  font-size: 14px;
  line-height: 1.5;
  border-radius: 4px;
  border: 1px solid #ddd;
  background-color: #f8f9fa;
  cursor: pointer;
  margin-right: 8px;
}

.dropdown-btn:hover {
  background-color: #e9ecef;
}

.main-content {
  flex: 1;
  padding: 20px;
}

.welcome-message {
  background-color: white;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  margin-bottom: 20px;
}

.welcome-message h2 {
  color: #333;
  margin-top: 0;
}
</style>