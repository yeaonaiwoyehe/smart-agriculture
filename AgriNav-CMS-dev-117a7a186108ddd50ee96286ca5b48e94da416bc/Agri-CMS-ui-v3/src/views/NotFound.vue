<template>
  <div class="page" style="display: grid; place-items: center; min-height: 70vh">
    <div class="card" style="padding: 40px 56px; text-align: center; max-width: 460px">
      <div style="font-size: 42px; font-weight: 800; color: var(--brand-600)">404</div>
      <div style="font-size: 16px; font-weight: 650; margin-top: 6px">页面不存在或无访问权限</div>
      <div class="hint" style="font-size: 12px; margin-top: 6px">
        当前角色：{{ store.roleMeta.name }}。可返回驾驶舱，或切换为有权限的角色后访问。
      </div>
      <el-button type="primary" style="margin-top: 18px" @click="$router.push('/dashboard')">返回驾驶舱</el-button>
    </div>
  </div>
</template>

<script setup>
import { useAppStore } from '@/stores/app'
const store = useAppStore()
</script>
