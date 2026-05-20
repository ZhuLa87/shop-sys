<template>
  <div class="min-h-screen bg-slate-50 font-sans text-slate-900 selection:bg-indigo-100 selection:text-indigo-900">
    <NuxtLayout>
      <NuxtPage />
    </NuxtLayout>
  </div>
</template>

<script setup lang="ts">
import { useAuthStore } from '~/stores/auth'
import { useCartStore } from '~/stores/cart'

const authStore = useAuthStore()
const cartStore = useCartStore()

// 在客戶端初始化時載入個人資料與購物車數據
onMounted(async () => {
  if (authStore.isAuthenticated) {
    await authStore.fetchProfile()
    await cartStore.fetchCart()
  }
})
</script>
