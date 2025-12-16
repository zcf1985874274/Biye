<template>
  <div>
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

    <!-- 支付方式选择对话框 -->
    <el-dialog
      title="选择支付方式"
      :visible.sync="paymentDialogVisible"
      width="40%"
      @close="handlePaymentDialogClose"
    >
    <div class="payment-methods">
      <el-radio v-model="paymentMethod" label="balance" class="payment-option">
        <div class="payment-info">
          <div class="payment-title">余额支付</div>
          <div class="payment-desc">使用账户余额直接支付</div>
        </div>
      </el-radio>
      <el-radio v-model="paymentMethod" label="alipay" class="payment-option">
        <div class="payment-info">
          <div class="payment-title">支付宝支付</div>
          <div class="payment-desc">使用支付宝扫码支付</div>
        </div>
      </el-radio>
    </div>
    
    <div v-if="paymentMethod === 'alipay' && alipayQrCode" class="qr-code-container">
      <div class="qr-code-title">请使用支付宝扫码支付</div>
      <div class="qr-code">
        <img :src="alipayQrCode" alt="支付宝二维码" />
      </div>
      <div class="qr-code-tip">支付完成后页面将自动跳转</div>
    </div>
    
    <span slot="footer" class="dialog-footer">
      <el-button @click="paymentDialogVisible = false">取消</el-button>
      <el-button 
        type="primary" 
        @click="handlePayment" 
        :loading="paymentLoading"
        :disabled="paymentMethod === null"
      >
        {{ paymentMethod === 'alipay' ? '生成支付二维码' : '确认支付' }}
      </el-button>
    </span>
  </el-dialog>
  </div>
</template>

<script>
import request from '@/utils/request'
import { mapGetters } from 'vuex'

export default {
  name: 'BookingDialog',
  props: {
    room: {
      type: Object,
      default: () =>({
        roomName: '',
        roomType: '',
        pricePerHour: 0,
        roomId: null,
        password: '',
        storeId: null
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
      paymentDialogVisible: false,
      paymentMethod: 'balance',
      paymentLoading: false,
      alipayQrCode: null,
      alipayOrderId: null,
      alipayStatusTimer: null,
      usageRecordCreated: false, // 防止重复创建使用记录
      form: {
        hours: 4 // 默认4小时
      }
    }
  },
  computed: {
    ...mapGetters([
      'userInfo'
    ]),
    userId() {
      return this.userInfo?.userId || null
    },
    totalPrice() {
      return this.form.hours * this.room.pricePerHour
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
      this.paymentDialogVisible = true
    },
    handlePaymentDialogClose() {
      // 重置支付相关状态
      this.paymentMethod = 'balance'
      this.alipayQrCode = null
      this.alipayOrderId = null
      this.usageRecordCreated = false
      // 清除定时器
      if (this.alipayStatusTimer) {
        clearInterval(this.alipayStatusTimer)
        this.alipayStatusTimer = null
      }
    },
    async handlePayment() {
      if (this.paymentLoading) return

      // 余额支付（你原来有的逻辑）
      if (this.paymentMethod === 'balance') {
        // 这里放你原来的余额支付代码
        this.$message.info('余额支付功能待开发')
        return
      }

      // 支付宝支付
      if (this.paymentMethod === 'alipay') {
        try {
          this.paymentLoading = true

          // 1. 创建支付记录
          const createRes = await request({
            url: '/api/payments',
            method: 'post',
            data: {
              userId: this.userId,
              paymentMethod: 'alipay',
              amount: this.totalPrice,
              description: `预订 ${this.room.roomName} ${this.form.hours}小时`
            }
          })

          if (createRes.code !== 200) {
            this.$message.error(createRes.message || '创建订单失败')
            return
          }

          const paymentId = createRes.data.paymentId
          this.alipayOrderId = paymentId

          // 2. 调用支付宝生成二维码
          const alipayRes = await request({
            url: `/api/alipay/pay/${paymentId}`,
            method: 'post'
          })

          // 终极正确写法！你的 request 返回的是完整响应
          if (alipayRes?.code === 200 && alipayRes?.data?.qrCode) {
            // 改成这行！国内超快，100% 显示
            this.alipayQrCode = `https://api.qrserver.com/v1/create-qr-code/?size=300x300&data=${encodeURIComponent(alipayRes.data.qrCode)}`
            this.$message.success('支付二维码已生成，请扫码支付')
            this.startPollingAlipayStatus()
          } else {
            this.$message.error('生成二维码失败')
            console.error('支付宝响应异常:', alipayRes)
          }
        } catch (err) {
          console.error('支付宝支付失败:', err)
          this.$message.error('支付失败，请稍后重试')
        } finally {
          this.paymentLoading = false
        }
      }
    },
    async processBalancePayment() {
      try {
        this.paymentLoading = true
        
        // 检查用户登录状态
        if (!this.userId) {
          this.$message.error('用户未登录或登录已过期，请重新登录');
          this.paymentDialogVisible = false;
          this.dialogVisible = false;
          // 触发登出操作
          this.$store.dispatch('user/logout');
          return;
        }
        
        // 调用余额支付接口
        const response = await request({
          url: '/api/payments',
          method: 'post',
          data: {
            userId: this.userId,
            paymentMethod: 'balance',
            amount: this.totalPrice
          }
        })
        
        if (response.code === 200) {
          // 支付成功，创建使用记录
          await this.createUsageRecord(response.data.paymentId)
        } else {
          this.$message.error(response.message || '支付失败')
        }
      } catch (error) {
        console.error('余额支付失败:', error)
        this.$message.error('支付失败，请稍后重试')
      } finally {
        this.paymentLoading = false
      }
    },
    async processAlipayPayment() {
      try {
        this.paymentLoading = true
        
        // 检查用户登录状态
        if (!this.userId) {
          this.$message.error('用户未登录或登录已过期，请重新登录');
          this.paymentDialogVisible = false;
          this.dialogVisible = false;
          // 触发登出操作
          this.$store.dispatch('user/logout');
          return;
        }
        
        // 如果已经生成了二维码，则直接检查支付状态
        if (this.alipayQrCode && this.alipayOrderId) {
          this.$message.info('正在检查支付状态...')
          this.startPollingAlipayStatus()
          return
        }
        
        // 1. 创建支付记录
        const createPaymentResponse = await request({
          url: '/api/payments',
          method: 'post',
          data: {
            userId: this.userId,
            paymentMethod: 'alipay',
            amount: this.totalPrice
          }
        })
        
        if (createPaymentResponse.code !== 200 || !createPaymentResponse.data) {
          this.$message.error(createPaymentResponse.message || '创建支付订单失败')
          return
        }
        
        this.alipayOrderId = createPaymentResponse.data.paymentId
        
        // 2. 调用支付宝支付接口生成二维码
        const alipayResponse = await request({
          url: `/api/alipay/pay/${this.alipayOrderId}`,
          method: 'post'
        })

        if (alipayResponse.code === 200 && alipayResponse.data) {
          const qrCodeData = alipayResponse.data.qrCode

          if (qrCodeData) {
            // 重点！把 payUrl 改成 qrCodeData
            this.alipayQrCode = `https://chart.googleapis.com/chart?cht=qr&chs=250x250&chl=${encodeURIComponent(qrCodeData)}&choe=UTF-8`

            this.$message.success('支付二维码已生成，请扫码支付')
            this.startPollingAlipayStatus()
          } else {
            this.$message.error('获取到的二维码数据为空')
          }
        } else {
          this.$message.error(alipayResponse.message || '生成支付二维码失败')
          console.error('生成二维码失败:', alipayResponse)
        }
      } catch (error) {
        console.error('支付宝支付失败:', error)
        this.$message.error('支付失败，请稍后重试')
      } finally {
        this.paymentLoading = false
      }
    },
    startPollingAlipayStatus() {
      if (!this.alipayOrderId) return
      
      // 清除现有定时器
      if (this.alipayStatusTimer) {
        clearInterval(this.alipayStatusTimer)
        this.alipayStatusTimer = null
      }
      
      let pollingCount = 0
      const maxPollingCount = 60 // 最多轮询60次（3分钟）
      
      this.alipayStatusTimer = setInterval(async () => {
        try {
          pollingCount++
          
          // 调用支付宝支付状态查询接口
          const response = await request({
            url: `/api/alipay/status/${this.alipayOrderId}`,
            method: 'get'
          })
          
          if (response.code === 200 && response.data) {
            const paymentStatus = response.data.paymentStatus
            if (paymentStatus === 'successful') {
              clearInterval(this.alipayStatusTimer)
              this.alipayStatusTimer = null
              await this.createUsageRecord(this.alipayOrderId)
            } else if (paymentStatus === 'failed') {
              clearInterval(this.alipayStatusTimer)
              this.alipayStatusTimer = null
              this.$message.error('支付失败，请重新尝试')
              this.paymentDialogVisible = false
            }
          }
          
          // 达到最大轮询次数，自动关闭
          if (pollingCount >= maxPollingCount) {
            clearInterval(this.alipayStatusTimer)
            this.alipayStatusTimer = null
            this.$message.warning('支付超时，请重新发起支付')
            this.paymentDialogVisible = false
          }
        } catch (error) {
          console.error('查询支付状态失败:', error)
        }
      }, 3000) // 每3秒查询一次
    },
    async createUsageRecord(paymentId) {
      // 防止重复创建使用记录
      if (this.usageRecordCreated) {
        console.warn('使用记录已创建，无需重复操作')
        return
      }
      
      try {
        this.usageRecordCreated = true
        
        // 计算时间
        const now = new Date()
        const startTime = new Date(now)
        const endTime = new Date(now.getTime() + this.form.hours * 60 * 60 * 1000)
        
        // 创建使用记录
        const response = await request({
          url: '/api/usage-records',
          method: 'post',
          data: {
            roomId: this.room.roomId,
            userId: this.userId,
            startTime: startTime.toISOString(), // 直接使用ISO格式时间
            endTime: endTime.toISOString(),     // 直接使用ISO格式时间
            totalPrice: this.totalPrice,
            storeId: this.room.storeId,
            paymentId: paymentId
          }
        })
        
        if (response.code === 200) {
          // 支付成功，关闭所有对话框
          this.paymentDialogVisible = false
          this.dialogVisible = false
          
          // 显示房间密码
          this.$alert(`房间密码: ${this.room.password}`, '预订成功', {
            confirmButtonText: '确定',
            callback: () => {}
          })
        } else {
          this.$message.error(response.message || '创建使用记录失败')
          this.usageRecordCreated = false
        }
      } catch (error) {
        console.error('创建使用记录失败:', error)
        this.$message.error('创建使用记录失败，请联系客服')
        this.usageRecordCreated = false
      }
    }
  },
  beforeDestroy() {
    // 清除定时器
    if (this.alipayStatusTimer) {
      clearInterval(this.alipayStatusTimer)
      this.alipayStatusTimer = null
    }
  }
}
</script>

<style scoped>
.el-select {
  width: 100%;
}

.payment-methods {
  margin-bottom: 20px;
}

.payment-option {
  display: block;
  width: 100%;
  margin-bottom: 15px;
  padding: 10px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
}

.payment-info {
  margin-left: 20px;
}

.payment-title {
  font-weight: bold;
  margin-bottom: 5px;
}

.payment-desc {
  color: #909399;
  font-size: 14px;
}

.qr-code-container {
  text-align: center;
  margin: 20px 0;
}

.qr-code-title {
  font-size: 16px;
  margin-bottom: 15px;
}

.qr-code {
  width: 200px;
  height: 200px;
  margin: 0 auto;
  border: 1px solid #eee;
  padding: 10px;
  background-color: #fff;
}

.qr-code img {
  width: 100%;
  height: 100%;
}

.qr-code-tip {
  margin-top: 15px;
  color: #909399;
  font-size: 14px;
}
</style>