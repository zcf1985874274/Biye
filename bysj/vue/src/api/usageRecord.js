import request from '@/utils/request'

export function getUsageRecords(params) {
  return request({
    url: '/api/usage-records',
    method: 'get',
    params
  }).then(response => {
    return {
      code: response.data?.code || 200,
      message: response.data?.message || '操作成功',
      data: response.data?.data || response.data // 确保 data 字段存在
    }
  }).catch(error => {
    return Promise.reject({
      code: error.response?.status || 500,
      message: error.response?.data?.message || error.message,
      data: error.response?.data
    })
  })
}

export function getDailyProfit() {
  return request({
    url: '/api/usage-records/daily-profit',
    method: 'get'
  })
}

export function getMonthlyProfit() {
  return request({
    url: '/api/usage-records/monthly-profit',
    method: 'get'
  })
}

export function getYearlyProfit() {
  return request({
    url: '/api/usage-records/yearly-profit',
    method: 'get'
  })
}
export function fetchOrderRecords(params) {
  return request({
    url: '/api/usage-records/self',
    method: 'get',
    params,
    headers: {
      Authorization: `Bearer ${localStorage.getItem('token')}`
    }
  }).then(response => {
    return {
      code: response.data?.code || 200,
      message: response.data?.message || '操作成功',
      data: response.data?.data || response.data // 确保 data 字段存在
    }
  }).catch(error => {
    return Promise.reject({
      code: error.response?.status || 500,
      message: error.response?.data?.message || error.message,
      data: error.response?.data
    })
  })
}


export function updateUsageRecord(data) {
  return request({
    url: `/api/usage-records/${data.recordId}`,
    method: 'put',
    data
  })
}

export function deleteUsageRecord(recordId) {
  return request({
    url: `/api/usage-records/${recordId}`,
    method: 'delete'
  })
}

export function getUsageRecordsByStoreId(storeId) {
  return request({
    url: `/api/usage-records/store/${storeId}`,
    method: 'get'
  }).then(response => {
    return {
      code: response.data?.code || 200,
      message: response.data?.message || '操作成功',
      data: response.data?.data || response.data
    }
  }).catch(error => {
    return Promise.reject({
      code: error.response?.status || 500,
      message: error.response?.data?.message || error.message,
      data: error.response?.data
    })
  })
}
