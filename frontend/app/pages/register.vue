<template>
  <div class="flex items-center justify-center min-h-[75vh] px-4">
    <div class="w-full max-w-md bg-white border border-slate-200/80 rounded-2xl shadow-xl shadow-slate-100 p-8 space-y-6">
      <!-- Title -->
      <div class="text-center space-y-2">
        <h2 class="text-2xl font-bold tracking-tight text-slate-900">加入 ShopSys 会员</h2>
        <p class="text-sm text-slate-500">
          只需幾秒鐘即可完成註冊,開始您的購物之旅
        </p>
      </div>

      <!-- Form -->
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        class="space-y-4"
        @keyup.enter="handleRegister"
      >
        <el-form-item label="會員姓名" prop="name">
          <el-input
            v-model="form.name"
            placeholder="請輸入您的真實姓名"
            size="large"
          />
        </el-form-item>

        <el-form-item label="電子信箱" prop="email">
          <el-input
            v-model="form.email"
            placeholder="example@test.com"
            size="large"
          />
        </el-form-item>

        <el-form-item label="密碼" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="最少 8 個字元"
            size="large"
            show-password
          />
        </el-form-item>

        <el-form-item label="聯絡電話 (選填)" prop="phone">
          <el-input
            v-model="form.phone"
            placeholder="0912345678"
            size="large"
          />
        </el-form-item>

        <!-- Submit Button -->
        <div class="pt-2">
          <button
            type="button"
            :disabled="loading"
            class="w-full py-3 bg-indigo-600 hover:bg-indigo-700 text-white font-semibold rounded-lg shadow-md shadow-indigo-100 hover:shadow-lg transition-all cursor-pointer flex items-center justify-center disabled:opacity-50"
            @click="handleRegister"
          >
            <span v-if="loading" class="animate-spin mr-2 h-4 w-4 border-2 border-white border-t-transparent rounded-full"></span>
            註冊新會員
          </button>
        </div>
      </el-form>

      <!-- Redirect Link -->
      <div class="text-center text-sm text-slate-500 pt-2 border-t border-slate-100">
        已經是會員了?
        <NuxtLink to="/login" class="text-indigo-600 font-semibold hover:underline">
          立即登入
        </NuxtLink>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { useAuthStore } from '~/stores/auth'

const authStore = useAuthStore()

const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  name: '',
  email: '',
  password: '',
  phone: ''
})

// 表單驗證規則
const rules = reactive<FormRules>({
  name: [
    { required: true, message: '姓名為必填欄位', trigger: 'blur' },
    { min: 2, message: '姓名長度最少需 2 個字元', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '電子信箱為必填欄位', trigger: 'blur' },
    { type: 'email', message: '請輸入有效的電子信箱格式', trigger: ['blur', 'change'] }
  ],
  password: [
    { required: true, message: '密碼為必填欄位', trigger: 'blur' },
    { min: 8, message: '密碼長度最少需 8 個字元', trigger: 'blur' }
  ],
  phone: [
    { pattern: /^[0-9\-+\s()]*$/, message: '請輸入有效的電話號碼', trigger: 'blur' }
  ]
})

const handleRegister = async () => {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        const payload = {
          email: form.email,
          password: form.password,
          name: form.name,
          phone: form.phone.trim() || null
        }
        
        const res = await authStore.register(payload)
        if (res.success) {
          notify({
            title: '註冊成功',
            message: '恭喜您註冊成功!系統已為您導向登入頁面.',
            type: 'success',
            duration: 3000,
          })
          navigateTo('/login')
        }
      } catch (error: any) {
        notify({
          title: '註冊失敗',
          message: error?.data?.message || '電子信箱可能已被註冊,請換一個信箱再試.',
          type: 'error',
          duration: 4000,
        })
      } finally {
        loading.value = false
      }
    }
  })
}
</script>
