interface EcpayCheckoutForm {
  actionUrl: string
  params: Record<string, string>
}

// 綠界 AIO 付款:向後端取得已簽章 (CheckMacValue) 的表單,再以 form POST 整頁導向綠界付款頁.
// 綠界付款頁不可用 iframe 嵌入,params 送出前也不可修改任何值,否則檢查碼會不符.
export const useEcpayCheckout = () => {
  const api = useApi()

  const redirectToEcpay = async (orderId: number) => {
    const res = await api.request<EcpayCheckoutForm>('/payments/ecpay/checkout', {
      method: 'POST',
      body: { orderId },
    })
    if (!res.success || !res.data) {
      throw new Error(res.message || '無法建立付款,請稍後再試.')
    }
    submitForm(res.data)
  }

  // 從綠界頁面按上一頁時,瀏覽器可能直接從 bfcache 還原頁面 (不會重跑 onMounted),
  // 送出中的 loading 狀態會卡住,由呼叫端在 callback 裡重設
  const onRestoreFromEcpay = (callback: () => void) => {
    const handler = (event: PageTransitionEvent) => {
      if (event.persisted) callback()
    }
    onMounted(() => window.addEventListener('pageshow', handler))
    onBeforeUnmount(() => window.removeEventListener('pageshow', handler))
  }

  return { redirectToEcpay, onRestoreFromEcpay }
}

const submitForm = ({ actionUrl, params }: EcpayCheckoutForm) => {
  const form = document.createElement('form')
  form.method = 'POST'
  form.action = actionUrl
  form.acceptCharset = 'UTF-8'
  form.style.display = 'none'

  for (const [name, value] of Object.entries(params)) {
    const input = document.createElement('input')
    input.type = 'hidden'
    input.name = name
    input.value = value
    form.appendChild(input)
  }

  document.body.appendChild(form)
  form.submit()
}
