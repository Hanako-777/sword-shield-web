<template>
  <div
    class="category-filter"
    role="radiogroup"
    :aria-label="ariaLabel"
    @keydown="onKeydown"
  >
    <button
      v-for="(opt, idx) in options"
      :key="opt.value || '__all__'"
      :ref="(el) => setBtnRef(idx, el)"
      type="button"
      role="radio"
      class="filter-btn"
      :class="{ 'is-active': modelValue === opt.value }"
      :aria-checked="modelValue === opt.value"
      :tabindex="isOptionFocusable(opt.value, idx) ? 0 : -1"
      @click="select(opt.value)"
    >
      <span>{{ opt.label }}</span>
      <span v-if="opt.count !== null" class="filter-count">{{ opt.count }}</span>
    </button>
  </div>
</template>

<script setup lang="ts">
import type { ComponentPublicInstance } from 'vue'
import type { CategoryFilterOption } from '../../composables/useArticleCategories'

const props = withDefaults(defineProps<{
  options: CategoryFilterOption[]
  modelValue: string
  ariaLabel?: string
}>(), {
  ariaLabel: '按分类筛选资讯'
})

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
}>()

const btnRefs = new Map<number, HTMLButtonElement>()

function setBtnRef(index: number, el: Element | ComponentPublicInstance | null) {
  if (el instanceof HTMLButtonElement) btnRefs.set(index, el)
  else btnRefs.delete(index)
}

function isOptionFocusable(value: string, idx: number) {
  const activeIdx = props.options.findIndex((o) => o.value === props.modelValue)
  return activeIdx === -1 ? idx === 0 : value === props.modelValue
}

function select(nextValue: string) {
  if (nextValue === props.modelValue) return
  emit('update:modelValue', nextValue)
}

function onKeydown(e: KeyboardEvent) {
  const opts = props.options
  if (!opts.length) return
  const currentIdx = Math.max(0, opts.findIndex((o) => o.value === props.modelValue))
  let nextIdx = -1
  if (e.key === 'ArrowRight' || e.key === 'ArrowDown') {
    nextIdx = (currentIdx + 1) % opts.length
  } else if (e.key === 'ArrowLeft' || e.key === 'ArrowUp') {
    nextIdx = (currentIdx - 1 + opts.length) % opts.length
  } else if (e.key === 'Home') {
    nextIdx = 0
  } else if (e.key === 'End') {
    nextIdx = opts.length - 1
  }
  if (nextIdx === -1) return
  e.preventDefault()
  select(opts[nextIdx].value)
  const btn = btnRefs.get(nextIdx)
  btn?.focus()
  btn?.scrollIntoView({ block: 'nearest', inline: 'nearest' })
}
</script>

<style scoped>
.category-filter {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.filter-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  height: 36px;
  padding: 0 14px;
  border: 1px solid rgba(21, 21, 21, 0.1);
  border-radius: 8px;
  background: #ffffff;
  color: var(--ss-ink-2);
  font-size: 14px;
  font-weight: 500;
  line-height: 1;
  white-space: nowrap;
  flex-shrink: 0;
  cursor: pointer;
  transition: background-color var(--ss-dur) var(--ss-ease), border-color var(--ss-dur) var(--ss-ease),
    color var(--ss-dur) var(--ss-ease);
}

.filter-btn:hover {
  border-color: rgba(21, 21, 21, 0.22);
  background: rgba(21, 21, 21, 0.03);
  color: #151515;
}

.filter-btn.is-active {
  border-color: #151515;
  background: #151515;
  color: #ffffff;
}

.filter-count {
  font-size: 12px;
  font-weight: 500;
  color: var(--ss-muted);
  font-variant-numeric: tabular-nums;
}

.filter-btn.is-active .filter-count {
  color: rgba(255, 255, 255, 0.72);
}

@media (max-width: 640px) {
  .category-filter {
    flex-wrap: nowrap;
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
    scrollbar-width: none;
    -ms-overflow-style: none;
    padding-bottom: 2px;
  }

  .category-filter::-webkit-scrollbar {
    display: none;
  }
}
</style>
