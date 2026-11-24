<template>
  <div>
    <div class="user-info-head" @click="editCropper()">
      <el-image :src="avatar" title="点击上传头像" class="img-circle img-lg">
        <template #error>
          <div class="image-slot">
            <i class="el-icon-picture-outline"></i>
          </div>
        </template>
      </el-image>
    </div>
    <el-dialog :title="title" v-model="open" width="800px" append-to-body @opened="modalOpened"
               @close="closeDialog">
      <el-row>
        <el-col :xs="24" :md="12" :style="{height: '350px'}">
          <VueCropper
            ref="cropperRef"
            :img="options.img"
            :info="true"
            :autoCrop="options.autoCrop"
            :autoCropWidth="options.autoCropWidth"
            :autoCropHeight="options.autoCropHeight"
            :fixedBox="options.fixedBox"
            :outputType="options.outputType"
            @realTime="realTime"
            v-if="visible"
          />
        </el-col>
        <el-col :xs="24" :md="12" :style="{height: '350px'}">
          <div class="avatar-upload-preview">
            <img :src="previews.url" :style="previews.img"/>
          </div>
        </el-col>
      </el-row>
      <br/>
      <el-row>
        <el-col :lg="2" :md="2">
          <el-upload action="#" :http-request="requestUpload" :show-file-list="false" :before-upload="beforeUpload">
            <el-button size="small">
              选择
              <i class="el-icon-upload el-icon--right"></i>
            </el-button>
          </el-upload>
        </el-col>
        <el-col :lg="{span: 1, offset: 2}" :md="2">
          <el-button icon="el-icon-plus" size="small" @click="changeScale(1)"></el-button>
        </el-col>
        <el-col :lg="{span: 1, offset: 1}" :md="2">
          <el-button icon="el-icon-minus" size="small" @click="changeScale(-1)"></el-button>
        </el-col>
        <el-col :lg="{span: 1, offset: 1}" :md="2">
          <el-button icon="el-icon-refresh-left" size="small" @click="rotateLeft()"></el-button>
        </el-col>
        <el-col :lg="{span: 1, offset: 1}" :md="2">
          <el-button icon="el-icon-refresh-right" size="small" @click="rotateRight()"></el-button>
        </el-col>
        <el-col :lg="{span: 2, offset: 6}" :md="2">
          <el-button type="primary" size="small" @click="uploadImg()">提 交</el-button>
        </el-col>
      </el-row>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, getCurrentInstance, onMounted, ref, watch } from 'vue'
import { VueCropper } from 'vue-cropper'
import { uploadAvatar } from '@/api/system/user'
import { useUserStore } from '@/stores/user'

const emit = defineEmits(['avatar-updated'])
const { proxy } = getCurrentInstance()
const userStore = useUserStore()

const avatar = computed(() => userStore.avatar)
const open = ref(false)
const visible = ref(false)
const title = ref('修改头像')
const options = ref({
  img: '',
  autoCrop: true,
  autoCropWidth: 200,
  autoCropHeight: 200,
  fixedBox: true,
  outputType: 'png'
})
const previews = ref({})
const cropperRef = ref(null)

watch(avatar, (newVal) => {
  options.value.img = newVal
})

function editCropper() {
  open.value = true
}

function modalOpened() {
  visible.value = true
}

function requestUpload() {
}

function beforeUpload(file) {
  if (file.type.indexOf('image/') == -1) {
    proxy.$modal.msgError('文件格式错误，请上传图片类型,如：JPG，PNG后缀的文件。')
  } else {
    const reader = new FileReader()
    reader.readAsDataURL(file)
    reader.onload = () => {
      options.value.img = reader.result
    }
  }
}

function changeScale(num) {
  num = num || 1
  cropperRef.value.changeScale(num)
}

function rotateLeft() {
  cropperRef.value.rotateLeft()
}

function rotateRight() {
  cropperRef.value.rotateRight()
}

function uploadImg() {
  cropperRef.value.getCropBlob(data => {
    let formData = new FormData()
    formData.append('avatarfile', data)
    uploadAvatar(formData).then(response => {
      open.value = false
      proxy.$modal.msgSuccess('修改成功')
      visible.value = false

      // 更新 Pinia store 中的头像
      userStore.avatar = response.imgUrl

      // 更新本地图片
      options.value.img = response.imgUrl

      // 向父组件发送事件
      emit('avatar-updated')
    })
  })
}

function realTime(data) {
  previews.value = data
}

function closeDialog() {
  options.value.img = userStore.avatar
  visible.value = false
}

onMounted(() => {
  options.value.img = userStore.avatar
})
</script>

<style scoped lang="scss">
.user-info-head {
  position: relative;
  display: inline-block;
  height: 120px;
}

.user-info-head:hover:after {
  content: '+';
  position: absolute;
  left: 0;
  right: 0;
  top: 0;
  bottom: 0;
  color: #eee;
  background: rgba(0, 0, 0, 0.5);
  font-size: 24px;
  font-style: normal;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  cursor: pointer;
  line-height: 110px;
  border-radius: 50%;
}
</style>
