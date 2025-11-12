<template>
  <div class="room-management">
    <div class="header">

      <el-button type="primary" @click="showAddDialog">添加房间</el-button>
    </div>

    <!-- 房间列表表格 -->
    <el-table :data="rooms" border style="width: 100%">
      <el-table-column prop="roomId" label="ID" width="80"></el-table-column>
      <el-table-column prop="roomName" label="房间名称"></el-table-column>
      <el-table-column prop="roomType" label="房间类型"></el-table-column>
      <el-table-column prop="password" label="密码"></el-table-column>
      <el-table-column prop="storeId" label="店铺ID"></el-table-column>
      <el-table-column prop="status" label="状态">
        <template #default="{row}">
          <el-tag :type="row.status === '空闲' ? 'success' : 'danger'">
            {{ row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="图片预览" width="120">
        <template #default="{row}">
          <el-image
            style="width: 100px; height: 60px"
            :src="row.imagePath || 'https://via.placeholder.com/100x60'"
            :preview-src-list="[row.imagePath || 'https://via.placeholder.com/100x60']"
            fit="cover"
          ></el-image>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180">
        <template #default="{row}">
          <el-button size="mini" @click="handleEdit(row)">编辑</el-button>
          <el-button size="mini" type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="pagination">
      <el-pagination
        @current-change="handlePageChange"
        :current-page="currentPage"
        :page-size="pageSize"
        layout="prev, pager, next"
        :total="total">
      </el-pagination>
    </div>

    <!-- 添加/编辑房间对话框 -->
    <el-dialog
      :title="dialogTitle"
      :visible.sync="dialogVisible"
      width="50%"
      @closed="resetForm">
      <el-form :model="form" :rules="rules" ref="form" label-width="100px">
        <el-form-item label="房间名称" prop="roomName">
          <el-input v-model="form.roomName"></el-input>
        </el-form-item>
        <el-form-item label="房间类型" prop="roomType">
          <el-input v-model="form.roomType"></el-input>
        </el-form-item>
        <el-form-item label="最小人数" prop="minPlayers">
          <el-input-number v-model="form.minPlayers" :min="1"></el-input-number>
        </el-form-item>
        <el-form-item label="最大人数" prop="maxPlayers">
          <el-input-number v-model="form.maxPlayers" :min="form.minPlayers"></el-input-number>
        </el-form-item>
        <el-form-item label="每小时价格" prop="pricePerHour">
          <el-input-number v-model="form.pricePerHour" :min="0" :precision="2"></el-input-number>
        </el-form-item>
        <el-form-item label="房间状态" prop="status">
          <el-select v-model="form.status">
            <el-option label="空闲" value="空闲"></el-option>
            <el-option label="使用中" value="使用中"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="房间描述" prop="description">
          <el-input type="textarea" v-model="form.description"></el-input>
        </el-form-item>
        <el-form-item label="房间密码" prop="password">
          <el-input v-model="form.password" placeholder="请输入房间密码"></el-input>
        </el-form-item>
        <el-form-item label="房间图片" prop="imagePath">
          <div class="image-uploader">
            <el-input v-model="form.imagePath" placeholder="输入图片URL"></el-input>
            <span class="or-text">或</span>
            <input
              type="file"
              ref="fileInput"
              accept="image/*"
              @change="handleFileChange"
              style="display: none">
            <el-button @click="$refs.fileInput.click()">选择本地图片</el-button>
          </div>
          <div class="image-preview" v-if="form.imagePath">
            <el-image
              style="width: 200px; height: 120px"
              :src="form.imagePath"
              fit="cover"></el-image>
          </div>
        </el-form-item>
        <el-form-item label="店铺ID" prop="storeId" v-if="isSupperAdmin">
          <el-input v-model="form.storeId" placeholder="请输入店铺ID"></el-input>
        </el-form-item>
        <el-form-item label="店铺ID" v-else>
          <el-input v-model="form.storeId" disabled placeholder="自动分配"></el-input>
        </el-form-item>
      </el-form>
      <span slot="footer" class="dialog-footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { getRooms, addRoom, updateRoom, deleteRoom, getRoomsByStoreId } from '@/api/room'

export default {
  name: 'RoomManagement',
  computed: {
    // 判断是否为超级管理员
    isSupperAdmin() {
      const adminInfo = this.$store.state.admin.adminInfo
      const role = adminInfo?.role || localStorage.getItem('adminRole') || 'admin'
      return role === 'super_admin'
    }
  },
  data() {
    return {
      rooms: [],
      currentPage: 1,
      pageSize: 10,
      total: 0,
      dialogVisible: false,
      dialogTitle: '添加房间',
      form: {
        roomId: null,
        roomName: '',
        roomType: '',
        minPlayers: 2,
        maxPlayers: 4,
        pricePerHour: 50,
        status: '空闲',
        description: '',
        imagePath: '',
        password: '',
        storeId: null
      },
      rules: {
        roomName: [
          { required: true, message: '请输入房间名称', trigger: 'blur' }
        ],
        roomType: [
          { required: true, message: '请输入房间类型', trigger: 'blur' }
        ],
        minPlayers: [
          { required: true, message: '请输入最小人数', trigger: 'blur' }
        ],
        maxPlayers: [
          { required: true, message: '请输入最大人数', trigger: 'blur' }
        ],
        pricePerHour: [
          { required: true, message: '请输入每小时价格', trigger: 'blur' }
        ],
        password: [
          { required: true, message: '请输入密码', trigger: 'blur' }
        ],
        storeId: [
          { 
            required: true, 
            message: '请输入店铺ID', 
            trigger: 'blur',
            validator: (rule, value, callback) => {
              // 超级管理员必须输入店铺ID
              if (this.isSupperAdmin && !value) {
                callback(new Error('请输入店铺ID'))
              } else {
                callback()
              }
            }
          }
        ]
      }
    }
  },
  created() {
    this.fetchRooms()
  },
  methods: {
    fetchRooms() {
      // 从admin模块获取token和权限信息
      const token = this.$store.state.admin.adminToken
      const adminInfo = this.$store.state.admin.adminInfo
      const storeId = this.$store.state.admin.storeId
      
      if (!token) {
        this.$store.dispatch('admin/adminLogout')
        this.$router.push('/adminlogin')
        return
      }

      // 获取管理员角色，优先从adminInfo获取，其次从localStorage
      const role = adminInfo?.role || localStorage.getItem('adminRole') || 'admin'

      // 检查普通管理员是否有storeId
      if (role !== 'super_admin' && !storeId) {
        this.$message.error('请选择店铺')
        return
      }

      // 根据权限调用不同的API
      let fetchPromise
      if (role === 'super_admin') {
        // 超级管理员可以查看所有房间
        fetchPromise = getRooms({
          page: this.currentPage,
          size: this.pageSize
        })
      } else {
        // 普通管理员只能查看自己店铺的房间
        fetchPromise = getRoomsByStoreId(storeId)
      }
      fetchPromise.then(response => {
        if (response.code === 200) {
          this.rooms = Array.isArray(response.data) ? response.data : []
          this.total = response.data.length || 0
        }
      }).catch(error => {
        this.$message.error(error.message || '获取房间列表失败')
        if (error.response?.status === 403) {
          this.$store.dispatch('admin/adminLogout')
          this.$router.push('/adminlogin')
        }
      })
    },
    handlePageChange(page) {
      this.currentPage = page
      this.fetchRooms()
    },
    showAddDialog() {
      this.dialogTitle = '添加房间'
      this.resetForm()
      this.dialogVisible = true
    },
    handleEdit(row) {
      this.dialogTitle = '编辑房间'
      this.form = { ...row }
      this.dialogVisible = true
    },
    handleDelete(row) {
      this.$confirm(`确定删除房间 "${row.roomName}" 吗?`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        // 从admin模块获取token
        const token = this.$store.state.admin.adminToken
        if (!token) {
          this.$store.dispatch('admin/adminLogout')
          this.$router.push('/adminlogin')
          return
        }

        deleteRoom(row.roomId).then(() => {
          this.$message.success('删除成功')
          this.fetchRooms()
        }).catch(error => {
          this.$message.error(error.message || '删除失败')
          if (error.response?.status === 403) {
            this.$store.dispatch('admin/adminLogout')
            this.$router.push('/adminlogin')
          }
        })
      }).catch(() => {
        this.$message.info('已取消删除')
      })
    },
    handleFileChange(event) {
      const file = event.target.files[0]
      if (!file) return

      if (!file.type.startsWith('image/')) {
        this.$message.error('请选择图片文件')
        return
      }

      const reader = new FileReader()
      reader.onload = (e) => {
        this.form.imagePath = e.target.result
      }
      reader.readAsDataURL(file)
    },
    resetForm() {
      if (this.$refs.form) {
        this.$refs.form.resetFields()
      }
      
      // 获取当前管理员信息
      const storeId = this.$store.state.admin.storeId
      
      this.form = {
        roomId: null,
        roomName: '',
        roomType: '',
        minPlayers: 2,
        maxPlayers: 4,
        pricePerHour: 50,
        status: '空闲',
        description: '',
        imagePath: '',
        password: '',
        // 普通管理员自动设置店铺ID，超级管理员需要手动输入
        storeId: this.isSupperAdmin ? null : storeId
      }
    },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (!valid) return

        // 从admin模块获取token
        const token = this.$store.state.admin.adminToken
        if (!token) {
          this.$store.dispatch('admin/adminLogout')
          this.$router.push('/adminlogin')
          return
        }

        // 确保普通管理员的店铺ID正确设置
        const storeId = this.$store.state.admin.storeId
        if (!this.isSupperAdmin && storeId) {
          this.form.storeId = storeId
        }

        // 验证店铺ID
        if (!this.form.storeId) {
          this.$message.error('店铺ID不能为空')
          return
        }

        const api = this.form.roomId ? updateRoom : addRoom
        const data = { ...this.form }

        api(data).then(() => {
          this.$message.success(this.form.roomId ? '更新成功' : '添加成功')
          this.dialogVisible = false
          this.fetchRooms()
        }).catch(error => {
          this.$message.error(error.message || (this.form.roomId ? '更新失败' : '添加失败'))
          if (error.response?.status === 403) {
            this.$store.dispatch('admin/adminLogout')
            this.$router.push('/adminlogin')
          }
        })
      })
    }
  }
}
</script>

<style scoped>
.room-management {
  padding: 20px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.pagination {
  margin-top: 20px;
  text-align: center;
}

.image-uploader {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
}

.or-text {
  margin: 0 10px;
  color: #999;
}

.image-preview {
  margin-top: 10px;
}
</style>
