import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import { companyApi } from '@/api/modules/company'
import type { Company, CompanyDetail } from '@/types/company'
import type { PageQuery } from '@/types/api'

export const useCompanyStore = defineStore('company', () => {
  // State
  const companies = ref<Company[]>([])
  const companyTotal = ref(0)
  const currentCompany = ref<CompanyDetail | null>(null)
  const loading = ref(false)

  // Getters
  const companyCount = computed(() => companies.value.length)

  // Actions
  const fetchCompanies = async (query: PageQuery, industry?: string, province?: string, keyword?: string, controllerType?: string) => {
    loading.value = true
    try {
      const result = await companyApi.list(query, industry, province, keyword, controllerType)
      companies.value = result.list
      companyTotal.value = result.total
    } finally {
      loading.value = false
    }
  }

  const fetchCompanyDetail = async (uscCode: string) => {
    loading.value = true
    try {
      const data = await companyApi.getByUscCode(uscCode)
      currentCompany.value = data
    } finally {
      loading.value = false
    }
  }

  return {
    companies,
    companyTotal,
    currentCompany,
    loading,
    companyCount,
    fetchCompanies,
    fetchCompanyDetail,
  }
})
