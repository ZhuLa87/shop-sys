<template>
  <div class="flex items-center justify-center min-h-[70vh] px-4">
    <div class="w-full max-w-lg bg-white border border-slate-200/80 rounded-2xl shadow-xl shadow-slate-100 p-8 space-y-6">
      <!-- Title -->
      <div class="space-y-1">
        <h2 class="text-2xl font-bold tracking-tight text-slate-900">個人基本資料</h2>
        <p class="text-sm text-slate-500">檢視與更新您的帳戶資料與安全性密碼</p>
      </div>

      <!-- Loading State -->
      <div v-if="loading" class="space-y-4 animate-pulse">
        <div class="h-10 bg-slate-100 rounded w-full"></div>
        <div class="h-10 bg-slate-100 rounded w-full"></div>
        <div class="h-10 bg-slate-100 rounded w-full"></div>
      </div>

      <!-- Profile Form -->
      <el-form
        v-else
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        class="space-y-4"
      >
        <el-form-item label="登入電子信箱" prop="email">
          <el-input v-model="form.email" placeholder="example@test.com" />
        </el-form-item>

        <el-form-item label="顯示名稱 (姓名)" prop="name">
          <el-input v-model="form.name" placeholder="請輸入姓名" />
        </el-form-item>

        <el-form-item label="聯絡電話" prop="phone">
          <el-input v-model="form.phone" placeholder="0912-345678" />
        </el-form-item>

        <el-form-item label="變更新密碼 (選填)" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="若不修改請留空,修改最少 8 碼"
            show-password
          />
        </el-form-item>

        <!-- Submit -->
        <div class="pt-4 border-t border-slate-100 flex items-center justify-between gap-4">
          <span class="text-xs text-slate-400">
            帳戶建立於:{{ formatDate(authStore.user?.createdAt) }}
          </span>
          
          <button
            type="button"
            :disabled="saving"
            class="px-6 py-2.5 bg-indigo-600 hover:bg-indigo-700 text-white font-semibold text-sm rounded-lg shadow-md shadow-indigo-100 hover:shadow-lg transition-all cursor-pointer flex items-center justify-center disabled:opacity-50"
            @click="handleSave"
          >
            <span v-if="saving" class="animate-spin mr-2 h-4 w-4 border-2 border-white border-t-transparent rounded-full"></span>
            儲存變更
          </button>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useAuthStore } from '~/stores/auth'
import type { FormInstance, FormRules } from 'element-plus'

const authStore = useAuthStore()

const loading = ref(true)
const saving = ref(false)
const formRef = ref<FormInstance>()

const form = reactive({
  email: '',
  name: '',
  phone: '',
  password: ''
})

// 表單驗證規則
const rules = reactive<FormRules>({
  email: [
    { required: true, message: '電子信箱為必填欄位', trigger: 'blur' },
    { type: 'email', message: '請輸入有效的電子信箱格式', trigger: ['blur', 'change'] }
  ],
  name: [
    { required: true, message: '顯示名稱不可為空', trigger: 'blur' },
    { min: 2, message: '顯示名稱至少 2 個字元', trigger: 'blur' }
  ],
  password: [
    // 選填,但如果有填,最少要 8 碼
    {
      validator: (rule: any, value: any, callback: any) => {
        if (value && value.length < 8) {
          callback(new Error('密碼長度最少需 8 個字元'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
})

// 載入最新個人資料
onMounted(async () => {
  loading.value = true
  if (authStore.isAuthenticated) {
    await authStore.fetchProfile()
    if (authStore.user) {
      form.email = authStore.user.email
      form.name = authStore.user.name
      form.phone = authStore.user.phone || ''
    }
  }
  loading.value = false
})

// 儲存更新
const handleSave = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (valid) {
      saving.value = true
      try {
        const payload: Record<string, any> = {
          email: form.email,
          name: form.name,
          phone: form.phone.trim() || null
        }
        
        // 只有在填入新密碼時才送出變更
        if (form.password) {
          payload.password = form.password
        }
        
        const res = await authStore.updateProfile(payload)
        if (res.success) {
          notify({
            title: '儲存成功',
            message: '個人資料已成功更新.',
            type: 'success',
            duration: 3000
          })
          form.password = '' // 清空密碼欄位
        }
      } catch (error: any) {
        notify({
          title: '儲存失敗',
          message: error?.data?.message || '電子信箱可能重複,請重試.',
          type: 'error',
          duration: 4000
        })
      } finally {
        saving.value = false
      }
    }
  })
}

// 格式化日期
const formatDate = (timestamp?: number) => {
  if (!timestamp) return '-'
  return new Date(timestamp).toLocaleDateString('zh-TW', {
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  })
}
</script>
