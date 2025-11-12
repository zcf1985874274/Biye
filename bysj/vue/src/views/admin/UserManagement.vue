<template>
  <div class="user-management">
    <div class="header">
      <el-button type="primary" @click="showAddDialog">添加用户</el-button>
    </div>

    <el-table :data="userList" border style="width: 100%">
      <el-table-column prop="userId" label="ID" width="80"></el-table-column>
      <el-table-column prop="username" label="用户名"></el-table-column>
      <el-table-column prop="email" label="邮箱"></el-table-column>
      <el-table-column prop="phone" label="电话"></el-table-column>
      <el-table-column prop="status" label="状态"></el-table-column>
      <el-table-column prop="lastLogin" label="最后登录时间">
        <template #default="{row}">
          {{ formatDate(row.lastLogin) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200">
        <template #default="{row}">
          <el-button size="mini" @click="handleEdit(row)">编辑</el-button>
          <el-button size="mini" type="danger" @click="handleDelete(row.userId)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 添加/编辑用户对话框 -->
    <el-dialog
      :title="dialogTitle"
      :visible.sync="dialogVisible"
      width="500px"
      @close="handleDialogClose">
      <el-form :model="userForm" :rules="userRules" ref="form" label-width="100px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="userForm.username" :disabled="isEdit" placeholder="请输入用户名"></el-input>
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="userForm.password" type="password" :placeholder="isEdit ? '留空则不修改密码' : '请输入密码'"></el-input>
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="userForm.email" placeholder="请输入邮箱"></el-input>
        </el-form-item>
        <el-form-item label="电话" prop="phone">
          <el-input v-model="userForm.phone" placeholder="请输入电话"></el-input>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </div>
    </el-dialog>

  </div>
</template>

<script>
import {register, updateUser, deleteUser, getAllUsers} from '@/api/user'

export default {
  name: 'UserManagement',
  data() {
    return {
      userList: [],
      dialogVisible: false,
      dialogTitle: '',
      isEdit: false,
      userForm: {
        userId: null,
        username: '',
        password: '',
        email: '',
        phone: ''
      },
      userRules: {
        username: [
          { required: true, message: '请输入用户名', trigger: 'blur' },
          { pattern: /^[a-zA-Z0-9_]{3,20}$/, message: '用户名必须为3-20位字母数字或下划线', trigger: 'blur' }
        ],
        password: [
          { validator: this.validatePassword, trigger: 'blur' }
        ],
        email: [
          { required: true, message: '请输入邮箱', trigger: 'blur' },
          { pattern: /^\S+@\S+\.\S+$/, message: '邮箱格式不正确', trigger: 'blur' }
        ],
        phone: [
          { required: true, message: '请输入电话', trigger: 'blur' },
          { pattern: /^1[3-9]\d{9}$/, message: '请输入有效的手机号', trigger: 'blur' }
        ]
      }
    }
  },
  created() {
    this.fetchUsers()
  },
  methods: {
    async fetchUsers() {
      this.loading = true
      try {
        // 从admin模块获取token
        const token = this.$store.state.admin.adminToken
        if (!token) {
          await this.$store.dispatch('admin/adminLogout')
          this.$router.push('/adminlogin')
          return
        }

        // 使用adminToken调用API
        const res = await getAllUsers({
          headers: {
            'Authorization': 'Bearer ' + token
          }
        })
        this.userList = res.data
      } catch (error) {
        console.error('获取用户列表失败:', error)
        if (error.response?.status === 401) {
          await this.$store.dispatch('admin/adminLogout')
          this.$message.error('管理员登录已过期，请重新登录')
          this.$router.push('/adminlogin')
        } else {
          this.$message.error('获取用户列表失败: ' + (error.message || '未知错误'))
        }
      } finally {
        this.loading = false
      }
    },
    showAddDialog() {
      console.log('显示添加用户对话框')
      this.dialogTitle = '添加用户'
      this.isEdit = false
      this.userForm = {
        userId: null,
        username: '',
        password: '',
        email: '',
        phone: ''
      }
      this.dialogVisible = true
      console.log('对话框状态:', this.dialogVisible)
    },
    handleEdit(row) {
      console.log('编辑用户:', row)
      this.dialogTitle = '编辑用户'
      this.isEdit = true
      this.userForm = {
        userId: row.userId,
        username: row.username,
        password: '',
        email: row.email,
        phone: row.phone
      }
      this.dialogVisible = true
      console.log('对话框状态:', this.dialogVisible)
    },
    async handleDelete(userId) {
      try {
        await this.$confirm('确定删除该用户吗?', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        await deleteUser(userId)
        this.$message.success('删除成功')
        this.fetchUsers()
      } catch (error) {
        if (error !== 'cancel') {
          this.$message.error('删除失败')
        }
      }
    },
    formatDate(date) {
      if (!date) return ''
      return new Date(date).toLocaleString()
    },
    validatePassword(rule, value, callback) {
      if (this.isEdit && !value) {
        // 编辑时密码可以为空
        callback()
      } else if (!value) {
        callback(new Error('请输入密码'))
      } else if (value.length < 6 || value.length > 20) {
        callback(new Error('密码必须为6-20位'))
      } else {
        callback()
      }
    },
    async handleSubmit() {
      try {
        await this.$refs.form.validate()
        
        if (this.isEdit) {
          const updateData = {
            userId: this.userForm.userId,
            email: this.userForm.email,
            phone: this.userForm.phone
          }

          if (this.userForm.password) {
            updateData.password = this.userForm.password
          }

          await updateUser({
            ...updateData,
            headers: {
              'Authorization': 'Bearer ' + this.$store.state.admin.adminToken
            }
          })
          this.$message.success('更新用户成功')
        } else {
          await register({
            username: this.userForm.username,
            password: this.userForm.password,
            email: this.userForm.email,
            phone: this.userForm.phone
          })
          this.$message.success('添加用户成功')
        }
        
        this.dialogVisible = false
        this.fetchUsers()
      } catch (error) {
        if (error.message) {
          this.$message.error(error.message)
        }
      }
    },
    handleDialogClose() {
      this.$refs.form?.resetFields()
    }
  }
}
</script>

<style scoped>
.user-management {
  padding: 20px;
}
.header {
  margin-bottom: 20px;
}
.dialog-footer {
  text-align: right;
}
.dialog-footer .el-button {
  margin-left: 10px;
}
</style>
