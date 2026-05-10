/* ================================================================
   ECharts 金融科技主题 — 专业投资终端风格
   ================================================================ */

import type { EChartsOption } from 'echarts'

/** 金融科技色彩序列 */
export const CHART_COLORS = [
  '#2B6AFF', // 主蓝
  '#F59E0B', // 琥珀金
  '#10B981', // 翡翠绿
  '#EF4444', // 标准红
  '#8B5CF6', // 紫罗兰
  '#06B6D4', // 青色
  '#F97316', // 亮橙
  '#6366F1', // 靛蓝
]

/** 向后兼容：霓虹色彩序列别名（已迁移至金融科技配色） */
export const NEON_COLORS = CHART_COLORS

/** 暗色主题基础配置 */
export const DARK_THEME_BASE: EChartsOption = {
  backgroundColor: 'transparent',
  textStyle: {
    fontFamily: "'JetBrains Mono', 'SF Mono', Consolas, -apple-system, sans-serif",
    color: '#9CA3AF',
  },
  title: {
    textStyle: { color: '#E8EAED', fontSize: 16, fontWeight: 600 },
    subtextStyle: { color: '#6B7280' },
  },
  legend: {
    textStyle: { color: '#9CA3AF' },
    pageTextStyle: { color: '#9CA3AF' },
    inactiveColor: '#4B5563',
  },
  tooltip: {
    backgroundColor: 'rgba(17, 19, 24, 0.95)',
    borderColor: 'rgba(43, 106, 255, 0.20)',
    borderWidth: 1,
    textStyle: { color: '#E8EAED', fontSize: 12 },
    extraCssText: 'backdrop-filter: blur(8px); border-radius: 8px; box-shadow: 0 4px 16px rgba(0,0,0,0.5);',
  },
  grid: {
    borderColor: 'rgba(255,255,255,0.06)',
  },
  categoryAxis: {
    axisLine: { show: true, lineStyle: { color: 'rgba(255,255,255,0.12)' } },
    axisTick: { show: false },
    axisLabel: { color: '#6B7280', fontSize: 11 },
    splitLine: { show: true, lineStyle: { color: 'rgba(255,255,255,0.04)', type: 'dashed' } },
    splitArea: { show: false },
  },
  valueAxis: {
    axisLine: { show: false },
    axisTick: { show: false },
    axisLabel: { color: '#6B7280', fontSize: 11 },
    splitLine: { show: true, lineStyle: { color: 'rgba(255,255,255,0.04)', type: 'dashed' } },
    splitArea: { show: false },
  },
  line: {
    smooth: true,
    symbol: 'circle',
    symbolSize: 6,
    lineStyle: { width: 2 },
    itemStyle: { borderWidth: 2, borderColor: '#0D0F14' },
    emphasis: {
      itemStyle: { borderWidth: 3, shadowBlur: 8, shadowColor: 'rgba(43, 106, 255, 0.3)' },
    },
  },
  bar: {
    itemStyle: { borderRadius: [2, 2, 0, 0] },
    emphasis: {
      itemStyle: { shadowBlur: 8, shadowColor: 'rgba(43, 106, 255, 0.2)' },
    },
  },
  pie: {
    itemStyle: { borderWidth: 2, borderColor: '#0D0F14' },
    emphasis: {
      itemStyle: { shadowBlur: 12, shadowColor: 'rgba(43, 106, 255, 0.2)' },
    },
  },
  radar: {
    axisLine: { lineStyle: { color: 'rgba(255,255,255,0.08)' } },
    splitLine: { lineStyle: { color: 'rgba(255,255,255,0.06)' } },
    splitArea: { show: false },
    axisName: { color: '#6B7280', fontSize: 11 },
  },
}

/** 为 ECharts option 注入金融科技主题 */
export function applyChartTheme(option: EChartsOption): EChartsOption {
  return {
    ...DARK_THEME_BASE,
    color: CHART_COLORS,
    ...option,
    // 深度合并 tooltip
    tooltip: {
      ...(DARK_THEME_BASE.tooltip as object),
      ...(option.tooltip as object || {}),
    },
    // 深度合并 grid
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true,
      ...(DARK_THEME_BASE.grid as object),
      ...(option.grid as object || {}),
    },
  }
}

/** 获取带柔和发光效果的 series 配置 */
export function getChartSeriesStyle(colorIndex: number = 0) {
  const color = CHART_COLORS[colorIndex % CHART_COLORS.length]
  return {
    lineStyle: {
      width: 2,
      shadowBlur: 4,
      shadowColor: color,
    },
    itemStyle: {
      color,
      borderWidth: 2,
      borderColor: '#0D0F14',
    },
    emphasis: {
      itemStyle: {
        borderWidth: 3,
        shadowBlur: 10,
        shadowColor: color,
      },
      lineStyle: {
        width: 3,
        shadowBlur: 8,
        shadowColor: color,
      },
    },
  }
}

/** 获取柱状图金融科技样式 */
export function getChartBarStyle(colorIndex: number = 0) {
  const color = CHART_COLORS[colorIndex % CHART_COLORS.length]
  return {
    itemStyle: {
      color,
      borderRadius: [2, 2, 0, 0],
      shadowBlur: 3,
      shadowColor: color,
    },
    emphasis: {
      itemStyle: {
        shadowBlur: 8,
        shadowColor: color,
      },
    },
  }
}
