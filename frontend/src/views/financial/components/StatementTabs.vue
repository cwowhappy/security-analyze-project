<script setup lang="ts">
const props = defineProps<{
  modelValue: string
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
}>()

const tabs = [
  { key: 'income', label: '利润表' },
  { key: 'balance', label: '资产负债表' },
  { key: 'cashflow', label: '现金流量表' },
]

const onClick = (key: string) => {
  emit('update:modelValue', key)
}
</script>

<template>
  <div class="sub-tab-nav">
    <button
      v-for="tab in tabs"
      :key="tab.key"
      :class="['sub-tab-btn', { active: props.modelValue === tab.key }]"
      @click="onClick(tab.key)"
    >
      {{ tab.label }}
    </button>
  </div>
</template>

<style scoped>
.sub-tab-nav {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  border-bottom: 1px solid var(--border);
  padding-bottom: 4px;
}

.sub-tab-btn {
  padding: 8px 16px;
  background: transparent;
  border: none;
  border-bottom: 2px solid transparent;
  font-family: var(--font);
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.15s;
  margin-bottom: -6px;
}

.sub-tab-btn:hover {
  color: var(--primary);
}

.sub-tab-btn.active {
  color: var(--primary);
  border-bottom-color: var(--primary);
  font-weight: 600;
}
</style>
