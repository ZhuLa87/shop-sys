import { type UseFetchOptions } from '#app'
import { appendResponseHeader, type H3Event } from 'h3'

interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
}

interface TokenRefreshResult {
  accessToken: string
}

// 防止多個請求同時觸發 token refresh (單例 promise).
// 只用在瀏覽器端; SSR 時 module 變數會被所有使用者的請求共用, 改存在各自的 event.context
let clientRefreshingPromise: Promise<TokenRefreshResult | null> | null = null

// refresh token 是後端核發的 HttpOnly cookie, JS 讀不到.
// SSR 時要把後端回的 Set-Cookie (新的 refresh token 或清除指令) 轉給瀏覽器
const forwardSetCookie = (event: H3Event | null, headers: Headers | undefined) => {
  if (!event || !headers) return
  for (const cookie of headers.getSetCookie()) {
    appendResponseHeader(event, 'set-cookie', cookie)
  }
}

export const useApi = () => {
  const config = useRuntimeConfig()
  const token = useCookie<string | null>('auth_token', {
    sameSite: 'lax',
    secure: true,
  })
  // tryRefreshToken 會在非同步 callback 裡執行, 要先在 composable context 內取好
  const event = import.meta.server ? useRequestEvent() ?? null : null
  const requestCookie = import.meta.server ? useRequestHeaders(['cookie']).cookie : undefined

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

  // 嘗試用 refresh token cookie 換發新的 access token
  // (沒有 cookie 時後端回 401, 結果為 null)
  const doRefresh = async (): Promise<TokenRefreshResult | null> => {
    try {
      const res = await $fetch.raw<ApiResponse<TokenRefreshResult>>(`${apiBase}/auth/refresh`, {
        method: 'POST',
        headers: {
          'Accept': 'application/json',
          ...(requestCookie ? { cookie: requestCookie } : {}),
        },
      })
      forwardSetCookie(event, res.headers)
      const body = res._data
      return body?.success && body.data ? body.data : null
    } catch (error: any) {
      forwardSetCookie(event, error?.response?.headers)
      return null
    }
  }

  const sharedRefresh = (): Promise<TokenRefreshResult | null> => {
    if (event) {
      const ctx = event.context as { refreshingPromise?: Promise<TokenRefreshResult | null> }
      // SSR 同一個請求內只換發一次; 舊 refresh token 已被輪替, 不能再送第二次
      ctx.refreshingPromise ??= doRefresh()
      return ctx.refreshingPromise
    }

    if (!clientRefreshingPromise) {
      clientRefreshingPromise = doRefresh().finally(() => { clientRefreshingPromise = null })
    }
    return clientRefreshingPromise
  }

  // 每個 useApi() 都有自己的 auth_token ref, 彼此不會即時同步.
  // 換發結果由多個呼叫端共用, 所以每個呼叫端都要寫進自己的 ref, 重試時才會帶新 token
  const tryRefreshToken = async (): Promise<TokenRefreshResult | null> => {
    const refreshed = await sharedRefresh()
    if (refreshed) token.value = refreshed.accessToken
    return refreshed
  }

  // 清除所有憑證並跳轉登入頁
  const clearAndRedirect = () => {
    // refresh_token cookie 是 HttpOnly, 由後端在 refresh 失敗時清除
    token.value = null
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
  }
}
