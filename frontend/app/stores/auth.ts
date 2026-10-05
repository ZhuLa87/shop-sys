import { defineStore } from 'pinia'

interface UserProfile {
  id: number
  email: string
  name: string
  role: string
  phone: string | null
  createdAt: number
  lastLoginAt: number | null
  avatarUrl: string | null
}

interface LoginResponse {
  accessToken: string
  tokenType: string
  expiresIn: number
}

export const useAuthStore = defineStore('auth', () => {
  const api = useApi()

  // 使用 useCookie 儲存 access token 與個人檔案,以便在 SSR 與頁面重新整理時保持登入狀態.
  // refresh token 由後端以 HttpOnly cookie 核發,前端碰不到
  const token = useCookie<string | null>('auth_token', {
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

  const login = async (email: string, password: string) => {
    const res = await api.request<LoginResponse>('/auth/login', {
      method: 'POST',
      body: { email, password }
    })

    if (res.success && res.data) {
      token.value = res.data.accessToken
      await fetchProfile()

      const cartStore = useCartStore()
      await cartStore.fetchCart()
    }
    return res
  }

  const register = async (body: any) => {
    return await api.request('/auth/register', {
      method: 'POST',
      body
    })
  }

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

  // 修改個人資料 (PUT /users/me 回傳 void,更新後重新拉取).
  // 改密碼或 Email 後後端會撤銷這個使用者所有的 token (包含目前這個), 此時不能再拉資料, 改由呼叫端引導重新登入
  const updateProfile = async (body: any) => {
    const reloginRequired = !!body.password || (!!body.email && body.email !== userProfile.value?.email)
    const res = await api.request<void>('/users/me', {
      method: 'PUT',
      body
    })
    if (res.success && !reloginRequired) {
      await fetchProfile()
    }
    return { ...res, reloginRequired: res.success && reloginRequired }
  }

  // token 已被後端撤銷時使用: 呼叫 /auth/logout 只會拿到 401, 直接清掉本地狀態.
  // HttpOnly 的 refresh_token cookie 已在後端失效, 下次換發失敗時由後端清除
  const clearSessionAndGoToLogin = (reason?: string) => {
    token.value = null
    userProfile.value = null
    useCartStore().clearCartState()

    if (import.meta.client) {
      window.location.href = reason ? `/login?reason=${reason}` : '/login'
    }
  }

  // 登出:通知後端黑名單目前 token 並清除 refresh_token cookie (HttpOnly,只有後端能清),再清除本地狀態
  const logout = async () => {
    try {
      await api.request('/auth/logout', { method: 'POST' })
    } catch {
      // 即使後端登出失敗,也繼續清除本地狀態
    }

    token.value = null
    userProfile.value = null

    const cartStore = useCartStore()
    cartStore.clearCartState()

    if (import.meta.client) {
      window.location.href = '/login'
    }
  }

  return {
    token,
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
    clearSessionAndGoToLogin,
    logout
  }
})
