<template>
  <div class="md-content" v-html="html"></div>
</template>

<script setup>
import { computed } from 'vue'
import MarkdownIt from 'markdown-it'

const md = new MarkdownIt({
  breaks: true,
  linkify: true,
  html: false
})

const props = defineProps({
  content: { type: String, default: '' }
})

const html = computed(() => (props.content ? md.render(props.content) : ''))
</script>

<style scoped>
.md-content {
  font-size: 15px;
  line-height: 1.8;
  word-break: break-word;
}

.md-content :deep(p) {
  margin: 0.4em 0;
}

.md-content :deep(h1),
.md-content :deep(h2),
.md-content :deep(h3) {
  font-family: var(--fc-font-display);
  margin: 1em 0 0.4em;
}

.md-content :deep(ul),
.md-content :deep(ol) {
  padding-left: 1.4em;
}

.md-content :deep(code) {
  background: oklch(92% 0.015 70);
  padding: 0.1em 0.4em;
  border-radius: 4px;
  font-size: 0.92em;
}

.md-content :deep(blockquote) {
  margin: 0.6em 0;
  padding-left: 1em;
  border-left: 3px solid var(--fc-rule);
  color: var(--fc-neutral);
}
</style>
