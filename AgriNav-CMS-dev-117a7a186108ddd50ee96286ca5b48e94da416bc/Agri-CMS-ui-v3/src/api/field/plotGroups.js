import request from '@/utils/request'

export function getPlotGroups(type) {
  return request({ url: '/landPlot/groups', method: 'get', params: { type }, returnBusinessErrors: true })
}
export function getPlotGroupAssignments(plotIds) {
  return request({ url: '/landPlot/groups/assignments', method: 'get', params: { plotIds: plotIds.join(',') }, returnBusinessErrors: true })
}
export function createPlotGroup(data) {
  return request({ url: '/landPlot/groups', method: 'post', data, returnBusinessErrors: true })
}
export function assignPlotGroup(data) {
  return request({ url: '/landPlot/groups/assignment', method: 'put', data, returnBusinessErrors: true })
}
