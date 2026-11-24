import test from 'node:test'
import assert from 'node:assert/strict'
import { parsePolygonWkt, outerPerimeterMetres, convertWgs84Paths } from '../src/utils/landPlotGeometry.js'
const wkt = 'POLYGON((114 38,114.001 38,114.001 38.001,114 38.001,114 38))'
test('parse longitude latitude, closure and optional SRID without changing source', () => {
  const ring = parsePolygonWkt('SRID=4326; ' + wkt)[0]
  assert.deepEqual(ring[0], [114, 38]); assert.deepEqual(ring[0], ring.at(-1)); assert.equal(ring.length, 5)
})
test('preserve holes and arbitrary whitespace', () => {
  assert.equal(parsePolygonWkt('POLYGON ((0 0, 3 0, 3 3, 0 0), (1 1, 2 1, 2 2, 1 1))').length, 2)
})
test('reject invalid, unclosed and wrong CRS geometries', () => {
  for (const value of ['', 'POINT(0 0)', 'SRID=3857;' + wkt, 'POLYGON((0 0,1 0,1 1,0 1))', 'POLYGON((181 0,1 0,1 1,181 0))']) assert.throws(() => parsePolygonWkt(value))
})
test('metre-based perimeter sanity check independent of display coordinates', () => {
  const degrees = 100 / 6371000 * 180 / Math.PI
  assert.ok(Math.abs(outerPerimeterMetres([[0,0],[degrees,0],[degrees,degrees],[0,degrees],[0,0]]) - 400) < 0.01)
})
test('convert every point with official gps conversion, preserve originals and ring boundaries', async () => {
  const paths = parsePolygonWkt(wkt); const snapshot = JSON.stringify(paths); let calls = 0
  const sdk = { convertFrom(points, type, done) {
    calls++; assert.equal(type, 'gps'); done('complete', { locations: points.map(([lon,lat]) => ({getLng:()=>lon+0.006,getLat:()=>lat+0.003})) })
  } }
  const converted = await convertWgs84Paths(sdk, paths)
  assert.equal(calls,1); assert.equal(JSON.stringify(paths),snapshot)
  assert.deepEqual(converted[0][0],[114.006,38.003]); assert.deepEqual(converted[0][0],converted[0].at(-1))
  converted[0][0][0] = 0; assert.equal(converted[0].at(-1)[0],114.006)
})
test('chunk requests and reassemble in input order', async () => {
  const path = Array.from({length:85},(_,i)=>[114+i/100000,38]); path.push(path[0]); let calls=0
  const sdk = {convertFrom(points,type,done) {calls++; assert.ok(points.length<=40); done('complete',{locations:points})}}
  assert.deepEqual(await convertWgs84Paths(sdk,[path]),[path]); assert.equal(calls,3)
})
test('failure never falls back to drawing raw WGS84', async () => {
  await assert.rejects(convertWgs84Paths({convertFrom(p,t,done){done('error',{})}},parsePolygonWkt(wkt)),/转换失败/)
})
test('timeout is recoverable and explicitly reported', async () => {
  await assert.rejects(convertWgs84Paths({convertFrom(){}},parsePolygonWkt(wkt),{timeoutMs:5}),/超时/)
})
test('reject incomplete or nonfinite conversion results', async () => {
  await assert.rejects(convertWgs84Paths({convertFrom(p,t,done){done('complete',{locations:[]})}},parsePolygonWkt(wkt)))
  await assert.rejects(convertWgs84Paths({convertFrom(p,t,done){done('complete',{locations:p.map(()=>[NaN,0])})}},parsePolygonWkt(wkt)))
})