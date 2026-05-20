<template>
  <div class="space-y-8">
    <!-- Back Link -->
    <NuxtLink to="/orders" class="inline-flex items-center gap-1.5 text-sm font-medium text-slate-500 hover:text-indigo-600 transition-colors">
      <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="w-4 h-4">
        <path stroke-linecap="round" stroke-linejoin="round" d="M10.5 19.5 3 12m0 0 7.5-7.5M3 12h18" />
      </svg>
      返回訂單列表
    </NuxtLink>

    <!-- Loading Skeleton -->
    <div v-if="loading" class="space-y-6 animate-pulse">
      <div class="h-10 bg-slate-100 rounded w-1/2"></div>
      <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div class="lg:col-span-2 bg-slate-100 rounded-2xl h-64"></div>
        <div class="bg-slate-100 rounded-2xl h-64"></div>
      </div>
    </div>

    <!-- 404 / Access Denied -->
    <div v-else-if="!order" class="flex flex-col items-center justify-center py-24 text-center space-y-4">
      <div class="w-16 h-16 rounded-full bg-slate-100 flex items-center justify-center text-slate-400">
        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-8 h-8">
          <path stroke-linecap="round" stroke-linejoin="round" d="M12 9v3.75m9-.75a9 9 0 1 1-18 0 9 9 0 0 1 18 0Zm-9 3.75h.008v.008H12v-.008Z" />
        </svg>
      </div>
      <div class="space-y-1">
        <h3 class="font-bold text-slate-800">找不到此訂單</h3>
        <p class="text-sm text-slate-400">{{ errorMessage || '訂單不存在,已取消,或您無權查看此訂單.' }}</p>
      </div>
      <NuxtLink to="/orders" class="rounded-lg bg-indigo-600 px-6 py-2.5 text-sm font-semibold text-white shadow-md hover:bg-indigo-700 transition-colors">
        返回訂單列表
      </NuxtLink>
    </div>

    <!-- Order Detail -->
    <div v-else class="space-y-6">
      <!-- Header -->
      <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div class="space-y-1">
          <div class="flex items-center gap-3">
            <h1 class="text-2xl sm:text-3xl font-extrabold tracking-tight text-slate-900">
              訂單 #{{ order.id }}
            </h1>
            <span :class="getStatusClass(order.status)" class="inline-flex items-center rounded-full px-3 py-1 text-sm font-semibold border">
              {{ getStatusLabel(order.status) }}
            </span>
          </div>
          <p class="text-sm text-slate-500">
            下單時間:{{ formatDate(order.createdAt) }}
          </p>
        </div>

        <!-- Total Amount (prominent) -->
        <div class="text-right">
          <p class="text-xs text-slate-400 mb-1">訂單總金額</p>
          <p class="text-2xl font-extrabold text-indigo-600">
            NT$ {{ formatPrice(order.totalAmount) }}
          </p>
        </div>
      </div>

      <!-- Status Timeline -->
      <div class="bg-white border border-slate-200/80 rounded-2xl p-6 shadow-sm">
        <h2 class="text-sm font-semibold text-slate-500 uppercase tracking-wider mb-4">訂單進度</h2>
        <div class="flex items-center gap-0">
          <template v-for="(step, idx) in statusSteps" :key="step.key">
            <div class="flex flex-col items-center flex-1 min-w-0">
              <div
                class="w-8 h-8 rounded-full border-2 flex items-center justify-center shrink-0 transition-all"
                :class="isStepReached(step.key) ? 'bg-indigo-600 border-indigo-600 text-white' : 'bg-white border-slate-200 text-slate-300'"
              >
                <svg v-if="isStepReached(step.key)" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.5" stroke="currentColor" class="w-4 h-4">
                  <path stroke-linecap="round" stroke-linejoin="round" d="m4.5 12.75 6 6 9-13.5" />
                </svg>
                <span v-else class="w-2 h-2 rounded-full bg-slate-200"></span>
              </div>
              <span class="mt-2 text-xs font-medium text-center px-1 leading-tight"
                :class="isStepReached(step.key) ? 'text-indigo-600' : 'text-slate-400'"
              >
                {{ step.label }}
              </span>
            </div>
            <!-- Connector Line -->
            <div
              v-if="idx < statusSteps.length - 1"
              class="h-0.5 flex-1 mb-4 -mx-1 transition-all"
              :class="isStepReached(statusSteps[idx + 1]?.key ?? '') ? 'bg-indigo-500' : 'bg-slate-200'"
            ></div>
          </template>
        </div>
      </div>

      <div class="grid grid-cols-1 lg:grid-cols-3 gap-6 items-start">

        <!-- Left: Order Items -->
        <div class="lg:col-span-2 bg-white border border-slate-200/80 rounded-2xl overflow-hidden shadow-sm">
          <div class="px-6 py-4 border-b border-slate-100">
            <h2 class="font-bold text-slate-900">訂購商品明細</h2>
          </div>

          <div class="divide-y divide-slate-50">
            <div
              v-for="item in order.items"
              :key="item.productId"
              class="flex items-center gap-4 px-6 py-4"
            >
              <!-- Product link placeholder (no image in OrderItemResponse) -->
              <div class="w-12 h-12 rounded-lg bg-slate-50 border border-slate-100 flex items-center justify-center shrink-0 text-slate-300">
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-6 h-6">
                  <path stroke-linecap="round" stroke-linejoin="round" d="m2.25 15.75 5.159-5.159a2.25 2.25 0 0 1 3.182 0l5.159 5.159m-1.5-1.5 1.409-1.409a2.25 2.25 0 0 1 3.182 0l2.909 2.909m-18 3.75h16.5a1.5 1.5 0 0 0 1.5-1.5V6a1.5 1.5 0 0 0-1.5-1.5H3.75A1.5 1.5 0 0 0 2.25 6v12a1.5 1.5 0 0 0 1.5 1.5Zm10.5-11.25h.008v.008h-.008V8.25Zm.375 0a.375.375 0 1 1-.75 0 .375.375 0 0 1 .75 0Z" />
                </svg>
              </div>

              <div class="flex-grow min-w-0">
                <NuxtLink
                  :to="`/products/${item.productId}`"
                  class="font-semibold text-slate-800 text-sm hover:text-indigo-600 transition-colors line-clamp-1 block"
                >
                  {{ item.productName }}
                </NuxtLink>
                <div class="text-xs text-slate-400 mt-0.5">
                  單價 NT$ {{ formatPrice(item.priceAtPurchase) }} × {{ item.quantity }} 件
                </div>
              </div>

              <div class="text-sm font-bold text-slate-900 shrink-0">
                NT$ {{ formatPrice(item.subtotal) }}
              </div>
            </div>
          </div>

          <!-- Subtotal Row -->
          <div class="px-6 py-4 border-t border-slate-100 bg-slate-50/50 flex justify-between items-center">
            <span class="text-sm text-slate-500">共 {{ totalQuantity }} 件商品</span>
            <div class="text-right">
              <div class="text-xs text-slate-400">運費:免運費</div>
              <div class="font-extrabold text-slate-900 text-base">
                合計:NT$ {{ formatPrice(order.totalAmount) }}
              </div>
            </div>
          </div>
        </div>

        <!-- Right: Shipping Info -->
        <div class="bg-white border border-slate-200/80 rounded-2xl shadow-sm overflow-hidden">
          <div class="px-6 py-4 border-b border-slate-100">
            <h2 class="font-bold text-slate-900">收件資訊</h2>
          </div>
          <div class="px-6 py-4 space-y-4">
            <div class="space-y-1">
              <p class="text-xs font-semibold text-slate-400 uppercase tracking-wider">收件人</p>
              <p class="text-sm font-semibold text-slate-800">{{ order.recipientName }}</p>
            </div>
            <div class="space-y-1">
              <p class="text-xs font-semibold text-slate-400 uppercase tracking-wider">聯絡電話</p>
              <p class="text-sm text-slate-700 font-mono">{{ order.recipientPhone }}</p>
            </div>
            <div class="space-y-1">
              <p class="text-xs font-semibold text-slate-400 uppercase tracking-wider">配送地址</p>
              <p class="text-sm text-slate-700 leading-relaxed">{{ order.recipientAddress }}</p>
            </div>
            <div class="pt-2 border-t border-slate-100 space-y-1">
              <p class="text-xs font-semibold text-slate-400 uppercase tracking-wider">訂單編號</p>
              <p class="text-sm font-mono text-slate-600">#{{ order.id }}</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
const route = useRoute()
const api = useApi()

const order = ref<any>(null)
const loading = ref(true)
const errorMessage = ref('')

// 訂單狀態進度條步驟 (CANCELLED 另外處理) 
const statusSteps = [
  { key: 'PENDING', label: '待付款' },
  { key: 'PAID', label: '已付款' },
  { key: 'SHIPPED', label: '已出貨' },
  { key: 'COMPLETED', label: '已完成' },
]

const statusOrder = ['PENDING', 'PAID', 'SHIPPED', 'COMPLETED']

const isStepReached = (stepKey: string) => {
  if (!order.value) return false
  if (order.value.status === 'CANCELLED') return false
  const currentIdx = statusOrder.indexOf(order.value.status)
  const stepIdx = statusOrder.indexOf(stepKey)
  return stepIdx <= currentIdx
}

// 載入訂單詳情
const loadOrder = async () => {
  loading.value = true
  try {
    const res = await api.request(`/orders/${route.params.id}`)
    if (res.success && res.data) {
      order.value = res.data
    }
  } catch (error: any) {
    order.value = null
    if (error?.status === 403) {
      errorMessage.value = '您無權查看此筆訂單.'
    } else if (error?.status === 404) {
      errorMessage.value = '此訂單不存在或已被刪除.'
    } else {
      errorMessage.value = error.message || '載入訂單時發生錯誤,請稍後再試.'
    }
  } finally {
    loading.value = false
  }
}

// 商品數量加總
const totalQuantity = computed(() => {
  if (!order.value?.items) return 0
  return order.value.items.reduce((sum: number, item: any) => sum + item.quantity, 0)
})

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

const getStatusClass = (status: string) => {
  const map: Record<string, string> = {
    'PENDING': 'bg-amber-50 text-amber-700 border-amber-200',
    'PAID': 'bg-blue-50 text-blue-700 border-blue-200',
    'SHIPPED': 'bg-indigo-50 text-indigo-700 border-indigo-200',
    'COMPLETED': 'bg-emerald-50 text-emerald-700 border-emerald-200',
    'CANCELLED': 'bg-rose-50 text-rose-700 border-rose-200',
  }
  return map[status] || 'bg-slate-50 text-slate-600 border-slate-200'
}

const formatPrice = (price: number) =>
  new Intl.NumberFormat('zh-TW', { maximumFractionDigits: 0 }).format(price)

const formatDate = (timestamp: number) => {
  if (!timestamp) return '-'
  return new Date(timestamp).toLocaleString('zh-TW', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  })
}

onMounted(() => {
  loadOrder()
})
</script>
