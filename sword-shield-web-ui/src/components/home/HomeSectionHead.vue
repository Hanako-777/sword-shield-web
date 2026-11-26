<template>
  <header class="home-section-head">
    <h2 :id="titleId" class="home-section-title">{{ title }}</h2>
    <div v-if="linkText || $slots.links" class="home-section-links">
      <slot name="links" />
      <router-link v-if="linkText && to" :to="to" class="home-section-link">
        <span>{{ linkText }}</span>
        <Icon name="arrow-right" :size="16" />
      </router-link>
    </div>
  </header>
</template>

<script setup lang="ts">
import Icon from '../ui/Icon.vue'

defineProps<{
  title: string
  titleId?: string
  linkText?: string
  to?: string
}>()
</script>

<style scoped>
.home-section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px 20px;
  flex-wrap: wrap;
  margin-bottom: 20px;
}

.home-section-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  line-height: 1.35;
  letter-spacing: -0.015em;
  color: #151515;
}

.home-section-links {
  display: flex;
  align-items: center;
  gap: 18px;
  flex-wrap: wrap;
}

.home-section-link,
.home-section-links :deep(.home-section-link) {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 0;
  border: 0;
  background: transparent;
  font-size: 16px;
  font-weight: 500;
  line-height: 1.4;
  color: #737373;
  white-space: nowrap;
  cursor: pointer;
  transition: color 0.2s ease;
}

.home-section-link:hover,
.home-section-links :deep(.home-section-link:hover) {
  color: #151515;
}

.home-section-link svg,
.home-section-links :deep(.home-section-link svg) {
  flex-shrink: 0;
}
</style>
