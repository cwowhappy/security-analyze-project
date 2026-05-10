<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { ElMessage, ElCollapse, ElCollapseItem, ElEmpty } from 'element-plus'
import { getIndustryPeers, getIndustryRank } from '@/api/research'
import type { PeerMetric, IndustryRankItem } from '@/types/research'
import IndustryPeersTable from '@/views/research/components/IndustryPeersTable.vue'
import IndustryRankTable from '@/views/research/components/IndustryRankTable.vue'

const props = defineProps<{
  stockCode: string
}>()

const emit = defineEmits<{
  select: [stockCode: string]
}>()

const loading = ref(false)
const peers = ref<PeerMetric[]>([])
const rankData = ref<IndustryRankItem[]>([])
const rankSortBy = ref('roe')
const rankOrder = ref('desc')
const activeCollapse = ref<string[]>([])

async function fetchData() {
  loading.value = true
  try {
    const [peersRes, rankRes] = await Promise.all([
      getIndustryPeers(props.stockCode),
      getIndustryRank(props.stockCode),
    ])
    peers.value = peersRes.peers
    rankData.value = rankRes.items
    rankSortBy.value = rankRes.sortBy || 'roe'
    rankOrder.value = rankRes.order || 'desc'
  } catch (err) {
    ElMessage.error('加载同行对比数据失败')
    peers.value = []
    rankData.value = []
  } finally {
    loading.value = false
  }
}

async function onRankSort(field: string) {
  const nextOrder = rankSortBy.value === field && rankOrder.value === 'desc' ? 'asc' : 'desc'
  rankSortBy.value = field
  rankOrder.value = nextOrder
  try {
    const res = await getIndustryRank(props.stockCode, field, nextOrder)
    rankData.value = res.items
  } catch (err) {
    ElMessage.error('排序加载失败')
  }
}

function onSelect(stockCode: string) {
  emit('select', stockCode)
}

onMounted(fetchData)
watch(() => props.stockCode, fetchData)
</script>

<template>
  <div v-loading="loading">
    <template v-if="peers.length > 0 || rankData.length > 0">
      <ElCollapse v-model="activeCollapse">
        <ElCollapseItem title="同行业公司速览" name="peers">
          <IndustryPeersTable :peers="peers" @select="onSelect" />
        </ElCollapseItem>
        <ElCollapseItem title="行业排名" name="rank">
          <IndustryRankTable
            :data="rankData"
            :current-stock-code="props.stockCode"
            :sort-by="rankSortBy"
            :order="rankOrder"
            @select="onSelect"
            @sort="onRankSort"
          />
        </ElCollapseItem>
      </ElCollapse>
    </template>
    <ElEmpty v-else description="暂无同行对比数据" />
  </div>
</template>
