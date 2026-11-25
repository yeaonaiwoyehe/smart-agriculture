// @/api/system/feedback.js
import request from '@/utils/request'

// 查询反馈列表
// 查询反馈列表
export function listFeedback(query) {
  return request({
    url: '/system/feedback/list',
    method: 'post',
    params: {
      current: query.pageNum,
      size: query.pageSize
    },
    data: {
      username: query.username,
      phoneNumber: query.phoneNumber,
      content: query.content,
      isChecked: query.isChecked,
      startCommitTime: query.startCommitTime,
      endCommitTime: query.endCommitTime,
      startCheckedTime: query.startCheckedTime,
      endCheckedTime: query.endCheckedTime,
      labels: query.labels // 这里传递的将是字典值数组
    }
  })
}
// 获取反馈类型字典数据
export function getFeedbackTypes() {
  return request({
    url: '/system/dict/data/type/sys_feedback_type',
    method: 'get'
  })
}
export function checkFeedback(feedback, reply) {
  return request({
    url: '/system/feedback/check',
    method: 'post',
    data: feedback,
    params: {
      reply: reply
    }
  })
}
