<template>
  <svg-icon v-if="icon" :icon-class="icon"/>
  <span v-if="title" :title="title.length > 5 ? title : undefined">{{ title }}</span>
</template>

<script setup>
defineOptions({ name: 'MenuItem' })

defineProps({
  icon: {
    type: String,
    default: ''
  },
  title: {
    type: String,
    default: ''
  }
})
</script>
