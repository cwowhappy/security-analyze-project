<script setup lang="ts">
import type { FinancialTab } from '@/stores/modules/financial'

const props = defineProps<{
  modelValue: FinancialTab
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: FinancialTab): void
}>()

const tabs: { key: FinancialTab; label: string; disabled?: boolean; badge?: string }[] = [
  { key: 'statements', label: '财务报表' },
  { key: 'analysis', label: '基本面分析' },
  { key: 'ai-report', label: 'AI 解读', disabled: true, badge: '敬请期待' },
]

const onClick = (key: FinancialTab, disabled?: boolean) => {
  if (disabled) return
  emit('update:modelValue', key)
}
</script>

<template>
  <div class="tab-nav">
    <button
      v-for="tab in tabs"
      :key="tab.key"
      :class="['tab-btn', { active: props.modelValue === tab.key, disabled: tab.disabled }]"
      @click="onClick(tab.key, tab.disabled)"
    >
      {{ tab.label }}
      <span v-if="tab.badge" class="tab-badge">{{ tab.badge }}</span>
    </button>
  </div>
</template>

<style scoped>
.tab-nav {
  display: flex;
  gap: 4px;
  padding: 4px;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.tab-btn {
  flex: 1;
  min-width: 100px;
  padding: 10px 16px;
  background: transparent;
  border: none;
  border-radius: var(--radius-md);
  font-family: var(--font);
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.15s;
  white-space: nowrap;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.tab-btn:hover:not(.disabled) {
  background: var(--primary-light);
  color: var(--primary);
}

.tab-btn.active {
  background: var(--primary);
  color: #fff;
}

.tab-btn.disabled {
  cursor: not-allowed;
  color: var(--text-muted);
  opacity: 0.7;
}

.tab-badge {
  display: inline-block;
  padding: 1px 6px;
  background: var(--bg);
  color: var(--text-muted);
  font-size: 10px;
  font-weight: 600;
  border-radius: 10px;
  border: 1px solid var(--border);
}

.tab-btn.active .tab-badge {
  background: rgba(255, 255, 255, 0.2);
  color: #fff;
  border-color: rgba(255, 255, 255, 0.3);
}
</style>
