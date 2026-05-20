import { ElNotification } from 'element-plus'
import type { NotificationOptions } from 'element-plus'

const NAV_HEIGHT = 64

export const notify = (options: NotificationOptions) =>
  ElNotification({
    offset: options.position?.startsWith('bottom') ? 16 : NAV_HEIGHT + 8,
    ...options,
  })
