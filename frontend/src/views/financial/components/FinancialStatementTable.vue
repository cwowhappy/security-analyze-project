<script setup lang="ts">
export interface TableRow {
  label: string
  values: string[]
  emphasis?: boolean
}

const props = defineProps<{
  rows: TableRow[]
  unit?: string
}>()

function getRowClass(row: TableRow): string {
  return row.emphasis ? 'data-row strong' : 'data-row'
}
</script>

<template>
  <div class="table-section">
    <div v-if="unit" class="table-unit-hint">单位：{{ unit }}</div>
    <div class="table-wrapper">
      <table class="financial-table">
        <thead>
          <tr>
            <th class="row-label">项目</th>
            <th v-for="(val, i) in rows[0]?.values" :key="i" class="col-header">{{ val }}</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="(row, idx) in rows.slice(1)"
            :key="idx"
            :class="getRowClass(row)"
          >
            <td class="row-label">{{ row.label }}</td>
            <td v-for="(val, i) in row.values" :key="i" class="data-cell">{{ val }}</td>
          </tr>
        </tbody>
      </table>
      <div v-if="rows.length <= 1" class="empty-state">
        暂无数据
      </div>
    </div>
  </div>
</template>

<style scoped>
.table-section {
  position: relative;
}

.table-unit-hint {
  text-align: right;
  font-size: 12px;
  color: var(--text-muted);
  margin-bottom: 8px;
  font-family: var(--font);
}

.table-wrapper {
  overflow-x: auto;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
}

.financial-table {
  width: 100%;
  border-collapse: collapse;
  font-family: var(--font);
  font-size: 13px;
}

.financial-table th,
.financial-table td {
  padding: 10px 14px;
  border-bottom: 1px solid var(--border);
  white-space: nowrap;
}

.financial-table th {
  background: var(--surface-variant);
  font-weight: 600;
  font-size: 13px;
  color: var(--text-secondary);
  text-align: right;
  position: sticky;
  top: 0;
  z-index: 2;
}

.financial-table .row-label {
  text-align: left;
  font-weight: 500;
  font-family: var(--font);
  color: var(--text-primary);
  position: sticky;
  left: 0;
  background: var(--surface);
  z-index: 3;
  min-width: 140px;
}

.financial-table .col-header:first-child {
  text-align: left;
}

.financial-table th:first-child {
  text-align: left;
  z-index: 4;
}

.financial-table .data-cell {
  text-align: right;
  font-family: var(--mono);
  font-size: 13px;
  color: var(--text-primary);
}

.financial-table .data-row:hover {
  background: var(--surface-variant);
}

.financial-table .data-row.strong .row-label {
  font-weight: 700;
  color: var(--text-primary);
}

.financial-table .data-row.strong .data-cell {
  font-weight: 600;
}

.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px 20px;
  color: var(--text-muted);
  font-size: 13px;
  font-family: var(--font);
}
</style>
