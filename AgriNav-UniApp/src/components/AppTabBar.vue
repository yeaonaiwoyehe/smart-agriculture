<script setup lang="ts">
import { computed } from 'vue'
import SfIcon from './SfIcon.vue'

const props = withDefaults(
  defineProps<{
    active?: 'home' | 'tasks' | 'settlement' | 'profile' | ''
    running?: boolean
  }>(),
  { active: '', running: false }
)

const leftTabs = [
  { key: 'home', path: '/pages/home/index', label: '首页', icon: 'home' },
  { key: 'tasks', path: '/pages/task/index', label: '任务', icon: 'list' }
] as const

const rightTabs = [
  { key: 'settlement', path: '/pages/settlement/index', label: '结算', icon: 'wallet' },
  { key: 'profile', path: '/pages/mine/index', label: '我的', icon: 'user' }
] as const

const iconColor = computed(() => (key: string) =>
  props.active === key ? '#c8f45a' : 'rgba(255,255,255,0.52)'
)

function go(path: string, key: string) {
  if (props.active === key) return
  uni.reLaunch({ url: path })
}

function talkToAgent() {
  uni.navigateTo({ url: '/pages/agent/index' })
}
</script>

<template>
  <view class="tabbar">
    <view
      v-for="item in leftTabs"
      :key="item.key"
      class="tab-item"
      :class="{ 'tab-item--active': active === item.key }"
      @tap="go(item.path, item.key)"
    >
      <view class="tab-ico">
        <SfIcon :name="item.icon" :size="42" :color="iconColor(item.key)" />
      </view>
      <text>{{ item.label }}</text>
    </view>

    <view class="tab-ai" :class="{ 'tab-ai--live': running }" @tap="talkToAgent">
      <SfIcon :name="running ? 'spark' : 'mic'" :size="48" color="#082a21" />
      <text class="tab-ai-tag">AI</text>
    </view>

    <view
      v-for="item in rightTabs"
      :key="item.key"
      class="tab-item"
      :class="{ 'tab-item--active': active === item.key }"
      @tap="go(item.path, item.key)"
    >
      <view class="tab-ico">
        <SfIcon :name="item.icon" :size="42" :color="iconColor(item.key)" />
      </view>
      <text>{{ item.label }}</text>
    </view>
  </view>
</template>
