import request from '@/utils/request'

export function statUser(query) {
  return request({
    url: '/stat/user',
    method: 'get',
    params: query
  })
}

export function statOrder(query) {
  return request({
    url: '/stat/order',
    method: 'get',
    params: query
  })
}

export function statOrderEnhanced(query) {
  return request({
    url: '/stat/order/enhanced',
    method: 'get',
    params: query
  })
}

export function statGoods(query) {
  return request({
    url: '/stat/goods',
    method: 'get',
    params: query
  })
}

export function statGoodsRating(query) {
  return request({
    url: '/stat/goods/rating',
    method: 'get',
    params: query
  })
}

export function statGoodsCategories() {
  return request({
    url: '/stat/goods/categories',
    method: 'get'
  })
}

export function statGoodsComment(query) {
  return request({
    url: '/stat/goods/comment',
    method: 'get',
    params: query
  })
}

export function getGoodsWordCloud(goodsId) {
  return request({
    url: `/stat/goods/wordcloud/${goodsId}`,
    method: 'get'
  })
}
