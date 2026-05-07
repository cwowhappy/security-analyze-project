<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ElCard } from 'element-plus'
import {
  OfficeBuilding,
  DataLine,
  Grid,
  TrendCharts,
  Wallet,
  UserFilled,
} from '@element-plus/icons-vue'

const router = useRouter()
const authStore = useAuthStore()

function goToCompanies() {
  router.push('/companies')
}

function goToCollectorDashboard() {
  router.push('/dashboard/collector')
}

function goToIndustries() {
  router.push('/industries')
}

function goToIndexes() {
  router.push('/indexes')
}

function goToPortfolios() {
  router.push('/portfolios')
}

function goToAdminUsers() {
  router.push('/admin/users')
}
</script>

<template>
  <div class="home">
    <h1 class="page-title">欢迎使用证券分析与投资系统</h1>

    <!-- 第一部分：证券分析板块 -->
    <section class="section">
      <div class="section-header">
        <div class="section-title">证券分析</div>
        <div class="section-desc">市场数据查询与分析工具</div>
      </div>
      <div class="card-grid">
        <ElCard class="entry-card" shadow="hover" @click="goToIndexes">
          <div class="card-content">
            <TrendCharts class="icon" />
            <div class="title">指数信息</div>
            <div class="desc">查看指数基本信息、趋势分析、关联ETF</div>
          </div>
        </ElCard>
        <ElCard class="entry-card" shadow="hover" @click="goToIndustries">
          <div class="card-content">
            <Grid class="icon" />
            <div class="title">行业信息</div>
            <div class="desc">按行业分类浏览上市公司，查看行业指数走势</div>
          </div>
        </ElCard>
        <ElCard class="entry-card" shadow="hover" @click="goToCompanies">
          <div class="card-content">
            <OfficeBuilding class="icon" />
            <div class="title">公司信息</div>
            <div class="desc">查询上市公司基本信息、行业分类、上市资料</div>
          </div>
        </ElCard>
      </div>
    </section>

    <!-- 第二部分：个人持仓 -->
    <section class="section">
      <div class="section-header">
        <div class="section-title">个人持仓</div>
        <div class="section-desc">管理投资组合与交易记录</div>
      </div>
      <div class="card-grid single">
        <ElCard class="entry-card highlight" shadow="hover" @click="goToPortfolios">
          <div class="card-content">
            <Wallet class="icon" />
            <div class="title">持仓管理</div>
            <div class="desc">创建投资组合，记录交易流水，实时跟踪持仓盈亏</div>
          </div>
        </ElCard>
      </div>
    </section>

    <!-- 第三部分：辅助功能 -->
    <section class="section">
      <div class="section-header">
        <div class="section-title">辅助功能</div>
        <div class="section-desc">系统运维与管理工具</div>
      </div>
      <div class="card-grid">
        <ElCard class="entry-card" shadow="hover" @click="goToCollectorDashboard">
          <div class="card-content">
            <DataLine class="icon" />
            <div class="title">采集监控</div>
            <div class="desc">查看数据采集任务执行状态与数据量概览</div>
          </div>
        </ElCard>
        <ElCard
          v-if="authStore.isAdmin"
          class="entry-card"
          shadow="hover"
          @click="goToAdminUsers"
        >
          <div class="card-content">
            <UserFilled class="icon" />
            <div class="title">用户管理</div>
            <div class="desc">管理系统用户注册审批、状态与权限</div>
          </div>
        </ElCard>
      </div>
    </section>
  </div>
</template>

<style scoped>
.home {
  padding: 8px;
}
.page-title {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 32px;
  letter-spacing: 0.5px;
}
.section {
  margin-bottom: 32px;
}
.section-header {
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--border-color);
}
.section-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 4px;
}
.section-desc {
  font-size: 13px;
  color: var(--text-secondary);
}
.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}
.card-grid.single {
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  max-width: 320px;
}
.entry-card {
  cursor: pointer;
  transition: all 0.3s ease;
  border: 1px solid var(--border-color);
  background: var(--bg-card);
}
.entry-card:hover {
  transform: translateY(-4px);
  border-color: var(--accent-primary);
  box-shadow: var(--shadow-glow);
}
.entry-card.highlight {
  border-color: var(--accent-primary-dim);
  background: linear-gradient(135deg, var(--bg-card) 0%, var(--bg-card-solid) 100%);
}
.entry-card.highlight:hover {
  border-color: var(--accent-primary);
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
  color: var(--accent-primary);
}
.title {
  margin-top: 12px;
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
}
.desc {
  margin-top: 8px;
  font-size: 14px;
  color: var(--text-secondary);
}
</style>
