<template>
  <span
    class="ss-icon"
    :class="{ 'is-filled': filled }"
    :style="{ '--ss-icon-size': sizePx }"
    :aria-hidden="ariaLabel ? undefined : 'true'"
    :aria-label="ariaLabel"
    :role="ariaLabel ? 'img' : undefined"
    v-html="svgHtml"
  />
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { iconMap, type IconName } from './icons'

const props = withDefaults(defineProps<{
  name: IconName
  size?: number | string
  filled?: boolean
  ariaLabel?: string
}>(), {
  size: 20,
  filled: false
})

const sizePx = computed(() => (typeof props.size === 'number' ? `${props.size}px` : props.size))
const svgHtml = computed(() => iconMap[props.name] || '')
</script>

<style scoped>
.ss-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: var(--ss-icon-size, 20px);
  height: var(--ss-icon-size, 20px);
  flex-shrink: 0;
  line-height: 1;
  color: currentColor;
  vertical-align: middle;
}

.ss-icon :deep(svg) {
  display: block;
  width: 100%;
  height: 100%;
}

.ss-icon.is-filled :deep(svg path),
.ss-icon.is-filled :deep(svg polygon),
.ss-icon.is-filled :deep(svg circle) {
  fill: currentColor;
}
</style>
