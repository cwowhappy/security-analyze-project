<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useCompanyStore } from '@/stores/modules/company'

const companyStore = useCompanyStore()
const router = useRouter()

const page = ref(1)
const size = ref(20)
const industry = ref('')
const province = ref('')
const keyword = ref('')

const fetchData = () => {
  companyStore.fetchCompanies(
    { page: page.value, size: size.value },
    industry.value || undefined,
    province.value || undefined,
    keyword.value || undefined,
  )
}

const goDetail = (uscCode: string) => {
  router.push(`/companies/${uscCode}`)
}

const prevPage = () => {
  if (page.value > 1) {
    page.value--
    fetchData()
  }
}

const nextPage = () => {
  if (companyStore.companies.length === size.value) {
    page.value++
    fetchData()
  }
}

onMounted(fetchData)
</script>

<template>
  <div>
    <div class="card">
      <div class="card-title">公司列表</div>
      <div class="filter-bar">
        <input v-model="keyword" placeholder="公司名称关键词" @keyup.enter="page = 1; fetchData()" />
        <input v-model="industry" placeholder="行业筛选" @keyup.enter="page = 1; fetchData()" />
        <input v-model="province" placeholder="省份筛选" @keyup.enter="page = 1; fetchData()" />
        <button class="btn btn-primary btn-sm" @click="page = 1; fetchData()">筛选</button>
      </div>

      <div v-if="companyStore.loading" class="loading-pulse">加载中...</div>

      <template v-else>
        <table class="table">
          <thead>
            <tr>
              <th>名称</th>
              <th>行业</th>
              <th>省份</th>
              <th>城市</th>
              <th class="text-right">成立日期</th>
              <th class="text-right">注册资本</th>
              <th class="text-right">员工人数</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="company in companyStore.companies"
              :key="company.id"
              @click="goDetail(company.unifiedSocialCreditCode)"
              class="cursor-row"
            >
              <td>
                <a @click.stop="goDetail(company.unifiedSocialCreditCode)">{{ company.name }}</a>
              </td>
              <td>{{ company.industry || '-' }}</td>
              <td>{{ company.province || '-' }}</td>
              <td>{{ company.city || '-' }}</td>
              <td class="text-right font-mono">{{ company.setupDate || '-' }}</td>
              <td class="text-right font-mono">{{ company.regCapital || '-' }}</td>
              <td class="text-right font-mono">{{ company.employees || '-' }}</td>
            </tr>
            <tr v-if="!companyStore.companies.length">
              <td colspan="7" class="empty-state">暂无数据</td>
            </tr>
          </tbody>
        </table>

        <div class="pagination" v-if="companyStore.companies.length">
          <button :disabled="page === 1" @click="prevPage">上一页</button>
          <span>第 {{ page }} 页 / 共 {{ Math.ceil(companyStore.companyTotal / size) || 1 }} 页</span>
          <button :disabled="companyStore.companies.length < size" @click="nextPage">下一页</button>
        </div>
      </template>
    </div>
  </div>
</template>

<style scoped>
.cursor-row {
  cursor: pointer;
}

.text-right {
  text-align: right;
}
</style>
