<template>
  <div class="space-y-12">
    <!-- Hero Banner Section -->
    <div class="relative overflow-hidden rounded-2xl bg-gradient-to-br from-indigo-50 via-slate-50 to-indigo-100/50 p-8 sm:p-12 md:p-16">
      <div class="relative z-10 max-w-xl space-y-4">
        <span class="inline-flex items-center rounded-full bg-indigo-50 border border-indigo-100 px-3 py-1 text-xs font-semibold text-indigo-700">
          新品上市 & 限時優惠
        </span>
        <h1 class="text-3xl sm:text-4xl md:text-5xl font-extrabold tracking-tight text-slate-900 leading-tight">
          極致簡約,<br class="hidden sm:inline" />打造您的智慧生活
        </h1>
        <p class="text-sm sm:text-base text-slate-600">
          探索我們精選的科技週邊與生活好物.我們堅持"少即是多"的設計美學,為您帶來極致舒適的使用體驗.
        </p>
      </div>
      <!-- Background Abstract Shape -->
      <div class="absolute right-0 bottom-0 top-0 w-1/3 bg-radial from-indigo-200/40 to-transparent blur-2xl pointer-events-none"></div>
    </div>

    <!-- Filter & Search Toolbar -->
    <div class="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between border-b border-slate-100 pb-6">
      <!-- Search Input -->
      <div class="relative max-w-sm w-full">
        <span class="absolute inset-y-0 left-0 flex items-center pl-3 pointer-events-none text-slate-400">
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="w-5 h-5">
            <path stroke-linecap="round" stroke-linejoin="round" d="m21 21-5.197-5.197m0 0A7.5 7.5 0 1 0 5.196 5.196a7.5 7.5 0 0 0 10.602 10.602Z" />
          </svg>
        </span>
        <input
          v-model="searchQuery"
          type="text"
          placeholder="搜尋商品名稱..."
          class="w-full pl-10 pr-4 py-2 bg-white border border-slate-200 rounded-lg text-sm placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-600 transition-all"
          @input="handleSearch"
        />
      </div>

      <!-- Sorting Options -->
      <div class="flex items-center gap-2 overflow-x-auto pb-1 sm:pb-0">
        <span class="text-xs font-semibold text-slate-400 uppercase tracking-wider mr-2 shrink-0">排序:</span>
        <button
          v-for="option in sortOptions"
          :key="option.value"
          class="px-3 py-1.5 rounded-lg text-xs font-medium border transition-all shrink-0 cursor-pointer"
          :class="[
            currentSort === option.value
              ? 'bg-indigo-600 text-white border-indigo-600 shadow-sm shadow-indigo-100'
              : 'bg-white text-slate-600 border-slate-200 hover:text-slate-900 hover:bg-slate-50'
          ]"
          @click="changeSort(option.value)"
        >
          {{ option.label }}
        </button>
      </div>
    </div>

    <!-- Product Grid Section -->
    <div v-if="loading" class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
      <!-- Skeleton Loading Cards -->
      <div v-for="i in 8" :key="i" class="animate-pulse bg-white border border-slate-100 rounded-xl overflow-hidden h-[360px]">
        <div class="bg-slate-100 h-48 w-full"></div>
        <div class="p-4 space-y-3">
          <div class="h-4 bg-slate-100 rounded w-2/3"></div>
          <div class="h-3 bg-slate-100 rounded w-full"></div>
          <div class="h-4 bg-slate-100 rounded w-1/3 pt-2"></div>
        </div>
      </div>
    </div>

    <div v-else-if="products.length === 0" class="flex flex-col items-center justify-center py-20 text-center space-y-4">
      <div class="w-16 h-16 rounded-full bg-slate-100 flex items-center justify-center text-slate-400">
        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-8 h-8">
          <path stroke-linecap="round" stroke-linejoin="round" d="m21 21-5.197-5.197m0 0A7.5 7.5 0 1 0 5.196 5.196a7.5 7.5 0 0 0 10.602 10.602Z" />
        </svg>
      </div>
      <div class="space-y-1">
        <h3 class="font-bold text-slate-800">查無商品</h3>
        <p class="text-sm text-slate-400">請嘗試不同的關鍵字或重設篩選條件</p>
      </div>
    </div>

    <div v-else class="space-y-8">
      <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
        <NuxtLink
          v-for="product in products"
          :key="product.id"
          :to="`/products/${product.id}`"
          class="group relative bg-white border border-slate-200/80 rounded-xl overflow-hidden hover:-translate-y-1 hover:shadow-lg transition-all duration-300 flex flex-col h-full"
        >
          <!-- Product Image & Badges -->
          <div class="relative bg-slate-50 aspect-square overflow-hidden shrink-0">
            <img
              v-if="product.coverImageUrl && !failedImages.has(product.id)"
              :src="product.coverImageUrl"
              :alt="product.name"
              class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
              loading="lazy"
              @error="failedImages.add(product.id)"
            />
            <div
              v-else
              class="w-full h-full flex items-center justify-center text-slate-400 text-sm"
            >
              此商品沒有圖片
            </div>
            
            <!-- Out of Stock Overlay -->
            <div v-if="product.stockQuantity <= 0" class="absolute inset-0 bg-black/40 backdrop-blur-[1px] flex items-center justify-center">
              <span class="px-3 py-1 rounded bg-black/70 text-white font-semibold text-xs tracking-wider uppercase">
                已售完
              </span>
            </div>
          </div>

          <!-- Product Details -->
          <div class="p-4 flex flex-col flex-grow justify-between">
            <div class="space-y-1">
              <h3 class="font-bold text-slate-800 text-sm group-hover:text-indigo-600 transition-colors line-clamp-1">
                {{ product.name }}
              </h3>
              <p class="text-xs text-slate-500 line-clamp-2 min-h-8">
                {{ product.description || '無詳細描述' }}
              </p>
            </div>

            <!-- Price and Cart Button -->
            <div class="flex items-center justify-between pt-4 border-t border-slate-50 mt-4 shrink-0">
              <span class="text-base font-bold text-slate-900">
                NT$ {{ formatPrice(product.price) }}
              </span>
              
              <div class="flex gap-2">
                <button
                  :disabled="product.stockQuantity <= 0"
                  class="p-3 rounded-lg bg-indigo-600 text-white hover:bg-indigo-700 disabled:bg-slate-100 disabled:text-slate-400 transition-all cursor-pointer flex items-center justify-center"
                  title="快速加入購物車"
                  @click.prevent.stop="quickAddToCart(product)"
                >
                  <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="w-5 h-5">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M2.25 3h1.386c.51 0 .955.343 1.087.835l.383 1.437M7.5 14.25a3 3 0 0 0-3 3h15.75m-12.75-3h11.218c1.121-2.3 2.1-4.684 2.924-7.138a60.114 60.114 0 0 0-16.536-1.84M7.5 14.25 5.106 5.272M6 20.25a.75.75 0 1 1-1.5 0 .75.75 0 0 1 1.5 0Zm12.75 0a.75.75 0 1 1-1.5 0 .75.75 0 0 1 1.5 0Z" />
                  </svg>
                </button>
              </div>
            </div>
          </div>
        </NuxtLink>
      </div>

      <!-- Element Plus Pagination -->
      <div v-if="totalPages > 1" class="flex justify-center pt-8">
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          layout="prev, pager, next"
          :total="totalElements"
          @current-change="handlePageChange"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useAuthStore } from '~/stores/auth'
import { useCartStore } from '~/stores/cart'

const authStore = useAuthStore()
const cartStore = useCartStore()
const api = useApi()

const failedImages = reactive(new Set<number>())

// 分頁與篩選狀態
const products = ref<any[]>([])
const loading = ref(true)
const currentPage = ref(1) // 1-indexed for Element Plus UI
const pageSize = ref(20)
const totalElements = ref(0)
const totalPages = ref(0)
const searchQuery = ref('')
const currentSort = ref('createdAt,desc')

// 搜尋防抖器 (Debounce)
let debounceTimeout: any

const sortOptions = [
  { label: '最新上架', value: 'createdAt,desc' },
  { label: '價格:低到高', value: 'price,asc' },
  { label: '價格:高到低', value: 'price,desc' },
  { label: '商品名稱 A-Z', value: 'name,asc' },
]

// 獲取商品列表資料
const loadProducts = async () => {
  loading.value = true
  try {
    const sortParam = currentSort.value
    const pageIndex = currentPage.value - 1 // Backend APIs are 0-indexed
    
    let path = `/products?page=${pageIndex}&size=${pageSize.value}&sort=${sortParam}`
    if (searchQuery.value.trim()) {
      path += `&keyword=${encodeURIComponent(searchQuery.value.trim())}`
    }

    const res = await api.request(path, { method: 'GET' })
    if (res.success && res.data) {
      products.value = res.data.content
      totalElements.value = res.data.totalElements
      totalPages.value = res.data.totalPages
    }
  } catch (error) {
    console.error('載入商品列表出錯:', error)
  } finally {
    loading.value = false
  }
}

// 觸發搜尋
const handleSearch = () => {
  clearTimeout(debounceTimeout)
  debounceTimeout = setTimeout(() => {
    currentPage.value = 1
    loadProducts()
  }, 350)
}

// 切換排序
const changeSort = (sortValue: string) => {
  currentSort.value = sortValue
  currentPage.value = 1
  loadProducts()
}

// 切換頁碼
const handlePageChange = (page: number) => {
  currentPage.value = page
  loadProducts()
}

// 快速加入購物車
const quickAddToCart = async (product: any) => {
  if (!authStore.isAuthenticated) {
    notify({
      title: '請先登入',
      message: '需要登入會員才能開始購物喔!',
      type: 'warning',
      duration: 3000,
    })
    navigateTo('/login')
    return
  }

  try {
    await cartStore.addToCart(product.id, 1)
    notify({
      title: '成功加入購物車',
      message: `已將 ${product.name} 加入您的購物車.`,
      type: 'success',
      duration: 3000,
    })
  } catch (error: any) {
    notify({
      title: '加入失敗',
      message: error?.data?.message || '庫存不足或系統錯誤.',
      type: 'error',
      duration: 3000,
    })
  }
}

// 輔助格式化方法
const formatPrice = (price: number) => {
  return new Intl.NumberFormat('zh-TW', { maximumFractionDigits: 0 }).format(price)
}

onMounted(() => {
  loadProducts()
})
</script>

<style scoped>
/* 自訂 UI 分頁樣式使其符合簡約設計 */
:deep(.el-pagination) {
  --el-pagination-hover-color: var(--color-indigo-600);
  --el-pagination-button-bg-color: transparent;
}
:deep(.el-pager li.is-active) {
  background-color: var(--color-indigo-600) !important;
  color: #fff !important;
  border-radius: 6px;
}
</style>
