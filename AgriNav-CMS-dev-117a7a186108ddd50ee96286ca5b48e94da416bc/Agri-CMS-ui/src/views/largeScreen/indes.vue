<template>
  <div></div>
</template>

<script>
export default {
  mounted() {
    // 在新标签页中打开链接，保留当前页面；地址使用当前站点，不再硬编码远程 IP
    window.open(window.location.origin + '/', '_blank')
  }
}
</script>
  