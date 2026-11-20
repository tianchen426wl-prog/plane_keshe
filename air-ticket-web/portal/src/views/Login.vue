<template>
  <div class="login-page">
    <AuthStory />
    <div class="login-card">
      <span class="auth-form-kicker">WELCOME ABOARD</span>
      <h2>登录你的账户</h2>
      <p class="auth-form-intro">登录后，预订航班并随时管理你的行程。</p>
      <div class="auth-tabs">
        <router-link to="/login">账户登录</router-link
        ><router-link to="/register">注册账户</router-link>
      </div>
      <el-form
        ref="loginForm"
        :model="form"
        :rules="rules"
        label-position="top"
        size="medium"
      >
        <el-form-item label="用户名" prop="username">
          <el-input
            v-model="form.username"
            placeholder="请输入用户名"
            prefix-icon="el-icon-user"
          />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            autocomplete="current-password"
            placeholder="请输入密码"
            prefix-icon="el-icon-lock"
            @keyup.enter.native="handleLogin"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            :loading="loading"
            style="width: 100%"
            @click="handleLogin"
          >
            登录并继续
          </el-button>
        </el-form-item>
      </el-form>
      <div class="link">
        <span>还没有账号？</span>
        <router-link to="/register">立即注册</router-link>
      </div>
      <p class="auth-note"><i class="el-icon-lock" /> 请妥善保管你的账户信息</p>
    </div>
  </div>
</template>

<script>
import AuthStory from '../components/AuthStory.vue'
import { login } from '../api/user'

export default {
  components: { AuthStory },
  name: 'Login',
  data() {
    return {
      form: {
        username: '',
        password: '',
      },
      rules: {
        username: [
          { required: true, message: '请输入用户名', trigger: 'blur' },
        ],
        password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
      },
      loading: false,
    }
  },
  methods: {
    handleLogin() {
      this.$refs.loginForm.validate((valid) => {
        if (!valid) return
        this.loading = true
        login({
          username: this.form.username,
          password: this.form.password,
        })
          .then((res) => {
            this.$store.dispatch('login', res.data)
            this.$message.success('登录成功')
            const redirect = this.$route.query.redirect || '/'
            this.$router.push(redirect)
            this.loading = false
          })
          .catch(() => {
            this.loading = false
          })
      })
    },
  },
}
</script>
