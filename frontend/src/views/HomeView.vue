<script setup lang="ts">
import { useRouter } from 'vue-router'
import { ElCard } from 'element-plus'
import { OfficeBuilding, DataLine } from '@element-plus/icons-vue'

const router = useRouter()

function goToCompanies() {
  router.push('/companies')
}

function goToCollectorDashboard() {
  router.push('/dashboard/collector')
}
</script>

<template>
  <div class="home">
    <h1>证券分析系统</h1>
    <div class="card-grid">
      <ElCard class="entry-card" shadow="hover" @click="goToCompanies">
        <div class="card-content">
          <OfficeBuilding class="icon" />
          <div class="title">公司信息</div>
          <div class="desc">查询上市公司基本信息、行业分类、上市资料</div>
        </div>
      </ElCard>
      <ElCard class="entry-card" shadow="hover" @click="goToCollectorDashboard">
        <div class="card-content">
          <DataLine class="icon" />
          <div class="title">采集监控</div>
          <div class="desc">查看数据采集任务执行状态与数据量概览</div>
        </div>
      </ElCard>
    </div>
  </div>
</template>

<style scoped>
.home {
  padding: 40px;
}

.card-grid {
  margin-top: 24px;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}

.entry-card {
  cursor: pointer;
  transition: transform 0.2s;
}

.entry-card:hover {
  transform: translateY(-4px);
}

.card-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: 16px;
}

.icon {
  width: 48px;
  height: 48px;
  color: #409eff;
}

.title {
  margin-top: 12px;
  font-size: 18px;
  font-weight: 600;
}

.desc {
  margin-top: 8px;
  font-size: 14px;
  color: #666;
}
</style>
