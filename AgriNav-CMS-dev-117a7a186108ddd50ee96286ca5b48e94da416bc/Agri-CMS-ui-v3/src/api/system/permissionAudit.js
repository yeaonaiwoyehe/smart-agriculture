import { get } from '@/utils/request'

/**
 * 权限审计总览（A-20 / MP-06）
 *
 * 真实接口：GET /permission/audit
 * 说明：
 *   - 返回单个审计对象（summary / userPermissions / roles / violations / risks），
 *     不是 TableDataInfo，因此不接收任何分页参数；
 *   - 后端仅管理员可访问（@PreAuthorize("@ss.hasRole('admin')")）；
 *   - 本文件只定义请求，不在此处调用接口。
 */
export function getPermissionAudit() {
  return get('/permission/audit')
}

export const permissionAuditApi = { getPermissionAudit }
export default permissionAuditApi
