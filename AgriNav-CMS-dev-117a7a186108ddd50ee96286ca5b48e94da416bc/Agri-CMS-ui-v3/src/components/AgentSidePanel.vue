<template>
  <div class="card agent-side" :style="{ height: height + 'px' }">
    <div class="agent-side-head">
      <div class="agent-avatar" style="width: 32px; height: 32px">{{ agentId.replace('MA-', 'M') }}</div>
      <div style="min-width: 0">
        <div style="font-size: 13px; font-weight: 650">{{ title }}</div>
        <div class="hint" style="font-size: 10.5px">{{ desc }}</div>
      </div>
      <span class="ai-chip" style="margin-left: auto">{{ agentId }}</span>
    </div>
    <div class="agent-side-body">
      <AgentChat :agent-id="agentId" compact />
    </div>
  </div>
</template>

<script setup>
import AgentChat from '@/components/AgentChat.vue'

defineProps({
  agentId: { type: String, default: 'MA-01' },
  title: { type: String, default: 'AI 业务助手' },
  desc: { type: String, default: '基于业务数据回答，不修改正式数据' },
  height: { type: Number, default: 560 }
})
</script>

<style scoped>
.agent-side {
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
  height: 100%;
}
.agent-side-head {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 12px 14px;
  border-bottom: 1px solid var(--border);
  background: linear-gradient(180deg, #f6fcf8, #fff);
  flex: none;
}
.agent-side-body {
  flex: 1;
  min-height: 0;
  display: flex;
  padding: 10px 12px 12px;
}
</style>
