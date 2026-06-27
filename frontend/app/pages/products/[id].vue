<template>
  <div class="space-y-8">
    <!-- Back Link -->
    <NuxtLink to="/" class="inline-flex items-center gap-1.5 text-sm font-medium text-slate-500 hover:text-indigo-600 transition-colors">
      <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="w-4 h-4">
        <path stroke-linecap="round" stroke-linejoin="round" d="M10.5 19.5 3 12m0 0 7.5-7.5M3 12h18" />
      </svg>
      返回商品列表
    </NuxtLink>

    <!-- Loading Skeleton -->
    <div v-if="loading" class="grid grid-cols-1 lg:grid-cols-2 gap-12 animate-pulse">
      <div class="bg-slate-100 rounded-2xl aspect-square"></div>
      <div class="space-y-6 pt-4">
        <div class="h-8 bg-slate-100 rounded w-3/4"></div>
        <div class="space-y-2">
          <div class="h-4 bg-slate-100 rounded w-full"></div>
          <div class="h-4 bg-slate-100 rounded w-5/6"></div>
          <div class="h-4 bg-slate-100 rounded w-2/3"></div>
        </div>
        <div class="h-10 bg-slate-100 rounded w-1/3"></div>
        <div class="h-12 bg-slate-100 rounded w-full"></div>
      </div>
    </div>

    <!-- 404 Not Found -->
    <div v-else-if="!product" class="flex flex-col items-center justify-center py-24 text-center space-y-4">
      <div class="w-16 h-16 rounded-full bg-slate-100 flex items-center justify-center text-slate-400">
        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-8 h-8">
          <path stroke-linecap="round" stroke-linejoin="round" d="M12 9v3.75m9-.75a9 9 0 1 1-18 0 9 9 0 0 1 18 0Zm-9 3.75h.008v.008H12v-.008Z" />
        </svg>
      </div>
      <div class="space-y-1">
        <h3 class="font-bold text-slate-800">找不到此商品</h3>
        <p class="text-sm text-slate-400">商品可能已下架或不存在,請返回列表重新瀏覽.</p>
      </div>
      <NuxtLink to="/" class="rounded-lg bg-indigo-600 px-6 py-2.5 text-sm font-semibold text-white shadow-md hover:bg-indigo-700 transition-colors">
        返回首頁
      </NuxtLink>
    </div>

    <!-- Product Detail -->
    <div v-else class="grid grid-cols-1 lg:grid-cols-2 gap-10 lg:gap-16 items-start">

      <!-- Left: Image Gallery -->
      <div class="space-y-3">
        <!-- Main Image -->
        <div class="relative bg-slate-50 border border-slate-200/80 rounded-2xl overflow-hidden aspect-square">
          <img
            v-if="activeImage && !activeImageFailed"
            :src="activeImage"
            :alt="product.name"
            class="w-full h-full object-cover transition-all duration-300"
            @error="activeImageFailed = true"
          />
          <div
            v-else
            class="w-full h-full flex items-center justify-center text-slate-400 text-sm"
          >
            此商品沒有封面圖
          </div>
          <!-- Status Overlay -->
          <div
            v-if="product.status !== 'ON_SHELF'"
            class="absolute inset-0 bg-black/40 backdrop-blur-[1px] flex items-center justify-center"
          >
            <span class="px-4 py-2 rounded-lg bg-black/70 text-white font-semibold text-sm tracking-wider uppercase">
              {{ product.status === 'OUT_OF_STOCK' ? '缺貨中' : '已下架' }}
            </span>
          </div>
        </div>

        <!-- Thumbnail Strip -->
        <div v-if="allImages.length > 1" class="flex gap-2 overflow-x-auto pb-1">
          <button
            v-for="(img, idx) in allImages"
            :key="idx"
            class="shrink-0 w-16 h-16 rounded-lg border-2 overflow-hidden transition-all cursor-pointer"
            :class="activeImage === img ? 'border-indigo-600' : 'border-slate-200 hover:border-slate-400'"
            @click="activeImage = img"
          >
            <img :src="img" class="w-full h-full object-cover" />
          </button>
        </div>
      </div>

      <!-- Right: Product Info -->
      <div class="space-y-6 lg:pt-2">
        <!-- Title & Status -->
        <div class="space-y-2">
          <div class="flex items-center gap-2">
            <span :class="getStatusClass(product.status)" class="inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-semibold border">
              {{ getStatusLabel(product.status) }}
            </span>
            <span v-if="product.stockQuantity > 0 && product.status === 'ON_SHELF'" class="text-xs text-slate-400">
              尚餘 {{ product.stockQuantity }} 件
            </span>
          </div>
          <h1 class="text-2xl sm:text-3xl font-extrabold tracking-tight text-slate-900">
            {{ product.name }}
          </h1>
        </div>

        <!-- Price -->
        <div class="flex items-baseline gap-3">
          <span class="text-3xl font-bold text-slate-900">
            NT$ {{ formatPrice(product.price) }}
          </span>
        </div>

        <!-- Description -->
        <div v-if="product.description" class="border-t border-slate-100 pt-5">
          <h3 class="text-sm font-semibold text-slate-500 uppercase tracking-wider mb-2">商品描述</h3>
          <p class="text-sm text-slate-700 leading-relaxed whitespace-pre-line">
            {{ product.description }}
          </p>
        </div>

        <!-- Add to Cart Section -->
        <div class="border-t border-slate-100 pt-5 space-y-4">
          <div class="flex items-center gap-4">
            <label class="text-sm font-semibold text-slate-700 shrink-0">購買數量</label>
            <el-input-number
              v-model="quantity"
              :min="1"
              :max="product.stockQuantity > 0 ? product.stockQuantity : 1"
              :disabled="product.status !== 'ON_SHELF' || product.stockQuantity <= 0"
              size="default"
              class="w-32"
              controls-position="right"
            />
          </div>

          <button
            :disabled="product.status !== 'ON_SHELF' || product.stockQuantity <= 0 || addingToCart"
            class="w-full py-3.5 bg-indigo-600 hover:bg-indigo-700 text-white font-bold rounded-xl shadow-md shadow-indigo-100 hover:shadow-lg transition-all cursor-pointer flex items-center justify-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed disabled:shadow-none"
            @click="handleAddToCart"
          >
            <span v-if="addingToCart" class="animate-spin h-4 w-4 border-2 border-white border-t-transparent rounded-full"></span>
            <svg v-else xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="w-5 h-5">
              <path stroke-linecap="round" stroke-linejoin="round" d="M2.25 3h1.386c.51 0 .955.343 1.087.835l.383 1.437M7.5 14.25a3 3 0 0 0-3 3h15.75m-12.75-3h11.218c1.121-2.3 2.1-4.684 2.924-7.138a60.114 60.114 0 0 0-16.536-1.84M7.5 14.25 5.106 5.272M6 20.25a.75.75 0 1 1-1.5 0 .75.75 0 0 1 1.5 0Zm12.75 0a.75.75 0 1 1-1.5 0 .75.75 0 0 1 1.5 0Z" />
            </svg>
            {{ product.status !== 'ON_SHELF' || product.stockQuantity <= 0 ? '無法購買' : '加入購物車' }}
          </button>
        </div>

        <!-- Admin Quick Links -->
        <div v-if="authStore.isProductManager" class="border-t border-slate-100 pt-4 flex items-center gap-3">
          <span class="text-xs font-semibold text-slate-400 uppercase tracking-wider">管理員工具</span>
          <button
            class="text-xs font-semibold text-indigo-600 hover:text-indigo-700 border border-indigo-200 hover:border-indigo-400 rounded-lg px-3 py-1.5 transition-all cursor-pointer"
            @click="navigateTo('/admin/products')"
          >
            編輯此商品
          </button>
          <button
            class="text-xs font-semibold text-slate-600 hover:text-slate-800 border border-slate-200 hover:border-slate-400 rounded-lg px-3 py-1.5 transition-all cursor-pointer"
            @click="navigateTo(`/admin/logs?productId=${product.id}`)"
          >
            查看庫存日誌
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useAuthStore } from '~/stores/auth'
import { useCartStore } from '~/stores/cart'

const route = useRoute()
const authStore = useAuthStore()
const cartStore = useCartStore()
const api = useApi()

const product = ref<any>(null)
const loading = ref(true)
const quantity = ref(1)
const addingToCart = ref(false)
const activeImage = ref<string>('')
const activeImageFailed = ref(false)

watch(activeImage, () => { activeImageFailed.value = false })

// 所有圖片 (封面 + 附圖) 
const allImages = computed<string[]>(() => {
  if (!product.value) return []
  const imgs: string[] = []
  if (product.value.coverImageUrl) imgs.push(product.value.coverImageUrl)
  if (Array.isArray(product.value.imageUrls)) {
    product.value.imageUrls.forEach((url: string) => {
      if (url && !imgs.includes(url)) imgs.push(url)
    })
  }
  return imgs
})

// 載入商品詳情
const loadProduct = async () => {
  loading.value = true
  try {
    const res = await api.request(`/products/${route.params.id}`)
    if (res.success && res.data) {
      product.value = res.data
      activeImage.value = res.data.coverImageUrl || ''
    }
  } catch (error: any) {
    const status = error?.status ?? error?.statusCode
    if (status === 404 || status === 400) {
      // 404: 商品不存在或已刪除;400: URL 帶入了無效 ID (如 null)
      product.value = null
    } else {
      notify({
        title: '載入失敗',
        message: error.message || '無法載入商品資料,請稍後再試.',
        type: 'error',
        duration: 4000,
      })
    }
  } finally {
    loading.value = false
  }
}

// 加入購物車
const handleAddToCart = async () => {
  if (!authStore.isAuthenticated) {
    notify({
      title: '請先登入',
      message: '需要登入會員才能加入購物車.',
      type: 'warning',
      duration: 3000,
    })
    navigateTo('/login')
    return
  }

  addingToCart.value = true
  try {
    await cartStore.addToCart(product.value.id, quantity.value)
    notify({
      title: '成功加入購物車',
      message: `已將 ${product.value.name} × ${quantity.value} 加入您的購物車.`,
      type: 'success',
      duration: 3000,
    })
  } catch (error: any) {
    notify({
      title: '加入失敗',
      message: error.message || '庫存不足或系統錯誤,請稍後再試.',
      type: 'error',
      duration: 4000,
    })
  } finally {
    addingToCart.value = false
  }
}

// 商品狀態轉換
const getStatusLabel = (status: string) => {
  const map: Record<string, string> = {
    'ON_SHELF': '上架中',
    'OFF_SHELF': '已下架',
    'OUT_OF_STOCK': '缺貨中',
  }
  return map[status] || status
}

const getStatusClass = (status: string) => {
  const map: Record<string, string> = {
    'ON_SHELF': 'bg-emerald-50 text-emerald-700 border-emerald-200',
    'OFF_SHELF': 'bg-slate-50 text-slate-600 border-slate-200',
    'OUT_OF_STOCK': 'bg-amber-50 text-amber-700 border-amber-200',
  }
  return map[status] || 'bg-slate-50 text-slate-600 border-slate-200'
}

const formatPrice = (price: number) =>
  new Intl.NumberFormat('zh-TW', { maximumFractionDigits: 0 }).format(price)

onMounted(() => {
  loadProduct()
})
</script>
