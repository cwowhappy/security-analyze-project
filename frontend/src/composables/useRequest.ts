import { ref, computed } from 'vue'

interface UseRequestOptions<T> {
  immediate?: boolean
  onSuccess?: (data: T) => void
  onError?: (error: Error) => void
}

/**
 * 通用异步请求 Composable
 * 封装加载状态、错误处理
 */
export function useRequest<T>(
  requestFn: () => Promise<T>,
  options: UseRequestOptions<T> = {}
) {
  const { immediate = false, onSuccess, onError } = options

  const data = ref<T | null>(null)
  const loading = ref(false)
  const error = ref<Error | null>(null)

  const isReady = computed(() => !loading.value && error.value === null && data.value !== null)

  const execute = async () => {
    loading.value = true
    error.value = null

    try {
      const result = await requestFn()
      data.value = result
      onSuccess?.(result)
      return result
    } catch (err) {
      const e = err instanceof Error ? err : new Error(String(err))
      error.value = e
      onError?.(e)
      throw e
    } finally {
      loading.value = false
    }
  }

  if (immediate) {
    execute()
  }

  return {
    data,
    loading,
    error,
    isReady,
    execute,
  }
}
