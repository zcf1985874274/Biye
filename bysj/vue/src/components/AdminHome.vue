<template>
  <div class="admin-container">
    <!-- 上层标题区 -->
    <div class="admin-header">
      <h1>管理员控制面板</h1>
    </div>

    <!-- 下层内容区 -->
    <div class="admin-content">
      <!-- 左侧导航栏 -->
      <div class="admin-sidebar">
        <el-menu
            default-active="1"
            class="admin-menu"
            background-color="#545c64"
            text-color="#fff"
            active-text-color="#ffd04b">
          <el-menu-item
              index="1"
              @click="navigateTo('AdminManagement')"
              v-if="isSuperAdmin">
            <i class="el-icon-user"></i>
            <span>管理员管理</span>
          </el-menu-item>

          <el-menu-item
              index="2"
              @click="navigateTo('UserManagement')"
              v-if="isSuperAdmin">
            <i class="el-icon-user"></i>
            <span>用户管理</span>
          </el-menu-item>

          <el-menu-item
              index="3"
              @click="navigateTo('StoreManagement')"
              v-if="isSuperAdmin">
            <i class="el-icon-office-building"></i>
            <span>店铺管理</span>
          </el-menu-item>

          <el-menu-item
              index="4"
              @click="navigateTo('RoomManagement')">
            <i class="el-icon-office-building"></i>
            <span>房间管理</span>
          </el-menu-item>

          <el-menu-item
              index="5"
              @click="navigateTo('UsageRecordManagement')">
            <i class="el-icon-document"></i>
            <span>订单管理</span>
          </el-menu-item>

          <el-menu-item
              index="6"
              @click="navigateTo('Statistics')">
            <i class="el-icon-data-analysis"></i>
            <span>数据统计</span>
          </el-menu-item>

          <el-menu-item
              index="7"
              @click="handleLogout">
            <i class="el-icon-switch-button"></i>
            <span>退出登录</span>
          </el-menu-item>
        </el-menu>
      </div>

      <!-- 右侧内容区 -->
      <div class="admin-main">
        <div v-if="showWelcomeMessage" class="welcome-message">
          <p>欢迎来到24小时自助棋牌室管理系统</p>
        </div>
        <router-view v-else></router-view>
      </div>
    </div>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'

export default {
  name: 'AdminHome',
  data() {
    return {
      showWelcomeMessage: true
    }
  },
  computed: {
    ...mapGetters('admin', ['adminInfo']),
    isSuperAdmin() {
      console.log('adminInfo:', this.adminInfo) // 调试信息
      console.log('role:', this.adminInfo?.role) // 调试信息
      return this.adminInfo?.role === 'super_admin' || this.adminInfo?.role === 'SUPER_ADMIN'
    }
  },
  methods: {
    handleLogout() {
      this.$store
          .dispatch('admin/adminLogout')
          .then(() => {
            this.$message.success('管理员退出登录成功')
            this.$router.push('/adminlogin')
          })
          .catch(error => {
            this.$message.error(error.message || '管理员退出登录失败')
          })
    },
    showWelcome() {
      this.showWelcomeMessage = true
    },
    navigateTo(routeName) {
      // 检查当前路由是否已经是目标路由
      if (this.$route.name === routeName) {
        console.log('已在目标路由:', routeName)
        return
      }
      this.showWelcomeMessage = false
      console.log('跳转到路由:', routeName); // 调试信息
      this.$router.push({ name: routeName }).then(() => {
        console.log('路由跳转成功:', routeName); // 调试信息
      }).catch(err => {
        // 捕获并忽略重复导航错误
        if (err.name !== 'NavigationDuplicated') {
          console.error('导航错误:', err)
        }
      })
    }
  }
}
</script>

<style scoped>
.admin-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  position: relative; /* 添加相对定位 */
}

.admin-header {
  height: 60px;
  background: #409eff;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.12);
  position: relative;
  z-index: 1;
}

.admin-header h1 {
  margin: 0;
  font-size: 20px;
}

.admin-content {
  display: flex;
  flex: 1;
  position: relative; /* 添加相对定位 */
}

.admin-sidebar {
  width: 20%;
  background: #545c64;
  position: relative;
  z-index: 1;
}

.admin-menu {
  height: 100%;
  border-right: none;
}

.admin-main {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  position: relative;
}

.welcome-message {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  color: #666;
}

/* 确保对话框能突破布局限制 */
.el-dialog__wrapper {
  position: fixed !important;
  z-index: 9999 !important;
}

.el-dialog {
  position: relative;
  z-index: 10000 !important;
}
</style>
