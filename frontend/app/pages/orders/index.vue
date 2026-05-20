<template>
  <div class="space-y-8">
    <div class="flex items-center justify-between">
      <h1 class="text-2xl sm:text-3xl font-extrabold text-slate-900 tracking-tight">我的訂單記錄</h1>
      <NuxtLink to="/" class="text-sm font-semibold text-indigo-600 hover:text-indigo-700">
        繼續購物 &rarr;
      </NuxtLink>
    </div>

    <!-- Loading State -->
    <div v-if="loading" class="space-y-4">
      <div v-for="i in 3" :key="i" class="animate-pulse bg-white border border-slate-100 rounded-2xl h-36 w-full"></div>
    </div>

    <!-- Empty Orders -->
    <div v-else-if="orders.length === 0" class="flex flex-col items-center justify-center py-20 text-center space-y-4 bg-white border border-slate-200/80 rounded-2xl p-8">
      <div class="w-16 h-16 rounded-full bg-slate-50 flex items-center justify-center text-slate-400">
        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-8 h-8">
          <path stroke-linecap="round" stroke-linejoin="round" d="M19.5 14.25v-2.625a3.375 3.375 0 0 0-3.375-3.375h-1.5A1.125 1.125 0 0 1 13.5 7.125v-1.5a3.375 3.375 0 0 0-3.375-3.375H8.25m0 12.75h7.5m-7.5 3H12M10.5 2.25H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 0 0-9-9Z" />
        </svg>
      </div>
      <div class="space-y-1">
        <h3 class="font-bold text-slate-800">目前尚無訂單</h3>
        <p class="text-sm text-slate-400">您還沒有建立過任何購物訂單喔!</p>
      </div>
      <NuxtLink to="/" class="rounded-lg bg-indigo-600 px-6 py-2.5 text-sm font-semibold text-white shadow-md hover:bg-indigo-700 transition-colors">
        去探索商品
      </NuxtLink>
    </div>

    <!-- Orders Card List -->
    <div v-else class="space-y-4">
      <div
        v-for="order in orders"
        :key="order.id"
        class="bg-white border border-slate-200/80 hover:border-slate-300 rounded-2xl p-6 transition-all shadow-sm hover:shadow-md flex flex-col md:flex-row md:items-center md:justify-between gap-6"
      >
        <!-- Info -->
        <div class="space-y-3">
          <div class="flex items-center gap-3">
            <span class="font-bold text-slate-900 text-sm tracking-tight">
              訂單編號 #{{ order.id }}
            </span>
            <span :class="getStatusClass(order.status)" class="inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-semibold border">
              {{ getStatusLabel(order.status) }}
            </span>
          </div>

          <div class="grid grid-cols-2 gap-x-8 gap-y-1 text-xs text-slate-500">
            <div>下單時間:{{ formatDate(order.createdAt) }}</div>
            <div>收件人:{{ order.recipientName }}</div>
            <div class="col-span-2 mt-1">
              商品內容:
              <span class="font-medium text-slate-700">
                {{ getOrderSummaryItems(order) }}
              </span>
            </div>
          </div>
        </div>

        <!-- Price & Button -->
        <div class="flex items-center justify-between md:flex-col md:items-end gap-2 border-t border-slate-100 md:border-t-0 pt-4 md:pt-0 shrink-0">
          <div class="flex items-baseline gap-1">
            <span class="text-xs text-slate-400">總金額</span>
            <span class="text-lg font-extrabold text-indigo-600">
              NT$ {{ formatPrice(order.totalAmount) }}
            </span>
          </div>

          <NuxtLink
            :to="`/orders/${order.id}`"
            class="px-4 py-2 border border-slate-200 hover:border-indigo-600 hover:text-indigo-600 text-slate-700 text-xs font-semibold rounded-lg bg-white transition-all cursor-pointer"
          >
            檢視詳情
          </NuxtLink>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'

const api = useApi()
const orders = ref<any[]>([])
const loading = ref(true)

const fetchOrders = async () => {
  loading.value = true
  try {
    const res = await api.request('/orders', { method: 'GET' })
    if (res.success && Array.isArray(res.data)) {
      orders.value = res.data
    }
  } catch (error) {
    console.error('載入訂單列表出錯:', error)
  } finally {
    loading.value = false
  }
}

// 狀態標籤轉換
const getStatusLabel = (status: string) => {
  const map: Record<string, string> = {
    'PENDING': '待付款',
    'PAID': '已付款',
    'SHIPPED': '已出貨',
    'COMPLETED': '已完成',
    'CANCELLED': '已取消',
  }
  return map[status] || status
}

// 狀態樣式轉換
const getStatusClass = (status: string) => {
  const map: Record<string, string> = {
    'PENDING': 'bg-amber-50 text-amber-700 border-amber-200',
    'PAID': 'bg-blue-50 text-blue-700 border-blue-200',
    'SHIPPED': 'bg-indigo-50 text-indigo-700 border-indigo-200',
    'COMPLETED': 'bg-emerald-50 text-emerald-700 border-emerald-200',
    'CANCELLED': 'bg-rose-50 text-rose-700 border-rose-200',
  }
  return map[status] || 'bg-slate-50 text-slate-700 border-slate-200'
}

// 組合商品文字摘要
const getOrderSummaryItems = (order: any) => {
  if (!order.items || order.items.length === 0) return '無商品資料'
  const firstItem = order.items[0]
  if (order.items.length === 1) {
    return `${firstItem.productName} × ${firstItem.quantity}`
  }
  return `${firstItem.productName} × ${firstItem.quantity} 等 ${order.items.length} 項商品`
}

// 格式化價格
const formatPrice = (price: number) => {
  return new Intl.NumberFormat('zh-TW', { maximumFractionDigits: 0 }).format(price)
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

onMounted(() => {
  fetchOrders()
})
</script>
