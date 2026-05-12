import { describe, it, expect, vi } from 'vitest'
import { useRequest } from '../useRequest'

describe('useRequest', () => {
  it('should set loading to true during request', async () => {
    const { loading, execute } = useRequest(() => Promise.resolve('ok'))
    expect(loading.value).toBe(false)

    const promise = execute()
    expect(loading.value).toBe(true)

    await promise
    expect(loading.value).toBe(false)
  })

  it('should return data on success', async () => {
    const { data, execute } = useRequest(() => Promise.resolve('hello'))
    await execute()
    expect(data.value).toBe('hello')
  })

  it('should set error on failure', async () => {
    const { error, execute } = useRequest(() => Promise.reject(new Error('fail')))
    await expect(execute()).rejects.toThrow('fail')
    expect(error.value).toBeInstanceOf(Error)
  })

  it('should call onSuccess callback when provided', async () => {
    const onSuccess = vi.fn()
    const { execute } = useRequest(() => Promise.resolve('data'), { onSuccess })
    await execute()
    expect(onSuccess).toHaveBeenCalledWith('data')
  })

  it('should call onError callback when provided', async () => {
    const onError = vi.fn()
    const err = new Error('boom')
    const { execute } = useRequest(() => Promise.reject(err), { onError })
    await expect(execute()).rejects.toThrow('boom')
    expect(onError).toHaveBeenCalledWith(err)
  })

  it('should execute immediately when immediate is true', async () => {
    const { data, loading } = useRequest(() => Promise.resolve('immediate'), { immediate: true })
    expect(loading.value).toBe(true)
    await new Promise((r) => setTimeout(r, 10))
    expect(data.value).toBe('immediate')
    expect(loading.value).toBe(false)
  })

  it('should compute isReady correctly', async () => {
    const { isReady, execute } = useRequest(() => Promise.resolve('ready'))
    expect(isReady.value).toBe(false)

    const promise = execute()
    expect(isReady.value).toBe(false)

    await promise
    expect(isReady.value).toBe(true)
  })

  it('should reset error before new execution', async () => {
    let shouldFail = true
    const { error, execute } = useRequest(() => {
      if (shouldFail) {
        return Promise.reject(new Error('fail'))
      }
      return Promise.resolve('ok')
    })

    await expect(execute()).rejects.toThrow('fail')
    expect(error.value).not.toBeNull()

    shouldFail = false
    await execute()
    expect(error.value).toBeNull()
  })

  it('should wrap non-Error rejections into Error', async () => {
    const { error, execute } = useRequest(() => Promise.reject('string-error'))
    await expect(execute()).rejects.toThrow('string-error')
    expect(error.value).toBeInstanceOf(Error)
    expect(error.value?.message).toBe('string-error')
  })
})
