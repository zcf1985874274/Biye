<template>
  <el-dialog
    :title="`预订房间 - ${room.roomName}`"
    :visible.sync="dialogVisible"
    width="50%"
    @close="handleClose"
    @update:visible="val => $emit('update:visible', val)"
  >
    <el-form :model="form" label-width="120px">
      <el-form-item label="房间类型">
        <el-input :value="room.roomType" disabled />
      </el-form-item>
      <el-form-item label="价格(每小时)">
        <el-input :value="`¥${room.pricePerHour}`" disabled />
      </el-form-item>
      <el-form-item label="预订时长(小时)" required>
        <el-select v-model="form.hours" placeholder="请选择预订时长">
          <el-option
            v-for="hour in [2, 4, 6, 8]"
            :key="hour"
            :label="`${hour}小时`"
            :value="hour"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="总费用">
        <el-input :value="`¥${form.hours * room.pricePerHour}`" disabled />
      </el-form-item>
    </el-form>
    <span slot="footer" class="dialog-footer">
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" @click="handleConfirm">确认支付</el-button>
    </span>
  </el-dialog>
</template>

<script>
export default {
  name: 'BookingDialog',
  props: {
    room: {
      type: Object,
      default: () => ({
        roomName: '',
        roomType: '',
        pricePerHour: 0,
        roomId: null,
        password: ''
      })
    },
    visible: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      dialogVisible: this.visible,
      form: {
        hours: 4 // 默认4小时
      }
    }
  },
  watch: {
    visible(newVal) {
      this.dialogVisible = newVal
    }
  },
  methods: {
    handleClose() {
      this.$emit('update:visible', false)
    },
    handleConfirm() {
      this.$confirm(`确认支付 ¥${this.form.hours * this.room.pricePerHour} 预订该房间?`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.$emit('confirm', {
          roomId: this.room.roomId,
          hours: this.form.hours,
          totalPrice: this.form.hours * this.room.pricePerHour,
          storeId: this.room.storeId
        })
        this.dialogVisible = false
        this.$alert(`房间密码: ${this.room.password}`, '预订成功', {
          confirmButtonText: '确定',
          callback: () => {}
        })
      }).catch(() => {
        this.$message({
          type: 'info',
          message: '已取消支付'
        })
      })
    }
  }
}
</script>

<style scoped>
.el-select {
  width: 100%;
}
</style>
