<template>
  <div v-if="loading" class="state skeleton" role="status" aria-label="加载中" aria-busy="true">
    <span v-for="n in 3" :key="n" class="ss-skeleton" />
  </div>
  <div v-else class="state" :role="error ? 'alert' : 'status'">
    <span>{{ error || empty }}</span>
    <el-button v-if="error" @click="$emit('retry')">重试</el-button>
  </div>
</template>

<script setup lang="ts">
defineProps<{ loading?: boolean; error?: string; empty?: string }>()
defineEmits<{ retry: [] }>()
</script>

<style scoped>
.state { display: grid; justify-items: start; gap: 12px; padding: 24px; color: var(--ss-muted); border-radius: 20px; background: var(--ss-bg-soft); }
.skeleton span { height: 13px; width: 84%; }
.skeleton span:first-child { width: 38%; height: 20px; }
.skeleton span:last-child { width: 62%; }
.state .el-button { min-width: 56px; min-height: 44px; }
@media (prefers-reduced-motion: reduce) { .skeleton span { animation: none; } }
</style>
