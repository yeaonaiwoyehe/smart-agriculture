const eventLabels = {
  STOP: '停留',
  OVERSPEED: '推断超速',
  GPS_SPIKE: 'GPS 跳点',
  GAP: '疑似漏采',
  TRANSFER_CANDIDATE: '疑似转场'
}

export async function getDemoWorkorder() {
  const response = await fetch('/b11-demo-api/trajectory/playback/anomalies/demo/workorder', {
    signal: AbortSignal.timeout(8000)
  })
  const { code, data } = await response.json()
  if (!response.ok || code !== 200 || data?.analysis?.source !== 'DEMO_FIXTURE' || !data.track?.length) {
    throw new Error('演示轨迹接口不可用')
  }
  const { analysis, track, boundary } = data
  return {
    id: analysis.trajectoryId,
    landId: analysis.plotId,
    landName: '演示地块',
    district: '本机演示',
    operatorName: '样例农机',
    type: '耕地',
    startAt: analysis.startTime,
    duration: (Date.parse(analysis.endTime) - Date.parse(analysis.startTime)) / 60000,
    width: data.widthCm,
    pointCount: track.length,
    track: track.map(({ longitude, latitude }) => [longitude, latitude]),
    trackTimes: track.map(({ time }) => time),
    breakBefore: data.breakBefore,
    landPoints: boundary,
    gpsArea: data.grossAreaMu,
    effectiveArea: data.effectiveAreaMu,
    landArea: data.landAreaMu,
    boundaryCut: data.outsideAreaMu,
    diffRate: data.grossAreaMu > 0 ? 100 * (data.grossAreaMu - data.effectiveAreaMu) / data.grossAreaMu : null,
    isDemo: true,
    anomalies: analysis.events.map((event) => {
      const marker = event.spikePoint || (event.kind === 'OVERSPEED' ? event.end : event.start)
      return {
        type: eventLabels[event.kind] || event.kind,
        point: marker.pointIndex,
        at: marker.point.time,
        desc: event.inferredSpeedKmh != null
          ? `推断速度 ${event.inferredSpeedKmh.toFixed(1)} km/h`
          : event.maxDisplacementMetres != null
            ? `场外最远位移 ${event.maxDisplacementMetres.toFixed(1)} 米`
            : `持续 ${event.durationSeconds} 秒`
      }
    })
  }
}
