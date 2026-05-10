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
  padding: 8px 16px;
  border-radius: 4px;
  border: 1px solid #dcdfe6;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s;
}
.base-button--primary {
  background-color: #409eff;
  color: #fff;
  border-color: #409eff;
}
.base-button--danger {
  background-color: #f56c6c;
  color: #fff;
  border-color: #f56c6c;
}
.base-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>
