<template>
  <el-tree-select
    v-model="innerValue"
    :data="treeData"
    :props="treeProps"
    node-key="value"
    :multiple="multiple"
    :clearable="clearable"
    :placeholder="placeholder"
    :disabled="disabled"
    :filterable="filterable"
    :teleported="appendToBody"
    check-strictly
    :render-after-expand="false"
    style="width: 100%"
  />
</template>

<script setup>
import { computed } from 'vue'

/**
 * vue-treeselect 的 Vue3 兼容封装
 * 保持原有 props/事件语义（options、normalizer、props.label/children、v-model），
 * 底层使用 Element Plus 的 el-tree-select 渲染，避免引入不兼容的 Vue2 组件库。
 */
defineOptions({ name: 'Treeselect' })

const props = defineProps({
  modelValue: { type: [String, Number, Array], default: null },
  options: { type: Array, default: () => [] },
  // vue-treeselect 的 props 配置，如 { label: 'deptName', children: 'children' }
  props: { type: Object, default: () => ({ label: 'label', children: 'children' }) },
  // vue-treeselect 的 normalizer，把节点转成 { id, label, children }
  normalizer: { type: Function, default: null },
  multiple: { type: Boolean, default: false },
  clearable: { type: Boolean, default: true },
  disabled: { type: Boolean, default: false },
  filterable: { type: Boolean, default: false },
  placeholder: { type: String, default: '请选择' },
  appendToBody: { type: Boolean, default: true }
})

const emit = defineEmits(['update:modelValue', 'input', 'select'])

const innerValue = computed({
  get: () => props.modelValue,
  set: (val) => {
    emit('update:modelValue', val)
    emit('input', val)
  }
})

const treeProps = {
  label: 'label',
  children: 'children'
}

function normalizeNode(node) {
  if (props.normalizer) {
    const n = props.normalizer(node)
    return {
      value: n.id,
      label: n.label,
      children: n.children && n.children.length ? n.children.map(normalizeNode) : undefined
    }
  }
  const labelKey = props.props.label || 'label'
  const childrenKey = props.props.children || 'children'
  const children = node[childrenKey]
  return {
    value: node.id !== undefined ? node.id : node.value,
    label: node[labelKey],
    children: children && children.length ? children.map(normalizeNode) : undefined
  }
}

const treeData = computed(() => (props.options || []).map(normalizeNode))
</script>
