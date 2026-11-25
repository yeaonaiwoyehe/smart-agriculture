<template>
  <header class="page-head">
    <div>
      <div v-if="code" class="eyebrow">{{ code }} · {{ group }}</div>
      <h1 class="page-title">{{ title }}</h1>
      <p v-if="desc" class="page-desc">{{ desc }}</p>
    </div>
    <div class="page-actions">
      <slot name="actions" />
    </div>
  </header>
</template>

<script setup>
defineProps({
  title: { type: String, required: true },
  desc: { type: String, default: '' },
  code: { type: String, default: '' },
  group: { type: String, default: '业务管理' }
})
</script>
