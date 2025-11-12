// import Layout from '@/layout/index'

const roomRouter = {
  path: '/rooms',
  // component: Layout,
  redirect: '/rooms/index',
  name: 'Room',
  meta: {
    title: '房间管理',
    icon: 'el-icon-office-building'
  },
  children: [
    // {
    //   path: 'index',
    //   component: () => import('@/views/RoomBooking'),
    //   name: 'RoomBooking',
    //   meta: {
    //     title: '房间预订',
    //     icon: 'el-icon-tickets'
    //   }
    // },
    // {
    //   path: 'manage',
    //   component: () => import('@/views/RoomManage'),
    //   name: 'RoomManage',
    //   meta: {
    //     title: '房间管理',
    //     icon: 'el-icon-setting',
    //     roles: ['admin']
    //   }
    // },
    // {
    //   path: 'detail/:id',
    //   component: () => import('@/components/RoomDetail'),
    //   name: 'RoomDetail',
    //   meta: {
    //     title: '房间详情',
    //     icon: 'el-icon-info',
    //     hidden: true
    //   }
    // }
  ]
}

export default roomRouter
