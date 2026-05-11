import request from '@/utils/request'

/**
 * 获取当前用户的动态路由列表
 */
export function getMyRoutes() {
  return request({
    url: '/menu/routes',
    method: 'get'
  })
}
