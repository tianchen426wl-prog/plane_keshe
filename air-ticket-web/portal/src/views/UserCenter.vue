<template>
  <div class="user-center">
    <h2 class="page-title">个人中心</h2>
    <el-tabs v-model="activeTab" type="border-card">
      <el-tab-pane label="常用乘机人" name="passengers"
        ><PassengerProfiles v-if="activeTab === 'passengers'"
      /></el-tab-pane>
      <!-- 个人信息 -->
      <el-tab-pane label="个人信息" name="info">
        <el-form
          ref="infoForm"
          :model="infoForm"
          :rules="infoRules"
          label-width="100px"
          style="max-width: 600px"
        >
          <el-form-item label="用户名">
            <el-input v-model="infoForm.username" disabled />
          </el-form-item>
          <el-form-item label="姓名" prop="name">
            <el-input v-model="infoForm.name" placeholder="请输入姓名" />
          </el-form-item>
          <el-form-item label="性别" prop="gender">
            <el-radio-group v-model="infoForm.gender">
              <el-radio label="男">男</el-radio>
              <el-radio label="女">女</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="手机号" prop="phone">
            <el-input v-model="infoForm.phone" placeholder="请输入手机号" />
          </el-form-item>
          <el-form-item label="邮箱" prop="email">
            <el-input v-model="infoForm.email" placeholder="请输入邮箱" />
          </el-form-item>
          <el-form-item label="身份证号" prop="identity">
            <el-input
              v-model="infoForm.identity"
              placeholder="请输入身份证号"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="saving" @click="saveInfo"
              >保存修改</el-button
            >
          </el-form-item>
        </el-form>
      </el-tab-pane>

      <!-- 修改密码 -->
      <el-tab-pane label="修改密码" name="password">
        <el-form
          ref="passwordForm"
          :model="passwordForm"
          :rules="passwordRules"
          label-width="120px"
          style="max-width: 500px"
        >
          <el-form-item label="旧密码" prop="oldPassword">
            <el-input
              v-model="passwordForm.oldPassword"
              type="password"
              placeholder="请输入旧密码"
            />
          </el-form-item>
          <el-form-item label="新密码" prop="newPassword">
            <el-input
              v-model="passwordForm.newPassword"
              type="password"
              placeholder="请输入新密码"
            />
          </el-form-item>
          <el-form-item label="确认新密码" prop="confirmPassword">
            <el-input
              v-model="passwordForm.confirmPassword"
              type="password"
              placeholder="请再次输入新密码"
            />
          </el-form-item>
          <el-form-item>
            <el-button
              type="primary"
              :loading="changing"
              @click="changePassword"
              >修改密码</el-button
            >
          </el-form-item>
        </el-form>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script>
import { mapState } from "vuex";
import { getUserInfo, updateUser, updatePassword } from "../api/user";
import PassengerProfiles from "../components/PassengerProfiles.vue";

export default {
  name: "UserCenter",
  components: { PassengerProfiles },
  data() {
    const validatePhone = (rule, value, callback) => {
      if (value && !/^1[3-9]\d{9}$/.test(value)) {
        callback(new Error("请输入正确的手机号"));
      } else {
        callback();
      }
    };
    const validateConfirmPassword = (rule, value, callback) => {
      if (value !== this.passwordForm.newPassword) {
        callback(new Error("两次输入的新密码不一致"));
      } else {
        callback();
      }
    };
    return {
      activeTab: "info",
      infoForm: {
        username: "",
        name: "",
        gender: "男",
        phone: "",
        email: "",
        identity: "",
      },
      infoRules: {
        name: [{ required: true, message: "请输入姓名", trigger: "blur" }],
        phone: [{ validator: validatePhone, trigger: "blur" }],
      },
      passwordForm: {
        oldPassword: "",
        newPassword: "",
        confirmPassword: "",
      },
      passwordRules: {
        oldPassword: [
          { required: true, message: "请输入旧密码", trigger: "blur" },
        ],
        newPassword: [
          { required: true, message: "请输入新密码", trigger: "blur" },
          { min: 6, message: "密码长度至少6位", trigger: "blur" },
        ],
        confirmPassword: [
          { required: true, message: "请确认新密码", trigger: "blur" },
          { validator: validateConfirmPassword, trigger: "blur" },
        ],
      },
      saving: false,
      changing: false,
    };
  },
  computed: {
    ...mapState(["user"]),
  },
  created() {
    this.loadUserInfo();
  },
  methods: {
    loadUserInfo() {
      if (!this.user) return;
      const userId = this.user.id || this.user.userId;
      getUserInfo(userId).then((res) => {
        const data = res.data;
        this.infoForm.username = data.username || "";
        this.infoForm.name = data.name || "";
        this.infoForm.gender = data.gender || "男";
        this.infoForm.phone = data.phone || "";
        this.infoForm.email = data.email || "";
        this.infoForm.identity = data.identity || "";
      });
    },
    saveInfo() {
      this.$refs.infoForm.validate((valid) => {
        if (!valid) return;
        this.saving = true;
        const userId = this.user.id || this.user.userId;
        updateUser({
          userId,
          name: this.infoForm.name,
          gender: this.infoForm.gender,
          phone: this.infoForm.phone,
          email: this.infoForm.email,
          identity: this.infoForm.identity,
        })
          .then(() => {
            this.$message.success("个人信息修改成功");
            this.saving = false;
          })
          .catch(() => {
            this.saving = false;
          });
      });
    },
    changePassword() {
      this.$refs.passwordForm.validate((valid) => {
        if (!valid) return;
        this.changing = true;
        const userId = this.user.id || this.user.userId;
        updatePassword({
          userId,
          oldPassword: this.passwordForm.oldPassword,
          newPassword: this.passwordForm.newPassword,
        })
          .then(() => {
            this.$message.success("密码修改成功，请重新登录");
            this.$store.dispatch("logout");
            this.$router.push("/login");
            this.changing = false;
          })
          .catch(() => {
            this.changing = false;
          });
      });
    },
  },
};
</script>

<style scoped>
.user-center {
  max-width: 900px;
  margin: 0 auto;
  padding: 20px;
}
.page-title {
  text-align: center;
  margin-bottom: 30px;
  color: #333;
  font-size: 28px;
}
</style>
