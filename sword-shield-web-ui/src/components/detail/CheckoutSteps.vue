<template>
  <ol class="steps" aria-label="购票进度">
    <li v-for="(label, i) in labels" :key="label" :aria-current="i === step ? 'step' : undefined" :class="{ done: i < step }">
      <span aria-hidden="true"><Icon v-if="i < step" name="check" :size="12" /><template v-else>{{ i + 1 }}</template></span>{{ label }}
    </li>
  </ol>
</template>
<script setup lang="ts">
import Icon from '../ui/Icon.vue'
defineProps<{ step: number }>()
const labels = ['选择票种', '确认订单', '支付', '完成']
</script>
<style scoped>
.steps { display: flex; list-style: none; align-items: center; gap: 8px; color: var(--ss-muted); font-size: 13px; }
.steps li { display: flex; align-items: center; gap: 6px; }
.steps li + li::before { content: ''; width: clamp(8px,2vw,28px); border-top: 1px solid var(--ss-line-strong); }
.steps span { display: grid; place-items: center; width: 21px; height: 21px; border-radius: 50%; background: var(--ss-fill); }
li[aria-current] { font-weight: 600; color: var(--ss-navy); }
li[aria-current] span { background: var(--ss-navy); color: white; }
.done span { background: var(--ss-teal-soft); color: var(--ss-teal-ink); }
@media(max-width:640px) { .steps { gap: 5px; font-size: 12px; } .steps li { gap: 3px; } .steps li + li::before { width: 4px; } }
</style>
