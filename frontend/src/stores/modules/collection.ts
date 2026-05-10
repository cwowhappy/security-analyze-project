import { ref } from 'vue'
import { defineStore } from 'pinia'
import { collectionTaskApi, collectionScheduleApi } from '@/api/modules/collection'
import type { CollectionTask, CollectionTaskSchedule } from '@/types/collection'
import type { PageQuery } from '@/types/api'

export const useCollectionStore = defineStore('collection', () => {
  // State
  const tasks = ref<CollectionTask[]>([])
  const taskTotal = ref(0)
  const currentTask = ref<CollectionTask | null>(null)
  const schedules = ref<CollectionTaskSchedule[]>([])
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

  const fetchSchedules = async () => {
    loading.value = true
    try {
      const data = await collectionScheduleApi.list()
      schedules.value = data
    } finally {
      loading.value = false
    }
  }

  const createSchedule = async (data: {
    name: string
    taskType: string
    cronExpression: string
    dataSource?: string
    taskParams?: Record<string, unknown>
  }) => {
    return collectionScheduleApi.create(data)
  }

  const updateSchedule = async (id: string, data: {
    name: string
    taskType: string
    cronExpression: string
    dataSource?: string
    taskParams?: Record<string, unknown>
    isEnabled?: boolean
  }) => {
    return collectionScheduleApi.update(id, data)
  }

  const deleteSchedule = async (id: string) => {
    return collectionScheduleApi.delete(id)
  }

  return {
    tasks,
    taskTotal,
    currentTask,
    schedules,
    loading,
    fetchTasks,
    fetchTaskDetail,
    createTask,
    fetchSchedules,
    createSchedule,
    updateSchedule,
    deleteSchedule,
  }
})
