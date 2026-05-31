interface PresignResponse {
  uploadUrl: string
  publicUrl: string
  objectKey: string
}

type UploadType = 'product-cover' | 'product-image' | 'avatar'

const ACCEPTED_IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/webp', 'image/gif']
const MAX_IMAGE_SIZE_MB = 5

export const useUpload = () => {
  const api = useApi()

  const validateImageFile = (file: File | Blob): void => {
    const type = file instanceof File ? file.type : file.type
    if (type && !ACCEPTED_IMAGE_TYPES.includes(type)) {
      throw new Error(`不支援的格式 "${type}",請上傳 JPEG, PNG, WebP 或 GIF.`)
    }
    if (file.size > MAX_IMAGE_SIZE_MB * 1024 * 1024) {
      throw new Error(`檔案大小超過 ${MAX_IMAGE_SIZE_MB}MB 限制.`)
    }
  }

  // 核心上傳函式: 取得 presigned URL 後直接 PUT 至 MinIO
  const uploadFile = async (
    file: File | Blob,
    type: UploadType,
    resourceId: number,
  ): Promise<string> => {
    validateImageFile(file)

    const filename = file instanceof File ? file.name : `upload-${Date.now()}.jpg`
    const contentType = file.type || 'image/jpeg'

    // Step 1: 向後端取得帶簽名的 PUT URL
    const res = await api.request<PresignResponse>('/upload/presign', {
      method: 'POST',
      body: { type, resourceId, filename, contentType },
    })

    if (!res.success || !res.data) {
      throw new Error(res.message || '無法取得上傳授權')
    }

    // Step 2: 直接 PUT 至 MinIO,不帶 JWT header
    const uploadRes = await fetch(res.data.uploadUrl, {
      method: 'PUT',
      body: file,
      headers: { 'Content-Type': contentType },
    })

    if (!uploadRes.ok) {
      throw new Error(`檔案傳輸失敗 (${uploadRes.status}),請稍後再試.`)
    }

    return res.data.publicUrl
  }

  const uploadProductImage = (file: File | Blob, type: 'product-cover' | 'product-image', resourceId: number) =>
    uploadFile(file, type, resourceId)

  const uploadAvatar = (file: File | Blob, userId: number) =>
    uploadFile(file, 'avatar', userId)

  return { uploadFile, uploadProductImage, uploadAvatar }
}
