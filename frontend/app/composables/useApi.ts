import { type UseFetchOptions } from '#app'

interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
}

interface TokenRefreshResult {
  accessToken: string
  refreshToken: string
}

// 防止多個請求同時觸發 token refresh (單例 promise) 
let refreshingPromise: Promise<TokenRefreshResult | null> | null = null

export const useApi = () => {
  const config = useRuntimeConfig()
  const token = useCookie<string | null>('auth_token', {
    sameSite: 'lax',
    secure: true,
  })
  const refreshTokenCookie = useCookie<string | null>('refresh_token', {
    sameSite: 'lax',
    secure: true,
  })

  const apiBase = config.public.apiBase || '/api/v1'

  const getHeaders = (): Record<string, string> => {
    const headers: Record<string, string> = {
      'Accept': 'application/json',
      'Content-Type': 'application/json',
    }
    if (token.value) {
      headers['Authorization'] = `Bearer ${token.value}`
    }
    return headers
  }

  // 嘗試用 refresh token 換發新的 access token
  const tryRefreshToken = async (): Promise<TokenRefreshResult | null> => {
    if (!refreshTokenCookie.value) return null

    if (refreshingPromise) return refreshingPromise

    refreshingPromise = $fetch<ApiResponse<TokenRefreshResult>>(
      `${apiBase}/auth/refresh`,
      {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
        body: { refreshToken: refreshTokenCookie.value },
      }
    )
      .then((res) => {
        if (res.success && res.data) {
          token.value = res.data.accessToken
          refreshTokenCookie.value = res.data.refreshToken
          return res.data
        }
        return null
      })
      .catch(() => null)
      .finally(() => { refreshingPromise = null })

    return refreshingPromise
  }

  // 清除所有憑證並跳轉登入頁
  const clearAndRedirect = () => {
    token.value = null
    refreshTokenCookie.value = null
    const userProfile = useCookie('user_profile')
    userProfile.value = null

    if (import.meta.client) {
      window.location.href = '/login'
    }
  }

  // 從 $fetch 的錯誤物件中提取後端回傳的訊息
  const extractErrorMessage = (error: any): string => {
    return (
      error?.data?.message ||
      error?.response?._data?.message ||
      error?.message ||
      '系統錯誤,請稍後再試.'
    )
  }

  // 1. 用於主動式操作的呼叫方法 (按鈕點擊,表單送出等) 
  const request = async <T = any>(
    path: string,
    options: Parameters<typeof $fetch>[1] = {}
  ): Promise<ApiResponse<T>> => {
    const url = `${apiBase}${path.startsWith('/') ? path : '/' + path}`

    const doFetch = () =>
      $fetch<ApiResponse<T>>(url, {
        ...options,
        headers: {
          ...getHeaders(),
          ...(options.headers as Record<string, string> | undefined),
        },
      })

    try {
      return await doFetch()
    } catch (error: any) {
      if (error?.status === 401) {
        // 嘗試刷新 token 後重試一次
        const refreshed = await tryRefreshToken()
        if (refreshed) {
          try {
            return await doFetch()
          } catch (retryError: any) {
            if (retryError?.status === 401) clearAndRedirect()
            throw retryError
          }
        } else {
          clearAndRedirect()
        }
      }
      // 將後端錯誤訊息掛在 error 上,讓呼叫端可以用 error.message 取得
      error.message = extractErrorMessage(error)
      throw error
    }
  }

  // 2. 用於頁面加載時的 Vue Data Fetching (支援 SSR) 
  const fetch = <T = any>(
    path: string,
    options: UseFetchOptions<ApiResponse<T>> = {}
  ) => {
    const url = `${apiBase}${path.startsWith('/') ? path : '/' + path}`

    return useFetch<ApiResponse<T>>(url, {
      ...options,
      headers: computed(() => ({
        ...getHeaders(),
        ...(options.headers as Record<string, string> | undefined),
      })),
      async onResponseError({ response, options: reqOptions }) {
        if (response.status === 401) {
          const refreshed = await tryRefreshToken()
          if (refreshed) {
            // 刷新成功:重新設定 header 讓 useFetch 下次使用新 token (headers 是 computed,自動更新) 
          } else {
            clearAndRedirect()
          }
        }
      },
    })
  }

  return {
    request,
    fetch,
    token,
    refreshToken: refreshTokenCookie,
  }
}
