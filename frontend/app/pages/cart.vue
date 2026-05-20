<template>
  <div class="space-y-8">
    <h1 class="text-2xl sm:text-3xl font-extrabold text-slate-900 tracking-tight">您的購物車</h1>

    <div v-if="loading" class="grid grid-cols-1 lg:grid-cols-3 gap-8">
      <div class="lg:col-span-2 bg-white border border-slate-100 rounded-2xl h-80 animate-pulse"></div>
      <div class="bg-white border border-slate-100 rounded-2xl h-80 animate-pulse"></div>
    </div>

    <div v-else-if="cartStore.items.length === 0 && step !== 4" class="flex flex-col items-center justify-center py-20 text-center space-y-4 bg-white border border-slate-200/80 rounded-2xl p-8">
      <div class="w-16 h-16 rounded-full bg-slate-50 flex items-center justify-center text-slate-400">
        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-8 h-8">
          <path stroke-linecap="round" stroke-linejoin="round" d="M15.75 10.5V6a3.75 3.75 0 1 0-7.5 0v4.5m11.356-1.993 1.263 12c.07.665-.45 1.243-1.119 1.243H4.25a1.125 1.125 0 0 1-1.12-1.243l1.264-12A1.125 1.125 0 0 1 5.513 7.5h12.974c.576 0 1.059.435 1.119 1.007ZM8.625 10.5a.375.375 0 1 1-.75 0 .375.375 0 0 1 .75 0Zm7.5 0a.375.375 0 1 1-.75 0 .375.375 0 0 1 .75 0Z" />
        </svg>
      </div>
      <div class="space-y-1">
        <h3 class="font-bold text-slate-800">購物車是空的</h3>
        <p class="text-sm text-slate-400">去挑選一些喜歡的商品吧!</p>
      </div>
      <NuxtLink to="/" class="rounded-lg bg-indigo-600 px-6 py-2.5 text-sm font-semibold text-white shadow-md hover:bg-indigo-700 transition-colors">
        開始購物
      </NuxtLink>
    </div>

    <div v-else class="space-y-8">
      <!-- Step indicator -->
      <el-steps :active="step - 1" finish-status="success" align-center>
        <el-step title="確認購物車" />
        <el-step title="寄送資訊" />
        <el-step title="付款資訊" />
        <el-step title="完成" />
      </el-steps>

      <div class="grid grid-cols-1 lg:grid-cols-3 gap-8 items-start">
        <!-- Left: step content -->
        <div class="lg:col-span-2">

          <!-- Step 1: Cart items -->
          <div v-if="step === 1" class="bg-white border border-slate-200/80 rounded-2xl overflow-hidden p-4 sm:p-6 space-y-4">
            <div
              v-for="item in cartStore.items"
              :key="item.id"
              class="flex items-center gap-4 py-4 border-b border-slate-100 last:border-0 last:pb-0 first:pt-0"
            >
              <div class="w-20 h-20 bg-slate-50 border border-slate-100 rounded-lg overflow-hidden shrink-0">
                <img :src="item.coverImageUrl || 'https://picsum.photos/seed/default/200/200'" class="w-full h-full object-cover" />
              </div>
              <div class="flex-grow min-w-0 space-y-1">
                <NuxtLink :to="`/products/${item.productId}`" class="font-bold text-slate-800 text-sm hover:text-indigo-600 transition-colors line-clamp-1 block">
                  {{ item.productName }}
                </NuxtLink>
                <div class="text-xs text-slate-500">單價: NT$ {{ formatPrice(item.price) }}</div>
                <div class="sm:hidden text-sm font-extrabold text-slate-900 mt-1">
                  小計: NT$ {{ formatPrice(item.subtotal) }}
                </div>
              </div>
              <div class="flex flex-col sm:flex-row items-end sm:items-center gap-2 sm:gap-6 shrink-0">
                <el-input-number
                  v-model="item.quantity"
                  :min="1"
                  size="small"
                  class="w-24"
                  :disabled="updatingItems[item.id]"
                  @change="(val) => handleQuantityChange(item, val)"
                />
                <div class="hidden sm:block text-sm font-extrabold text-slate-900 w-24 text-right">
                  NT$ {{ formatPrice(item.subtotal) }}
                </div>
                <button
                  class="p-1.5 text-slate-400 hover:text-rose-600 hover:bg-rose-50 rounded-lg transition-colors cursor-pointer"
                  title="刪除商品"
                  @click="handleRemoveItem(item.id, item.productName)"
                >
                  <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="w-4 h-4">
                    <path stroke-linecap="round" stroke-linejoin="round" d="m14.74 9-.346 9m-4.788 0L9.26 9m9.968-3.21c.342.052.682.107 1.022.166m-1.022-.165L18.16 19.673a2.25 2.25 0 0 1-2.244 2.077H8.084a2.25 2.25 0 0 1-2.244-2.077L4.772 5.79m14.456 0a48.108 48.108 0 0 0-3.478-.397m-12 .562c.34-.059.68-.114 1.022-.165m0 0a48.11 48.11 0 0 1 3.478-.397m7.5 0v-.916c0-1.18-.91-2.164-2.09-2.201a51.964 51.964 0 0 0-3.32 0c-1.18.037-2.09 1.022-2.09 2.201v.916m7.5 0a48.667 48.667 0 0 0-7.5 0" />
                  </svg>
                </button>
              </div>
            </div>

            <div class="pt-2 border-t border-slate-100">
              <button
                class="w-full py-3 bg-indigo-600 hover:bg-indigo-700 text-white font-semibold rounded-lg shadow-md shadow-indigo-100 transition-all cursor-pointer flex items-center justify-center gap-2"
                @click="step = 2"
              >
                下一步:填寫寄送資訊
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="w-4 h-4">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M13.5 4.5 21 12m0 0-7.5 7.5M21 12H3" />
                </svg>
              </button>
            </div>
          </div>

          <!-- Step 2: Shipping info -->
          <div v-else-if="step === 2" class="bg-white border border-slate-200/80 rounded-2xl p-6 space-y-6 shadow-sm">
            <h3 class="font-bold text-slate-900 border-b border-slate-100 pb-3">收件人資訊</h3>
            <el-form
              ref="shippingFormRef"
              :model="shippingForm"
              :rules="shippingRules"
              label-position="top"
            >
              <el-form-item label="收件人姓名" prop="recipientName">
                <el-input v-model="shippingForm.recipientName" placeholder="請輸入收件人真實姓名" />
              </el-form-item>
              <el-form-item label="收件人電話" prop="recipientPhone">
                <el-input v-model="shippingForm.recipientPhone" placeholder="0912-345678" />
              </el-form-item>
              <el-form-item label="收件地址" prop="recipientAddress">
                <el-input v-model="shippingForm.recipientAddress" placeholder="請輸入完整配送地址" />
              </el-form-item>
            </el-form>
            <div class="flex gap-3 pt-2">
              <button
                class="flex-1 py-3 bg-white border border-slate-300 text-slate-700 font-semibold rounded-lg hover:bg-slate-50 transition-colors cursor-pointer flex items-center justify-center gap-2"
                @click="step = 1"
              >
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="w-4 h-4">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M10.5 19.5 3 12m0 0 7.5-7.5M3 12h18" />
                </svg>
                上一步
              </button>
              <button
                class="flex-1 py-3 bg-indigo-600 hover:bg-indigo-700 text-white font-semibold rounded-lg shadow-md shadow-indigo-100 transition-all cursor-pointer flex items-center justify-center gap-2"
                @click="goToStep3"
              >
                下一步:填寫付款資訊
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="w-4 h-4">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M13.5 4.5 21 12m0 0-7.5 7.5M21 12H3" />
                </svg>
              </button>
            </div>
          </div>

          <!-- Step 3: Payment info -->
          <div v-else-if="step === 3" class="bg-white border border-slate-200/80 rounded-2xl p-6 space-y-6 shadow-sm">
            <h3 class="font-bold text-slate-900 border-b border-slate-100 pb-3">選擇付款方式</h3>
            <div class="space-y-3">
              <div
                v-for="method in paymentMethods"
                :key="method.value"
                class="flex items-center gap-4 p-4 border-2 rounded-xl cursor-pointer transition-all"
                :class="paymentMethod === method.value ? 'border-indigo-500 bg-indigo-50' : 'border-slate-200 hover:border-slate-300 bg-white'"
                @click="paymentMethod = method.value"
              >
                <div
                  class="w-10 h-10 rounded-lg flex items-center justify-center shrink-0"
                  :class="paymentMethod === method.value ? 'bg-indigo-100 text-indigo-600' : 'bg-slate-100 text-slate-500'"
                >
                  <component :is="method.icon" class="w-5 h-5" />
                </div>
                <div class="flex-grow">
                  <div class="font-semibold text-slate-800 text-sm">{{ method.label }}</div>
                  <div class="text-xs text-slate-500 mt-0.5">{{ method.description }}</div>
                </div>
                <div
                  class="w-5 h-5 rounded-full border-2 flex items-center justify-center shrink-0 transition-all"
                  :class="paymentMethod === method.value ? 'border-indigo-500 bg-indigo-500' : 'border-slate-300'"
                >
                  <div v-if="paymentMethod === method.value" class="w-2 h-2 rounded-full bg-white"></div>
                </div>
              </div>
            </div>

            <div class="flex gap-3 pt-2">
              <button
                class="flex-1 py-3 bg-white border border-slate-300 text-slate-700 font-semibold rounded-lg hover:bg-slate-50 transition-colors cursor-pointer flex items-center justify-center gap-2"
                @click="step = 2"
              >
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="w-4 h-4">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M10.5 19.5 3 12m0 0 7.5-7.5M3 12h18" />
                </svg>
                上一步
              </button>
              <button
                type="button"
                :disabled="submitting"
                class="flex-1 py-3 bg-indigo-600 hover:bg-indigo-700 text-white font-semibold rounded-lg shadow-md shadow-indigo-100 transition-all cursor-pointer flex items-center justify-center gap-2 disabled:opacity-50"
                @click="handleCheckout"
              >
                <span v-if="submitting" class="animate-spin h-4 w-4 border-2 border-white border-t-transparent rounded-full"></span>
                確認送出訂單
              </button>
            </div>
          </div>
          <!-- Step 4: Complete -->
          <div v-else-if="step === 4" class="lg:col-span-2">
            <div class="bg-white border border-slate-200/80 rounded-2xl p-8 sm:p-12 text-center space-y-8 shadow-sm">
              <div class="w-20 h-20 rounded-full bg-emerald-50 flex items-center justify-center mx-auto">
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="w-10 h-10 text-emerald-500">
                  <path stroke-linecap="round" stroke-linejoin="round" d="m4.5 12.75 6 6 9-13.5" />
                </svg>
              </div>

              <div class="space-y-2">
                <h2 class="text-2xl font-extrabold text-slate-900">訂單已成功送出!</h2>
                <p class="text-slate-500 text-sm">感謝您的購買,我們將盡快為您處理.</p>
                <p class="text-xs text-slate-400 font-mono mt-1">訂單編號:#{{ completedOrderId }}</p>
              </div>

              <div class="text-left bg-slate-50 border border-slate-100 rounded-xl p-5 space-y-4">
                <div class="flex justify-between text-sm">
                  <span class="text-slate-500">購買商品</span>
                  <span class="font-semibold text-slate-800">{{ completedItemCount }} 件</span>
                </div>
                <div class="flex justify-between text-sm">
                  <span class="text-slate-500">收件人</span>
                  <span class="font-semibold text-slate-800">{{ shippingForm.recipientName }}</span>
                </div>
                <div class="flex justify-between text-sm">
                  <span class="text-slate-500">收件地址</span>
                  <span class="font-semibold text-slate-800 text-right max-w-[60%]">{{ shippingForm.recipientAddress }}</span>
                </div>
                <div class="flex justify-between text-sm">
                  <span class="text-slate-500">付款方式</span>
                  <span class="font-semibold text-slate-800">{{ paymentMethods.find(m => m.value === paymentMethod)?.label }}</span>
                </div>
                <div class="flex justify-between text-base font-bold border-t border-slate-200 pt-4">
                  <span class="text-slate-900">訂單總額</span>
                  <span class="text-indigo-600">NT$ {{ formatPrice(completedAmount) }}</span>
                </div>
              </div>

              <div class="flex flex-col sm:flex-row gap-3 pt-2">
                <NuxtLink
                  :to="`/orders/${completedOrderId}`"
                  class="flex-1 py-3 bg-indigo-600 hover:bg-indigo-700 text-white font-semibold rounded-lg shadow-md shadow-indigo-100 transition-all flex items-center justify-center gap-2"
                >
                  <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-4 h-4">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M9 12h3.75M9 15h3.75M9 18h3.75m3 .75H18a2.25 2.25 0 0 0 2.25-2.25V6.108c0-1.135-.845-2.098-1.976-2.192a48.424 48.424 0 0 0-1.123-.08m-5.801 0c-.065.21-.1.433-.1.664 0 .414.336.75.75.75h4.5a.75.75 0 0 0 .75-.75 2.25 2.25 0 0 0-.1-.664m-5.8 0A2.251 2.251 0 0 1 13.5 2.25H15c1.012 0 1.867.668 2.15 1.586m-5.8 0c-.376.023-.75.05-1.124.08C9.095 4.01 8.25 4.973 8.25 6.108V8.25m0 0H4.875c-.621 0-1.125.504-1.125 1.125v11.25c0 .621.504 1.125 1.125 1.125h9.75c.621 0 1.125-.504 1.125-1.125V9.375c0-.621-.504-1.125-1.125-1.125H8.25ZM6.75 12h.008v.008H6.75V12Zm0 3h.008v.008H6.75V15Zm0 3h.008v.008H6.75V18Z" />
                  </svg>
                  查看訂單詳情
                </NuxtLink>
                <NuxtLink
                  to="/"
                  class="flex-1 py-3 bg-white border border-slate-300 text-slate-700 font-semibold rounded-lg hover:bg-slate-50 transition-colors flex items-center justify-center gap-2"
                >
                  <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-4 h-4">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M15.75 10.5V6a3.75 3.75 0 1 0-7.5 0v4.5m11.356-1.993 1.263 12c.07.665-.45 1.243-1.119 1.243H4.25a1.125 1.125 0 0 1-1.12-1.243l1.264-12A1.125 1.125 0 0 1 5.513 7.5h12.974c.576 0 1.059.435 1.119 1.007Z" />
                  </svg>
                  繼續購物
                </NuxtLink>
              </div>
            </div>
          </div>
        </div>

        <!-- Right: Order summary (hidden on step 4) -->
        <div v-if="step < 4" class="space-y-4">
          <div class="bg-white border border-slate-200/80 rounded-2xl p-6 space-y-4 shadow-sm">
            <h3 class="font-bold text-slate-900 border-b border-slate-100 pb-3">訂單摘要</h3>
            <div class="space-y-2 text-sm">
              <div class="flex justify-between text-slate-600">
                <span>商品總數量</span>
                <span>{{ cartStore.totalCount }} 件</span>
              </div>
              <div class="flex justify-between text-slate-600">
                <span>運費</span>
                <span class="text-emerald-600 font-semibold">免運費</span>
              </div>
              <div class="flex justify-between text-base font-bold text-slate-900 border-t border-slate-100 pt-3">
                <span>結帳總額</span>
                <span class="text-indigo-600">NT$ {{ formatPrice(cartStore.totalAmount) }}</span>
              </div>
            </div>
          </div>

          <!-- Shipping summary shown on step 3 -->
          <div v-if="step === 3" class="bg-white border border-slate-200/80 rounded-2xl p-6 space-y-3 shadow-sm">
            <h3 class="font-bold text-slate-900 border-b border-slate-100 pb-3">寄送資訊確認</h3>
            <div class="space-y-2 text-sm text-slate-600">
              <div class="flex gap-2">
                <span class="text-slate-400 shrink-0">收件人</span>
                <span class="font-medium text-slate-800">{{ shippingForm.recipientName }}</span>
              </div>
              <div class="flex gap-2">
                <span class="text-slate-400 shrink-0">電話</span>
                <span class="font-medium text-slate-800">{{ shippingForm.recipientPhone }}</span>
              </div>
              <div class="flex gap-2">
                <span class="text-slate-400 shrink-0">地址</span>
                <span class="font-medium text-slate-800">{{ shippingForm.recipientAddress }}</span>
              </div>
            </div>
            <button class="text-xs text-indigo-600 hover:underline cursor-pointer mt-1" @click="step = 2">
              修改寄送資訊
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, h } from 'vue'
import { useCartStore } from '~/stores/cart'
import { ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'

const cartStore = useCartStore()
const api = useApi()

const loading = ref(false)
const submitting = ref(false)
const step = ref(1)
const completedOrderId = ref<number | null>(null)
const completedAmount = ref(0)
const completedItemCount = ref(0)
const shippingFormRef = ref<FormInstance>()
const updatingItems = ref<Record<number, boolean>>({})

const shippingForm = reactive({
  recipientName: '',
  recipientPhone: '',
  recipientAddress: ''
})

const paymentMethod = ref('cod')

const paymentMethods = [
  {
    value: 'cod',
    label: '貨到付款',
    description: '商品送達時以現金支付',
    icon: h('svg', { xmlns: 'http://www.w3.org/2000/svg', fill: 'none', viewBox: '0 0 24 24', 'stroke-width': '1.5', stroke: 'currentColor' }, [
      h('path', { 'stroke-linecap': 'round', 'stroke-linejoin': 'round', d: 'M2.25 18.75a60.07 60.07 0 0 1 15.797 2.101c.727.198 1.453-.342 1.453-1.096V18.75M3.75 4.5v.75A.75.75 0 0 1 3 6h-.75m0 0v-.375c0-.621.504-1.125 1.125-1.125H20.25M2.25 6v9m18-10.5v.75c0 .414.336.75.75.75h.75m-1.5-1.5h.375c.621 0 1.125.504 1.125 1.125v9.75c0 .621-.504 1.125-1.125 1.125h-.375m1.5-1.5H21a.75.75 0 0 0-.75.75v.75m0 0H3.75m0 0h-.375a1.125 1.125 0 0 1-1.125-1.125V15m1.5 1.5v-.75A.75.75 0 0 0 3 15h-.75M15 10.5a3 3 0 1 1-6 0 3 3 0 0 1 6 0Zm3 0h.008v.008H18V10.5Zm-12 0h.008v.008H6V10.5Z' })
    ])
  },
  {
    value: 'credit_card',
    label: '信用卡付款',
    description: 'Visa,Mastercard,JCB',
    icon: h('svg', { xmlns: 'http://www.w3.org/2000/svg', fill: 'none', viewBox: '0 0 24 24', 'stroke-width': '1.5', stroke: 'currentColor' }, [
      h('path', { 'stroke-linecap': 'round', 'stroke-linejoin': 'round', d: 'M2.25 8.25h19.5M2.25 9h19.5m-16.5 5.25h6m-6 2.25h3m-3.75 3h15a2.25 2.25 0 0 0 2.25-2.25V6.75A2.25 2.25 0 0 0 19.5 4.5h-15a2.25 2.25 0 0 0-2.25 2.25v10.5A2.25 2.25 0 0 0 4.5 19.5Z' })
    ])
  },
  {
    value: 'atm',
    label: 'ATM 轉帳',
    description: '取得虛擬帳號後於期限內完成轉帳',
    icon: h('svg', { xmlns: 'http://www.w3.org/2000/svg', fill: 'none', viewBox: '0 0 24 24', 'stroke-width': '1.5', stroke: 'currentColor' }, [
      h('path', { 'stroke-linecap': 'round', 'stroke-linejoin': 'round', d: 'M7.5 21 3 16.5m0 0L7.5 12M3 16.5h13.5m0-13.5L21 7.5m0 0L16.5 12M21 7.5H7.5' })
    ])
  }
]

const shippingRules = reactive<FormRules>({
  recipientName: [
    { required: true, message: '收件人姓名不可為空', trigger: 'blur' },
    { min: 2, message: '名字過短', trigger: 'blur' }
  ],
  recipientPhone: [
    { required: true, message: '收件人聯絡電話不可為空', trigger: 'blur' },
    { pattern: /^[0-9\-+\s()]*$/, message: '請輸入有效的電話號碼格式', trigger: 'blur' }
  ],
  recipientAddress: [
    { required: true, message: '收件地址不可為空', trigger: 'blur' },
    { min: 5, message: '請輸入更詳細的地址', trigger: 'blur' }
  ]
})

onMounted(async () => {
  loading.value = true
  await cartStore.fetchCart()
  loading.value = false
})

const goToStep3 = async () => {
  if (!shippingFormRef.value) return
  await shippingFormRef.value.validate((valid) => {
    if (valid) step.value = 3
  })
}

const handleQuantityChange = async (item: any, targetVal: number | undefined) => {
  if (targetVal === undefined || targetVal < 1) return
  updatingItems.value[item.id] = true
  try {
    await cartStore.updateCartItemQuantity(item.productId, targetVal, item.id)
    notify({
      title: '購物車已更新',
      message: '商品數量已變更.',
      type: 'success',
      position: 'bottom-right',
      duration: 2000
    })
  } catch (error: any) {
    notify({
      title: '更新失敗',
      message: error?.data?.message || '庫存不足或系統錯誤.',
      type: 'error',
      duration: 3000
    })
    await cartStore.fetchCart()
  } finally {
    updatingItems.value[item.id] = false
  }
}

const handleRemoveItem = async (cartItemId: number, productName: string) => {
  try {
    await ElMessageBox.confirm(
      `確定要將 ${productName} 從購物車中移出嗎?`,
      '提示',
      { confirmButtonText: '確定', cancelButtonText: '取消', type: 'warning' }
    )
    await cartStore.removeFromCart(cartItemId)
    notify({
      title: '已移除商品',
      message: `${productName} 已從您的購物車中移除.`,
      type: 'info',
      duration: 2500
    })
  } catch (error: any) {
    if (error !== 'cancel') {
      notify({
        title: '移除失敗',
        message: '系統忙碌中,請稍後再試.',
        type: 'error',
        duration: 3000
      })
    }
  }
}

const handleCheckout = async () => {
  submitting.value = true
  try {
    const res = await api.request('/orders', {
      method: 'POST',
      body: {
        recipientName: shippingForm.recipientName,
        recipientPhone: shippingForm.recipientPhone,
        recipientAddress: shippingForm.recipientAddress,
        paymentMethod: paymentMethod.value
      }
    })

    if (res.success && res.data) {
      completedOrderId.value = res.data.id
      completedAmount.value = cartStore.totalAmount
      completedItemCount.value = cartStore.totalCount
      cartStore.clearCartState()
      step.value = 4
    }
  } catch (error: any) {
    notify({
      title: '結帳失敗',
      message: error?.data?.message || '庫存不足或樂觀鎖衝突,請確認後再試.',
      type: 'error',
      duration: 5000
    })
    await cartStore.fetchCart()
  } finally {
    submitting.value = false
  }
}

const formatPrice = (price: number) =>
  new Intl.NumberFormat('zh-TW', { maximumFractionDigits: 0 }).format(price)
</script>
