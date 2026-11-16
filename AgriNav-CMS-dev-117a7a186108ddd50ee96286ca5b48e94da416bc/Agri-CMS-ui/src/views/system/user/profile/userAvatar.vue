<template>
  <div>
    <div class="user-info-head" @click="editCropper()">
      <el-image :src="avatar" title="点击上传头像" class="img-circle img-lg">
        <div slot="error" class="image-slot">
          <i class="el-icon-picture-outline"></i>
        </div>
      </el-image>
    </div>
    <el-dialog :title="title" :visible.sync="open" width="800px" append-to-body @opened="modalOpened"
               @close="closeDialog">
      <el-row>
        <el-col :xs="24" :md="12" :style="{height: '350px'}">
          <vue-cropper
            ref="cropper"
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

<script>
import {VueCropper} from "vue-cropper";
import {uploadAvatar} from "@/api/system/user";
import {mapGetters, mapActions} from 'vuex';

export default {
  components: {VueCropper},
  computed: {
    ...mapGetters(['avatar'])
  },
  data() {
    return {
      open: false,
      visible: false,
      title: "修改头像",
      options: {
        img: '',
        autoCrop: true,
        autoCropWidth: 200,
        autoCropHeight: 200,
        fixedBox: true,
        outputType: "png"
      },
      previews: {}
    };
  },
  watch: {
    avatar(newVal) {
      this.options.img = newVal;
    }
  },
  created() {
    this.options.img = this.avatar;
  },
  methods: {
    ...mapActions({
      updateUserInfo: 'user/GetInfo'
    }),
    editCropper() {
      this.open = true;
    },
    modalOpened() {
      this.visible = true;
    },
    requestUpload() {
    },
    beforeUpload(file) {
      if (file.type.indexOf("image/") == -1) {
        this.$modal.msgError("文件格式错误，请上传图片类型,如：JPG，PNG后缀的文件。");
      } else {
        const reader = new FileReader();
        reader.readAsDataURL(file);
        reader.onload = () => {
          this.options.img = reader.result;
        };
      }
    },
    changeScale(num) {
      num = num || 1;
      this.$refs.cropper.changeScale(num);
    },
    rotateLeft() {
      this.$refs.cropper.rotateLeft();
    },
    rotateRight() {
      this.$refs.cropper.rotateRight();
    },
    uploadImg() {
      this.$refs.cropper.getCropBlob(data => {
        let formData = new FormData();
        formData.append("avatarfile", data);
        uploadAvatar(formData).then(response => {
          this.open = false;
          this.$modal.msgSuccess("修改成功");
          this.visible = false;

          // 更新 Vuex store 中的头像
          this.$store.commit('SET_AVATAR', response.imgUrl);

          // 更新本地图片
          this.options.img = response.imgUrl;

          // 向父组件发送事件
          this.$emit('avatar-updated');
        });
      });
    },
    realTime(data) {
      this.previews = data;
    },
    closeDialog() {
      this.options.img = this.$store.getters.avatar;
      this.visible = false;
    }
  }
};
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
