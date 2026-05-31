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
        <div class="flex justify-center"><div class="w-24 h-24 bg-slate-100 rounded-full"></div></div>
        <div class="h-10 bg-slate-100 rounded w-full"></div>
        <div class="h-10 bg-slate-100 rounded w-full"></div>
      </div>

      <template v-else>
        <!-- Avatar Section -->
        <div class="flex flex-col items-center gap-3 pb-4 border-b border-slate-100">
          <!-- Avatar Preview -->
          <div class="relative group">
            <div class="w-24 h-24 rounded-full overflow-hidden border-4 border-white shadow-lg shadow-slate-200 bg-indigo-50">
              <img
                v-if="authStore.user?.avatarUrl"
                :src="authStore.user.avatarUrl"
                class="w-full h-full object-cover"
                alt="頭像"
              />
              <div v-else class="w-full h-full flex items-center justify-center text-3xl font-bold text-indigo-600 select-none">
                {{ authStore.user?.name?.[0]?.toUpperCase() || 'U' }}
              </div>
            </div>
            <!-- Hover overlay -->
            <label
              :class="uploadingAvatar ? 'opacity-60 cursor-not-allowed pointer-events-none' : 'cursor-pointer'"
              class="absolute inset-0 rounded-full bg-black/40 flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity"
            >
              <input
                type="file"
                accept="image/jpeg,image/png,image/webp,image/gif"
                class="sr-only"
                :disabled="uploadingAvatar"
                @change="handleAvatarFileSelect"
              />
              <svg v-if="!uploadingAvatar" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="w-6 h-6 text-white">
                <path stroke-linecap="round" stroke-linejoin="round" d="M6.827 6.175A2.31 2.31 0 0 1 5.186 7.23c-.38.054-.757.112-1.134.175C2.999 7.58 2.25 8.507 2.25 9.574V18a2.25 2.25 0 0 0 2.25 2.25h15A2.25 2.25 0 0 0 21.75 18V9.574c0-1.067-.75-1.994-1.802-2.169a47.865 47.865 0 0 0-1.134-.175 2.31 2.31 0 0 1-1.64-1.055l-.822-1.316a2.192 2.192 0 0 0-1.736-1.039 48.774 48.774 0 0 0-5.232 0 2.192 2.192 0 0 0-1.736 1.039l-.821 1.316Z" />
                <path stroke-linecap="round" stroke-linejoin="round" d="M16.5 12.75a4.5 4.5 0 1 1-9 0 4.5 4.5 0 0 1 9 0ZM18.75 10.5h.008v.008h-.008V10.5Z" />
              </svg>
              <span v-else class="animate-spin h-5 w-5 border-2 border-white border-t-transparent rounded-full" />
            </label>
          </div>
          <p class="text-xs text-slate-400">點擊頭像更換</p>
        </div>

        <!-- Profile Form -->
        <el-form
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
              帳戶建立於: {{ formatDate(authStore.user?.createdAt) }}
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
      </template>
    </div>
  </div>

  <!-- Avatar Cropper Dialog -->
  <ImageCropperDialog
    v-model="cropperVisible"
    :image-src="pendingAvatarSrc"
    :aspect-ratio="1"
    :output-size="400"
    @confirm="handleAvatarCropConfirm"
  />
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useAuthStore } from '~/stores/auth'
import type { FormInstance, FormRules } from 'element-plus'

const authStore = useAuthStore()
const upload = useUpload()

const loading = ref(true)
const saving = ref(false)
const uploadingAvatar = ref(false)
const cropperVisible = ref(false)
const pendingAvatarSrc = ref('')
const formRef = ref<FormInstance>()

const form = reactive({
  email: '',
  name: '',
  phone: '',
  password: '',
})

const rules = reactive<FormRules>({
  email: [
    { required: true, message: '電子信箱為必填欄位', trigger: 'blur' },
    { type: 'email', message: '請輸入有效的電子信箱格式', trigger: ['blur', 'change'] },
  ],
  name: [
    { required: true, message: '顯示名稱不可為空', trigger: 'blur' },
    { min: 2, message: '顯示名稱至少 2 個字元', trigger: 'blur' },
  ],
  password: [
    {
      validator: (_rule: any, value: any, callback: any) => {
        if (value && value.length < 8) callback(new Error('密碼長度最少需 8 個字元'))
        else callback()
      },
      trigger: 'blur',
    },
  ],
})

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

// 選擇頭像檔案後開啟裁切器
const handleAvatarFileSelect = (event: Event) => {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  input.value = ''

  const reader = new FileReader()
  reader.onload = (e) => {
    pendingAvatarSrc.value = e.target?.result as string
    cropperVisible.value = true
  }
  reader.readAsDataURL(file)
}

// 裁切完成後上傳
const handleAvatarCropConfirm = async (blob: Blob) => {
  if (!authStore.user?.id) return
  uploadingAvatar.value = true
  try {
    const avatarUrl = await upload.uploadAvatar(blob, authStore.user.id)
    // 更新後端資料並刷新 store
    await authStore.updateProfile({ avatarUrl })
    notify({ title: '頭像已更新', message: '新頭像已成功上傳並儲存.', type: 'success', duration: 3000 })
  } catch (error: any) {
    notify({ title: '頭像上傳失敗', message: error.message || '請稍後再試.', type: 'error', duration: 4000 })
  } finally {
    uploadingAvatar.value = false
  }
}

const handleSave = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    saving.value = true
    try {
      const payload: Record<string, any> = {
        email: form.email,
        name: form.name,
        phone: form.phone.trim() || null,
      }
      if (form.password) payload.password = form.password

      const res = await authStore.updateProfile(payload)
      if (res.success) {
        notify({ title: '儲存成功', message: '個人資料已成功更新.', type: 'success', duration: 3000 })
        form.password = ''
      }
    } catch (error: any) {
      notify({ title: '儲存失敗', message: error?.data?.message || '電子信箱可能重複,請重試.', type: 'error', duration: 4000 })
    } finally {
      saving.value = false
    }
  })
}

const formatDate = (timestamp?: number) => {
  if (!timestamp) return '-'
  return new Date(timestamp).toLocaleDateString('zh-TW', { year: 'numeric', month: 'long', day: 'numeric' })
}
</script>
