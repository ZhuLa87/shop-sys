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

    <!-- Product Table -->
    <div class="bg-white border border-slate-200/80 rounded-2xl overflow-hidden shadow-sm">
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

        <el-table-column label="操作" width="160" align="right">
          <template #default="{ row }">
            <div class="flex justify-end gap-2">
              <el-button @click="openEditDialog(row)">編輯</el-button>
              <el-button type="danger" plain @click="handleDelete(row)">刪除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="totalPages > 1" class="p-4 border-t border-slate-100 flex justify-end">
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          layout="prev, pager, next"
          :total="totalElements"
          @current-change="handlePageChange"
        />
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

          <el-form-item label="初始庫存量" prop="stockQuantity">
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

        <!-- Cover Image Upload -->
        <el-form-item label="商品封面圖片">
          <div class="w-full space-y-2">
            <div class="flex items-center gap-4">
              <!-- Preview -->
              <div class="w-20 h-20 rounded-xl overflow-hidden border-2 border-slate-200 bg-slate-50 shrink-0 flex items-center justify-center">
                <img v-if="form.coverImageUrl" :src="form.coverImageUrl" class="w-full h-full object-cover" alt="封面預覽" />
                <svg v-else xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-8 h-8 text-slate-300">
                  <path stroke-linecap="round" stroke-linejoin="round" d="m2.25 15.75 5.159-5.159a2.25 2.25 0 0 1 3.182 0l5.159 5.159m-1.5-1.5 1.409-1.409a2.25 2.25 0 0 1 3.182 0l2.909 2.909m-18 3.75h16.5a1.5 1.5 0 0 0 1.5-1.5V6a1.5 1.5 0 0 0-1.5-1.5H3.75A1.5 1.5 0 0 0 2.25 6v12a1.5 1.5 0 0 0 1.5 1.5Zm10.5-11.25h.008v.008h-.008V8.25Zm.375 0a.375.375 0 1 1-.75 0 .375.375 0 0 1 .75 0Z" />
                </svg>
              </div>
              <!-- Actions -->
              <div class="flex gap-2 flex-wrap">
                <label :class="uploadingCover ? 'opacity-60 cursor-not-allowed pointer-events-none' : 'cursor-pointer'">
                  <input
                    type="file"
                    accept="image/jpeg,image/png,image/webp,image/gif"
                    class="sr-only"
                    :disabled="uploadingCover"
                    @change="handleCoverFileSelect"
                  />
                  <span class="inline-flex items-center gap-1.5 px-3 py-1.5 bg-white border border-slate-200 hover:border-indigo-400 text-slate-700 hover:text-indigo-600 text-sm font-medium rounded-lg transition-all">
                    <span v-if="uploadingCover" class="animate-spin h-3.5 w-3.5 border-2 border-current border-t-transparent rounded-full" />
                    <svg v-else xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="w-3.5 h-3.5">
                      <path stroke-linecap="round" stroke-linejoin="round" d="M3 16.5v2.25A2.25 2.25 0 0 0 5.25 21h13.5A2.25 2.25 0 0 0 21 18.75V16.5m-13.5-9L12 3m0 0 4.5 4.5M12 3v13.5" />
                    </svg>
                    {{ uploadingCover ? '上傳中...' : (form.coverImageUrl ? '更換圖片' : '選擇圖片') }}
                  </span>
                </label>
                <button
                  v-if="form.coverImageUrl"
                  type="button"
                  class="px-3 py-1.5 text-sm text-slate-400 hover:text-rose-600 border border-slate-200 hover:border-rose-200 rounded-lg transition-all cursor-pointer"
                  @click="form.coverImageUrl = ''"
                >
                  清除
                </button>
              </div>
            </div>
            <p class="text-xs text-slate-400">選擇後可裁切顯示範圍 (1:1),支援 JPEG, PNG, WebP, GIF,上限 5MB.</p>
          </div>
        </el-form-item>

        <!-- Sub Images Upload -->
        <div class="space-y-2">
          <label class="block text-sm font-semibold text-slate-700">商品附圖 (可多張)</label>

          <div class="grid grid-cols-5 gap-2">
            <!-- Existing images -->
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

            <!-- Upload slot -->
            <label
              v-if="!uploadingSubImage"
              class="aspect-square rounded-lg border-2 border-dashed border-slate-200 hover:border-indigo-400 bg-slate-50 hover:bg-indigo-50/40 flex flex-col items-center justify-center gap-1 cursor-pointer transition-all"
            >
              <input
                type="file"
                accept="image/jpeg,image/png,image/webp,image/gif"
                class="sr-only"
                @change="handleSubImageFileSelect"
              />
              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="w-5 h-5 text-slate-400">
                <path stroke-linecap="round" stroke-linejoin="round" d="M12 4.5v15m7.5-7.5h-15" />
              </svg>
              <span class="text-xs text-slate-400">新增</span>
            </label>

            <!-- Loading slot -->
            <div
              v-else
              class="aspect-square rounded-lg border-2 border-dashed border-indigo-300 bg-indigo-50/50 flex items-center justify-center"
            >
              <span class="animate-spin h-5 w-5 border-2 border-indigo-400 border-t-transparent rounded-full" />
            </div>
          </div>

          <p class="text-xs text-slate-400">選擇後可裁切顯示範圍 (1:1),支援 JPEG, PNG, WebP, GIF.</p>
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
import { ref, reactive, onMounted } from 'vue'
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

// 分頁狀態
const products = ref<any[]>([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const totalElements = ref(0)
const totalPages = ref(0)

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
    const res = await api.request(`/products?page=${pageIndex}&size=${pageSize.value}&sort=createdAt,desc`)
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

const handleDelete = async (row: any) => {
  try {
    await ElMessageBox.confirm(
      `您確定要"直接刪除"商品"${row.name}"嗎?這將會直接從資料庫移除,且無法復原!`,
      '高風險警告',
      { confirmButtonText: '確定刪除', cancelButtonText: '取消', confirmButtonClass: 'el-button--danger', type: 'error' }
    )
    const res = await api.request(`/products/${row.id}`, { method: 'DELETE' })
    if (res.success) {
      notify({ title: '刪除成功', message: `商品"${row.name}"已被刪除.`, type: 'success', duration: 3000 })
      loadProducts()
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
