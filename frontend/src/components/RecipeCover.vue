<template>
  <div class="cover" :class="{ 'is-fallback': showFallback }">
    <img v-if="!showFallback && src" :src="src" :alt="title || '封面'" loading="lazy" @error="showFallback = true" />
    <div v-else class="fallback" :style="fallbackStyle">
      <span class="fallback-emoji">{{ emoji }}</span>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'

/**
 * 菜谱封面：src 为空或加载失败时显示暖色渐变占位（不同 seed 不同色调，避免同质化）。
 */
const props = defineProps({
  src: { type: String, default: '' },
  title: { type: String, default: '' },
  seed: { type: Number, default: 0 }
})

const showFallback = ref(!props.src)
watch(() => props.src, (v) => { showFallback.value = !v })

const EMOJIS = ['🍳', '🥘', '🍰', '🥟', '🍜', '🥗', '🍢', '🍮']
const GRADIENTS = [
  'linear-gradient(135deg, #f6c08b, #e8590c)',
  'linear-gradient(135deg, #f2b8a0, #c7470a)',
  'linear-gradient(135deg, #eec37f, #d97706)',
  'linear-gradient(135deg, #f7c6c0, #b42318)',
  'linear-gradient(135deg, #d9c088, #b45309)',
  'linear-gradient(135deg, #f3bb9e, #ea580c)'
]

const emoji = computed(() => EMOJIS[Math.abs(props.seed) % EMOJIS.length])
const fallbackStyle = computed(() => ({ background: GRADIENTS[Math.abs(props.seed) % GRADIENTS.length] }))
</script>

<style scoped>
.cover {
  width: 100%;
  height: 100%;
  overflow: hidden;
  position: relative;
}

.cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.fallback {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.fallback-emoji {
  font-size: var(--fc-fs-4xl);
  filter: drop-shadow(0 4px 8px rgba(0, 0, 0, 0.15));
}
</style>
