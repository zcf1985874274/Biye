<template>
  <div class="statistics-container">
    <h2>数据统计</h2>
    <div class="toggle-buttons">
      <el-button @click="showChartData" type="primary">柱状图统计</el-button>
      <el-button @click="showTableData" type="primary">表格统计</el-button>
    </div>
    <el-tabs v-model="activeTab" type="card" @tab-change="handleTabChange">
      <el-tab-pane label="每日盈利" name="daily">
        <div class="time-range-selector">
          <el-radio-group v-model="dailyTimeRange" @change="onDailyTimeRangeChange" size="small">
            <el-radio-button label="7">最近7天</el-radio-button>
            <el-radio-button label="30">最近30天</el-radio-button>
            <el-radio-button label="365">最近1年</el-radio-button>
          </el-radio-group>
        </div>
        <div class="chart-container" v-show="showChart">
          <div ref="dailyChart" class="chart"></div>
        </div>
        <el-table :data="dailyData" style="width: 100%" v-show="!showChart">
          <el-table-column prop="date" label="日期" width="180"></el-table-column>
          <el-table-column v-if="isSuperAdmin" prop="storeName" label="店铺名称" width="180"></el-table-column>
          <el-table-column prop="totalProfit" label="总盈利" width="180"></el-table-column>
          <el-table-column label="房间盈利">
            <template #default="scope">
              <el-table :data="scope.row.roomProfits" style="width: 100%">
                <el-table-column prop="roomName" label="房间名称"></el-table-column>
                <el-table-column prop="profit" label="盈利"></el-table-column>
              </el-table>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane v-if="isSuperAdmin" label="每月盈利" name="monthly">
        <div class="chart-container" v-show="showChart">
          <div ref="monthlyChart" class="chart"></div>
        </div>
        <el-table :data="monthlyData" style="width: 100%" v-show="!showChart">
          <el-table-column prop="month" label="月份" width="180"></el-table-column>
          <el-table-column prop="totalProfit" label="总盈利" width="180"></el-table-column>
          <el-table-column label="店铺盈利">
            <template #default="scope">
              <el-table :data="scope.row.storeProfits" style="width: 100%">
                <el-table-column prop="storeName" label="店铺名称"></el-table-column>
                <el-table-column prop="profit" label="盈利"></el-table-column>
              </el-table>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane v-if="isSuperAdmin" label="每年盈利" name="yearly">
        <div class="chart-container" v-show="showChart">
          <div ref="yearlyChart" class="chart"></div>
        </div>
        <el-table :data="yearlyData" style="width: 100%" v-show="!showChart">
          <el-table-column prop="year" label="年份" width="180"></el-table-column>
          <el-table-column prop="totalProfit" label="总盈利" width="180"></el-table-column>
          <el-table-column label="店铺盈利">
            <template #default="scope">
              <el-table :data="scope.row.storeProfits" style="width: 100%">
                <el-table-column prop="storeName" label="店铺名称"></el-table-column>
                <el-table-column prop="profit" label="盈利"></el-table-column>
              </el-table>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-dialog v-model="detailDialogVisible" title="详细盈利" width="50%">
        <el-table :data="detailData" style="width: 100%">
          <el-table-column prop="storeName" label="店铺名称" width="180"></el-table-column>
          <el-table-column prop="profit" label="盈利" width="180"></el-table-column>
        </el-table>
      </el-dialog>
    </el-tabs>
  </div>
</template>

<script>
import { mapGetters } from 'vuex';
import { getDailyProfit, getMonthlyProfit, getYearlyProfit } from '@/api/usageRecord';
import * as echarts from 'echarts';

export default {
  name: 'StatisticsView',
  data() {
    return {
      activeTab: 'daily',
      dailyData: [],
      monthlyData: [],
      yearlyData: [],
      detailDialogVisible: false,
      detailData: [],
      dailyChart: null,
      monthlyChart: null,
      yearlyChart: null,
      showChart: false,
      dailyTimeRange: '7', // 默认显示最近7天
      originalDailyData: [], // 存储原始数据
    };
  },
  computed: {
    ...mapGetters('admin', ['adminInfo']),
    isSuperAdmin() {
      return this.adminInfo?.role === 'super_admin' || this.adminInfo?.role === 'SUPER_ADMIN';
    },
  },
  created() {
    this.fetchData();
  },
  mounted() {
    this.initCharts();
  },
  beforeUnmount() {
    this.destroyCharts();
  },
  methods: {
    async fetchData() {
      try {
        // 调用后端 API 获取每日盈利数据
        const dailyResponse = await getDailyProfit();
        const groupedData = {};
        dailyResponse.data.forEach(item => {
          // 如果是admin权限且store_id不匹配，则跳过
          if (this.adminInfo?.role === 'admin' && item.storeId !== this.$store.state.admin.storeId) {
            return;
          }
          if (!groupedData[item.date]) {
            groupedData[item.date] = {
              date: item.date,
              storeName: item.storeName,
              totalProfit: 0,
              roomProfits: []
            };
          }
          groupedData[item.date].totalProfit += item.totalProfit;
          groupedData[item.date].roomProfits.push({
            roomName: item.roomName,
            profit: item.totalProfit
          });
        });
        this.originalDailyData = Object.values(groupedData);
        this.filterDailyDataByTimeRange();
        this.updateDailyChart();

        if (this.isSuperAdmin) {
          // 调用后端 API 获取每月盈利数据
          const monthlyResponse = await getMonthlyProfit();
          const monthlyGroupedData = {};
          monthlyResponse.data.forEach(item => {
            if (!monthlyGroupedData[item.month]) {
              monthlyGroupedData[item.month] = {
                month: item.month,
                totalProfit: 0,
                storeProfits: []
              };
            }
            monthlyGroupedData[item.month].totalProfit += item.totalProfit;
            monthlyGroupedData[item.month].storeProfits.push({
              storeName: item.storeName,
              profit: item.totalProfit
            });
          });
          this.monthlyData = Object.values(monthlyGroupedData);
          this.updateMonthlyChart();

          // 调用后端 API 获取每年盈利数据
          const yearlyResponse = await getYearlyProfit();
          const yearlyGroupedData = {};
          yearlyResponse.data.forEach(item => {
            if (!yearlyGroupedData[item.year]) {
              yearlyGroupedData[item.year] = {
                year: item.year,
                totalProfit: 0,
                storeProfits: []
              };
            }
            yearlyGroupedData[item.year].totalProfit += item.totalProfit;
            yearlyGroupedData[item.year].storeProfits.push({
              storeName: item.storeName,
              profit: item.totalProfit
            });
          });
          this.yearlyData = Object.values(yearlyGroupedData);
          this.updateYearlyChart();
        }
      } catch (error) {
        console.error('获取数据失败:', error);
        this.$message.error('获取数据失败，请稍后重试');
      }
    },
    showMonthlyDetail(row) {
      this.detailData = this.monthlyData.filter(item => item.month === row.month).map(item => ({
        storeName: item.storeName,
        profit: item.totalProfit
      }));
      this.detailDialogVisible = true;
    },
    showYearlyDetail(row) {
      this.detailData = this.yearlyData.filter(item => item.year === row.year).map(item => ({
        storeName: item.storeName,
        profit: item.totalProfit
      }));
      this.detailDialogVisible = true;
    },
    onDailyTimeRangeChange() {
      this.filterDailyDataByTimeRange();
      this.updateDailyChart();
    },
    filterDailyDataByTimeRange() {
      if (!this.originalDailyData.length) {
        this.dailyData = [];
        return;
      }

      const days = parseInt(this.dailyTimeRange);
      const endDate = new Date();
      const startDate = new Date();
      startDate.setDate(endDate.getDate() - days + 1);

      // 格式化日期为 YYYY-MM-DD
      const formatDate = (date) => {
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        return `${year}-${month}-${day}`;
      };

      const startDateStr = formatDate(startDate);
      const endDateStr = formatDate(endDate);

      // 过滤数据
      this.dailyData = this.originalDailyData.filter(item => {
        return item.date >= startDateStr && item.date <= endDateStr;
      });

      // 按日期排序
      this.dailyData.sort((a, b) => new Date(a.date) - new Date(b.date));
    },
    showChartData() {
      this.showChart = true;
      this.$nextTick(() => {
        // 根据当前激活的标签页重新初始化对应的图表
        if (this.activeTab === 'daily') {
          this.updateDailyChart();
        } else if (this.activeTab === 'monthly') {
          this.updateMonthlyChart();
        } else if (this.activeTab === 'yearly') {
          this.updateYearlyChart();
        }
        if (this.$refs.dailyChart && !this.dailyChart) {
          this.dailyChart = echarts.init(this.$refs.dailyChart);
        }
        if (this.$refs.monthlyChart && this.isSuperAdmin && !this.monthlyChart) {
          this.monthlyChart = echarts.init(this.$refs.monthlyChart);
        }
        if (this.$refs.yearlyChart && this.isSuperAdmin && !this.yearlyChart) {
          this.yearlyChart = echarts.init(this.$refs.yearlyChart);
        }
      });
    },
    showTableData() {
      this.showChart = false;
    },
    initCharts() {
      this.$nextTick(() => {
        if (this.$refs.dailyChart) {
          this.dailyChart = echarts.init(this.$refs.dailyChart);
        }
        if (this.$refs.monthlyChart && this.isSuperAdmin) {
          this.monthlyChart = echarts.init(this.$refs.monthlyChart);
        }
        if (this.$refs.yearlyChart && this.isSuperAdmin) {
          this.yearlyChart = echarts.init(this.$refs.yearlyChart);
        }
      });
    },
    destroyCharts() {
      if (this.dailyChart) {
        this.dailyChart.dispose();
        this.dailyChart = null;
      }
      if (this.monthlyChart) {
        this.monthlyChart.dispose();
        this.monthlyChart = null;
      }
      if (this.yearlyChart) {
        this.yearlyChart.dispose();
        this.yearlyChart = null;
      }
    },
    updateDailyChart() {
      if (!this.dailyChart || !this.dailyData.length) return;
      
      const xAxisData = this.dailyData.map(item => item.date);
      const seriesData = this.dailyData.map(item => item.totalProfit);
      
      const option = {
        title: {
          text: '每日盈利统计',
          left: 'center',
          top: 10,
          textStyle: {
            fontSize: 16,
            fontWeight: 'bold'
          }
        },
        grid: {
          left: '3%',
          right: '3%',
          bottom: '25%',
          top: '15%',
          containLabel: true
        },
        tooltip: {
          trigger: 'axis',
          formatter: '{b}<br/>{a}: ¥{c}',
          position: function (point, params, dom, rect, size) {
            // 获取容器和tooltip的尺寸
            const containerWidth = size.viewSize[0];
            const containerHeight = size.viewSize[1];
            const tooltipWidth = size.contentSize[0];
            const tooltipHeight = size.contentSize[1];
            
            // 计算tooltip位置
            let x = point[0];
            let y = point[1];
            
            // 确保tooltip不超出右边界
            if (x + tooltipWidth > containerWidth) {
              x = containerWidth - tooltipWidth - 10;
            }
            
            // 确保tooltip不超出左边界
            if (x < 10) {
              x = 10;
            }
            
            // 确保tooltip不超出下边界
            if (y + tooltipHeight > containerHeight) {
              y = point[1] - tooltipHeight - 10;
            }
            
            // 确保tooltip不超出上边界
            if (y < 10) {
              y = 10;
            }
            
            return [x, y];
          }
        },
        xAxis: {
          type: 'category',
          data: xAxisData,
          axisLabel: {
            rotate: 45,
            interval: 0,
            fontSize: 12,
            margin: 25,
            formatter: function(value) {
              // 如果日期太长，可以截取显示
              if (value.length > 10) {
                return value.substring(5);
              }
              return value;
            }
          }
        },
        yAxis: {
          type: 'value',
          name: '盈利金额(元)',
          nameTextStyle: {
            padding: [0, 0, 0, 30]
          },
          axisLabel: {
            formatter: '¥{value}'
          }
        },
        series: [{
          name: '总盈利',
          type: 'bar',
          data: seriesData,
          barWidth: '70%',
          barMaxWidth: 100,
          barGap: '15%',
          itemStyle: {
            color: '#409EFF'
          }
        }]
      };
      
      this.dailyChart.setOption(option);
    },
    updateMonthlyChart() {
      if (!this.monthlyChart || !this.monthlyData.length) return;
      
      const xAxisData = this.monthlyData.map(item => item.month);
      const seriesData = this.monthlyData.map(item => item.totalProfit);
      
      const option = {
        title: {
          text: '每月盈利统计',
          left: 'center',
          top: 10,
          textStyle: {
            fontSize: 16,
            fontWeight: 'bold'
          }
        },
        grid: {
          left: '3%',
          right: '3%',
          bottom: '25%',
          top: '15%',
          containLabel: true
        },
        tooltip: {
          trigger: 'axis',
          formatter: '{b}<br/>{a}: ¥{c}',
          position: function (point, params, dom, rect, size) {
            // 获取容器和tooltip的尺寸
            const containerWidth = size.viewSize[0];
            const containerHeight = size.viewSize[1];
            const tooltipWidth = size.contentSize[0];
            const tooltipHeight = size.contentSize[1];
            
            // 计算tooltip位置
            let x = point[0];
            let y = point[1];
            
            // 确保tooltip不超出右边界
            if (x + tooltipWidth > containerWidth) {
              x = containerWidth - tooltipWidth - 10;
            }
            
            // 确保tooltip不超出左边界
            if (x < 10) {
              x = 10;
            }
            
            // 确保tooltip不超出下边界
            if (y + tooltipHeight > containerHeight) {
              y = point[1] - tooltipHeight - 10;
            }
            
            // 确保tooltip不超出上边界
            if (y < 10) {
              y = 10;
            }
            
            return [x, y];
          }
        },
        xAxis: {
          type: 'category',
          data: xAxisData,
          axisLabel: {
            rotate: 45,
            interval: 0,
            fontSize: 11,
            margin: 20
          }
        },
        yAxis: {
          type: 'value',
          name: '盈利金额(元)',
          nameTextStyle: {
            padding: [0, 0, 0, 30]
          },
          axisLabel: {
            formatter: '¥{value}'
          }
        },
        series: [{
          name: '总盈利',
          type: 'bar',
          data: seriesData,
          barWidth: '60%',
          barMaxWidth: 80,
          barGap: '20%',
          itemStyle: {
            color: '#67C23A'
          }
        }]
      };
      
      this.monthlyChart.setOption(option);
    },
    updateYearlyChart() {
      if (!this.yearlyChart || !this.yearlyData.length) return;
      
      const xAxisData = this.yearlyData.map(item => item.year);
      const seriesData = this.yearlyData.map(item => item.totalProfit);
      
      const option = {
        title: {
          text: '每年盈利统计',
          left: 'center',
          top: 10,
          textStyle: {
            fontSize: 16,
            fontWeight: 'bold'
          }
        },
        grid: {
          left: '3%',
          right: '3%',
          bottom: '25%',
          top: '15%',
          containLabel: true
        },
        tooltip: {
          trigger: 'axis',
          formatter: '{b}<br/>{a}: ¥{c}',
          position: function (point, params, dom, rect, size) {
            // 获取容器和tooltip的尺寸
            const containerWidth = size.viewSize[0];
            const containerHeight = size.viewSize[1];
            const tooltipWidth = size.contentSize[0];
            const tooltipHeight = size.contentSize[1];
            
            // 计算tooltip位置
            let x = point[0];
            let y = point[1];
            
            // 确保tooltip不超出右边界
            if (x + tooltipWidth > containerWidth) {
              x = containerWidth - tooltipWidth - 10;
            }
            
            // 确保tooltip不超出左边界
            if (x < 10) {
              x = 10;
            }
            
            // 确保tooltip不超出下边界
            if (y + tooltipHeight > containerHeight) {
              y = point[1] - tooltipHeight - 10;
            }
            
            // 确保tooltip不超出上边界
            if (y < 10) {
              y = 10;
            }
            
            return [x, y];
          }
        },
        xAxis: {
          type: 'category',
          data: xAxisData,
          axisLabel: {
            rotate: 45,
            interval: 0,
            fontSize: 11,
            margin: 20
          }
        },
        yAxis: {
          type: 'value',
          name: '盈利金额(元)',
          nameTextStyle: {
            padding: [0, 0, 0, 30]
          },
          axisLabel: {
            formatter: '¥{value}'
          }
        },
        series: [{
          name: '总盈利',
          type: 'bar',
          data: seriesData,
          barWidth: '60%',
          barMaxWidth: 80,
          barGap: '20%',
          itemStyle: {
            color: '#E6A23C'
          }
        }]
      };
      
      this.yearlyChart.setOption(option);
    },
    handleTabChange(tabName) {
      this.$nextTick(() => {
        if (tabName === 'daily' && this.dailyChart) {
          this.dailyChart.resize();
        } else if (tabName === 'monthly' && this.monthlyChart) {
          this.monthlyChart.resize();
        } else if (tabName === 'yearly' && this.yearlyChart) {
          this.yearlyChart.resize();
        }
      });
    },
  },
};
</script>

<style scoped>
.statistics-container {
  padding: 20px;
}

.time-range-selector {
  margin-bottom: 15px;
  text-align: center;
}

.chart-container {
  margin-bottom: 20px;
}

.chart {
  width: 100%;
  height: 450px;
  min-width: 1400px;
}
</style>