<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { companyApi } from '@/api/modules/company'
import type { Company } from '@/types/company'

const router = useRouter()

const page = ref(1)
const size = ref(20)
const keyword = ref('')
const industry = ref('')
const province = ref('')
const controllerType = ref('')

const companies = ref<Company[]>([])
const total = ref(0)
const loading = ref(false)

const controllerTypes = [
  { label: '国企', value: '国企' },
  { label: '民营', value: '民营' },
  { label: '外资', value: '外资' },
  { label: '其他', value: '其他' },
]

async function fetchData() {
  loading.value = true
  try {
    const result = await companyApi.list(
      { page: page.value, size: size.value },
      industry.value || undefined,
      province.value || undefined,
      keyword.value || undefined,
      controllerType.value || undefined,
    )
    companies.value = result.list ?? []
    total.value = result.total ?? 0
  } finally {
    loading.value = false
  }
}

function goDetail(uscCode: string) {
  router.push(`/companies/${uscCode}`)
}

function prevPage() {
  if (page.value > 1) {
    page.value--
    fetchData()
  }
}

function nextPage() {
  if (companies.value.length === size.value) {
    page.value++
    fetchData()
  }
}

function resetFilters() {
  page.value = 1
  keyword.value = ''
  industry.value = ''
  province.value = ''
  controllerType.value = ''
  fetchData()
}

function fmt(n: number | null) {
  if (!n) return '-'
  return n.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ',')
}

function ctBadgeClass(t: string | null) {
  const map: Record<string, string> = { '国企': 'bs-gq', '民营': 'bs-my', '外资': 'bs-wz', '其他': 'bs-qt' }
  return map[t || '其他'] || 'bs-qt'
}

onMounted(fetchData)
</script>

<template>
  <div>
    <div class="pg-hd">
      <h1 class="pg-t">公司列表</h1>
      <p class="pg-d">上市公司工商信息、管理层与主营业务信息</p>
    </div>

    <div class="bar">
      <div class="sb">
        <input v-model="keyword" placeholder="搜索公司名称或信用代码..." @keyup.enter="page = 1; fetchData()" />
      </div>
      <div class="fg">
        <label>行业</label>
        <input v-model="industry" placeholder="行业筛选" @keyup.enter="page = 1; fetchData()" style="min-width:120px" />
      </div>
      <div class="fg">
        <label>省份</label>
        <input v-model="province" placeholder="省份筛选" @keyup.enter="page = 1; fetchData()" style="min-width:120px" />
      </div>
      <div class="fg">
        <label>企业性质</label>
        <select v-model="controllerType" @change="page = 1; fetchData()">
          <option value="">全部</option>
          <option v-for="t in controllerTypes" :key="t.value" :value="t.value">{{ t.label }}</option>
        </select>
      </div>
      <button class="btn btn-s" @click="resetFilters">重置</button>
    </div>

    <div class="tc">
      <div v-if="loading" style="text-align:center;padding:40px;color:var(--text-muted)">加载中...</div>
      <table v-else>
        <thead>
          <tr>
            <th>公司全称</th>
            <th>简称</th>
            <th>行业</th>
            <th>省份</th>
            <th>城市</th>
            <th>企业性质</th>
            <th>法人代表</th>
            <th class="nr">员工人数</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="company in companies" :key="company.id" @click="goDetail(company.unifiedSocialCreditCode)" class="cursor-row">
            <td>{{ company.name }}</td>
            <td>{{ company.shortName || '-' }}</td>
            <td>{{ company.industry || '-' }}</td>
            <td>{{ company.province || '-' }}</td>
            <td>{{ company.city || '-' }}</td>
            <td><span class="bs" :class="ctBadgeClass(company.controllerType)">{{ company.controllerType || '其他' }}</span></td>
            <td>{{ company.legalRepresentative || '-' }}</td>
            <td class="nr">{{ fmt(company.employees) }}</td>
            <td><button class="lb" @click.stop="goDetail(company.unifiedSocialCreditCode)">详情</button></td>
          </tr>
          <tr v-if="!companies.length">
            <td colspan="9" style="text-align:center;padding:40px;color:var(--text-muted)">无匹配结果</td>
          </tr>
        </tbody>
      </table>
    </div>

    <div class="pagination" v-if="companies.length">
      <button :disabled="page === 1" @click="prevPage">上一页</button>
      <span>第 {{ page }} 页 / 共 {{ Math.ceil(total / size) || 1 }} 页</span>
      <button :disabled="companies.length < size" @click="nextPage">下一页</button>
    </div>
  </div>
</template>

<style scoped>
.pg-hd {
  margin-bottom: 24px;
}

.pg-t {
  font-size: 22px;
  font-weight: 700;
  margin-bottom: 4px;
  color: var(--text-primary);
}

.pg-d {
  font-size: 13px;
  color: var(--text-secondary);
}

.bar {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  padding: 14px 18px;
  margin-bottom: 18px;
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  box-shadow: var(--shadow-sm);
}

.sb {
  position: relative;
  flex: 1;
  min-width: 180px;
}

.sb input {
  width: 100%;
  padding: 8px 12px 8px 36px;
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  font-size: 13px;
  font-family: var(--font);
  color: var(--text-primary);
  background: var(--bg);
  transition: border-color 0.15s, background 0.25s;
}

.sb input:focus {
  outline: none;
  border-color: var(--primary);
  box-shadow: 0 0 0 3px rgba(99, 91, 255, 0.1);
}

.sb::before {
  content: '';
  position: absolute;
  left: 10px;
  top: 50%;
  transform: translateY(-50%);
  width: 14px;
  height: 14px;
  background: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' fill='%236B7A8D' viewBox='0 0 24 24'%3E%3Cpath d='M21 21l-4.35-4.35A7.5 7.5 0 1 0 16.65 4.35L21 8.65 21 21zM10 4a6 6 0 1 1-6 6 6 6 0 0 1 6-6z'/%3E%3C/svg%3E") center/contain no-repeat;
  opacity: 0.6;
}

.fg {
  display: flex;
  gap: 6px;
  align-items: center;
}

.fg label {
  font-size: 12px;
  color: var(--text-muted);
  font-weight: 500;
  white-space: nowrap;
}

select, .fg input {
  padding: 8px 30px 8px 10px;
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  font-size: 13px;
  font-family: var(--font);
  color: var(--text-primary);
  background: var(--surface);
  cursor: pointer;
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12'%3E%3Cpath d='M6 8L1 3h10z' fill='%236B7A8D'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 8px center;
  transition: background 0.25s;
}

select:focus, .fg input:focus {
  outline: none;
  border-color: var(--primary);
  box-shadow: 0 0 0 3px rgba(99, 91, 255, 0.1);
}

.btn {
  padding: 8px 16px;
  border-radius: var(--radius-md);
  font-size: 13px;
  font-weight: 600;
  font-family: var(--font);
  cursor: pointer;
  transition: all 0.15s;
  border: 1px solid transparent;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: var(--primary);
  color: #fff;
}

.btn:hover {
  background: var(--primary-hover);
}

.btn-s {
  background: var(--surface);
  color: var(--text-primary);
  border-color: var(--border);
}

.btn-s:hover {
  background: var(--surface-hover);
}

.tc {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  overflow: hidden;
  box-shadow: var(--shadow-sm);
}

table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

thead {
  background: var(--bg);
  border-bottom: 1px solid var(--border);
}

th {
  padding: 11px 16px;
  text-align: left;
  font-weight: 600;
  font-size: 11px;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  white-space: nowrap;
}

td {
  padding: 11px 16px;
  border-bottom: 1px solid var(--border);
  vertical-align: middle;
}

tbody tr {
  transition: background 0.1s;
}

tbody tr:hover {
  background: var(--surface-hover);
}

tbody tr:last-child td {
  border-bottom: none;
}

.nr {
  text-align: right;
  font-family: var(--mono);
  font-size: 12px;
}

th.nr {
  text-align: right;
}

.bs {
  display: inline-block;
  padding: 3px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
}

.bs-gq {
  background: rgba(0, 120, 220, 0.12);
  color: #0078DC;
}

[data-theme="dark"] .bs-gq {
  background: rgba(0, 120, 220, 0.2);
  color: #5BABFF;
}

.bs-my {
  background: rgba(0, 180, 80, 0.12);
  color: #00B450;
}

[data-theme="dark"] .bs-my {
  background: rgba(0, 180, 80, 0.2);
  color: #4DE080;
}

.bs-wz {
  background: rgba(220, 100, 0, 0.12);
  color: #DC6400;
}

[data-theme="dark"] .bs-wz {
  background: rgba(220, 100, 0, 0.2);
  color: #FF9F4A;
}

.bs-qt {
  background: var(--surface-hover);
  color: var(--text-secondary);
}

.lb {
  color: var(--primary);
  background: none;
  border: none;
  cursor: pointer;
  font-size: 13px;
  font-weight: 600;
  font-family: var(--font);
  padding: 4px 8px;
  border-radius: 4px;
  transition: background 0.1s;
}

.lb:hover {
  background: rgba(99, 91, 255, 0.1);
}

.cursor-row {
  cursor: pointer;
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 6px;
  margin-top: 16px;
}

.pagination button {
  padding: 6px 14px;
  border: 1px solid var(--border);
  background: var(--surface);
  color: var(--text-secondary);
  border-radius: var(--radius-md);
  cursor: pointer;
  font-size: 13px;
  transition: all 0.15s;
}

.pagination button:hover:not(:disabled) {
  border-color: var(--border);
  color: var(--text-primary);
  background: var(--surface-hover);
}

.pagination button:disabled {
  opacity: 0.3;
  cursor: not-allowed;
}

.pagination span {
  color: var(--text-muted);
  font-size: 13px;
  padding: 0 8px;
}
</style>
