<template>
  <div class="space-y-6">
    <!-- Header -->
    <div class="border-b border-slate-100 pb-5">
      <h1 class="text-2xl sm:text-3xl font-extrabold text-slate-900 tracking-tight">會員權限管理</h1>
      <p class="text-sm text-slate-500">僅限超級管理員存取.可在此升降使用者權限,停用或啟用會員帳戶.</p>
    </div>

    <!-- Users Table -->
    <div class="bg-white border border-slate-200/80 rounded-2xl overflow-hidden shadow-sm">
      <el-table :data="users" style="width: 100%" v-loading="loading">
        <!-- User ID -->
        <el-table-column prop="id" label="會員 ID" width="90" />

        <!-- Email -->
        <el-table-column prop="email" label="電子信箱" min-width="180" />

        <!-- Name -->
        <el-table-column prop="name" label="姓名" width="120" />

        <!-- Phone -->
        <el-table-column label="手機號碼" width="140">
          <template #default="{ row }">
            <span class="text-slate-600 font-mono text-xs">{{ maskPhone(row.phone) || '-' }}</span>
          </template>
        </el-table-column>

        <!-- Role with tag -->
        <el-table-column label="權限角色" width="160">
          <template #default="{ row }">
            <span :class="getRoleClass(row.role)" class="inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-semibold border">
              {{ getRoleLabel(row.role) }}
            </span>
          </template>
        </el-table-column>

        <!-- Enabled Status -->
        <el-table-column label="帳戶啟用狀態" width="130">
          <template #default="{ row }">
            <el-switch
              v-model="row.enabled"
              inline-prompt
              active-text="啟用"
              inactive-text="停用"
              :loading="statusLoading[row.id]"
              @change="(val) => toggleUserStatus(row, val)"
            />
          </template>
        </el-table-column>

        <!-- Actions -->
        <el-table-column label="操作" width="100" align="right">
          <template #default="{ row }">
            <el-button size="small" @click="openEditDialog(row)">編輯</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- Edit User Dialog -->
    <el-dialog
      v-model="dialogVisible"
      title="修改會員權限資料"
      width="500px"
      destroy-on-close
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        class="space-y-4"
      >
        <el-form-item label="電子信箱" prop="email">
          <el-input v-model="form.email" />
        </el-form-item>

        <el-form-item label="姓名" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>

        <el-form-item label="手機號碼" prop="phone">
          <el-input v-model="form.phone" placeholder="0912-345678" />
        </el-form-item>

        <el-form-item label="系統權限角色" prop="role">
          <el-select v-model="form.role" placeholder="選擇角色" class="w-full">
            <el-option label="一般顧客 (CUSTOMER)" value="CUSTOMER" />
            <el-option label="商品管理員 (PRODUCT_MANAGER)" value="PRODUCT_MANAGER" />
            <el-option label="超級管理員 (SUPER_ADMIN)" value="SUPER_ADMIN" />
          </el-select>
        </el-form-item>

        <el-form-item label="帳戶重設密碼 (選填)" prop="password">
          <el-input v-model="form.password" placeholder="若無變更請留白,最少 8 碼" show-password />
        </el-form-item>

        <el-form-item label="帳號啟用狀態" prop="enabled">
          <el-switch v-model="form.enabled" active-text="啟用" inactive-text="停用" />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="flex justify-end gap-3 pt-4 border-t border-slate-100">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="saving" @click="handleSave">儲存變更</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useAuthStore } from '~/stores/auth'
import type { FormInstance, FormRules } from 'element-plus'

const authStore = useAuthStore()
const api = useApi()

const users = ref<any[]>([])
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const editingUserId = ref<number | null>(null)
const statusLoading = ref<Record<number, boolean>>({})

// 編輯會員資料表單
const form = reactive({
  email: '',
  name: '',
  phone: '',
  role: 'CUSTOMER',
  enabled: true,
  password: ''
})

const rules = reactive<FormRules>({
  email: [
    { required: true, message: '信箱不可為空', trigger: 'blur' },
    { type: 'email', message: '格式不正確', trigger: 'blur' }
  ],
  name: [{ required: true, message: '姓名不可為空', trigger: 'blur' }],
  role: [{ required: true, message: '請指派角色', trigger: 'change' }],
  password: [
    {
      validator: (rule: any, value: any, callback: any) => {
        if (value && value.length < 8) {
          callback(new Error('密碼最少需 8 個字元'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
})

// 防非超級管理員越權
onMounted(() => {
  if (!authStore.isSuperAdmin) {
    notify({
      title: '存取拒絕',
      message: '您非超級管理員,無權限存取此頁面.',
      type: 'error'
    })
    navigateTo('/')
  } else {
    fetchUsers()
  }
})

// 載入所有會員
const fetchUsers = async () => {
  loading.value = true
  try {
    const res = await api.request('/users', { method: 'GET' })
    if (res.success && Array.isArray(res.data)) {
      users.value = res.data
    }
  } catch (error) {
    console.error('載入會員清單出錯:', error)
  } finally {
    loading.value = false
  }
}

// 快速切換啟停用狀態
const toggleUserStatus = async (row: any, val: string | number | boolean) => {
  const isEnabled = !!val
  statusLoading.value[row.id] = true
  try {
    const payload = {
      email: row.email,
      name: row.name,
      phone: row.phone || null,
      role: row.role,
      enabled: isEnabled
    }
    
    const res = await api.request(`/users/${row.id}`, {
      method: 'PUT',
      body: payload
    })
    
    if (res.success) {
      notify({
        title: '狀態已變更',
        message: `會員 ${row.name} 帳戶已成功${isEnabled ? '啟用' : '停用'}.`,
        type: 'success',
        duration: 2500
      })
    }
  } catch (error: any) {
    notify({
      title: '變更失敗',
      message: error?.data?.message || '狀態變更時出錯.',
      type: 'error',
      duration: 3000
    })
    // 復原
    row.enabled = !isEnabled
  } finally {
    statusLoading.value[row.id] = false
  }
}

// 開啟編輯視窗
const openEditDialog = (row: any) => {
  editingUserId.value = row.id
  dialogVisible.value = true
  
  form.email = row.email
  form.name = row.name
  form.phone = row.phone || ''
  form.role = row.role
  form.enabled = row.enabled
  form.password = ''
}

// 儲存修改
const handleSave = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (valid && editingUserId.value) {
      saving.value = true
      
      const payload: Record<string, any> = {
        email: form.email,
        name: form.name,
        phone: form.phone.trim() || null,
        role: form.role,
        enabled: form.enabled
      }
      
      if (form.password) {
        payload.password = form.password
      }

      try {
        const res = await api.request(`/users/${editingUserId.value}`, {
          method: 'PUT',
          body: payload
        })
        
        if (res.success) {
          notify({
            title: '修改成功',
            message: `已儲存會員 ${form.name} 的修改資料.`,
            type: 'success',
            duration: 3000
          })
          dialogVisible.value = false
          
          // 如果修改的是自己,同步更新 authStore
          if (editingUserId.value === authStore.user?.id) {
            await authStore.fetchProfile()
          }
          
          fetchUsers()
        }
      } catch (error: any) {
        notify({
          title: '修改失敗',
          message: error?.data?.message || '請確認輸入資料是否重複或格式錯誤.',
          type: 'error',
          duration: 4000
        })
      } finally {
        saving.value = false
      }
    }
  })
}

// PII 遮罩
const maskPhone = (phone?: string) => {
  if (!phone) return ''
  const trimmed = phone.trim()
  if (trimmed.length <= 6) return '***'
  return trimmed.slice(0, 4) + '***' + trimmed.slice(-3)
}

// 角色標籤與翻譯
const getRoleLabel = (role: string) => {
  const map: Record<string, string> = {
    'CUSTOMER': '一般顧客',
    'PRODUCT_MANAGER': '商品管理員',
    'SUPER_ADMIN': '超級管理員',
  }
  return map[role] || role
}

const getRoleClass = (role: string) => {
  const map: Record<string, string> = {
    'CUSTOMER': 'bg-slate-50 text-slate-700 border-slate-200',
    'PRODUCT_MANAGER': 'bg-indigo-50 text-indigo-700 border-indigo-200',
    'SUPER_ADMIN': 'bg-rose-50 text-rose-700 border-rose-200',
  }
  return map[role] || 'bg-slate-50 text-slate-700 border-slate-200'
}
</script>
