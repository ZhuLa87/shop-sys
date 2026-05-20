import { defineStore } from 'pinia'

interface UserProfile {
  id: number
  email: string
  name: string
  role: string
  phone: string | null
  createdAt: number
  lastLoginAt: number | null
}

interface LoginResponse {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
}

export const useAuthStore = defineStore('auth', () => {
  const api = useApi()

  // 使用 useCookie 儲存 token 與個人檔案,以便在 SSR 與頁面重新整理時保持登入狀態
  const token = useCookie<string | null>('auth_token', {
    sameSite: 'lax',
    secure: true,
  })

  const refreshToken = useCookie<string | null>('refresh_token', {
    sameSite: 'lax',
    secure: true,
  })

  const userProfile = useCookie<UserProfile | null>('user_profile', {
    sameSite: 'lax',
    secure: true,
  })

  const isAuthenticated = computed(() => !!token.value)
  const user = computed(() => userProfile.value)
  const role = computed(() => userProfile.value?.role || null)

  const isCustomer = computed(() => role.value === 'CUSTOMER')
  const isProductManager = computed(() => role.value === 'PRODUCT_MANAGER' || role.value === 'SUPER_ADMIN')
  const isSuperAdmin = computed(() => role.value === 'SUPER_ADMIN')

  // 登入
  const login = async (email: string, password: string) => {
    const res = await api.request<LoginResponse>('/auth/login', {
      method: 'POST',
      body: { email, password }
    })

    if (res.success && res.data) {
      token.value = res.data.accessToken
      refreshToken.value = res.data.refreshToken
      await fetchProfile()

      const cartStore = useCartStore()
      await cartStore.fetchCart()
    }
    return res
  }

  // 註冊
  const register = async (body: any) => {
    return await api.request('/auth/register', {
      method: 'POST',
      body
    })
  }

  // 取得最新個人資料
  const fetchProfile = async () => {
    if (!token.value) return
    try {
      const res = await api.request<UserProfile>('/users/me')
      if (res.success) {
        userProfile.value = res.data
      }
    } catch {
      logout()
    }
  }

  // 修改個人資料 (PUT /users/me 回傳 void,更新後重新拉取)
  const updateProfile = async (body: any) => {
    const res = await api.request<void>('/users/me', {
      method: 'PUT',
      body
    })
    if (res.success) {
      await fetchProfile()
    }
    return res
  }

  // 登出:通知後端黑名單目前 token,再清除本地狀態
  const logout = async () => {
    if (token.value && refreshToken.value) {
      try {
        await api.request('/auth/logout', {
          method: 'POST',
          body: { refreshToken: refreshToken.value }
        })
      } catch {
        // 即使後端登出失敗,也繼續清除本地狀態
      }
    }

    token.value = null
    refreshToken.value = null
    userProfile.value = null

    const cartStore = useCartStore()
    cartStore.clearCartState()

    if (import.meta.client) {
      window.location.href = '/login'
    }
  }

  return {
    token,
    refreshToken,
    userProfile,
    isAuthenticated,
    user,
    role,
    isCustomer,
    isProductManager,
    isSuperAdmin,
    login,
    register,
    fetchProfile,
    updateProfile,
    logout
  }
})
