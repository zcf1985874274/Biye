import service from '@/utils/request'

// 获取所有店铺
export function getAllStores() {
  return service({
    url: '/api/stores',
    method: 'get'
  })
}

// 根据ID获取店铺信息
export function getStoreById(storeId) {
  return service({
    url: `/api/stores/${storeId}`,
    method: 'get'
  })
}

// 添加店铺
export function addStore(data) {
  return service({
    url: '/api/stores',
    method: 'post',
    data
  })
}

// 更新店铺信息
export function updateStore(data) {
  return service({
    url: `/api/stores/${data.storeId}`,
    method: 'put',
    data
  })
}

// 删除店铺
export function deleteStore(storeId) {
  return service({
    url: `/api/stores/${storeId}`,
    method: 'delete'
  })
}

export default {
  getAllStores,
  getStoreById,
  addStore,
  updateStore,
  deleteStore
}