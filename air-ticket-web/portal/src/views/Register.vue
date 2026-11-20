<template>
  <div class="register-page">
    <AuthStory register />
    <div class="register-card">
      <span class="auth-form-kicker">JOIN THE JOURNEY</span>
      <h2>创建你的账户</h2>
      <p class="auth-form-intro">填写账户信息，开启便捷的航空出行体验。</p>
      <div class="auth-tabs">
        <router-link to="/login">账户登录</router-link
        ><router-link to="/register">注册账户</router-link>
      </div>
      <el-form
        ref="registerForm"
        :model="form"
        :rules="rules"
        label-position="top"
        size="medium"
      >
        <div class="auth-field-grid">
          <el-form-item label="用户名" prop="username">
            <el-input v-model="form.username" placeholder="请输入用户名" />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input
              v-model="form.password"
              type="password"
              show-password
              autocomplete="new-password"
              placeholder="请输入密码"
            />
          </el-form-item>
          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input
              v-model="form.confirmPassword"
              type="password"
              show-password
              autocomplete="new-password"
              placeholder="请再次输入密码"
            />
          </el-form-item>
          <el-form-item label="姓名" prop="name">
            <el-input v-model="form.name" placeholder="请输入姓名" />
          </el-form-item>
          <el-form-item label="性别" prop="gender">
            <el-radio-group v-model="form.gender">
              <el-radio label="男">男</el-radio>
              <el-radio label="女">女</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="手机号" prop="phone">
            <el-input v-model="form.phone" placeholder="请输入手机号" />
          </el-form-item>
          <el-form-item label="邮箱" prop="email">
            <el-input v-model="form.email" placeholder="请输入邮箱" />
          </el-form-item>
          <el-form-item label="身份证号" prop="identity">
            <el-input v-model="form.identity" placeholder="请输入身份证号" />
          </el-form-item>
        </div>
        <el-form-item>
          <el-button
            type="primary"
            :loading="loading"
            style="width: 100%"
            @click="handleRegister"
          >
            创建账户
          </el-button>
        </el-form-item>
      </el-form>
      <div class="link">
        <span>已有账号？</span>
        <router-link to="/login">立即登录</router-link>
      </div>
      <p class="auth-note"><i class="el-icon-lock" /> 请妥善保管你的账户信息</p>
    </div>
  </div>
</template>

<script>
import AuthStory from '../components/AuthStory.vue'
import { register } from '../api/user'

export default {
  components: { AuthStory },
  name: 'Register',
  data() {
    const validateConfirmPassword = (rule, value, callback) => {
      if (value !== this.form.password) {
        callback(new Error('两次输入的密码不一致'))
      } else {
        callback()
      }
    }
    const validatePhone = (rule, value, callback) => {
      if (value && !/^1[3-9]\d{9}$/.test(value)) {
        callback(new Error('请输入正确的手机号'))
      } else {
        callback()
      }
    }
    const validateIdentity = (rule, value, callback) => {
      if (value && !/^\d{17}[\dXx]$/.test(value)) {
        callback(new Error('请输入正确的身份证号'))
      } else {
        callback()
      }
    }
    return {
      form: {
        username: '',
        password: '',
        confirmPassword: '',
        name: '',
        gender: '男',
        phone: '',
        email: '',
        identity: '',
      },
      rules: {
        username: [
          { required: true, message: '请输入用户名', trigger: 'blur' },
        ],
        password: [
          { required: true, message: '请输入密码', trigger: 'blur' },
          { min: 6, message: '密码长度至少6位', trigger: 'blur' },
        ],
        confirmPassword: [
          { required: true, message: '请确认密码', trigger: 'blur' },
          { validator: validateConfirmPassword, trigger: 'blur' },
        ],
        name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
        phone: [
          { required: true, message: '请输入手机号', trigger: 'blur' },
          { validator: validatePhone, trigger: 'blur' },
        ],
        identity: [{ validator: validateIdentity, trigger: 'blur' }],
      },
      loading: false,
    }
  },
  methods: {
    handleRegister() {
      this.$refs.registerForm.validate((valid) => {
        if (!valid) return
        this.loading = true
        register({
          username: this.form.username,
          password: this.form.password,
          name: this.form.name,
          gender: this.form.gender === '男' ? 0 : 1,
          phone: this.form.phone,
          email: this.form.email,
          identity: this.form.identity,
        })
          .then(() => {
            this.$message.success('注册成功，请登录')
            this.$router.push('/login')
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
