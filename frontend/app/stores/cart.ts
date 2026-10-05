import { defineStore } from 'pinia'

interface CartItem {
  id: number
  productId: number
  productName: string
  coverImageUrl: string | null
  price: number
  quantity: number
  subtotal: number
}

export const useCartStore = defineStore('cart', () => {
  const api = useApi()
  const items = ref<CartItem[]>([])

  const totalCount = computed(() => {
    return items.value.reduce((sum, item) => sum + item.quantity, 0)
  })

  const totalAmount = computed(() => {
    return items.value.reduce((sum, item) => sum + item.subtotal, 0)
  })

  const fetchCart = async () => {
    const authStore = useAuthStore()
    if (!authStore.isAuthenticated) {
      items.value = []
      return
    }
    
    try {
      const res = await api.request<CartItem[]>('/carts')
      if (res.success && res.data) {
        items.value = res.data
      }
    } catch (error) {
      console.error('無法加載購物車內容:', error)
    }
  }

  const addToCart = async (productId: number, quantity: number) => {
    const authStore = useAuthStore()
    if (!authStore.isAuthenticated) {
      throw new Error('UNAUTHORIZED')
    }

    try {
      const res = await api.request<CartItem>('/carts', {
        method: 'POST',
        body: { productId, quantity }
      })
      
      if (res.success) {
        // 加入成功後重新拉取最新購物車清單,以確保與資料庫同步 (處理累加數量等邏輯)
        await fetchCart()
      }
      return res
    } catch (error) {
      throw error
    }
  }

  const removeFromCart = async (cartItemId: number) => {
    try {
      const res = await api.request(`/carts/${cartItemId}`, {
        method: 'DELETE'
      })
      if (res.success) {
        items.value = items.value.filter(item => item.id !== cartItemId)
      }
      return res
    } catch (error) {
      throw error
    }
  }

  // 更新購物車商品數量 (解決後端 @Min(1) 限制)
  // currentQuantity 由呼叫端傳入異動前的數量,避免輸入元件已就地改掉 item.quantity 而算出 0 差值
  const updateCartItemQuantity = async (productId: number, targetQuantity: number, currentCartItemId: number, currentQuantity?: number) => {
    const item = items.value.find(i => i.id === currentCartItemId)
    if (!item) return

    const diff = targetQuantity - (currentQuantity ?? item.quantity)
    if (diff > 0) {
      // 增加:直接呼叫 addToCart 增加差值
      await addToCart(productId, diff)
    } else if (diff < 0) {
      // 減少:先刪除舊的,再重新加入目標數量
      await removeFromCart(currentCartItemId)
      if (targetQuantity > 0) {
        await addToCart(productId, targetQuantity)
      } else {
        await fetchCart()
      }
    }
  }

  const clearCartState = () => {
    items.value = []
  }

  return {
    items,
    totalCount,
    totalAmount,
    fetchCart,
    addToCart,
    removeFromCart,
    updateCartItemQuantity,
    clearCartState
  }
})
