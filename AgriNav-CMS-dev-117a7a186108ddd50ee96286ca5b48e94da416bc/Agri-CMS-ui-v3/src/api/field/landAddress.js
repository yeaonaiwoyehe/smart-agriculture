import request from '@/utils/request'

export function getProvinces() {
  return request({
    url: '/landAddress/getProvince',
    method: 'get'
  })
}

export function getCities(data) {
  return request({
    url: '/landAddress/getCity',
    method: 'post',
    data: data
  })
}

export function getDistricts(data) {
  return request({
    url: '/landAddress/getDistrict',
    method: 'post',
    data: data
  })
}

export function getVillages(data) {
  return request({
    url: '/landAddress/getVillage',
    method: 'post',
    data: data
  })
}

export function getHamlets(data) {
  return request({
    url: '/landAddress/getHamlet',
    method: 'post',
    data: data
  })
}
