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
        <!-- Image Column -->
        <el-table-column label="商品封面" width="100">
          <template #default="{ row }">
            <div class="w-12 h-12 rounded-lg bg-slate-50 border border-slate-100 overflow-hidden">
              <img :src="row.coverImageUrl || 'https://picsum.photos/seed/default/100/100'" class="w-full h-full object-cover" />
            </div>
          </template>
        </el-table-column>

        <!-- Name Column -->
        <el-table-column prop="name" label="商品名稱" min-width="180" show-overflow-tooltip />

        <!-- Price Column -->
        <el-table-column label="價格" width="120">
          <template #default="{ row }">
            <span class="font-semibold text-slate-800">NT$ {{ formatPrice(row.price) }}</span>
          </template>
        </el-table-column>

        <!-- Stock Column -->
        <el-table-column label="庫存量" width="100">
          <template #default="{ row }">
            <span :class="row.stockQuantity === 0 ? 'text-rose-600 font-bold' : 'text-slate-600'">
              {{ row.stockQuantity }}
            </span>
          </template>
        </el-table-column>

        <!-- Status Column -->
        <el-table-column label="狀態" width="120">
          <template #default="{ row }">
            <span :class="getStatusClass(row.status)" class="inline-flex items-center rounded-full px-2 py-0.5 text-xs font-semibold border">
              {{ getStatusLabel(row.status) }}
            </span>
          </template>
        </el-table-column>

        <!-- Actions Column -->
        <el-table-column label="操作" width="160" align="right">
          <template #default="{ row }">
            <div class="flex justify-end gap-2">
              <el-button size="small" @click="openEditDialog(row)">編輯</el-button>
              <el-button size="small" type="danger" plain @click="handleDelete(row)">刪除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- Pagination -->
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

    <!-- Product Add/Edit Dialog -->
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

        <el-form-item label="商品封面圖片 URL" prop="coverImageUrl">
          <el-input v-model="form.coverImageUrl" placeholder="https://picsum.photos/seed/.../400/400" />
        </el-form-item>

        <!-- Sub Images (Dynamic array) -->
        <div class="space-y-2">
          <label class="block text-sm font-semibold text-slate-700">商品附圖清單 URL (可多選)</label>
          <div v-for="(img, index) in form.imageUrls" :key="index" class="flex gap-2 items-center">
            <el-input v-model="form.imageUrls[index]" placeholder="附圖連結 URL" />
            <el-button type="danger" circle plain @click="removeSubImage(index)">
              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="w-4 h-4">
                <path stroke-linecap="round" stroke-linejoin="round" d="M6 18 18 6M6 6l12 12" />
              </svg>
            </el-button>
          </div>
          <button
            type="button"
            class="text-xs font-semibold text-indigo-600 hover:text-indigo-700 flex items-center cursor-pointer pt-1"
            @click="addSubImage"
          >
            + 增加一張附圖
          </button>
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
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useAuthStore } from '~/stores/auth'
import { ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'

const authStore = useAuthStore()
const api = useApi()

// 防非管理員越權
onMounted(() => {
  if (!authStore.isProductManager) {
    notify({
      title: '權限不足',
      message: '您沒有存取商品管理後台的權限.',
      type: 'error',
    })
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

// 編輯表單
const form = reactive({
  name: '',
  price: 0,
  stockQuantity: 0,
  status: 'ON_SHELF',
  coverImageUrl: '',
  description: '',
  imageUrls: [] as string[]
})

// 表單校驗規則
const rules = reactive<FormRules>({
  name: [{ required: true, message: '商品名稱不可為空', trigger: 'blur' }],
  price: [{ required: true, message: '請填寫價格', trigger: 'blur' }],
  stockQuantity: [{ required: true, message: '請填寫庫存數量', trigger: 'blur' }],
  status: [{ required: true, message: '請選擇上架狀態', trigger: 'blur' }]
})

// 加載商品列表
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

// 換頁
const handlePageChange = (page: number) => {
  currentPage.value = page
  loadProducts()
}

// 開啟新增視窗
const openAddDialog = () => {
  isEdit.value = false
  editingProductId.value = null
  dialogVisible.value = true
  
  // 重置表單值
  form.name = ''
  form.price = 0
  form.stockQuantity = 0
  form.status = 'ON_SHELF'
  form.coverImageUrl = ''
  form.description = ''
  form.imageUrls = []
}

// 開啟編輯視窗
const openEditDialog = async (row: any) => {
  isEdit.value = true
  editingProductId.value = row.id
  dialogVisible.value = true
  
  // 載入商品詳情 (以取得完整的附圖清單 urls)
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

// 動態附圖操作
const addSubImage = () => {
  form.imageUrls.push('')
}

const removeSubImage = (index: number) => {
  form.imageUrls.splice(index, 1)
}

// 儲存商品
const handleSave = async () => {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (valid) {
      saving.value = true
      
      // 過濾空白附圖 url
      const cleanedSubImages = form.imageUrls.filter(url => url.trim() !== '')

      const payload = {
        name: form.name,
        price: form.price,
        stockQuantity: form.stockQuantity,
        status: form.status,
        coverImageUrl: form.coverImageUrl.trim() || null,
        description: form.description.trim() || null,
        imageUrls: cleanedSubImages
      }

      try {
        let res
        if (isEdit.value && editingProductId.value) {
          // 修改商品
          res = await api.request(`/products/${editingProductId.value}`, {
            method: 'PUT',
            body: payload
          })
        } else {
          // 新增商品
          res = await api.request('/products', {
            method: 'POST',
            body: payload
          })
        }

        if (res.success) {
          notify({
            title: isEdit.value ? '商品修改成功' : '商品新增成功',
            message: `商品"${form.name}"已儲存.`,
            type: 'success',
            duration: 3000
          })
          dialogVisible.value = false
          loadProducts()
        }
      } catch (error: any) {
        notify({
          title: '儲存失敗',
          message: error?.data?.message || '資料格式錯誤,請檢查欄位.',
          type: 'error',
          duration: 4000
        })
      } finally {
        saving.value = false
      }
    }
  })
}

// 刪除商品
const handleDelete = async (row: any) => {
  try {
    await ElMessageBox.confirm(
      `您確定要"直接刪除"商品"${row.name}"嗎?這將會直接從資料庫移除,且無法復原!`,
      '高風險警告',
      {
        confirmButtonText: '確定刪除',
        cancelButtonText: '取消',
        confirmButtonClass: 'el-button--danger',
        type: 'error',
      }
    )
    
    const res = await api.request(`/products/${row.id}`, { method: 'DELETE' })
    if (res.success) {
      notify({
        title: '刪除成功',
        message: `商品"${row.name}"已被刪除.`,
        type: 'success',
        duration: 3000
      })
      loadProducts()
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      notify({
        title: '刪除失敗',
        message: error?.data?.message || '刪除時出錯.',
        type: 'error',
        duration: 4000
      })
    }
  }
}

// 價格與狀態對照
const formatPrice = (price: number) => {
  return new Intl.NumberFormat('zh-TW', { maximumFractionDigits: 0 }).format(price)
}

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
    'OFF_SHELF': 'bg-slate-50 text-slate-700 border-slate-200',
    'OUT_OF_STOCK': 'bg-rose-50 text-rose-700 border-rose-200',
  }
  return map[status] || 'bg-slate-50 text-slate-700 border-slate-200'
}
</script>
