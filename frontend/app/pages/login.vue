<template>
  <div class="flex items-center justify-center min-h-[70vh] px-4">
    <div class="w-full max-w-md bg-white border border-slate-200/80 rounded-2xl shadow-xl shadow-slate-100 p-8 space-y-6">
      <!-- Title -->
      <div class="text-center space-y-2">
        <h2 class="text-2xl font-bold tracking-tight text-slate-900">歡迎回來</h2>
        <p class="text-sm text-slate-500">
          登入以繼續探索優質商品並完成結帳
        </p>
      </div>

      <!-- Form -->
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        class="space-y-4"
        @keyup.enter="handleLogin"
      >
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
            placeholder="請輸入密碼"
            size="large"
            show-password
          />
        </el-form-item>

        <!-- Submit Button -->
        <div class="pt-2">
          <button
            type="button"
            :disabled="loading"
            class="w-full py-3 bg-indigo-600 hover:bg-indigo-700 text-white font-semibold rounded-lg shadow-md shadow-indigo-100 hover:shadow-lg transition-all cursor-pointer flex items-center justify-center disabled:opacity-50"
            @click="handleLogin"
          >
            <span v-if="loading" class="animate-spin mr-2 h-4 w-4 border-2 border-white border-t-transparent rounded-full"></span>
            登入會員
          </button>
        </div>
      </el-form>

      <!-- Redirect Link -->
      <div class="text-center text-sm text-slate-500 pt-2 border-t border-slate-100">
        還不是會員嗎?
        <NuxtLink to="/register" class="text-indigo-600 font-semibold hover:underline">
          立即註冊
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
  email: '',
  password: ''
})

// 表單驗證規則
const rules = reactive<FormRules>({
  email: [
    { required: true, message: '電子信箱為必填欄位', trigger: 'blur' },
    { type: 'email', message: '請輸入有效的電子信箱格式', trigger: ['blur', 'change'] }
  ],
  password: [
    { required: true, message: '密碼為必填欄位', trigger: 'blur' },
    { min: 8, message: '密碼長度最少需 8 個字元', trigger: 'blur' }
  ]
})

const handleLogin = async () => {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        const res = await authStore.login(form.email, form.password)
        if (res.success) {
          notify({
            title: '登入成功',
            message: `歡迎回來,${authStore.user?.name}!`,
            type: 'success',
            duration: 3000,
          })
          
          // 登入成功後跳轉至首頁或上一頁
          navigateTo('/')
        }
      } catch (error: any) {
        notify({
          title: '登入失敗',
          message: error?.data?.message || '信箱或密碼錯誤,請重新再試.',
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
