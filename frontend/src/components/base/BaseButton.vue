<script setup lang="ts">
interface Props {
  type?: 'primary' | 'default' | 'danger'
  disabled?: boolean
}

withDefaults(defineProps<Props>(), {
  type: 'default',
  disabled: false,
})

const emit = defineEmits<{
  click: [event: MouseEvent]
}>()

const handleClick = (e: MouseEvent) => {
  emit('click', e)
}
</script>

<template>
  <button
    class="base-button"
    :class="`base-button--${type}`"
    :disabled="disabled"
    @click="handleClick"
  >
    <slot />
  </button>
</template>

<style scoped>
.base-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 7px 16px;
  border-radius: var(--radius-sm);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all var(--transition-fast);
  white-space: nowrap;
  letter-spacing: 0.2px;
  border: 1px solid transparent;
}

.base-button--primary {
  background: var(--primary);
  color: #fff;
  border-color: var(--primary);
}

.base-button--primary:hover {
  background: var(--primary-dim);
  border-color: var(--primary-dim);
}

.base-button--default {
  background: transparent;
  color: var(--text-secondary);
  border-color: var(--border-default);
}

.base-button--default:hover {
  color: var(--text-primary);
  border-color: var(--border-strong);
  background: rgba(255, 255, 255, 0.03);
}

.base-button--danger {
  background: var(--red);
  color: #fff;
  border-color: var(--red);
}

.base-button--danger:hover {
  background: #c0392b;
  border-color: #c0392b;
}

.base-button:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
</style>
