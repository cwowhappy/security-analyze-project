import { describe, it, expect } from 'vitest'
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
})
