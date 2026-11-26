<template>
  <img :src="image" alt="" decoding="async" @error="useFallback" />
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { fallbackImages } from './programme'

const props = defineProps<{ src: string; index: number }>()
const image = ref(props.src)
watch(() => props.src, src => { image.value = src })
function useFallback() {
  const fallback = fallbackImages[props.index % fallbackImages.length]
  const local = '/covers/' + (props.index % 5 + 1) + '.svg'
  if (image.value !== local) image.value = image.value === props.src && props.src !== fallback ? fallback : local
}
</script>

