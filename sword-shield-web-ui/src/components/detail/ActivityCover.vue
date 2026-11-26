<template>
  <img :key="src + String(readable)" class="cover" :src="image" :alt="title"
    :crossorigin="readable ? 'anonymous' : undefined" @load="sample" @error="retryImage" />
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
const props = defineProps<{ src?: string; title: string }>()
const emit = defineEmits<{ tint: [color: string] }>()
const fallback = '/covers/1.svg'
const readable = ref(true)
const failed = ref(false)
const image = computed(() => !failed.value && props.src ? props.src : fallback)
watch(() => props.src, () => {
  readable.value = true
  failed.value = false
  emit('tint', '#F4F6F8')
})
function retryImage() {
  emit('tint', '#F4F6F8')
  if (readable.value) readable.value = false
  else failed.value = true
}
function sample(event: Event) {
  if (!readable.value || !props.src || failed.value) return
  try {
    const canvas = document.createElement('canvas')
    canvas.width = canvas.height = 32
    const ctx = canvas.getContext('2d')
    if (!ctx) return
    ctx.drawImage(event.currentTarget as HTMLImageElement, 0, 0, 32, 32)
    const { data } = ctx.getImageData(0, 0, 32, 32)
    const bins = new Map<number, { count: number; r: number; g: number; b: number }>()
    for (let i = 0; i < data.length; i += 4) {
      if (data[i + 3] < 128) continue
      const [r, g, b] = [data[i], data[i + 1], data[i + 2]]
      const key = (r >> 5) * 64 + (g >> 5) * 8 + (b >> 5)
      const bin = bins.get(key) || { count: 0, r: 0, g: 0, b: 0 }
      bin.count++
      bin.r += r
      bin.g += g
      bin.b += b
      bins.set(key, bin)
    }
    const top = [...bins.values()].sort((a, b) => b.count - a.count)[0]
    if (!top) return
    const rgb = [top.r, top.g, top.b].map(sum => Math.round(255 * .94 + sum / top.count * .06))
    emit('tint', 'rgb(' + rgb.join(', ') + ')')
  } catch { emit('tint', '#F4F6F8') }
}
</script>

<style scoped>
.cover { display: block; width: 100%; aspect-ratio: 1; object-fit: cover; background: #f4f6f8; border-radius: 20px;
  box-shadow: 0 9px 24px -16px rgb(11 37 69 / 30%); }
</style>
