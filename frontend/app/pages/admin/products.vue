<template>
  <div class="space-y-6">
    <!-- Header -->
    <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 border-b border-slate-100 pb-5">
      <div>
        <h1 class="text-2xl sm:text-3xl font-extrabold text-slate-900 tracking-tight">商品管理後台</h1>
        <p class="text-sm text-slate-500">新增,編輯或下架平台商品,每次編輯時圖片將全量更新</p>
      </div>

      <div class="flex items-center gap-3">
        <NuxtLink to="/admin/logs" class="px-4 py-2 border border-slate-200 text-slate-700 hover:border-indigo-600 hover:text-indigo-600 text-sm font-semibold rounded-lg bg-white transition-all cursor-pointer">
          查看庫存異動日誌
        </NuxtLink>
        <button
          class="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white text-sm font-semibold rounded-lg shadow-md shadow-indigo-100 hover:shadow-lg transition-all cursor-pointer flex items-center"
          @click="openAddDialog"
        >
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.5" stroke="currentColor" class="w-4 h-4 mr-1">
            <path stroke-linecap="round" stroke-linejoin="round" d="M12 4.5v15m7.5-7.5h-15" />
          </svg>
          新增商品
        </button>
      </div>
    </div>

    <!-- Product Table Card: toolbar + table + pagination -->
    <div class="bg-white border border-slate-200/80 rounded-2xl overflow-hidden shadow-sm">

      <!-- Toolbar -->
      <div class="flex flex-col sm:flex-row sm:items-center gap-3 px-4 py-3 border-b border-slate-100">
        <!-- Search input -->
        <div class="relative w-full sm:w-64">
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.8" stroke="currentColor"
            class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400 pointer-events-none">
            <path stroke-linecap="round" stroke-linejoin="round" d="m21 21-5.197-5.197m0 0A7.5 7.5 0 1 0 5.196 5.196a7.5 7.5 0 0 0 10.607 10.607Z" />
          </svg>
          <input
            v-model="searchKeyword"
            type="text"
            placeholder="搜尋商品名稱..."
            class="w-full pl-9 pr-4 py-1.5 text-sm border border-slate-200 rounded-lg bg-slate-50 text-slate-700 placeholder-slate-400 focus:outline-none focus:bg-white focus:ring-2 focus:ring-indigo-500/30 focus:border-indigo-400 transition-all"
          />
        </div>

        <!-- Status segmented control -->
        <div class="flex items-center gap-0.5 bg-slate-100 rounded-lg p-0.5 shrink-0">
          <button
            v-for="opt in statusOptions"
            :key="opt.value"
            class="px-3 py-1.5 text-xs font-medium rounded-md transition-all cursor-pointer"
            :class="statusFilter === opt.value
              ? 'bg-white shadow-sm text-slate-900 font-semibold'
              : 'text-slate-500 hover:text-slate-700'"
            @click="setStatusFilter(opt.value)"
          >
            {{ opt.label }}
          </button>
        </div>

        <!-- Result count -->
        <div class="sm:ml-auto text-xs text-slate-400 shrink-0">
          共 <span class="font-semibold text-slate-600">{{ totalElements }}</span> 筆商品
        </div>
      </div>

      <!-- Table -->
      <el-table :data="products" style="width: 100%" v-loading="loading">
        <el-table-column label="商品封面" width="100">
          <template #default="{ row }">
            <el-image
              :src="row.coverImageUrl || ''"
              :preview-src-list="row.coverImageUrl ? [row.coverImageUrl] : []"
              fit="cover"
              :alt="row.name"
              preview-teleported
              class="w-12 h-12 rounded-lg overflow-hidden border border-slate-100 bg-slate-50 cursor-zoom-in"
            >
              <template #error>
                <div class="w-full h-full flex items-center justify-center text-slate-400 text-xs text-center leading-tight px-0.5">
                  沒有圖片
                </div>
              </template>
            </el-image>
          </template>
        </el-table-column>

        <el-table-column prop="name" label="商品名稱" min-width="180" show-overflow-tooltip />

        <el-table-column label="價格" width="120">
          <template #default="{ row }">
            <span class="font-semibold text-slate-800">NT$ {{ formatPrice(row.price) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="庫存量" width="100">
          <template #default="{ row }">
            <span :class="row.stockQuantity === 0 ? 'text-rose-600 font-bold' : 'text-slate-600'">
              {{ row.stockQuantity }}
            </span>
          </template>
        </el-table-column>

        <el-table-column label="狀態" width="120">
          <template #default="{ row }">
            <span :class="getStatusClass(row.status)" class="inline-flex items-center rounded-full px-2 py-0.5 text-xs font-semibold border">
              {{ getStatusLabel(row.status) }}
            </span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="240" align="right">
          <template #default="{ row }">
            <!-- 按鈕間距交給 Element Plus 的 .el-button + .el-button (margin-left: 12px), 再加 gap 會重複計算而撐爆欄寬 -->
            <div class="flex justify-end">
              <el-button tag="a" :href="`/products/${row.id}`" target="_blank" rel="noopener">檢視</el-button>
              <el-button @click="openEditDialog(row)">編輯</el-button>
              <el-button type="danger" plain @click="handleDelete(row)">刪除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- Empty state -->
      <div v-if="!loading && products.length === 0" class="py-16 flex flex-col items-center gap-3">
        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.2" stroke="currentColor" class="w-12 h-12 text-slate-200">
          <path stroke-linecap="round" stroke-linejoin="round" d="m21 21-5.197-5.197m0 0A7.5 7.5 0 1 0 5.196 5.196a7.5 7.5 0 0 0 10.607 10.607Z" />
        </svg>
        <p class="text-sm text-slate-400">找不到符合條件的商品</p>
        <button
          v-if="searchKeyword || statusFilter"
          class="text-xs text-indigo-500 hover:text-indigo-700 underline underline-offset-2 cursor-pointer transition-colors"
          @click="clearFilters"
        >
          清除篩選條件
        </button>
      </div>

      <!-- Pagination -->
      <div v-if="totalPages > 1" class="px-4 py-3 border-t border-slate-100 flex justify-end">
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          layout="prev, pager, next"
          :total="totalElements"
          @current-change="handlePageChange"
        />
      </div>
    </div>

    <!-- Trash / Deleted Products -->
    <div class="bg-white border border-slate-200/80 rounded-2xl overflow-hidden shadow-sm">
      <button
        class="w-full flex items-center justify-between px-6 py-4 text-left hover:bg-slate-50 transition-colors cursor-pointer"
        @click="trashOpen = !trashOpen; trashOpen && loadDeletedProducts()"
      >
        <div class="flex items-center gap-2">
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-5 h-5 text-slate-400">
            <path stroke-linecap="round" stroke-linejoin="round" d="m14.74 9-.346 9m-4.788 0L9.26 9m9.968-3.21c.342.052.682.107 1.022.166m-1.022-.165L18.16 19.673a2.25 2.25 0 0 1-2.244 2.077H8.084a2.25 2.25 0 0 1-2.244-2.077L4.772 5.79m14.456 0a48.108 48.108 0 0 0-3.478-.397m-12 .562c.34-.059.68-.114 1.022-.165m0 0a48.11 48.11 0 0 1 3.478-.397m7.5 0v-.916c0-1.18-.91-2.164-2.09-2.201a51.964 51.964 0 0 0-3.32 0c-1.18.037-2.09 1.022-2.09 2.201v.916m7.5 0a48.667 48.667 0 0 0-7.5 0" />
          </svg>
          <span class="font-semibold text-slate-700">已刪除商品</span>
          <span v-if="deletedProducts.length" class="text-xs font-semibold bg-rose-100 text-rose-600 rounded-full px-2 py-0.5">
            {{ deletedProducts.length }}
          </span>
        </div>
        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor"
          class="w-4 h-4 text-slate-400 transition-transform duration-200"
          :class="trashOpen ? 'rotate-180' : ''"
        >
          <path stroke-linecap="round" stroke-linejoin="round" d="m19.5 8.25-7.5 7.5-7.5-7.5" />
        </svg>
      </button>

      <div v-if="trashOpen" class="border-t border-slate-100">
        <div v-if="trashLoading" class="p-8 flex justify-center text-slate-400 text-sm">載入中...</div>
        <div v-else-if="deletedProducts.length === 0" class="p-8 text-center text-slate-400 text-sm">回收桶是空的</div>
        <el-table v-else :data="deletedProducts" style="width: 100%">
          <el-table-column label="封面" width="80">
            <template #default="{ row }">
              <el-image :src="row.coverImageUrl || ''" fit="cover" class="w-10 h-10 rounded-lg border border-slate-100 bg-slate-50">
                <template #error>
                  <div class="w-full h-full flex items-center justify-center text-slate-300 text-xs">無</div>
                </template>
              </el-image>
            </template>
          </el-table-column>
          <el-table-column prop="name" label="商品名稱" min-width="160" show-overflow-tooltip />
          <el-table-column label="刪除時間" width="180">
            <template #default="{ row }">
              <span class="text-sm text-slate-500">{{ formatDeletedAt(row.deletedAt) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100" align="right">
            <template #default="{ row }">
              <el-button size="small" @click="handleRestore(row)">還原</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <!-- Add / Edit Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '修改商品資訊' : '新增商品'"
      width="600px"
      destroy-on-close
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        class="space-y-4"
      >
        <el-form-item label="商品名稱" prop="name">
          <el-input v-model="form.name" placeholder="請輸入商品名稱" />
        </el-form-item>

        <div class="grid grid-cols-2 gap-4">
          <el-form-item label="價格 (NT$)" prop="price">
            <el-input-number v-model="form.price" :min="0" class="w-full" controls-position="right" />
          </el-form-item>

          <el-form-item label="庫存量" prop="stockQuantity">
            <el-input-number v-model="form.stockQuantity" :min="0" class="w-full" controls-position="right" />
          </el-form-item>
        </div>

        <el-form-item label="上架狀態" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio-button value="ON_SHELF">上架中</el-radio-button>
            <el-radio-button value="OFF_SHELF">已下架</el-radio-button>
            <el-radio-button value="OUT_OF_STOCK">缺貨中</el-radio-button>
          </el-radio-group>
        </el-form-item>

        <!-- Images Upload (cover + sub in unified grid) -->
        <div class="space-y-2">
          <div class="flex items-center justify-between">
            <label class="block text-sm font-semibold text-slate-700">商品圖片</label>
            <span class="text-xs text-slate-400">第一格為封面,支援 JPEG, PNG, WebP, GIF</span>
          </div>

          <div class="grid grid-cols-5 gap-2">

            <!-- Cover cell (always first) -->
            <div class="relative group aspect-square">
              <!-- Uploading -->
              <div
                v-if="uploadingCover"
                class="w-full h-full rounded-lg border-2 border-dashed border-indigo-300 bg-indigo-50/50 flex items-center justify-center"
              >
                <span class="animate-spin h-5 w-5 border-2 border-indigo-400 border-t-transparent rounded-full" />
              </div>
              <!-- Has image -->
              <template v-else-if="form.coverImageUrl">
                <div class="w-full h-full rounded-lg overflow-hidden border-2 border-indigo-300 bg-slate-50">
                  <img :src="form.coverImageUrl" class="w-full h-full object-cover" alt="封面" />
                </div>
                <button
                  type="button"
                  class="absolute top-1 right-1 w-5 h-5 bg-black/60 hover:bg-rose-500 text-white rounded-full flex items-center justify-center opacity-0 group-hover:opacity-100 transition-all cursor-pointer"
                  @click="form.coverImageUrl = ''"
                >
                  <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.5" stroke="currentColor" class="w-3 h-3">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M6 18 18 6M6 6l12 12" />
                  </svg>
                </button>
              </template>
              <!-- Empty upload zone -->
              <label
                v-else
                class="w-full h-full rounded-lg border-2 border-dashed border-indigo-200 hover:border-indigo-400 bg-indigo-50/30 hover:bg-indigo-50/60 flex flex-col items-center justify-center gap-1 cursor-pointer transition-all"
              >
                <input type="file" accept="image/jpeg,image/png,image/webp,image/gif" class="sr-only" @change="handleCoverFileSelect" />
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="w-5 h-5 text-indigo-300">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M12 4.5v15m7.5-7.5h-15" />
                </svg>
              </label>
              <!-- Cover badge -->
              <span class="absolute bottom-1 left-1/2 -translate-x-1/2 text-[9px] bg-indigo-600 text-white px-1.5 py-0.5 rounded font-bold leading-none pointer-events-none whitespace-nowrap z-10">封面</span>
            </div>

            <!-- Sub image cells -->
            <div
              v-for="(url, idx) in form.imageUrls"
              :key="url + idx"
              class="relative group aspect-square rounded-lg overflow-hidden border-2 border-slate-200 bg-slate-50"
            >
              <img :src="url" class="w-full h-full object-cover" :alt="`附圖 ${idx + 1}`" loading="lazy" />
              <button
                type="button"
                class="absolute top-1 right-1 w-5 h-5 bg-black/60 hover:bg-rose-500 text-white rounded-full flex items-center justify-center opacity-0 group-hover:opacity-100 transition-all cursor-pointer"
                @click="removeSubImage(idx)"
              >
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.5" stroke="currentColor" class="w-3 h-3">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M6 18 18 6M6 6l12 12" />
                </svg>
              </button>
            </div>

            <!-- Sub image upload slot -->
            <label
              v-if="!uploadingSubImage"
              class="aspect-square rounded-lg border-2 border-dashed border-slate-200 hover:border-indigo-400 bg-slate-50 hover:bg-indigo-50/40 flex flex-col items-center justify-center gap-1 cursor-pointer transition-all"
            >
              <input type="file" accept="image/jpeg,image/png,image/webp,image/gif" class="sr-only" @change="handleSubImageFileSelect" />
              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="w-5 h-5 text-slate-400">
                <path stroke-linecap="round" stroke-linejoin="round" d="M12 4.5v15m7.5-7.5h-15" />
              </svg>
              <span class="text-xs text-slate-400">新增</span>
            </label>
            <div
              v-else
              class="aspect-square rounded-lg border-2 border-dashed border-indigo-300 bg-indigo-50/50 flex items-center justify-center"
            >
              <span class="animate-spin h-5 w-5 border-2 border-indigo-400 border-t-transparent rounded-full" />
            </div>

          </div>
        </div>

        <el-form-item label="商品描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="4" placeholder="請填寫商品詳細規格,簡介或售後說明" />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="flex justify-end gap-3 pt-4 border-t border-slate-100">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="saving" @click="handleSave">確認儲存</el-button>
        </div>
      </template>
    </el-dialog>
  </div>

  <!-- Cover Image Cropper -->
  <ImageCropperDialog
    v-model="cropperCoverVisible"
    :image-src="pendingCropSrc"
    :aspect-ratio="1"
    :output-size="800"
    @confirm="handleCoverCropConfirm"
  />

  <!-- Sub Image Cropper -->
  <ImageCropperDialog
    v-model="cropperSubVisible"
    :image-src="pendingCropSrc"
    :aspect-ratio="1"
    :output-size="800"
    @confirm="handleSubImageCropConfirm"
  />
</template>

<script setup lang="ts">
import { ref, reactive, watch, onMounted } from 'vue'
import { useAuthStore } from '~/stores/auth'
import { ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'

const authStore = useAuthStore()
const api = useApi()
const upload = useUpload()

// 裁切器狀態
const cropperCoverVisible = ref(false)
const cropperSubVisible = ref(false)
const pendingCropSrc = ref('')

onMounted(() => {
  if (!authStore.isProductManager) {
    notify({ title: '權限不足', message: '您沒有存取商品管理後台的權限.', type: 'error' })
    navigateTo('/')
  } else {
    loadProducts()
  }
})

// 搜尋/篩選狀態
const searchKeyword = ref('')
const statusFilter = ref('')
let debounceTimer: ReturnType<typeof setTimeout> | null = null

const statusOptions = [
  { label: '全部', value: '' },
  { label: '上架中', value: 'ON_SHELF' },
  { label: '已下架', value: 'OFF_SHELF' },
  { label: '缺貨中', value: 'OUT_OF_STOCK' },
]

watch(searchKeyword, () => {
  if (debounceTimer) clearTimeout(debounceTimer)
  debounceTimer = setTimeout(() => {
    currentPage.value = 1
    loadProducts()
  }, 400)
})

const setStatusFilter = (value: string) => {
  statusFilter.value = value
  currentPage.value = 1
  loadProducts()
}

const clearFilters = () => {
  searchKeyword.value = ''
  statusFilter.value = ''
  currentPage.value = 1
  loadProducts()
}

// 分頁狀態
const products = ref<any[]>([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const totalElements = ref(0)
const totalPages = ref(0)

const trashOpen = ref(false)
const trashLoading = ref(false)
const deletedProducts = ref<any[]>([])

const dialogVisible = ref(false)
const isEdit = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()
const editingProductId = ref<number | null>(null)

// 上傳用的資源 ID:新增時用時間戳作暫時 ID,編輯時用真實商品 ID
const uploadResourceId = ref(0)
const uploadingCover = ref(false)
const uploadingSubImage = ref(false)

const form = reactive({
  name: '',
  price: 0,
  stockQuantity: 0,
  status: 'ON_SHELF',
  coverImageUrl: '',
  description: '',
  imageUrls: [] as string[],
})

const rules = reactive<FormRules>({
  name: [{ required: true, message: '商品名稱不可為空', trigger: 'blur' }],
  price: [{ required: true, message: '請填寫價格', trigger: 'blur' }],
  stockQuantity: [{ required: true, message: '請填寫庫存數量', trigger: 'blur' }],
  status: [{ required: true, message: '請選擇上架狀態', trigger: 'blur' }],
})

const loadProducts = async () => {
  loading.value = true
  try {
    const pageIndex = currentPage.value - 1
    const params = new URLSearchParams({
      page: String(pageIndex),
      size: String(pageSize.value),
      sort: 'createdAt,desc',
    })
    if (searchKeyword.value.trim()) params.set('keyword', searchKeyword.value.trim())
    if (statusFilter.value) params.set('status', statusFilter.value)

    const res = await api.request(`/products/admin?${params}`)
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

const handlePageChange = (page: number) => {
  currentPage.value = page
  loadProducts()
}

const openAddDialog = () => {
  isEdit.value = false
  editingProductId.value = null
  uploadResourceId.value = Date.now()
  dialogVisible.value = true
  form.name = ''
  form.price = 0
  form.stockQuantity = 0
  form.status = 'ON_SHELF'
  form.coverImageUrl = ''
  form.description = ''
  form.imageUrls = []
}

const openEditDialog = async (row: any) => {
  isEdit.value = true
  editingProductId.value = row.id
  uploadResourceId.value = row.id
  dialogVisible.value = true
  try {
    const res = await api.request(`/products/${row.id}`)
    if (res.success && res.data) {
      const data = res.data
      form.name = data.name
      form.price = data.price
      form.stockQuantity = data.stockQuantity
      form.status = data.status
      form.coverImageUrl = data.coverImageUrl || ''
      form.description = data.description || ''
      form.imageUrls = data.imageUrls ? [...data.imageUrls] : []
    }
  } catch (error) {
    console.error('載入商品詳情失敗:', error)
  }
}

// 共用: 讀取檔案為 data URL 並開啟裁切器
const openCropperWithFile = (file: File, onOpen: (src: string) => void) => {
  const reader = new FileReader()
  reader.onload = (e) => {
    pendingCropSrc.value = e.target?.result as string
    onOpen(pendingCropSrc.value)
  }
  reader.readAsDataURL(file)
}

// 封面圖片: 選擇 -> 裁切
const handleCoverFileSelect = (event: Event) => {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  input.value = ''
  openCropperWithFile(file, () => { cropperCoverVisible.value = true })
}

const handleCoverCropConfirm = async (blob: Blob) => {
  uploadingCover.value = true
  try {
    form.coverImageUrl = await upload.uploadProductImage(blob, 'product-cover', uploadResourceId.value)
  } catch (error: any) {
    notify({ title: '封面圖片上傳失敗', message: error.message || '請稍後再試.', type: 'error', duration: 4000 })
  } finally {
    uploadingCover.value = false
  }
}

// 附圖: 選擇 -> 裁切
const handleSubImageFileSelect = (event: Event) => {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  input.value = ''
  openCropperWithFile(file, () => { cropperSubVisible.value = true })
}

const handleSubImageCropConfirm = async (blob: Blob) => {
  uploadingSubImage.value = true
  try {
    const publicUrl = await upload.uploadProductImage(blob, 'product-image', uploadResourceId.value)
    form.imageUrls.push(publicUrl)
  } catch (error: any) {
    notify({ title: '附圖上傳失敗', message: error.message || '請稍後再試.', type: 'error', duration: 4000 })
  } finally {
    uploadingSubImage.value = false
  }
}

const removeSubImage = (index: number) => {
  form.imageUrls.splice(index, 1)
}

const handleSave = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    saving.value = true
    const payload = {
      name: form.name,
      price: form.price,
      stockQuantity: form.stockQuantity,
      status: form.status,
      coverImageUrl: form.coverImageUrl.trim() || null,
      description: form.description.trim() || null,
      imageUrls: form.imageUrls.filter(url => url.trim() !== ''),
    }
    try {
      let res
      if (isEdit.value && editingProductId.value) {
        res = await api.request(`/products/${editingProductId.value}`, { method: 'PUT', body: payload })
      } else {
        res = await api.request('/products', { method: 'POST', body: payload })
      }
      if (res.success) {
        notify({
          title: isEdit.value ? '商品修改成功' : '商品新增成功',
          message: `商品"${form.name}"已儲存.`,
          type: 'success',
          duration: 3000,
        })
        dialogVisible.value = false
        loadProducts()
      }
    } catch (error: any) {
      notify({ title: '儲存失敗', message: error.message || '資料格式錯誤,請檢查欄位.', type: 'error', duration: 4000 })
    } finally {
      saving.value = false
    }
  })
}

const loadDeletedProducts = async () => {
  trashLoading.value = true
  try {
    const res = await api.request('/products/deleted')
    if (res.success && res.data) deletedProducts.value = res.data
  } catch (error) {
    console.error('載入回收桶出錯:', error)
  } finally {
    trashLoading.value = false
  }
}

const handleRestore = async (row: any) => {
  try {
    const res = await api.request(`/products/${row.id}/restore`, { method: 'PUT' })
    if (res.success) {
      notify({ title: '還原成功', message: `商品"${row.name}"已還原.`, type: 'success', duration: 3000 })
      deletedProducts.value = deletedProducts.value.filter(p => p.id !== row.id)
      loadProducts()
    }
  } catch (error: any) {
    notify({ title: '還原失敗', message: error.message || '還原時出錯.', type: 'error', duration: 4000 })
  }
}

const formatDeletedAt = (iso: string) => {
  if (!iso) return '-'
  return new Date(iso).toLocaleString('zh-TW', { dateStyle: 'short', timeStyle: 'short' })
}

const handleDelete = async (row: any) => {
  try {
    await ElMessageBox.confirm(
      `確定要刪除商品"${row.name}"嗎?刪除後將不再對顧客顯示.`,
      '確認刪除',
      { confirmButtonText: '確定刪除', cancelButtonText: '取消', confirmButtonClass: 'el-button--danger', type: 'warning' }
    )
    const res = await api.request(`/products/${row.id}`, { method: 'DELETE' })
    if (res.success) {
      notify({ title: '刪除成功', message: `商品"${row.name}"已被刪除.`, type: 'success', duration: 3000 })
      loadProducts()
      if (trashOpen.value) loadDeletedProducts()
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      notify({ title: '刪除失敗', message: error.message || '刪除時出錯.', type: 'error', duration: 4000 })
    }
  }
}

const formatPrice = (price: number) =>
  new Intl.NumberFormat('zh-TW', { maximumFractionDigits: 0 }).format(price)

const getStatusLabel = (status: string) => {
  const map: Record<string, string> = { ON_SHELF: '上架中', OFF_SHELF: '已下架', OUT_OF_STOCK: '缺貨中' }
  return map[status] || status
}

const getStatusClass = (status: string) => {
  const map: Record<string, string> = {
    ON_SHELF: 'bg-emerald-50 text-emerald-700 border-emerald-200',
    OFF_SHELF: 'bg-slate-50 text-slate-700 border-slate-200',
    OUT_OF_STOCK: 'bg-rose-50 text-rose-700 border-rose-200',
  }
  return map[status] || 'bg-slate-50 text-slate-700 border-slate-200'
}
</script>
