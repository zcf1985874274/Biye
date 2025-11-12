<template>
  <div class="store-management">
    <div class="header">
      <el-button type="primary" @click="showAddDialog" v-if="isSuperAdmin">添加店铺</el-button>
    </div>

    <el-table :data="storeList" border style="width: 100%">
      <el-table-column prop="storeId" label="店铺ID" width="100"></el-table-column>
      <el-table-column prop="storeName" label="店铺名称"></el-table-column>
      <el-table-column prop="storeAddress" label="店铺地址"></el-table-column>
      <el-table-column label="操作" width="200">
        <template #default="{row}">
          <el-button size="mini" @click="handleEdit(row)">编辑</el-button>
          <el-button 
            size="mini" 
            type="danger" 
            @click="handleDelete(row.storeId)"
            v-if="isSuperAdmin">
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 添加/编辑店铺对话框 -->
    <el-dialog
      :title="dialogTitle"
      :visible.sync="dialogVisible"
      width="600px"
      @close="handleDialogClose">
      <el-form :model="storeForm" :rules="storeRules" ref="form" label-width="100px">
        <el-form-item label="店铺名称" prop="storeName">
          <el-input v-model="storeForm.storeName" placeholder="请输入店铺名称"></el-input>
        </el-form-item>
        <el-form-item label="店铺地址" prop="storeAddress">
          <el-input v-model="storeForm.storeAddress" placeholder="请输入店铺地址"></el-input>
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
import { mapGetters } from 'vuex'
import { getAllStores, addStore, updateStore, deleteStore } from '@/api/store'

export default {
  name: 'StoreManagement',
  data() {
    return {
      storeList: [],
      dialogVisible: false,
      dialogTitle: '',
      isEdit: false,
      storeForm: {
        storeId: null,
        storeName: '',
        storeAddress: ''
      },
      storeRules: {
        storeName: [
          { required: true, message: '请输入店铺名称', trigger: 'blur' },
          { min: 2, max: 50, message: '店铺名称长度在 2 到 50 个字符', trigger: 'blur' }
        ],
        storeAddress: [
          { required: true, message: '请输入店铺地址', trigger: 'blur' },
          { min: 5, max: 200, message: '地址长度在 5 到 200 个字符', trigger: 'blur' }
        ]
      }
    }
  },
  computed: {
    ...mapGetters('admin', ['adminInfo']),
    isSuperAdmin() {
      return this.adminInfo?.role === 'super_admin' || this.adminInfo?.role === 'SUPER_ADMIN'
    },
    canManageStore() {
      // 超级管理员可以管理所有店铺，普通管理员只能管理自己的店铺
      return (storeId) => {
        if (this.isSuperAdmin) {
          return true
        }
        return this.adminInfo?.storeId === storeId
      }
    }
  },
  mounted() {
    this.fetchStores()
  },
  methods: {
    async fetchStores() {
      try {
        const response = await getAllStores()
        console.log('获取店铺列表:', response.data)
        
        if (this.isSuperAdmin) {
          // 超级管理员可以看到所有店铺
          this.storeList = response.data || []
        } else {
          // 普通管理员只能看到自己管理的店铺
          const adminStoreId = this.$store.state.admin.storeId
          this.storeList = (response.data || []).filter(store => store.storeId === adminStoreId)
        }
      } catch (error) {
        console.error('获取店铺列表失败:', error)
        this.$message.error(error.message || '获取店铺列表失败')
      }
    },
    showAddDialog() {
      console.log('显示添加店铺对话框')
      this.dialogTitle = '添加店铺'
      this.isEdit = false
      this.storeForm = {
        storeId: null,
        storeName: '',
        storeAddress: ''
      }
      this.dialogVisible = true
    },
    handleEdit(row) {
      
      console.log('编辑店铺:', row)
      this.dialogTitle = '编辑店铺'
      this.isEdit = true
      this.storeForm = {
        storeId: row.storeId,
        storeName: row.storeName,
        storeAddress: row.storeAddress
      }
      this.dialogVisible = true
    },
    async handleDelete(storeId) {

      
      try {
        await this.$confirm('此操作将永久删除该店铺, 是否继续?', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        
        await deleteStore(storeId)
        this.$message.success('删除店铺成功')
        this.fetchStores()
      } catch (error) {
        if (error !== 'cancel') {
          this.$message.error(error.message || '删除店铺失败')
        }
      }
    },
    async handleSubmit() {
      try {
        await this.$refs.form.validate()
        
        if (this.isEdit) {

          
          await updateStore(this.storeForm)
          this.$message.success('更新店铺成功')
        } else {
          await addStore(this.storeForm)
          this.$message.success('添加店铺成功')
        }
        
        this.dialogVisible = false
        this.fetchStores()
      } catch (error) {
        if (error.message) {
          this.$message.error(error.message)
        }
      }
    },
    handleDialogClose() {
      this.$refs.form?.resetFields()
    },
    formatDate(date) {
      if (!date) return ''
      return new Date(date).toLocaleString()
    }
  }
}
</script>

<style scoped>
.store-management {
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