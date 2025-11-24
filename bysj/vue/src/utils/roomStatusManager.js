/**
 * 房间状态管理器
 * 用于跨组件和跨标签页的房间状态同步
 */
class RoomStatusManager {
  constructor() {
    this.listeners = new Map()
    this.setupEventListeners()
  }

  /**
   * 设置事件监听器
   */
  setupEventListeners() {
    // 监听localStorage变化（跨标签页通信）
    if (typeof window !== 'undefined') {
      window.addEventListener('storage', (e) => {
        if (e.key === 'roomStatusUpdate') {
          try {
            const event = JSON.parse(e.newValue)
            this.notifyListeners(event)
          } catch (error) {
            console.warn('解析房间状态更新事件失败:', error)
          }
        }
      })

      // 监听自定义事件（同标签页内通信）
      window.addEventListener('roomStatusUpdate', (e) => {
        this.notifyListeners(e.detail)
      })
    }
  }

  /**
   * 添加监听器
   * @param {string} id 监听器ID
   * @param {Function} callback 回调函数
   */
  addListener(id, callback) {
    this.listeners.set(id, callback)
  }

  /**
   * 移除监听器
   * @param {string} id 监听器ID
   */
  removeListener(id) {
    this.listeners.delete(id)
  }

  /**
   * 通知房间状态更新
   * @param {Object} event 更新事件
   */
  notifyListeners(event) {
    console.log('通知房间状态更新:', event)
    
    for (const [id, callback] of this.listeners) {
      try {
        callback(event)
      } catch (error) {
        console.warn(`监听器 ${id} 执行失败:`, error)
      }
    }
  }

  /**
   * 广播房间状态更新
   * @param {number} roomId 房间ID
   * @param {string} status 新状态
   * @param {string} roomName 房间名称（可选）
   */
  broadcastRoomStatusUpdate(roomId, status, roomName = '') {
    const event = {
      type: 'ROOM_STATUS_UPDATE',
      roomId,
      status,
      roomName,
      timestamp: Date.now()
    }

    // 通过localStorage广播（跨标签页）
    if (typeof localStorage !== 'undefined') {
      localStorage.setItem('roomStatusUpdate', JSON.stringify(event))
    }

    // 通过自定义事件广播（同标签页）
    if (typeof window !== 'undefined') {
      window.dispatchEvent(new CustomEvent('roomStatusUpdate', {
        detail: event
      }))
    }
  }

  /**
   * 清理所有监听器
   */
  cleanup() {
    this.listeners.clear()
  }
}

// 创建单例实例
const roomStatusManager = new RoomStatusManager()

export default roomStatusManager