<template>
  <div class="room-list-container">
    <div class="filter-section">
      <el-select
          v-model="selectedStoreId"
          placeholder="请选择门店"
          @change="fetchRoomsByStore"
          style="margin-right: 20px; width: 200px"
      >
        <el-option
            v-for="store in stores"
            :key="store.storeId"
            :label="store.storeName"
            :value="store.storeId"
        ></el-option>
      </el-select>

      <el-radio-group v-model="filterType" @change="handleFilterChange">

      </el-radio-group>
    </div>

    <div class="rooms-grid">
      <div
          v-for="room in rooms"
          :key="room.roomId"
          class="room-card"
      >
        <div class="room-image">
          <img :src="room.imagePath || 'https://via.placeholder.com/300x200'" alt="房间图片">
        </div>
        <div class="room-info">
          <h3>{{ room.roomName }}</h3>
          <p class="room-type">{{ room.roomType }}</p>
          <p class="room-players">人数: {{ room.minPlayers }}-{{ room.maxPlayers }}人</p>
          <p class="room-price">¥{{ room.pricePerHour }}/小时</p>
          <p class="room-status" :class="room.status === 'available' ? 'available' : 'occupied'">
            {{ room.status === 'available' ? '可预订' : '已占用' }}
          </p>

          <el-button
              type="primary"
              size="small"
              :disabled="room.status !== 'available'"
              @click="handleBook(room)"
          >
            立即预订
          </el-button>
        </div>
      </div>
    </div>

    <div class="pagination-section">
      <el-pagination
          @current-change="handlePageChange"
          :current-page="currentPage"
          :page-size="pageSize"
          layout="prev, pager, next"
          :total="totalRooms"
      >
      </el-pagination>
    </div>

    <booking-dialog
        v-if="selectedRoom"
        :room="selectedRoom"
        :visible.sync="bookingDialogVisible"
        @confirm="handleBookingConfirm"
    />
  </div>
</template>

<script>
import BookingDialog from './BookingDialog.vue'
import request from '@/utils/request'
import { getRoomsByStoreId} from '@/api/room'
import { getAllStores } from '@/api/store'
import roomStatusManager from '@/utils/roomStatusManager'
export default {
  name: 'RoomList',
  components: {
    BookingDialog
  },
  data() {
    return {
      rooms: [],
      stores: [],
      selectedStoreId: null,
      filterType: 'all',
      currentPage: 1,
      pageSize: 10,
      selectedRoom: null,
      totalRooms: 0,
      pollingTimer: null, // 定时器
      pollingInterval: 10000, // 10秒轮询一次
      isVisible: true, // 页面是否可见
      roomStatusListenerId: `RoomList_${Date.now()}`, // 唯一监听器ID
    }
  },
  created() {
    this.fetchStores()
    this.fetchRooms()
    this.startPolling()
    this.setupRoomStatusListener()
  },
  
  mounted() {
    // 监听页面可见性变化
    document.addEventListener('visibilitychange', this.handleVisibilityChange)
    window.addEventListener('focus', this.handleWindowFocus)
    window.addEventListener('blur', this.handleWindowBlur)
  },
  
  beforeDestroy() {
    this.stopPolling()
    document.removeEventListener('visibilitychange', this.handleVisibilityChange)
    window.removeEventListener('focus', this.handleWindowFocus)
    window.removeEventListener('blur', this.handleWindowBlur)
    
    // 移除房间状态监听器
    roomStatusManager.removeListener(this.roomStatusListenerId)
  },


  methods: {
    async fetchRooms() {
      try {
        console.log('开始获取房间数据...');
        const { filterType, currentPage: page, pageSize: size } = this;
        const response = await this.$store.dispatch('room/fetchRooms', {
          filterType,
          page,
          size
        });
        console.log('获取到的完整响应:', response);

        if (response?.code === 200) {
          const roomData = Array.isArray(response?.data) ? response.data : [];
          console.log('处理前的房间数据:', roomData);

          if (roomData.length === 0) {
            console.warn('警告: 获取到的房间数据为空数组');
          }

          // 使用Vue.set确保响应式更新
          this.$set(this, 'rooms', roomData.map(room => {
            const statusMap = {
              '空闲': 'available',
              '使用中': 'occupied'
            };
            return {
              roomId: room.roomId || 0,
              roomName: room.roomName || '',
              roomType: room.roomType || '',
              maxPlayers: room.maxPlayers || 0,
              minPlayers: room.minPlayers || 0,
              pricePerHour: room.pricePerHour || 0,
              description: room.description || '',
              imagePath: room.imagePath || 'https://via.placeholder.com/300x200',
              status: statusMap[room.status] || 'occupied',
              password: room.password || '',
              storeId: room.storeId || 1 // 确保包含storeId，默认为1
            };
          }));
        } else {
          console.error('获取房间列表失败:', response.message || '未知错误');
        }
      } catch (error) {
        console.error('获取房间列表失败 - 错误:', error);
        this.$message.error('获取房间列表失败，请检查网络连接或稍后再试');
      }
    },

    fetchRoomsByStore() {
      if (!this.selectedStoreId) {
        this.fetchRooms();  // 如果没有选定门店，调用 fetchRooms 获取所有房间
        return;
      }

      getRoomsByStoreId(this.selectedStoreId).then(response => {
        const roomData = Array.isArray(response?.data) ? response.data : [];
        this.$set(this, 'rooms', roomData.map(room => {
          const statusMap = {
            '空闲': 'available',
            '使用中': 'occupied'
          };
          return {
            roomId: room.roomId || 0,
            roomName: room.roomName || '',
            roomType: room.roomType || '',
            maxPlayers: room.maxPlayers || 0,
            minPlayers: room.minPlayers || 0,
            pricePerHour: room.pricePerHour || 0,
            description: room.description || '',
            imagePath: room.imagePath || 'https://via.placeholder.com/300x200',
            status: statusMap[room.status] || 'occupied',
            password: room.password || '',
            storeId: room.storeId || this.selectedStoreId || 1 // 确保包含storeId
          };
        }));
      }).catch(error => {
        console.error('获取房间列表失败:', error);
        this.$message.error('获取房间列表失败，请稍后再试');
      });
    },

    fetchStores() {
      getAllStores().then(response => {
        this.stores = response.data;
      }).catch(error => {
        console.error('获取门店列表失败:', error);
        this.$message.error('获取门店列表失败，请稍后再试');
      });
    },



    handlePageChange(page) {
      this.currentPage = page;
      this.fetchRooms();
    },

    handleFilterChange(value) {
      if (value === 'available') {
        this.fetchAvailableRoomsByStore();
      } else {
        this.fetchRoomsByStore();
      }
    },

    handleBook(room) {
      this.selectedRoom = room;
      this.bookingDialogVisible = true;
    },

    async handleBookingConfirm(bookingInfo) {
      try {
        // 1. 获取用户ID
        let userId = this.$store.state.user?.userInfo?.userId ||
            this.$store.state.user?.userId ||
            this.$store.state.userInfo?.userId;

        if (!userId) {
          const username = localStorage.getItem('username');
          if (username) {
            const userInfo = await this.$store.dispatch('user/getUserInfo');
            userId = userInfo?.userId;
          }

          if (!userId) {
            throw new Error('无法获取用户信息，请重新登录');
          }
        }

        // 2. 计算开始和结束时间
        const startTime = new Date();
        const endTime = new Date(startTime.getTime() + bookingInfo.hours * 60 * 60 * 1000);

        // 获取房间信息以获取storeId
        const room = this.rooms.find(r => r.roomId === bookingInfo.roomId);
        const storeId = bookingInfo.storeId || room?.storeId || 1; // 默认为1如果找不到
        
        console.log('预订信息:', {
          roomId: bookingInfo.roomId,
          userId: userId,
          storeId: storeId,
          room: room
        });

        // 3. 调用API创建使用记录
        const recordResponse = await request({
          url: '/api/usage-records',
          method: 'post',
          headers: {
            'Authorization': 'Bearer ' + localStorage.getItem('token')
          },
          data: {
            roomId: bookingInfo.roomId,
            userId: userId,
            startTime: startTime.toISOString(),
            endTime: endTime.toISOString(),
            totalPrice: bookingInfo.totalPrice,
            storeId: storeId
          }
        });

        if (!recordResponse || recordResponse.code !== 200) {
          throw new Error(recordResponse?.message || '创建使用记录失败');
        }

        // 4. 更新房间状态为"使用中"
        const updateResponse = await request({
          url: `/api/rooms/${bookingInfo.roomId}/status`,
          method: 'patch',
          data: {
            status: '使用中'
          }
        });

        if (!updateResponse || updateResponse.code !== 200) {
          throw new Error(updateResponse?.message || '更新房间状态失败');
        }

        // 5. 刷新房间列表
        await this.fetchRooms();
        
        // 6. 通知其他用户更新房间状态
        this.notifyRoomStatusUpdate(bookingInfo.roomId, '使用中');

        this.$message.success(`房间预订成功! 总费用: ¥${bookingInfo.totalPrice}`);
      } catch (error) {
        this.$message.error(`预订失败: ${error.message}`);
        console.error('预订错误:', error);
      }
    },

    // 开始轮询
    startPolling() {
      if (this.pollingTimer) {
        clearInterval(this.pollingTimer)
      }
      
      this.pollingTimer = setInterval(() => {
        if (this.isVisible) {
          this.fetchRoomsSilently()
        }
      }, this.pollingInterval)
      
      console.log('房间状态轮询已启动，间隔:', this.pollingInterval, 'ms')
    },

    // 停止轮询
    stopPolling() {
      if (this.pollingTimer) {
        clearInterval(this.pollingTimer)
        this.pollingTimer = null
        console.log('房间状态轮询已停止')
      }
    },

    // 静默刷新房间数据（不显示加载状态）
    async fetchRoomsSilently() {
      try {
        if (this.selectedStoreId) {
          await getRoomsByStoreId(this.selectedStoreId).then(response => {
            const roomData = Array.isArray(response?.data) ? response.data : []
            this.updateRoomsData(roomData)
          })
        } else {
          const { filterType, currentPage: page, pageSize: size } = this
          const response = await this.$store.dispatch('room/fetchRooms', {
            filterType,
            page,
            size
          })
          
          if (response?.code === 200) {
            const roomData = Array.isArray(response?.data) ? response.data : []
            this.updateRoomsData(roomData)
          }
        }
      } catch (error) {
        console.warn('静默刷新房间数据失败:', error)
      }
    },

    // 更新房间数据
    updateRoomsData(roomData) {
      const statusMap = {
        '空闲': 'available',
        '使用中': 'occupied'
      }
      
      const newRooms = roomData.map(room => ({
        roomId: room.roomId || 0,
        roomName: room.roomName || '',
        roomType: room.roomType || '',
        maxPlayers: room.maxPlayers || 0,
        minPlayers: room.minPlayers || 0,
        pricePerHour: room.pricePerHour || 0,
        description: room.description || '',
        imagePath: room.imagePath || 'https://via.placeholder.com/300x200',
        status: statusMap[room.status] || 'occupied',
        password: room.password || '',
        storeId: room.storeId || this.selectedStoreId || 1
      }))

      // 检查房间状态变化并通知用户
      this.checkRoomStatusChanges(this.rooms, newRooms)
      
      // 更新房间数据
      this.$set(this, 'rooms', newRooms)
    },

    // 检查房间状态变化
    checkRoomStatusChanges(oldRooms, newRooms) {
      const oldRoomMap = new Map(oldRooms.map(room => [room.roomId, room.status]))
      const newRoomMap = new Map(newRooms.map(room => [room.roomId, room.status]))
      
      for (const [roomId, newStatus] of newRoomMap) {
        const oldStatus = oldRoomMap.get(roomId)
        if (oldStatus && oldStatus !== newStatus) {
          const room = newRooms.find(r => r.roomId === roomId)
          const statusText = newStatus === 'available' ? '可预订' : '已占用'
          
          // 如果房间从占用变为可用，显示通知
          if (oldStatus === 'occupied' && newStatus === 'available') {
            this.$message({
              message: `房间 "${room.roomName}" 现在可预订了！`,
              type: 'success',
              duration: 3000
            })
          }
          // 如果房间从可用变为占用，显示通知
          else if (oldStatus === 'available' && newStatus === 'occupied') {
            this.$message({
              message: `房间 "${room.roomName}" 已被预订`,
              type: 'info',
              duration: 3000
            })
          }
        }
      }
    },

    // 通知房间状态更新
    notifyRoomStatusUpdate(roomId, status) {
      const room = this.rooms.find(r => r.roomId === roomId)
      const roomName = room ? room.roomName : ''
      
      // 使用房间状态管理器广播更新
      roomStatusManager.broadcastRoomStatusUpdate(roomId, status, roomName)
    },

    // 处理页面可见性变化
    handleVisibilityChange() {
      this.isVisible = !document.hidden
      if (this.isVisible) {
        console.log('页面变为可见，立即刷新房间数据')
        this.fetchRoomsSilently()
      }
    },

    // 处理窗口获得焦点
    handleWindowFocus() {
      this.isVisible = true
      console.log('窗口获得焦点，立即刷新房间数据')
      this.fetchRoomsSilently()
    },

    // 处理窗口失去焦点
    handleWindowBlur() {
      this.isVisible = false
    },

    // 设置房间状态监听器
    setupRoomStatusListener() {
      roomStatusManager.addListener(this.roomStatusListenerId, (event) => {
        console.log('收到房间状态更新:', event)
        
        // 如果是其他用户的操作，立即刷新房间数据
        if (event.type === 'ROOM_STATUS_UPDATE') {
          this.fetchRoomsSilently()
          
          // 显示状态变化通知
          const room = this.rooms.find(r => r.roomId === event.roomId)
          if (room && event.roomName) {
            const statusText = event.status === '使用中' ? '已被预订' : '现在可预订'
            const messageType = event.status === '使用中' ? 'info' : 'success'
            
            this.$message({
              message: `房间 "${event.roomName}" ${statusText}`,
              type: messageType,
              duration: 3000
            })
          }
        }
      })
    }
  }

}
</script>

<style scoped>
.room-list-container {
  padding: 20px;
}

.filter-section {
  margin-bottom: 20px;
}

.rooms-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 20px;
}

.room-card {
  background: white;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  transition: transform 0.2s;
}

.room-card:hover {
  transform: translateY(-5px);
}

.room-image img {
  width: 100%;
  height: 180px;
  object-fit: cover;
}

.room-info {
  padding: 15px;
}

.room-info h3 {
  margin: 0 0 10px 0;
  font-size: 16px;
  color: #333;
}

.room-info p {
  margin: 5px 0;
  font-size: 14px;
  color: #666;
}

.room-price {
  color: #f56c6c;
  font-weight: bold;
}

.room-status.available {
  color: #67c23a;
}

.room-status.occupied {
  color: #f56c6c;
}

.pagination-section {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}
</style>
