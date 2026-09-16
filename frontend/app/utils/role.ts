// 使用者角色的顯示名稱與徽章樣式,對應後端 com.zzowo.shop_sys.enums.Role
// 後台多處會顯示角色 (使用者管理, 庫存稽核日誌) ,集中在這裡避免各頁各自寫一份

const ROLE_LABELS: Record<string, string> = {
  CUSTOMER: '一般顧客',
  FINANCE: '財務人員',
  MARKETING: '行銷人員',
  CUSTOMER_SERVICE: '客服人員',
  ORDER_MANAGER: '訂單管理員',
  PRODUCT_MANAGER: '商品管理員',
  SUPER_ADMIN: '超級管理員',
}

const ROLE_CLASSES: Record<string, string> = {
  CUSTOMER: 'bg-slate-50 text-slate-700 border-slate-200',
  FINANCE: 'bg-emerald-50 text-emerald-700 border-emerald-200',
  MARKETING: 'bg-amber-50 text-amber-700 border-amber-200',
  CUSTOMER_SERVICE: 'bg-cyan-50 text-cyan-700 border-cyan-200',
  ORDER_MANAGER: 'bg-violet-50 text-violet-700 border-violet-200',
  PRODUCT_MANAGER: 'bg-indigo-50 text-indigo-700 border-indigo-200',
  SUPER_ADMIN: 'bg-rose-50 text-rose-700 border-rose-200',
}

const FALLBACK_CLASS = 'bg-slate-50 text-slate-700 border-slate-200'

// 角色代碼 -> 中文名稱 (未知代碼原樣顯示)
export const getRoleLabel = (role?: string | null): string => {
  if (!role) return ''
  return ROLE_LABELS[role] ?? role
}

// 角色代碼 -> 徽章配色
export const getRoleClass = (role?: string | null): string => {
  if (!role) return FALLBACK_CLASS
  return ROLE_CLASSES[role] ?? FALLBACK_CLASS
}
