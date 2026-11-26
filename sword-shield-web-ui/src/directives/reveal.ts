import type { ObjectDirective } from 'vue'

const observers = new WeakMap<HTMLElement, IntersectionObserver>()

export const vReveal: ObjectDirective<HTMLElement> = {
  mounted(el) {
    const motion = window.matchMedia('(prefers-reduced-motion: reduce)')
    if (motion.matches || !('IntersectionObserver' in window)) return
    const observer = new IntersectionObserver(([entry]) => {
      if (!entry.isIntersecting) return
      observer.disconnect()
      el.classList.remove('awaiting-reveal')
    }, { threshold: 0.01 })
    el.classList.add('awaiting-reveal')
    observer.observe(el)
    observers.set(el, observer)
  },
  unmounted(el) {
    observers.get(el)?.disconnect()
    observers.delete(el)
  }
}

