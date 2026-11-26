import { ref } from 'vue'

/** Tracks one independent read (detail, comments, catalogue or orders). */
export function useReadState() {
  const loading = ref(false)
  const error = ref('')
  let pending = Promise.resolve()
  function run(read: () => Promise<void>, fallback: string) {
    loading.value = true
    error.value = ''
    const next = pending.then(async () => {
      error.value = ''
      try {
        await read()
      } catch (cause) {
        error.value = cause instanceof Error && !(cause as { isAxiosError?: boolean }).isAxiosError
          ? cause.message || fallback : fallback
      }
    })
    pending = next
    return next.finally(() => {
      if (pending === next) loading.value = false
    })
  }
  return { loading, error, run }
}
