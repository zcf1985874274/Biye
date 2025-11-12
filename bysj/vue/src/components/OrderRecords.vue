<template>
  <div class="order-records-container">
    <header class="header">
      <h1>订单记录</h1>
      <div class="user-info">
        <span>欢迎，{{ username }}</span>
      </div>
    </header>
    <main class="main-content">
      <div class="filter-section">
        <label for="year">年份：</label>
        <select id="year" v-model="selectedYear" class="dropdown-select">
          <option v-for="year in years" :key="year" :value="year">{{ year }}</option>
        </select>
        <label for="month">月份：</label>
        <select id="month" v-model="selectedMonth" class="dropdown-select">
          <option v-for="month in months" :key="month" :value="month">{{ month }}</option>
        </select>
        <button class="query-btn" @click="fetchOrderRecords">查询</button>
      </div>
      <div class="summary-section">
        <p>本月总消费：{{ monthlyTotal }} 元</p>
        <p>今日总消费：{{ dailyTotal }} 元</p>
      </div>
      <div class="records-table-container">
        <table class="records-table">
          <thead>
            <tr>
              <th>房间名称</th>
              <th>开始时间</th>
              <th>结束时间</th>
              <th>总价</th>
              <th>房间密码</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="record in orderRecords" :key="record.recordId">
              <td>{{ record.roomName || '未知房间' }}</td>
              <td>{{ formatDate(record.startTime) }}</td>
              <td>{{ formatDate(record.endTime) }}</td>
              <td>{{ record.totalPrice }} 元</td>
              <td>
                <span v-if="isOrderActive(record.endTime)">{{ record.roomPassword }}</span>
                <span v-else>已过期</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </main>
  </div>
</template>

<script>
import { fetchOrderRecords } from '@/api/usageRecord';
import { mapGetters } from 'vuex';

export default {
  name: 'OrderRecords',
  data() {
    return {
      selectedYear: new Date().getFullYear(),
      selectedMonth: new Date().getMonth() + 1,
      years: [2023, 2024, 2025],
      months: Array.from({ length: 12 }, (_, i) => i + 1),
      orderRecords: [],
      monthlyTotal: 0,
      dailyTotal: 0,
      loading: false,
      error: null,
    };
  },
  computed: {
    ...mapGetters('user', ['username'])
  },
  created() {
    this.fetchOrderRecords();
  },
  methods: {
    async fetchOrderRecords() {
      this.loading = true;
      this.error = null;
      try {
        const params = {
          year: this.selectedYear,
          month: this.selectedMonth,
        };
        const response = await fetchOrderRecords(params);
        if (response.code === 200) {
          console.log("后端返回数据:", response);
          this.orderRecords = response.data || []; // 直接使用 response.data
          console.log("当前订单记录:", this.orderRecords);
          this.calculateTotals();
        } else {
          this.error = response.message || '获取订单记录失败';
          if (response.code === 403) {
            console.error('权限不足，请检查用户角色或令牌有效性');
          }
        }
      } catch (error) {
        this.error = error.message || '请求失败，请稍后重试';
        if (error.response?.status === 403) {
          console.error('权限不足，请检查用户角色或令牌有效性');
        }
      } finally {
        this.loading = false;
      }
    },
    calculateTotals() {
      this.monthlyTotal = this.orderRecords.reduce(
        (sum, record) => sum + record.totalPrice,
        0
      );
      this.dailyTotal = this.orderRecords
        .filter(
          (record) =>
            new Date(record.startTime).getDate() === new Date().getDate()
        )
        .reduce((sum, record) => sum + record.totalPrice, 0);
    },
    formatDate(date) {
      return new Date(date).toLocaleString();
    },
    isOrderActive(endTime) {
      return new Date(endTime) > new Date();
    },
  },
  mounted() {
    this.fetchOrderRecords();
  },
};
</script>

<style scoped>
.order-records-container {
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

.main-content {
  flex: 1;
  padding: 20px;
}

.filter-section {
  margin-bottom: 20px;
  display: flex;
  align-items: center;
}

.filter-section label {
  margin-right: 10px;
}

.dropdown-select {
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  margin-right: 10px;
}

.query-btn {
  padding: 8px 12px;
  background-color: #409eff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.query-btn:hover {
  background-color: #66b1ff;
}

.summary-section {
  margin-bottom: 20px;
}

.records-table-container {
  background-color: white;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.records-table {
  width: 100%;
  border-collapse: collapse;
}

.records-table th, .records-table td {
  border: 1px solid #ddd;
  padding: 8px;
  text-align: left;
}

.records-table th {
  background-color: #f2f2f2;
}
</style>