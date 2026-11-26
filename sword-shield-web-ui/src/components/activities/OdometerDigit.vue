<template>
  <span class="odo-digit" :class="{ 'is-faded': faded }" aria-hidden="true">
    <span
      class="odo-strip"
      :class="{ 'is-animating': animating }"
      :style="{ transform: `translateY(-${rowIndex}em)` }"
      @transitionend="onTransitionEnd"
    >
      <span v-for="(n, idx) in strip" :key="idx" class="odo-cell">{{ n }}</span>
    </span>
  </span>
</template>

<script setup lang="ts">
import { onBeforeUnmount, ref, watch } from 'vue'

const props = defineProps<{
  value: number
  direction: 1 | -1
  faded?: boolean
}>()

// 3 组 0-9（共 30 行），静止时停在中间组（10 + digit），确保前进与后退滚动方向严格相反
const strip = [
  0, 1, 2, 3, 4, 5, 6, 7, 8, 9,
  0, 1, 2, 3, 4, 5, 6, 7, 8, 9,
  0, 1, 2, 3, 4, 5, 6, 7, 8, 9
]

const clampDigit = (n: number) => ((Math.trunc(n) % 10) + 10) % 10

const rowIndex = ref(10 + clampDigit(props.value))
const animating = ref(false)
let settleTimer: number | undefined

function settle(digit: number) {
  window.clearTimeout(settleTimer)
  animating.value = false
  rowIndex.value = 10 + digit
}

function onTransitionEnd() {
  settle(clampDigit(props.value))
}

watch(
  () => props.value,
  (nextVal, prevVal) => {
    const nextD = clampDigit(nextVal)
    const prevD = clampDigit(prevVal)
    if (nextD === prevD) return

    const reduced = typeof window !== 'undefined' && window.matchMedia('(prefers-reduced-motion: reduce)').matches
    if (reduced) {
      settle(nextD)
      return
    }

    // 先无动画复位到中间组起点，下一帧再向对应方向滚到目标数字
    window.clearTimeout(settleTimer)
    animating.value = false
    rowIndex.value = 10 + prevD

    const targetRow =
      props.direction >= 0
        ? (nextD > prevD ? 10 + nextD : 20 + nextD)
        : (nextD < prevD ? 10 + nextD : nextD)

    requestAnimationFrame(() => {
      requestAnimationFrame(() => {
        animating.value = true
        rowIndex.value = targetRow
        settleTimer = window.setTimeout(() => settle(nextD), 480)
      })
    })
  }
)

onBeforeUnmount(() => {
  window.clearTimeout(settleTimer)
})
</script>

<style scoped>
.odo-digit {
  display: inline-block;
  height: 1em;
  width: 0.64em;
  overflow: hidden;
  line-height: 1;
  vertical-align: top;
  transition: opacity 0.25s ease;
}

.odo-digit.is-faded {
  opacity: 0;
}

.odo-strip {
  display: flex;
  flex-direction: column;
  will-change: transform;
}

.odo-strip.is-animating {
  transition: transform 0.45s cubic-bezier(0.22, 1, 0.36, 1);
}

.odo-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 1em;
  line-height: 1;
}

@media (prefers-reduced-motion: reduce) {
  .odo-digit,
  .odo-strip.is-animating {
    transition: none;
  }
}
</style>
