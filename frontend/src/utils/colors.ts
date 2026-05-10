/* ================================================================
   颜色工具函数 — 统一使用 CSS 变量，消除硬编码色值
   ================================================================ */

/** 综合评分颜色 — 返回 CSS 变量值 */
export function getScoreColor(score: number): string {
  if (score >= 80) return 'var(--success-color)'
  if (score >= 60) return 'var(--warning-color)'
  return 'var(--danger-color)'
}

/** 综合评分背景色（淡化版） */
export function getScoreColorDim(score: number): string {
  if (score >= 80) return 'var(--success-color-dim)'
  if (score >= 60) return 'var(--warning-color-dim)'
  return 'var(--danger-color-dim)'
}

/** 估值分位颜色 — 低估绿/合理黄/高估红 */
export function getPercentileColor(percentile: number): string {
  if (percentile < 0.3) return 'var(--success-color)'
  if (percentile > 0.7) return 'var(--danger-color)'
  return 'var(--warning-color)'
}

/** 涨跌方向颜色 */
export function getUpDownColor(direction: 'up' | 'down' | 'flat'): string {
  switch (direction) {
    case 'up': return 'var(--up-color)'
    case 'down': return 'var(--down-color)'
    default: return 'var(--text-tertiary)'
  }
}

/** 数值变化颜色（正数红/负数绿，A股投资场景） */
export function getValueChangeColor(value: number): string {
  if (value > 0) return 'var(--up-color)'
  if (value < 0) return 'var(--down-color)'
  return 'var(--text-tertiary)'
}

/** 预警等级颜色 */
export function getWarningColor(level: 'high' | 'medium' | 'low'): string {
  switch (level) {
    case 'high': return 'var(--danger-color)'
    case 'medium': return 'var(--warning-color)'
    default: return 'var(--success-color)'
  }
}

/** 预警等级背景色 */
export function getWarningColorDim(level: 'high' | 'medium' | 'low'): string {
  switch (level) {
    case 'high': return 'var(--danger-color-dim)'
    case 'medium': return 'var(--warning-color-dim)'
    default: return 'var(--success-color-dim)'
  }
}

/** 市场板块颜色 */
export function getMarketColor(market: string): string {
  switch (market?.toUpperCase()) {
    case 'SH': return 'var(--accent-primary)'
    case 'SZ': return 'var(--accent-pink)'
    case 'BJ': return 'var(--accent-warm)'
    case 'HK': return '#06B6D4'
    default: return 'var(--text-secondary)'
  }
}

/** ECharts 颜色索引 — 按顺序获取金融科技色 */
export function getChartColor(index: number): string {
  const colors = [
    '#2B6AFF', '#F59E0B', '#10B981', '#EF4444',
    '#8B5CF6', '#06B6D4', '#F97316', '#6366F1',
  ]
  return colors[index % colors.length]
}
