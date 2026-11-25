import test from 'node:test'
import assert from 'node:assert/strict'
import { internalRoutes } from '../src/utils/internalRoutes.js'

test('external menus remain intact while internal registration excludes external paths', () => {
  const menus = [{ path: 'http://ruoyi.vip', meta: { title: '官网' } }, { path: '/agri', children: [{ path: 'landplot' }, { path: 'https://example.com' }] }]
  const before = structuredClone(menus)
  const registered = internalRoutes(menus)
  assert.deepEqual(registered, [{ path: '/agri', children: [{ path: 'landplot' }] }])
  assert.deepEqual(menus, before)
})
test('internal iframe routes and relative child routes are preserved', () => {
  const routes = [{ path: '/doc', meta: { link: 'https://example.com' } }, { path: '/system', children: [{ path: 'user' }] }]
  assert.deepEqual(internalRoutes(routes), routes)
})
