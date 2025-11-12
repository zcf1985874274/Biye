<template>
  <div class="admin-management">
    <div class="header">
      <el-button
        type="primary"
        @click="showAddDialog"
        v-if="isSuperAdmin">
        添加管理员
      </el-button>
    </div>

    <el-table :data="adminList" border style="width: 100%">
      <el-table-column prop="adminId" label="ID" width="80"></el-table-column>
      <el-table-column prop="username" label="用户名"></el-table-column>
      <el-table-column prop="role" label="角色">
        <template #default="{row}">
          <el-tag :type="row.role === 'super_admin' ? 'danger' : 'primary'">
            {{ row.role === 'super_admin' ? '超级管理员' : '普通管理员' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态"></el-table-column>
      <el-table-column prop="storeId" label="门店ID"></el-table-column>
      <el-table-column prop="lastLogin" label="最后登录时间">
        <template #default="{row}">
          {{ formatDate(row.lastLogin) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" v-if="isSuperAdmin">
        <template #default="{row}">
          <el-button size="mini" @click="handleEdit(row)">编辑</el-button>
          <el-button
            size="mini"
            type="danger"
            @click="handleDelete(row.adminId)"
            :disabled="row.role === 'super_admin'">
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 添加/编辑管理员对话框 -->
    <el-dialog
      :title="dialogTitle"
      :visible.sync="dialogVisible"
      width="500px"
      @close="handleDialogClose">
      <el-form :model="adminForm" :rules="adminRules" ref="form" label-width="100px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="adminForm.username" :disabled="isEdit" placeholder="请输入用户名"></el-input>
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="adminForm.password" type="password" :placeholder="isEdit ? '留空则不修改密码' : '请输入密码'"></el-input>
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="adminForm.role" placeholder="请选择角色" style="width: 100%">
            <el-option label="普通管理员" value="admin"></el-option>
            <el-option label="超级管理员" value="super_admin"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="门店ID" prop="storeId">
          <el-input-number v-model="adminForm.storeId" :min="0" placeholder="请输入门店ID（可选）" style="width: 100%"></el-input-number>
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
import { getAdmins, addAdmin, updateAdmin, deleteAdmin } from '@/api/admin'
import { mapGetters } from 'vuex'

export default {
  name: 'AdminManagement',
  data() {
    return {
      adminList: [],
      dialogVisible: false,
      dialogTitle: '',
      isEdit: false,
      adminForm: {
        adminId: null,
        username: '',
        password: '',
        role: 'admin',
        storeId: null
      },
      adminRules: {
        username: [
          { required: true, message: '请输入用户名', trigger: 'blur' },
          { pattern: /^[a-zA-Z0-9_]{3,20}$/, message: '用户名必须为3-20位字母数字或下划线', trigger: 'blur' }
        ],
        password: [
          { validator: this.validatePassword, trigger: 'blur' }
        ],
        role: [
          { required: true, message: '请选择角色', trigger: 'change' }
        ]
      }
    }
  },
  computed: {
    ...mapGetters('admin', ['adminInfo']),
    isSuperAdmin() {
      return this.adminInfo?.role === 'super_admin'
    }
  },
  created() {
    this.fetchAdmins()
  },
  methods: {
    async fetchAdmins() {
      try {
        const res = await getAdmins()
        this.adminList = res.data
      } catch (error) {
        this.$message.error('获取管理员列表失败')
      }
    },
    showAddDialog() {
      console.log('显示添加对话框')
      this.dialogTitle = '添加管理员'
      this.isEdit = false
      this.adminForm = {
        adminId: null,
        username: '',
        password: '',
        role: 'admin',
        storeId: null
      }
      this.dialogVisible = true
      console.log('对话框状态:', this.dialogVisible)
    },
    
    handleEdit(row) {
      console.log('编辑管理员:', row)
      this.dialogTitle = '编辑管理员'
      this.isEdit = true
      this.adminForm = {
        adminId: row.adminId,
        username: row.username,
        password: '',
        role: row.role,
        storeId: row.storeId
      }
      this.dialogVisible = true
      console.log('对话框状态:', this.dialogVisible)
    },
    
    async handleDelete(id) {
      try {
        await this.$confirm('确定删除该管理员吗?', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        });
        await deleteAdmin(id);
        this.$message.success('删除成功');
        this.fetchAdmins();
      } catch (error) {
        if (error !== 'cancel') {
          this.$message.error('删除失败');
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
        
        const admin = {
          username: this.adminForm.username,
          role: this.adminForm.role,
          storeId: this.adminForm.storeId || null
        }
        
        if (this.isEdit) {
          admin.adminId = this.adminForm.adminId
          if (this.adminForm.password) {
            admin.password = this.adminForm.password
          }
          await updateAdmin(admin)
          this.$message.success('更新成功')
        } else {
          admin.password = this.adminForm.password
          admin.status = '未登录'
          await addAdmin(admin)
          this.$message.success('添加成功')
        }
        
        this.dialogVisible = false
        this.fetchAdmins()
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
.admin-management {
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
