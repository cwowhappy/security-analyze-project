<script setup lang="ts">
import { ref, watch } from 'vue'

const props = defineProps<{
  modelValue: { startDate: string; endDate: string }
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: { startDate: string; endDate: string }): void
}>()

const startDate = ref(props.modelValue.startDate)
const endDate = ref(props.modelValue.endDate)

watch(() => props.modelValue, (val) => {
  startDate.value = val.startDate
  endDate.value = val.endDate
}, { deep: true })

function emitChange() {
  emit('update:modelValue', { startDate: startDate.value, endDate: endDate.value })
}

const shortcuts = [
  {
    label: '今天',
    value: () => {
      const now = new Date()
      return { start: now, end: now }
    },
  },
  {
    label: '近7天',
    value: () => {
      const end = new Date()
      const start = new Date()
      start.setDate(start.getDate() - 7)
      return { start, end }
    },
  },
  {
    label: '近30天',
    value: () => {
      const end = new Date()
      const start = new Date()
      start.setDate(start.getDate() - 30)
      return { start, end }
    },
  },
]

function applyShortcut(shortcut: typeof shortcuts[0]) {
  const { start, end } = shortcut.value()
  startDate.value = formatDate(start)
  endDate.value = formatDate(end)
  emitChange()
}

function formatDate(date: Date): string {
  return date.toISOString().split('T')[0]
}
</script>

<template>
  <div class="date-range-picker">
    <div class="shortcuts">
      <button
        v-for="s in shortcuts"
        :key="s.label"
        class="shortcut-btn"
        @click="applyShortcut(s)"
      >
        {{ s.label }}
      </button>
    </div>
    <div class="inputs">
      <input
        v-model="startDate"
        type="date"
        class="date-input"
        :max="endDate"
        @change="emitChange"
      />
      <span class="date-separator">至</span>
      <input
        v-model="endDate"
        type="date"
        class="date-input"
        :min="startDate"
        @change="emitChange"
      />
    </div>
  </div>
</template>

<style scoped>
.date-range-picker {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.shortcuts {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.shortcut-btn {
  padding: 4px 10px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: var(--surface);
  color: var(--text-secondary);
  font-size: 12px;
  cursor: pointer;
  transition: all 0.15s;
}

.shortcut-btn:hover {
  border-color: var(--primary);
  color: var(--primary);
}

.inputs {
  display: flex;
  align-items: center;
  gap: 8px;
}

.date-input {
  padding: 6px 10px;
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  background: var(--surface);
  color: var(--text-primary);
  font-size: 13px;
  outline: none;
}

.date-input:focus {
  border-color: var(--primary);
}

.date-separator {
  font-size: 13px;
  color: var(--text-muted);
}
</style>
