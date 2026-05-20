<template>
  <div class="space-y-6">
    <!-- Header -->
    <div class="flex items-center justify-between border-b border-slate-100 pb-5">
      <div>
        <h1 class="text-2xl sm:text-3xl font-extrabold text-slate-900 tracking-tight">庫存稽核日誌</h1>
        <p class="text-sm text-slate-500">全系統所有商品的庫存異動流水帳,用於盤點與異常追蹤</p>
      </div>

      <NuxtLink to="/admin/products" class="px-4 py-2 border border-slate-200 text-slate-700 hover:border-indigo-600 hover:text-indigo-600 text-sm font-semibold rounded-lg bg-white transition-all cursor-pointer">
        &larr; 返回商品管理
      </NuxtLink>
    </div>

    <!-- Logs Table -->
    <div class="bg-white border border-slate-200/80 rounded-2xl overflow-hidden shadow-sm">
      <el-table :data="logs" style="width: 100%" v-loading="loading">
        <!-- Log ID -->
        <el-table-column prop="id" label="記錄編號" width="90" />

        <!-- Product Name -->
        <el-table-column prop="productName" label="商品名稱" min-width="180" show-overflow-tooltip />

        <!-- Change Amount with Color -->
        <el-table-column label="變動數量" width="120">
          <template #default="{ row }">
            <span
              :class="[
                row.changeAmount > 0 ? 'text-emerald-600 font-bold' : 'text-rose-600 font-bold'
              ]"
            >
              {{ row.changeAmount > 0 ? '+' : '' }}{{ row.changeAmount }}
            </span>
          </template>
        </el-table-column>

        <!-- Reason with tag -->
        <el-table-column label="變動原因" width="140">
          <template #default="{ row }">
            <span :class="getReasonClass(row.reason)" class="inline-flex items-center rounded px-2 py-0.5 text-xs font-medium border">
              {{ getReasonLabel(row.reason) }}
            </span>
          </template>
        </el-table-column>

        <!-- Operator ID -->
        <el-table-column label="操作者 ID" width="120">
          <template #default="{ row }">
            <span class="text-slate-500 font-mono text-xs">{{ row.operatorId || '系統自動' }}</span>
          </template>
        </el-table-column>

        <!-- Log Time -->
        <el-table-column label="異動時間" width="180">
          <template #default="{ row }">
            <span class="text-slate-500 text-xs">{{ formatDate(row.createdAt) }}</span>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useAuthStore } from '~/stores/auth'

const authStore = useAuthStore()
const api = useApi()

const logs = ref<any[]>([])
const loading = ref(false)

// 權限檢查與資料載入
onMounted(() => {
  if (!authStore.isProductManager) {
    notify({
      title: '權限不足',
      message: '您沒有權限訪問庫存異動日誌.',
      type: 'error'
    })
    navigateTo('/')
  } else {
    fetchInventoryLogs()
  }
})

// 載入日誌
const fetchInventoryLogs = async () => {
  loading.value = true
  try {
    const res = await api.request('/products/inventory-logs', { method: 'GET' })
    if (res.success && Array.isArray(res.data)) {
      logs.value = res.data
    }
  } catch (error) {
    console.error('載入庫存日誌出錯:', error)
  } finally {
    loading.value = false
  }
}

// 變動原因翻譯
const getReasonLabel = (reason: string) => {
  const map: Record<string, string> = {
    'ORDER': '訂單扣減',
    'RESTOCK': '進貨補庫',
    'ADJUSTMENT': '手動調整',
    'CANCEL': '取消歸還',
    'RETURN': '退貨入庫'
  }
  return map[reason] || reason
}

// 變動原因標籤顏色
const getReasonClass = (reason: string) => {
  const map: Record<string, string> = {
    'ORDER': 'bg-amber-50 text-amber-700 border-amber-200',
    'RESTOCK': 'bg-emerald-50 text-emerald-700 border-emerald-200',
    'ADJUSTMENT': 'bg-blue-50 text-blue-700 border-blue-200',
    'CANCEL': 'bg-indigo-50 text-indigo-700 border-indigo-200',
    'RETURN': 'bg-rose-50 text-rose-700 border-rose-200',
  }
  return map[reason] || 'bg-slate-50 text-slate-700 border-slate-200'
}

// 格式化日期
const formatDate = (timestamp: number) => {
  if (!timestamp) return '-'
  return new Date(timestamp).toLocaleString('zh-TW', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false
  })
}
</script>
