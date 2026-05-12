import { ref } from 'vue'
import { defineStore } from 'pinia'
import { collectionTaskApi } from '@/api/modules/collection'
import type { CollectionTask } from '@/types/collection'
import type { PageQuery } from '@/types/api'

export const useCollectionStore = defineStore('collection', () => {
  // State
  const tasks = ref<CollectionTask[]>([])
  const taskTotal = ref(0)
  const currentTask = ref<CollectionTask | null>(null)
  const loading = ref(false)

  // Actions
  const fetchTasks = async (query: PageQuery, status?: string, taskType?: string) => {
    loading.value = true
    try {
      const result = await collectionTaskApi.list(query, status, taskType)
      tasks.value = result.list
      taskTotal.value = result.total
    } finally {
      loading.value = false
    }
  }

  const fetchTaskDetail = async (id: string) => {
    loading.value = true
    try {
      const data = await collectionTaskApi.getById(id)
      currentTask.value = data
    } finally {
      loading.value = false
    }
  }

  const createTask = async (taskType: string, dataSource?: string, taskParams?: Record<string, unknown>) => {
    return collectionTaskApi.create({ taskType, dataSource, taskParams })
  }

  return {
    tasks,
    taskTotal,
    currentTask,
    loading,
    fetchTasks,
    fetchTaskDetail,
    createTask,
  }
})
