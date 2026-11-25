/**
 * 10-系统管理（113 个接口）
 * 依据 AgriNav接口清单.md 自动生成，方法与路径逐条对应。
 */
import { get, post, put, del, patch } from '@/utils/request'

export const postPermissionAdd = (...args) => post('/permission/add', ...args)
export const getPermissionCheck = (...args) => get('/permission/check', ...args)
export const removePermissionDelete = (...args) => del('/permission/delete', ...args)
export const getPermissionGetById = (id, ...rest) => get(`/permission/get/${encodeURIComponent(id)}`, ...rest)
export const getPermissionList = (...args) => get('/permission/list', ...args)
export const postPermissionUpdate = (...args) => post('/permission/update', ...args)
export const postSystemChangelog = (...args) => post('/system/changelog', ...args)
export const putSystemChangelog = (...args) => put('/system/changelog', ...args)
export const removeSystemChangelogById = (id, ...rest) => del(`/system/changelog/${encodeURIComponent(id)}`, ...rest)
export const getSystemChangelogById = (id, ...rest) => get(`/system/changelog/${encodeURIComponent(id)}`, ...rest)
export const getSystemChangelogListByReleaseNoteId = (releaseNoteId, ...rest) => get(`/system/changelog/list/${encodeURIComponent(releaseNoteId)}`, ...rest)
export const postSystemConfig = (...args) => post('/system/config', ...args)
export const putSystemConfig = (...args) => put('/system/config', ...args)
export const getSystemConfigByConfigId = (configId, ...rest) => get(`/system/config/${encodeURIComponent(configId)}`, ...rest)
export const removeSystemConfigByConfigIds = (configIds, ...rest) => del(`/system/config/${encodeURIComponent(configIds)}`, ...rest)
export const getSystemConfigConfigKeyByConfigKey = (configKey, ...rest) => get(`/system/config/configKey/${encodeURIComponent(configKey)}`, ...rest)
export const postSystemConfigExport = (...args) => post('/system/config/export', ...args)
export const getSystemConfigList = (...args) => get('/system/config/list', ...args)
export const removeSystemConfigRefreshCache = (...args) => del('/system/config/refreshCache', ...args)
export const postSystemDept = (...args) => post('/system/dept', ...args)
export const putSystemDept = (...args) => put('/system/dept', ...args)
export const removeSystemDeptByDeptId = (deptId, ...rest) => del(`/system/dept/${encodeURIComponent(deptId)}`, ...rest)
export const getSystemDeptByDeptId = (deptId, ...rest) => get(`/system/dept/${encodeURIComponent(deptId)}`, ...rest)
export const getSystemDeptList = (...args) => get('/system/dept/list', ...args)
export const getSystemDeptListExcludeByDeptId = (deptId, ...rest) => get(`/system/dept/list/exclude/${encodeURIComponent(deptId)}`, ...rest)
export const postSystemDictData = (...args) => post('/system/dict/data', ...args)
export const putSystemDictData = (...args) => put('/system/dict/data', ...args)
export const getSystemDictDataByDictCode = (dictCode, ...rest) => get(`/system/dict/data/${encodeURIComponent(dictCode)}`, ...rest)
export const removeSystemDictDataByDictCodes = (dictCodes, ...rest) => del(`/system/dict/data/${encodeURIComponent(dictCodes)}`, ...rest)
export const postSystemDictDataExport = (...args) => post('/system/dict/data/export', ...args)
export const getSystemDictDataList = (...args) => get('/system/dict/data/list', ...args)
export const getSystemDictDataTypeByDictType = (dictType, ...rest) => get(`/system/dict/data/type/${encodeURIComponent(dictType)}`, ...rest)
export const postSystemDictType = (...args) => post('/system/dict/type', ...args)
export const putSystemDictType = (...args) => put('/system/dict/type', ...args)
export const getSystemDictTypeByDictId = (dictId, ...rest) => get(`/system/dict/type/${encodeURIComponent(dictId)}`, ...rest)
export const removeSystemDictTypeByDictIds = (dictIds, ...rest) => del(`/system/dict/type/${encodeURIComponent(dictIds)}`, ...rest)
export const postSystemDictTypeExport = (...args) => post('/system/dict/type/export', ...args)
export const getSystemDictTypeList = (...args) => get('/system/dict/type/list', ...args)
export const getSystemDictTypeOptionselect = (...args) => get('/system/dict/type/optionselect', ...args)
export const removeSystemDictTypeRefreshCache = (...args) => del('/system/dict/type/refreshCache', ...args)
export const postSystemFeedback = (...args) => post('/system/feedback', ...args)
export const postSystemFeedbackCheck = (...args) => post('/system/feedback/check', ...args)
export const getSystemFeedbackLabels = (...args) => get('/system/feedback/labels', ...args)
export const postSystemFeedbackList = (...args) => post('/system/feedback/list', ...args)
export const getSystemFeedbackMyFeedback = (...args) => get('/system/feedback/myFeedback', ...args)
export const postSystemKeyExchange = (...args) => post('/system/key/exchange', ...args)
export const getSystemKeyPublic = (...args) => get('/system/key/public', ...args)
export const postSystemMenu = (...args) => post('/system/menu', ...args)
export const putSystemMenu = (...args) => put('/system/menu', ...args)
export const removeSystemMenuByMenuId = (menuId, ...rest) => del(`/system/menu/${encodeURIComponent(menuId)}`, ...rest)
export const getSystemMenuByMenuId = (menuId, ...rest) => get(`/system/menu/${encodeURIComponent(menuId)}`, ...rest)
export const getSystemMenuList = (...args) => get('/system/menu/list', ...args)
export const getSystemMenuRoleMenuTreeselectByRoleId = (roleId, ...rest) => get(`/system/menu/roleMenuTreeselect/${encodeURIComponent(roleId)}`, ...rest)
export const getSystemMenuTreeselect = (...args) => get('/system/menu/treeselect', ...args)
export const postSystemNotice = (...args) => post('/system/notice', ...args)
export const putSystemNotice = (...args) => put('/system/notice', ...args)
export const getSystemNoticeByNoticeId = (noticeId, ...rest) => get(`/system/notice/${encodeURIComponent(noticeId)}`, ...rest)
export const removeSystemNoticeByNoticeIds = (noticeIds, ...rest) => del(`/system/notice/${encodeURIComponent(noticeIds)}`, ...rest)
export const getSystemNoticeList = (...args) => get('/system/notice/list', ...args)
export const removeSystemNotification = (...args) => del('/system/notification', ...args)
export const getSystemNotification = (...args) => get('/system/notification', ...args)
export const putSystemNotification = (...args) => put('/system/notification', ...args)
export const removeSystemNotificationById = (id, ...rest) => del(`/system/notification/${encodeURIComponent(id)}`, ...rest)
export const putSystemNotificationById = (id, ...rest) => put(`/system/notification/${encodeURIComponent(id)}`, ...rest)
export const getSystemNotificationUnread = (...args) => get('/system/notification/unread', ...args)
export const postSystemPost = (...args) => post('/system/post', ...args)
export const putSystemPost = (...args) => put('/system/post', ...args)
export const getSystemPostByPostId = (postId, ...rest) => get(`/system/post/${encodeURIComponent(postId)}`, ...rest)
export const removeSystemPostByPostIds = (postIds, ...rest) => del(`/system/post/${encodeURIComponent(postIds)}`, ...rest)
export const postSystemPostExport = (...args) => post('/system/post/export', ...args)
export const getSystemPostList = (...args) => get('/system/post/list', ...args)
export const getSystemPostOptionselect = (...args) => get('/system/post/optionselect', ...args)
export const postSystemReleaseNote = (...args) => post('/system/releaseNote', ...args)
export const putSystemReleaseNote = (...args) => put('/system/releaseNote', ...args)
export const removeSystemReleaseNoteById = (id, ...rest) => del(`/system/releaseNote/${encodeURIComponent(id)}`, ...rest)
export const getSystemReleaseNoteById = (id, ...rest) => get(`/system/releaseNote/${encodeURIComponent(id)}`, ...rest)
export const getSystemReleaseNoteList = (...args) => get('/system/releaseNote/list', ...args)
export const postSystemRole = (...args) => post('/system/role', ...args)
export const putSystemRole = (...args) => put('/system/role', ...args)
export const getSystemRoleByRoleId = (roleId, ...rest) => get(`/system/role/${encodeURIComponent(roleId)}`, ...rest)
export const removeSystemRoleByRoleIds = (roleIds, ...rest) => del(`/system/role/${encodeURIComponent(roleIds)}`, ...rest)
export const getSystemRoleAuthUserAllocatedList = (...args) => get('/system/role/authUser/allocatedList', ...args)
export const putSystemRoleAuthUserCancel = (...args) => put('/system/role/authUser/cancel', ...args)
export const putSystemRoleAuthUserCancelAll = (...args) => put('/system/role/authUser/cancelAll', ...args)
export const putSystemRoleAuthUserSelectAll = (...args) => put('/system/role/authUser/selectAll', ...args)
export const getSystemRoleAuthUserUnallocatedList = (...args) => get('/system/role/authUser/unallocatedList', ...args)
export const putSystemRoleChangeStatus = (...args) => put('/system/role/changeStatus', ...args)
export const putSystemRoleDataScope = (...args) => put('/system/role/dataScope', ...args)
export const getSystemRoleDeptTreeByRoleId = (roleId, ...rest) => get(`/system/role/deptTree/${encodeURIComponent(roleId)}`, ...rest)
export const postSystemRoleExport = (...args) => post('/system/role/export', ...args)
export const getSystemRoleList = (...args) => get('/system/role/list', ...args)
export const getSystemRoleOptionselect = (...args) => get('/system/role/optionselect', ...args)
export const postSystemUser = (...args) => post('/system/user', ...args)
export const putSystemUser = (...args) => put('/system/user', ...args)
export const getSystemUser = (...args) => get('/system/user/', ...args)
export const getSystemUserByUserId = (userId, ...rest) => get(`/system/user/${encodeURIComponent(userId)}`, ...rest)
export const removeSystemUserByUserIds = (userIds, ...rest) => del(`/system/user/${encodeURIComponent(userIds)}`, ...rest)
export const putSystemUserAuthRole = (...args) => put('/system/user/authRole', ...args)
export const getSystemUserAuthRoleByUserId = (userId, ...rest) => get(`/system/user/authRole/${encodeURIComponent(userId)}`, ...rest)
export const postSystemUserBind = (...args) => post('/system/user/bind', ...args)
export const putSystemUserChangeStatus = (...args) => put('/system/user/changeStatus', ...args)
export const getSystemUserCheckByPhoneNumber = (phoneNumber, ...rest) => get(`/system/user/check/${encodeURIComponent(phoneNumber)}`, ...rest)
export const getSystemUserDeptTree = (...args) => get('/system/user/deptTree', ...args)
export const postSystemUserExport = (...args) => post('/system/user/export', ...args)
export const postSystemUserImportData = (...args) => post('/system/user/importData', ...args)
export const postSystemUserImportTemplate = (...args) => post('/system/user/importTemplate', ...args)
export const getSystemUserList = (...args) => get('/system/user/list', ...args)
export const getSystemUserProfile = (...args) => get('/system/user/profile', ...args)
export const putSystemUserProfile = (...args) => put('/system/user/profile', ...args)
export const postSystemUserProfileAvatar = (...args) => post('/system/user/profile/avatar', ...args)
export const putSystemUserProfilePhoneNumber = (...args) => put('/system/user/profile/phoneNumber', ...args)
export const putSystemUserProfileUpdatePwd = (...args) => put('/system/user/profile/updatePwd', ...args)
export const putSystemUserResetPwd = (...args) => put('/system/user/resetPwd', ...args)

export const systemApi = {
  postPermissionAdd,
  getPermissionCheck,
  removePermissionDelete,
  getPermissionGetById,
  getPermissionList,
  postPermissionUpdate,
  postSystemChangelog,
  putSystemChangelog,
  removeSystemChangelogById,
  getSystemChangelogById,
  getSystemChangelogListByReleaseNoteId,
  postSystemConfig,
  putSystemConfig,
  getSystemConfigByConfigId,
  removeSystemConfigByConfigIds,
  getSystemConfigConfigKeyByConfigKey,
  postSystemConfigExport,
  getSystemConfigList,
  removeSystemConfigRefreshCache,
  postSystemDept,
  putSystemDept,
  removeSystemDeptByDeptId,
  getSystemDeptByDeptId,
  getSystemDeptList,
  getSystemDeptListExcludeByDeptId,
  postSystemDictData,
  putSystemDictData,
  getSystemDictDataByDictCode,
  removeSystemDictDataByDictCodes,
  postSystemDictDataExport,
  getSystemDictDataList,
  getSystemDictDataTypeByDictType,
  postSystemDictType,
  putSystemDictType,
  getSystemDictTypeByDictId,
  removeSystemDictTypeByDictIds,
  postSystemDictTypeExport,
  getSystemDictTypeList,
  getSystemDictTypeOptionselect,
  removeSystemDictTypeRefreshCache,
  postSystemFeedback,
  postSystemFeedbackCheck,
  getSystemFeedbackLabels,
  postSystemFeedbackList,
  getSystemFeedbackMyFeedback,
  postSystemKeyExchange,
  getSystemKeyPublic,
  postSystemMenu,
  putSystemMenu,
  removeSystemMenuByMenuId,
  getSystemMenuByMenuId,
  getSystemMenuList,
  getSystemMenuRoleMenuTreeselectByRoleId,
  getSystemMenuTreeselect,
  postSystemNotice,
  putSystemNotice,
  getSystemNoticeByNoticeId,
  removeSystemNoticeByNoticeIds,
  getSystemNoticeList,
  removeSystemNotification,
  getSystemNotification,
  putSystemNotification,
  removeSystemNotificationById,
  putSystemNotificationById,
  getSystemNotificationUnread,
  postSystemPost,
  putSystemPost,
  getSystemPostByPostId,
  removeSystemPostByPostIds,
  postSystemPostExport,
  getSystemPostList,
  getSystemPostOptionselect,
  postSystemReleaseNote,
  putSystemReleaseNote,
  removeSystemReleaseNoteById,
  getSystemReleaseNoteById,
  getSystemReleaseNoteList,
  postSystemRole,
  putSystemRole,
  getSystemRoleByRoleId,
  removeSystemRoleByRoleIds,
  getSystemRoleAuthUserAllocatedList,
  putSystemRoleAuthUserCancel,
  putSystemRoleAuthUserCancelAll,
  putSystemRoleAuthUserSelectAll,
  getSystemRoleAuthUserUnallocatedList,
  putSystemRoleChangeStatus,
  putSystemRoleDataScope,
  getSystemRoleDeptTreeByRoleId,
  postSystemRoleExport,
  getSystemRoleList,
  getSystemRoleOptionselect,
  postSystemUser,
  putSystemUser,
  getSystemUser,
  getSystemUserByUserId,
  removeSystemUserByUserIds,
  putSystemUserAuthRole,
  getSystemUserAuthRoleByUserId,
  postSystemUserBind,
  putSystemUserChangeStatus,
  getSystemUserCheckByPhoneNumber,
  getSystemUserDeptTree,
  postSystemUserExport,
  postSystemUserImportData,
  postSystemUserImportTemplate,
  getSystemUserList,
  getSystemUserProfile,
  putSystemUserProfile,
  postSystemUserProfileAvatar,
  putSystemUserProfilePhoneNumber,
  putSystemUserProfileUpdatePwd,
  putSystemUserResetPwd,
}
export default systemApi
