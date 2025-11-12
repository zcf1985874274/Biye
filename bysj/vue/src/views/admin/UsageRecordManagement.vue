<template>
  <div class="usage-record-management">
    <el-table
        :data="filteredRecords"
        style="width: 100%"
        border
        stripe
        v-loading="loading">
      <el-table-column
          prop="recordId"
          label="记录ID"
          min-width="80">
      </el-table-column>
      <el-table-column
          prop="roomName"
          label="房间名称"
          min-width="120">
      </el-table-column>
      <el-table-column
          prop="startTime"
          label="开始时间"
          min-width="160">
        <template #default="scope">
          {{ formatDate(scope.row.startTime) }}
        </template>
      </el-table-column>
      <el-table-column
          prop="endTime"
          label="结束时间"
          min-width="160">
        <template #default="scope">
          {{ formatDate(scope.row.endTime) }}
        </template>
      </el-table-column>
      <el-table-column
          prop="totalPrice"
          label="总价"
          min-width="100">
      </el-table-column>
      <el-table-column
          label="操作"
          min-width="150"
          v-if="isSuperAdmin">
        <template #default="scope">
          <el-button
              size="mini"
              @click="handleEdit(scope.row)">编辑</el-button>
          <el-button
              size="mini"
              type="danger"
              @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 编辑对话框 -->
    <el-dialog
        title="编辑使用记录"
        :visible.sync="editDialogVisible"
        width="500px"
        @close="resetEditForm">
      <el-form
          ref="editForm"
          :model="editForm"
          :rules="editRules"
          label-width="100px">
        <el-form-item label="房间名称" prop="roomName">
          <el-input v-model="editForm.roomName" disabled></el-input>
        </el-form-item>
        <el-form-item label="开始时间" prop="startTime">
          <el-date-picker
              v-model="editForm.startTime"
              type="datetime"
              placeholder="选择开始时间"
              format="yyyy-MM-dd HH:mm:ss"
              value-format="yyyy-MM-dd HH:mm:ss"
              style="width: 100%">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="结束时间" prop="endTime">
          <el-date-picker
              v-model="editForm.endTime"
              type="datetime"
              placeholder="选择结束时间"
              format="yyyy-MM-dd HH:mm:ss"
              value-format="yyyy-MM-dd HH:mm:ss"
              style="width: 100%">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="总价" prop="totalPrice">
          <el-input-number
              v-model="editForm.totalPrice"
              :min="0"
              :precision="2"
              style="width: 100%">
          </el-input-number>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleUpdateRecord" :loading="updateLoading">确定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { mapGetters } from 'vuex';
import {getUsageRecords, deleteUsageRecord, getUsageRecordsByStoreId, updateUsageRecord} from '@/api/usageRecord';

export default {
  name: 'UsageRecordManagement',
  data() {
    return {
      records: [],
      loading: false,
      editDialogVisible: false,
      updateLoading: false,
      editForm: {
        recordId: null,
        roomName: '',
        startTime: '',
        endTime: '',
        totalPrice: 0
      },
      editRules: {
        startTime: [
          { required: true, message: '请选择开始时间', trigger: 'change' }
        ],
        endTime: [
          { required: true, message: '请选择结束时间', trigger: 'change' }
        ],
        totalPrice: [
          { required: true, message: '请输入总价', trigger: 'blur' },
          { type: 'number', min: 0, message: '总价必须大于等于0', trigger: 'blur' }
        ]
      }
    };
  },
  computed: {
    ...mapGetters('admin', ['adminInfo']),
    isSuperAdmin() {
      const adminInfo = this.$store.state.admin.adminInfo;
      const role = adminInfo?.role || localStorage.getItem('adminRole') || 'admin';
      return role === 'super_admin' || role === 'SUPER_ADMIN';
    },
    filteredRecords() {
      console.log('Admin Info:', this.$store.state.admin.adminInfo);
      console.log('StoreId:', this.$store.state.admin.storeId);
      console.log('Records:', this.records);
      console.log('Records StoreIds:', this.records.map(record => record.storeId));
      if (this.isSuperAdmin) {
        return this.records;
      } else {
        const filtered = this.records.filter(record => record.storeId == Number(this.$store.state.admin.storeId));
        console.log('Filtered Records:', filtered);
        return filtered;
      }
    },
  },
  methods: {
    formatDate(dateString) {
      if (!dateString) return '';
      const date = new Date(dateString);
      return date.toLocaleString();
    },
    async fetchRecords() {
      this.loading = true;
      try {
        const token = this.$store.state.admin.adminToken;
        const adminInfo = this.$store.state.admin.adminInfo;
        const storeId = this.$store.state.admin.storeId; // 确保 storeId 是数字或字符串
        console.log('Admin Info in fetchRecords:', adminInfo);
        console.log('StoreId in fetchRecords:', storeId);

        if (!token) {
          this.$store.dispatch('admin/adminLogout');
          this.$router.push('/adminlogin');
          return;
        }
        const role = adminInfo?.role || localStorage.getItem('adminRole') || 'admin';

        if (role !== 'super_admin' && !storeId) {
          this.$message.error('请选择店铺');
          return;
        }

        let response;
        if (role === 'super_admin') {
          response = await getUsageRecords({
            page: this.currentPage,
            size: this.pageSize
          });
        } else {
          // 确保 storeId 是数字或字符串
          response = await getUsageRecordsByStoreId(Number(storeId)); // 强制转换为数字
        }
        console.log('Fetched records:', response.data);
        this.records = response.data;
      } catch (error) {
        this.$message.error('获取记录失败: ' + error.message);
      } finally {
        this.loading = false;
      }
    },
    handleEdit(record) {
      this.editForm = {
        recordId: record.recordId,
        roomName: record.roomName,
        startTime: record.startTime,
        endTime: record.endTime,
        totalPrice: record.totalPrice
      };
      this.editDialogVisible = true;
    },
    async handleUpdateRecord() {
      try {
        await this.$refs.editForm.validate();
        this.updateLoading = true;
        
        const updateData = {
          recordId: this.editForm.recordId,
          startTime: this.editForm.startTime,
          endTime: this.editForm.endTime,
          totalPrice: this.editForm.totalPrice
        };
        
        await updateUsageRecord(updateData);
        this.$message.success('更新成功');
        this.editDialogVisible = false;
        this.fetchRecords();
      } catch (error) {
        if (error.message) {
          this.$message.error('更新失败: ' + error.message);
        }
      } finally {
        this.updateLoading = false;
      }
    },
    resetEditForm() {
      this.$refs.editForm && this.$refs.editForm.resetFields();
      this.editForm = {
        recordId: null,
        roomName: '',
        startTime: '',
        endTime: '',
        totalPrice: 0
      };
    },
    async handleDelete(record) {
      try {
        await this.$confirm('确认删除该记录吗?', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning',
        });
        await deleteUsageRecord(record.recordId);
        this.$message.success('删除成功');
        this.fetchRecords();
      } catch (error) {
        if (error !== 'cancel') {
          this.$message.error('删除失败: ' + error.message);
        }
      }
    },
  },
  created() {
    console.log('Admin Info in created:', this.$store.state.admin.adminInfo);
    console.log('StoreId in created:', this.$store.state.admin.storeId);
    this.fetchRecords();
  },
};
</script>

<style scoped>
.usage-record-management {
  padding: 20px;
}
</style>