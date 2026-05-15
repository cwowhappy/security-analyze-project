/**
 * 财务数据格式化工具
 */

export function useFinancialFormatter() {
  /** 格式化金额（自动转换为 万/亿） */
  const formatMoney = (value: number | null, unit: 'yuan' | 'wan' | 'yi' = 'wan'): string => {
    if (value === null || value === undefined) return '-'
    const div = unit === 'yi' ? 1e8 : unit === 'wan' ? 1e4 : 1
    const suffix = unit === 'yi' ? '亿' : unit === 'wan' ? '万' : '元'
    const formatted = (value / div).toLocaleString('zh-CN', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    })
    return `${formatted} ${suffix}`
  }

  /** 格式化百分比 */
  const formatPercent = (value: number | null, digits: number = 2): string => {
    if (value === null || value === undefined) return '-'
    return `${value.toFixed(digits)}%`
  }

  /** 格式化比率（如 PE/PB） */
  const formatRatio = (value: number | null, digits: number = 2): string => {
    if (value === null || value === undefined) return '-'
    return value.toFixed(digits)
  }

  /** 格式化增长率（带箭头颜色类） */
  const formatGrowth = (value: number | null): { text: string; cls: string } => {
    if (value === null || value === undefined) return { text: '-', cls: 'neutral' }
    const sign = value >= 0 ? '+' : ''
    const cls = value > 0 ? 'positive' : value < 0 ? 'negative' : 'neutral'
    return { text: `${sign}${value.toFixed(2)}%`, cls }
  }

  /** 格式化报告期 */
  const formatReportDate = (dateStr: string, reportType: string): string => {
    const year = dateStr.substring(0, 4)
    const map: Record<string, string> = {
      Y: '年报',
      Q1: '一季报',
      Q2: '半年报',
      Q3: '三季报',
      Q4: '年报',
    }
    return `${year}${map[reportType] || reportType}`
  }

  return {
    formatMoney,
    formatPercent,
    formatRatio,
    formatGrowth,
    formatReportDate,
  }
}
