# 财务报告分析模块 · 设计规范

> 版本：v1.0 | 日期：2026-05-12 | 基于：Stripe Design System

---

## 一、组件设计令牌

### 1.1 色彩系统（扩展自 Stripe）

```css
/* === 财务报告分析模块专用色彩 === */

/* 财务指标专用语义色 */
--indicator-positive: #00D924;   /* 正向指标（如营收增长）*/
--indicator-negative: #FF3B30;   /* 负向指标（如负债率过高）*/
--indicator-neutral: #697386;    /* 中性指标 */

--indicator-growth: #00C853;     /* 成长性指标 */
--indicator-profitability: #635BFF; /* 盈利能力指标 */
--indicator-valuation: #FF9500;  /* 估值指标 */
--indicator-debt: #FF6B6B;       /* 偿债指标 */

/* 趋势图表专用色 */
--chart-revenue: #635BFF;        /* 营收 */
--chart-profit: #00D924;         /* 净利润 */
--chart-cfo: #00A3FF;            /* 经营现金流 */
--chart-roa: #FF9500;            /* ROA */
--chart-roe: #C9A0FF;            /* ROE */
--chart-gross: #FF6B9D;          /* 毛利率 */

/* 表格斑马纹 */
--table-row-alt: rgba(99, 91, 255, 0.03);
```

### 1.2 排版系统

```css
/* 财务数据专用排版 */
.financial-value {
  font-family: var(--mono);
  font-size: 14px;
  font-weight: 500;
  letter-spacing: -0.02em;
}

.financial-value-large {
  font-family: var(--mono);
  font-size: 20px;
  font-weight: 600;
  letter-spacing: -0.03em;
}

.financial-label {
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  color: var(--text-muted);
}

.financial-percent {
  font-family: var(--mono);
  font-size: 13px;
  font-weight: 600;
}

/* 增长指标 */
.growth-positive { color: var(--indicator-positive); }
.growth-negative { color: var(--indicator-negative); }
.growth-neutral { color: var(--text-muted); }
```

### 1.3 间距系统

```css
/* 财务看板间距 */
--fin-dashboard-gap: 16px;       /* 指标卡片间距 */
--fin-section-gap: 24px;         /* 区块间距 */
--fin-table-cell-pad: 12px;      /* 表格单元格内边距 */
--fin-card-pad: 20px;            /* 卡片内边距 */

/* 财务指标卡片 */
.indicator-card {
  padding: var(--fin-card-pad);
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
}

.indicator-card-grid {
  display: grid;
  gap: var(--fin-dashboard-gap);
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
}
```

---

## 二、组件样式规范

### 2.1 财务指标卡片（IndicatorCard）

```vue
<!-- src/views/financial/components/IndicatorCard.vue -->

<template>
  <div class="indicator-card">
    <div class="indicator-label">{{ label }}</div>
    <div class="indicator-value" :class="valueClass">
      {{ formattedValue }}
    </div>
    <div class="indicator-meta" v-if="meta">
      <span class="indicator-period">{{ meta.period }}</span>
      <span class="indicator-compare" :class="compareClass">
        {{ meta.compare }}
      </span>
    </div>
  </div>
</template>

<style scoped>
.indicator-card {
  padding: var(--fin-card-pad);
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  transition: box-shadow 0.15s, border-color 0.15s;
}

.indicator-card:hover {
  box-shadow: var(--shadow-md);
  border-color: var(--primary);
}

.indicator-label {
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  color: var(--text-muted);
  margin-bottom: 8px;
}

.indicator-value {
  font-family: var(--mono);
  font-size: 24px;
  font-weight: 600;
  color: var(--text-primary);
  letter-spacing: -0.02em;
  margin-bottom: 8px;
}

.indicator-value.positive { color: var(--indicator-positive); }
.indicator-value.negative { color: var(--indicator-negative); }

.indicator-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
}

.indicator-period {
  color: var(--text-muted);
}

.indicator-compare {
  font-weight: 500;
  padding: 2px 6px;
  border-radius: 4px;
}

.indicator-compare.up {
  background: rgba(0, 217, 36, 0.1);
  color: var(--indicator-positive);
}

.indicator-compare.down {
  background: rgba(255, 59, 48, 0.1);
  color: var(--indicator-negative);
}
</style>
```

### 2.2 财务报表表格（FinancialTable）

```vue
<!-- src/views/financial/components/FinancialTable.vue -->

<template>
  <div class="financial-table-wrapper">
    <table class="financial-table">
      <thead>
        <tr>
          <th class="sticky-col">指标</th>
          <th v-for="period in periods" :key="period" class="text-right">
            {{ period }}
          </th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="row in data" :key="row.key">
          <td class="sticky-col">{{ row.label }}</td>
          <td v-for="period in periods" :key="period" class="text-right">
            {{ formatValue(row[period]) }}
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<style scoped>
.financial-table-wrapper {
  overflow-x: auto;
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
}

.financial-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.financial-table th,
.financial-table td {
  padding: var(--fin-table-cell-pad);
  border-bottom: 1px solid var(--border);
}

.financial-table th {
  background: var(--bg);
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  color: var(--text-muted);
  white-space: nowrap;
}

.financial-table tbody tr:hover {
  background: var(--table-row-alt);
}

.financial-table tbody tr:last-child td {
  border-bottom: none;
}

.financial-table .sticky-col {
  position: sticky;
  left: 0;
  background: var(--surface);
  font-weight: 500;
  color: var(--text-primary);
  z-index: 1;
}

.financial-table .text-right {
  text-align: right;
  font-family: var(--mono);
}

.financial-table .positive {
  color: var(--indicator-positive);
}

.financial-table .negative {
  color: var(--indicator-negative);
}
</style>
```

### 2.3 趋势图表（TrendChart）

```vue
<!-- src/views/financial/components/TrendChart.vue -->

<template>
  <div class="trend-chart-container">
    <div class="chart-header">
      <h3 class="chart-title">{{ title }}</h3>
      <div class="chart-legend">
        <span v-for="series in seriesList" :key="series.key" class="legend-item">
          <span class="legend-dot" :style="{ background: series.color }"></span>
          {{ series.label }}
        </span>
      </div>
    </div>
    <div class="chart-area" ref="chartRef">
      <!-- Chart.js / ECharts 渲染区域 -->
    </div>
  </div>
</template>

<style scoped>
.trend-chart-container {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  padding: var(--fin-card-pad);
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 12px;
}

.chart-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.chart-legend {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--text-secondary);
}

.legend-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.chart-area {
  height: 280px;
}
</style>
```

---

## 三、页面布局规范

### 3.1 财务分析标签页结构

顶层标签页：
```
┌─────────────────────────────────────────────────────────────────┐
│ 股票名称 | 代码        [基本面分析] [财务报表] [AI 解读]         │
├─────────────────────────────────────────────────────────────────┤
```

#### 3.1.1 基本面分析

```
│                                                                 │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │ 核心指标看板（4x3 网格）                                    ││
│  │ ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐                ││
│  │ │ ROE    │ │ 毛利率 │ │ 净利率 │ │ 营收增速│                ││
│  │ │ 12.5%  │ │ 35.2%  │ │ 18.7%  │ │ +16.9% │                ││
│  │ └────────┘ └────────┘ └────────┘ └────────┘                ││
│  │ ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐                ││
│  │ │ PE     │ │ PB     │ │ 股息率  │ │ 资产负债│                ││
│  │ │ 8.5    │ │ 1.2    │ │ 3.2%   │ │ 45.6%  │                ││
│  │ └────────┘ └────────┘ └────────┘ └────────┘                ││
│  └─────────────────────────────────────────────────────────────┘│
│                                                                 │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │ 趋势图表                                                     ││
│  │ [营收 & 净利润趋势] [ROE趋势] [现金流趋势]                  ││
│  └─────────────────────────────────────────────────────────────┘│
│                                                                 │
│  ┌─────────────────────────────┐ ┌─────────────────────────────┐│
│  │ 杜邦分析                     │ │ 同业对比                     ││
│  │ ROE = 净利率 × 周转 × 杠杆   │ │ 行业均值/中位数/排名对比      ││
│  └─────────────────────────────┘ └─────────────────────────────┘│
└─────────────────────────────────────────────────────────────────┘
```

#### 3.1.2 财务报表

内含子标签：利润表 | 资产负债表 | 现金流量表

```
│  [利润表] [资产负债表] [现金流量表]                              │
│                                                                 │
│  ┌────────────────┬────────┬────────┬────────┬────────┐        │
│  │ 指标           │ 2024Y │ 2023Y │ 2022Y │ 2021Y │        │
│  ├────────────────┼────────┼────────┼────────┼────────┤        │
│  │ 营业收入(万)   │1,234,567│1,056,789│ 901,234│ 789,012│        │
│  │ ...            │      ...│      ...│      ...│      ...│        │
│  └────────────────┴────────┴────────┴────────┴────────┘        │
```

#### 3.1.3 AI 解读（待实现）

占位展示，功能开发中。

### 3.2 财务报表标签页结构

```
┌─────────────────────────────────────────────────────────────────┐
│ [利润表] [资产负债表] [现金流量表]                               │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  报告期：[年报 ▼]  单位：[万元 ▼]  [同比] [环比]                 │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │ ┌────────────────┬────────┬────────┬────────┬────────┐       ││
│  │ │ 指标           │ 2024Y │ 2023Y │ 2022Y │ 2021Y │       ││
│  │ ├────────────────┼────────┼────────┼────────┼────────┤       ││
│  │ │ 营业收入(万)   │1,234,567│1,056,789│ 901,234│ 789,012│       ││
│  │ │ 营业成本(万)   │  801,234│  689,012│ 589,234│ 512,345│       ││
│  │ │ 毛利(万)       │  433,333│  367,777│ 312,000│ 276,667│       ││
│  │ │ 毛利率         │   35.1% │   34.8% │  34.6% │  35.1% │       ││
│  │ │ 销售费用(万)   │   56,789│   48,901│  42,345│  38,901│       ││
│  │ │ ...            │      ...│      ...│      ...│      ...│       ││
│  │ └────────────────┴────────┴────────┴────────┴────────┘       ││
│  └─────────────────────────────────────────────────────────────┘│
│                                                                 │
│  [上一页] [下一页]  共 12 期数据                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 四、亮/暗主题适配

```css
/* Light Theme */
[data-theme="light"] {
  --indicator-positive: #00D924;
  --indicator-negative: #FF3B30;
  --chart-revenue: #635BFF;
  --chart-profit: #00D924;
  --table-row-alt: rgba(99, 91, 255, 0.03);
}

/* Dark Theme */
[data-theme="dark"] {
  --indicator-positive: #00E82C;
  --indicator-negative: #FF5C55;
  --chart-revenue: #7C73FF;
  --chart-profit: #00E82C;
  --table-row-alt: rgba(124, 115, 255, 0.08);
  
  /* 深色背景下的卡片 */
  .indicator-card {
    background: var(--surface);
    border-color: var(--border);
  }
  
  .trend-chart-container {
    background: var(--surface);
  }
}
```

---

## 五、组件使用示例

```vue
<!-- 股票详情页中的财务分析模块 -->

<template>
  <div class="financial-analysis">
    <!-- 标签页 -->
    <div class="fin-tabs">
      <button 
        v-for="tab in tabs" 
        :key="tab.id"
        :class="['fin-tab', { active: activeTab === tab.id }]"
        @click="activeTab = tab.id"
      >
        {{ tab.label }}
      </button>
    </div>

    <!-- 核心指标看板 -->
    <div v-if="activeTab === 'overview'" class="fin-overview">
      <div class="indicator-card-grid">
        <IndicatorCard 
          v-for="item in coreIndicators" 
          :key="item.key"
          :label="item.label"
          :value="item.value"
          :meta="item.meta"
        />
      </div>
      
      <TrendChart 
        title="营收 & 净利润趋势"
        :series="revenueProfitSeries"
      />
      
      <div class="fin-comparison-grid">
        <YoYComparison :data="yoyData" />
        <DupontAnalysis :data="dupontData" />
      </div>
    </div>

    <!-- 财务报表 -->
    <div v-if="activeTab === 'statements'" class="fin-statements">
      <div class="fin-tab-sub">
        <button 
          v-for="stmt in statements" 
          :key="stmt.id"
          :class="['fin-tab', { active: activeStatement === stmt.id }]"
          @click="activeStatement = stmt.id"
        >
          {{ stmt.label }}
        </button>
      </div>
      
      <FinancialTable 
        :data="currentStatementData"
        :periods="periods"
      />
    </div>
  </div>
</template>
```

---

## 六、响应式规则

| 断点 | 指标卡片网格 | 财务报表 | 图表 |
|------|-------------|---------|------|
| < 640px | 2列 | 横向滚动 | 高度 200px |
| 640-1024px | 3列 | 横向滚动 | 高度 240px |
| > 1024px | 4列 | 完整展示 | 高度 280px |

```css
@media (max-width: 640px) {
  .indicator-card-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
  }
  
  .fin-comparison-grid {
    grid-template-columns: 1fr;
  }
}

@media (min-width: 1024px) {
  .indicator-card-grid {
    grid-template-columns: repeat(4, 1fr);
  }
  
  .fin-comparison-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
```

---

*本规范基于 Stripe Design System 扩展，专用于证券分析系统的财务报告分析模块。*
