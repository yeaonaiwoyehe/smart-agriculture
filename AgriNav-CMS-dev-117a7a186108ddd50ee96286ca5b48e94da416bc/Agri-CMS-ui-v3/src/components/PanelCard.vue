<template>
  <section class="card panel">
    <div v-if="title || $slots.extra" class="panel-head">
      <div>
        <div class="panel-title"><span class="dot" />{{ title }}</div>
        <div v-if="sub" class="panel-sub">{{ sub }}</div>
      </div>
      <div class="panel-extra"><slot name="extra" /></div>
    </div>
    <div class="panel-body" :class="{ flush, tight }">
      <slot />
    </div>
  </section>
</template>

<script setup>
defineProps({
  title: { type: String, default: '' },
  sub: { type: String, default: '' },
  flush: { type: Boolean, default: false },
  tight: { type: Boolean, default: false }
})
</script>
