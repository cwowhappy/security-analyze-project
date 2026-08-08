<template>
  <div>
    <h1>仪表盘</h1>
    <el-row :gutter="16">
      <el-col :span="8">
        <el-statistic title="今日事件" :value="128" />
      </el-col>
      <el-col :span="8">
        <el-statistic title="高危事件" :value="12" />
      </el-col>
      <el-col :span="8">
        <el-statistic title="采集任务" :value="5" />
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
</script>
