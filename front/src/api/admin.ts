import request from '@/utils/request'

/**
 * 获取所有用户列表
 */
export function getUserList() {
  return request({
    url: '/admin/users',
    method: 'get'
  })
}

/**
 * 修改用户角色
 * @param userId 用户ID
 * @param roleId 角色ID
 */
export function updateUserRole(userId: number, roleId: number) {
  return request({
    url: `/admin/users/${userId}/role`,
    method: 'put',
    data: { roleId }
  })
}
