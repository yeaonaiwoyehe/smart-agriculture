<template>
  <div class="login">
    <el-form ref="loginForm" :model="loginForm" :rules="loginRules" class="login-form">
      <h3 class="title">农业土地与作物种植面积监测系统</h3>
      <el-form-item prop="phoneNumber">
        <el-input
          v-model="loginForm.phoneNumber"
          type="text"
          auto-complete="off"
          placeholder="手机号"
        >
          <svg-icon slot="prefix" icon-class="user" class="el-input__icon input-icon"/>
        </el-input>
      </el-form-item>
      <el-form-item prop="captcha">
        <el-input
          v-model="loginForm.captcha"
          type="text"
          auto-complete="off"
          placeholder="验证码"
          style="width: 63%"
          @keyup.enter.native="handleLogin"
        >
          <svg-icon slot="prefix" icon-class="validCode" class="el-input__icon input-icon"/>
        </el-input>
        <div class="login-code">
          <el-button @click="getCaptchaCode" class="login-code-btn">获取验证码</el-button>
        </div>
      </el-form-item>
      <el-form-item style="width:100%;">
        <el-button
          :loading="loading"
          size="medium"
          type="primary"
          style="width:100%;"
          @click.native.prevent="handleLogin"
        >
          <span v-if="!loading">登 录</span>
          <span v-else>登 录 中...</span>
        </el-button>
      </el-form-item>
      <div class="other-login">
        <div class="divider">
          <span class="line"></span>
          <span class="text">其他登录方式</span>
          <span class="line"></span>
        </div>
        <div class="social-login">
          <img
            src="@/assets/images/wechat.png"
            alt="微信登录"
            class="wechat-icon"
            @click="handleWechatLogin"
          />
          <el-button
            type="text"
            class="qrcode-icon"
            @click="showQRCodeDialog"
          >
            <i class="el-icon-mobile"></i>
          </el-button>
        </div>
      </div>
    </el-form>
    <!-- 添加二维码对话框 -->
    <el-dialog
      title="扫码登录"
      :visible.sync="qrDialogVisible"
      width="360px"
      class="qr-dialog"
      :close-on-click-modal="false"
      @closed="handleDialogClose"
    >
      <div class="qr-code-container" style="text-align: center;">
        <img v-if="qrcode" :src="'data:image/png;base64,' + qrcode" alt="二维码" />
        <div class="qr-tip">请使用手机扫描二维码登录</div>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {login, getCaptcha, bindWechat, loginOAuth, getCaptchaQR, validateQR} from "@/api/login";
import {setToken} from '@/utils/auth'
import {initRSAKey, enableEncrypt} from "@/utils/encrypt";

export default {
  name: "Login",
  data() {
    return {
      loginForm: {
        phoneNumber: "15888888888",
        captcha: ""
      },
      loginRules: {
        phoneNumber: [
          {required: true, trigger: "blur", message: "请输入您的手机号"},
          {pattern: /^1[3-9]\d{9}$/, message: "请输入正确的手机号码", trigger: "blur"}
        ],
        // 演示模式免验证码：验证码非必填，留空即可直接登录
        captcha: [{required: false, trigger: "change"}]
      },
      loading: false,
      redirect: undefined,
      wechatUserDetails: null,
      qrDialogVisible: false,
      qrcode: '',
      qrUuid: '',
      qrCheckTimer: null
    };
  },
  created() {
    const userDetailsStr = this.$route.query.userDetails;
    if (userDetailsStr) {
      try {
        const data = JSON.parse(decodeURIComponent(userDetailsStr));
        if (data.code === 601) {
          this.wechatUserDetails = data.userDetails;
          this.handleWechatCallback(data);
        }
      } catch (e) {
        console.error('解析微信回调数据失败:', e);
      }
    }
    const code = this.$route.query.code;
    const state = this.$route.query.state;

    if (code) {
      console.log('收到微信授权码:', code);
      this.handleOAuthCallback(code);
    }

    this.redirect = this.$route.query.redirect;
  },
  watch: {
    $route: {
      handler: function (route) {
        this.redirect = route.query && route.query.redirect;
      },
      immediate: true
    }
  },
  methods: {
    getCaptchaCode() {
      getCaptcha(this.loginForm.phoneNumber).then(response => {
        if (response.code === 200) {
          this.$message.success('发送成功');
        } else {
          this.$message.error('发送失败');
        }
      });
    },
    handleLogin() {
      this.$refs.loginForm.validate(valid => {
        if (valid) {
          this.loading = true;
          login(this.loginForm.phoneNumber, this.loginForm.captcha).then(response => {
            setToken(response.token);

            // 初始化加密模块
            if(response.rsa_public_key) {
              initRSAKey(response.rsa_public_key).then(() => {
                console.log('加密模块初始化成功');
                // 启用加密
                enableEncrypt();

                // 继续获取用户信息等操作
                this.$store.dispatch('GetInfo').then(() => {
                  this.$store.dispatch('GenerateRoutes').then(accessRoutes => {
                    this.$router.addRoutes(accessRoutes);
                    this.$router.push({path: this.redirect || '/'}).then(() => {
                      console.log('路由跳转成功,当前路由:', this.$route);
                      this.$message.success('登录成功');
                    }).catch(err => {
                      console.error('路由跳转失败:', err);
                      this.$message.error('登录成功，但页面跳转失败');
                    });
                  });
                });
              }).catch(err => {
                console.error('加密模块初始化失败:', err);
                this.$message.error('加密模块初始化失败,请重试');
                this.loading = false;
              });
            } else {
              console.error('未获取到RSA公钥');
              this.$message.error('登录失败,未获取加密密');
              this.loading = false;
            }
          }).catch(error => {
            console.error('录失败:', error);
            this.loading = false;
          });
        }
      });
    },
    handleWechatLogin() {
      const redirectUri = encodeURIComponent('http://localhost:80/login');
      const appId = 'wxc49afcc91a9c6d7c'; // 从环境变量或配置中获取
      const state = Math.random().toString(36).substr(2);
      window.location.href = `https://open.weixin.qq.com/connect/qrconnect?appid=${appId}&redirect_uri=${redirectUri}&response_type=code&scope=snsapi_login&state=${state}#wechat_redirect`;
    },
    handleWechatCallback(data) {
      if (data.code === 601) {
        const userDetails = encodeURIComponent(JSON.stringify(data.userDetails));
        this.$router.push({
          path: '/bind-phone',
          query: {
            userDetails,
            code: this.$route.query.code,
            noAuth: 'true'
          }
        });
      }
    },
    handleOAuthCallback(code) {
      this.loading = true;

      loginOAuth(code)
        .then(res => {
          if (res.code === 601) {
            const userDetails = encodeURIComponent(JSON.stringify(res.userDetails));
            this.$router.push({
              path: '/bind-phone',
              query: {
                userDetails,
                code,
                noAuth: 'true'
              }
            });
            return;
          }

          if (res.code === 200) {
            setToken(res.token);
            this.$router.push({ path: this.redirect || '/' });
          }
        })
        .catch(error => {
          console.error('OAuth登录失败:', error);
        })
        .finally(() => {
          this.loading = false;
        });
    },
    showQRCodeDialog() {
      this.qrDialogVisible = true;
      this.getQRCode();
    },

    async getQRCode() {
      try {
        const res = await getCaptchaQR();
        if (res.code === 200 && res.uuid) {
          this.qrUuid = res.uuid;
          this.qrcode = res.qrcode;
          this.startQRCheck();
        }
      } catch (error) {
        console.error('获取二维码失败:', error);
        this.$message.error('获取二维码失败');
      }
    },

    startQRCheck() {
      if (this.qrCheckTimer) {
        clearInterval(this.qrCheckTimer);
      }

      this.qrCheckTimer = setInterval(async () => {
        try {
          const res = await validateQR(this.qrUuid);
          if (res.code === 200) {
            clearInterval(this.qrCheckTimer);
            this.qrDialogVisible = false;

            if (res.token) {
              await this.handleLoginSuccess(res);
            }
          }
        } catch (error) {
          console.error('二维码检查失败:', error);
        }
      }, 1000);
    },

    handleDialogClose() {
      if (this.qrCheckTimer) {
        clearInterval(this.qrCheckTimer);
        this.qrCheckTimer = null;
      }
      this.qrcode = '';
      this.qrUuid = '';
    },

    async handleLoginSuccess(response) {
      setToken(response.token);

      try {
        // 获取用户信息
        await this.$store.dispatch('GetInfo');

        // 生成路由
        const accessRoutes = await this.$store.dispatch('GenerateRoutes');
        this.$router.addRoutes(accessRoutes);

        // 跳转到首页或重定向页面
        await this.$router.push({ path: this.redirect || '/' });
        this.$message.success('登录成功');
      } catch (error) {
        console.error('登录后处理失败:', error);
        this.$message.error('登录成功，但页面跳转失败');
      }
    }
  },

  beforeDestroy() {
    if (this.qrCheckTimer) {
      clearInterval(this.qrCheckTimer);
    }
  }
};
</script>

<style rel="stylesheet/scss" lang="scss">
.login {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
  background-image: url("../assets/images/login-background.jpg");
  background-size: cover;
}

.title {
  margin: 0px auto 30px auto;
  text-align: center;
  color: #707070;
}

.login-form {
  border-radius: 6px;
  background: #ffffff;
  width: 400px;
  padding: 25px 25px 5px 25px;

  .el-input {
    height: 38px;

    input {
      height: 38px;
    }
  }

  .input-icon {
    height: 39px;
    width: 14px;
    margin-left: 2px;
  }
}

.login-tip {
  font-size: 13px;
  text-align: center;
  color: #bfbfbf;
}

.login-code {
  width: 33%;
  height: 38px;
  float: right;

  img {
    cursor: pointer;
    vertical-align: middle;
  }
}

.el-login-footer {
  height: 40px;
  line-height: 40px;
  position: fixed;
  bottom: 0;
  width: 100%;
  text-align: center;
  color: #fff;
  font-family: Arial;
  font-size: 12px;
  letter-spacing: 1px;
}

.login-code-img {
  height: 38px;
}

.login-code-btn {
  width: 100%;
  height: 38px;
}

.other-login {
  margin-top: 20px;
  text-align: center;

  .divider {
    display: flex;
    align-items: center;
    margin: 20px 0;

    .line {
      flex: 1;
      height: 1px;
      background: #dcdfe6;
    }

    .text {
      color: #909399;
      padding: 0 12px;
      font-size: 14px;
    }
  }

  .social-login {
    margin-top: 15px;

    .wechat-icon {
      width: 40px;
      height: 40px;
      cursor: pointer;
      transition: all 0.3s;

      &:hover {
        transform: scale(1.1);
      }
    }
  }
}

// 对话框样式优化
:deep(.el-dialog) {
  border-radius: 8px;

  .el-dialog__header {
    padding: 20px;
    border-bottom: 1px solid #eee;
  }

  .el-dialog__body {
    padding: 30px 20px;
  }

  .el-dialog__footer {
    padding: 10px 20px 20px;
    border-top: 1px solid #eee;
  }
}

.social-login {
  .qrcode-icon {
    padding: 0;
    margin-left: 20px;

    i {
      font-size: 40px;
      transition: all 0.3s;

      &:hover {
        transform: scale(1.1);
        color: #409EFF;
      }
    }
  }
}

// 添加二维码对话框样式
.qr-dialog {
  .qr-code-container {
    text-align: center;
    padding: 20px;

    img {
      max-width: 200px;
      height: auto;
      margin-bottom: 15px;
    }

    .qr-tip {
      color: #606266;
      font-size: 14px;
    }
  }
}
</style>
