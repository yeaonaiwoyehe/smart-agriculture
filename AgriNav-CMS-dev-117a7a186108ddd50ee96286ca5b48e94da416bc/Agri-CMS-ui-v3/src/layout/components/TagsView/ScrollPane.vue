<template>
  <el-scrollbar ref="scrollContainer" :vertical="false" class="scroll-container" @wheel.prevent="handleScroll">
    <slot/>
  </el-scrollbar>
</template>

<script setup>
import { computed, getCurrentInstance, onBeforeUnmount, onMounted, ref } from 'vue'

const tagAndTagSpacing = 4 // tagAndTagSpacing

defineOptions({ name: 'ScrollPane' })

const emit = defineEmits(['scroll'])

const { proxy } = getCurrentInstance()

const scrollContainer = ref(null)

const scrollWrapper = computed(() => scrollContainer.value && scrollContainer.value.wrapRef)

onMounted(() => {
  if (scrollWrapper.value) {
    scrollWrapper.value.addEventListener('scroll', emitScroll, true)
  }
})

onBeforeUnmount(() => {
  if (scrollWrapper.value) {
    scrollWrapper.value.removeEventListener('scroll', emitScroll)
  }
})

function handleScroll(e) {
  const eventDelta = e.wheelDelta || -e.deltaY * 40
  const $scrollWrapper = scrollWrapper.value
  if (!$scrollWrapper) return
  $scrollWrapper.scrollLeft = $scrollWrapper.scrollLeft + eventDelta / 4
}

function emitScroll() {
  emit('scroll')
}

function moveToTarget(currentTag, tagList = []) {
  const $container = proxy.$el
  const $containerWidth = $container.offsetWidth
  const $scrollWrapper = scrollWrapper.value
  if (!$scrollWrapper) return

  let firstTag = null
  let lastTag = null

  // find first tag and last tag
  if (tagList.length > 0) {
    firstTag = tagList[0]
    lastTag = tagList[tagList.length - 1]
  }

  if (firstTag === currentTag) {
    $scrollWrapper.scrollLeft = 0
  } else if (lastTag === currentTag) {
    $scrollWrapper.scrollLeft = $scrollWrapper.scrollWidth - $containerWidth
  } else {
    // find preTag and nextTag
    const currentIndex = tagList.findIndex(item => item === currentTag)
    const prevTag = tagList[currentIndex - 1]
    const nextTag = tagList[currentIndex + 1]
    if (!prevTag || !nextTag) return

    // the tag's offsetLeft after of nextTag
    const afterNextTagOffsetLeft = nextTag.$el.offsetLeft + nextTag.$el.offsetWidth + tagAndTagSpacing

    // the tag's offsetLeft before of prevTag
    const beforePrevTagOffsetLeft = prevTag.$el.offsetLeft - tagAndTagSpacing

    if (afterNextTagOffsetLeft > $scrollWrapper.scrollLeft + $containerWidth) {
      $scrollWrapper.scrollLeft = afterNextTagOffsetLeft - $containerWidth
    } else if (beforePrevTagOffsetLeft < $scrollWrapper.scrollLeft) {
      $scrollWrapper.scrollLeft = beforePrevTagOffsetLeft
    }
  }
}

defineExpose({ moveToTarget })
</script>

<style lang="scss" scoped>
.scroll-container {
  white-space: nowrap;
  position: relative;
  overflow: hidden;
  width: 100%;

  ::v-deep {
    .el-scrollbar__bar {
      bottom: 0px;
    }

    .el-scrollbar__wrap {
      height: 49px;
    }
  }
}
</style>
