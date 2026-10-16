/** 格式化后端返回的时间 (ISO -> yyyy-MM-dd HH:mm:ss) */
export function formatTime(value) {
  if (!value) return ''
  return String(value).replace('T', ' ').slice(0, 19)
}

/** 金额展示 */
export function formatPrice(value) {
  if (value === null || value === undefined) return '--'
  return Number(value).toFixed(2)
}

/** 逗号分隔图片 -> 数组 */
export function imageList(images) {
  if (!images) return []
  return String(images).split(',').filter(Boolean)
}
