<template>
  <div>
    <h1>安全事件</h1>
    <el-table :data="events" style="width: 100%">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="title" label="标题" />
      <el-table-column prop="severity" label="严重等级" width="120" />
      <el-table-column prop="source" label="来源" width="180" />
      <el-table-column prop="createdAt" label="时间" width="180" />
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

interface SecurityEvent {
  id: number
  title: string
  severity: string
  source: string
  createdAt: string
}

const events = ref<SecurityEvent[]>([
  { id: 1, title: 'SQL 注入尝试', severity: 'HIGH', source: 'WAF', createdAt: '2026-08-07 10:00:00' },
  { id: 2, title: '异常登录行为', severity: 'MEDIUM', source: 'SIEM', createdAt: '2026-08-07 09:30:00' }
])
</script>
