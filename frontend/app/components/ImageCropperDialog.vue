<template>
  <el-dialog
    v-model="dialogVisible"
    title="選擇顯示範圍"
    width="560px"
    :close-on-click-modal="false"
    align-center
    @opened="initCropper"
    @closed="destroyCropper"
  >
    <div class="flex flex-col items-center gap-3">
      <!-- Cropper container -->
      <div class="w-full rounded-xl overflow-hidden bg-slate-900" style="max-height: 420px;">
        <img
          ref="imgRef"
          :src="imageSrc"
          class="block max-w-full opacity-0"
          alt="crop-target"
        />
      </div>
      <p class="text-xs text-slate-400 self-start">
        拖曳裁切框調整顯示範圍,使用滾輪縮放圖片
      </p>
    </div>

    <template #footer>
      <div class="flex justify-end gap-3">
        <el-button @click="handleCancel">取消</el-button>
        <el-button type="primary" :loading="confirming" @click="handleConfirm">
          確認裁切
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import type Cropper from 'cropperjs'

const props = withDefaults(defineProps<{
  modelValue: boolean
  imageSrc: string
  aspectRatio?: number
  outputSize?: number
}>(), {
  aspectRatio: 1,
  outputSize: 800,
})

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  'confirm': [blob: Blob]
}>()

const imgRef = ref<HTMLImageElement | null>(null)
const confirming = ref(false)
let cropperInstance: Cropper | null = null

const dialogVisible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val),
})

const initCropper = async () => {
  if (!import.meta.client || !imgRef.value) return

  const CropperClass = (await import('cropperjs')).default

  cropperInstance = new CropperClass(imgRef.value, {
    aspectRatio: props.aspectRatio,
    viewMode: 1,
    dragMode: 'move',
    autoCropArea: 0.8,
    restore: false,
    guides: true,
    center: true,
    highlight: true,
    cropBoxMovable: true,
    cropBoxResizable: true,
    toggleDragModeOnDblclick: false,
    ready() {
      // 讓圖片在 cropper 初始化後顯示
      if (imgRef.value) imgRef.value.style.opacity = '1'
    },
  })
}

const destroyCropper = () => {
  if (cropperInstance) {
    cropperInstance.destroy()
    cropperInstance = null
  }
  if (imgRef.value) imgRef.value.style.opacity = '0'
}

const handleConfirm = () => {
  if (!cropperInstance) return
  confirming.value = true

  const canvas = cropperInstance.getCroppedCanvas({
    width: props.outputSize,
    height: props.outputSize,
    imageSmoothingEnabled: true,
    imageSmoothingQuality: 'high',
  })

  canvas.toBlob(
    (blob) => {
      confirming.value = false
      if (!blob) {
        return
      }
      emit('confirm', blob)
      dialogVisible.value = false
    },
    'image/jpeg',
    0.92,
  )
}

const handleCancel = () => {
  dialogVisible.value = false
}
</script>
