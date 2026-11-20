<template>
  <section v-loading="loading">
    <p class="muted">
      保存常用成人乘机人，下次预订时直接选择。最多20位，仅保存演示姓名。
    </p>
    <div v-if="error" class="ui-error" role="alert">
      {{ error }} <el-button type="text" @click="load">重新加载</el-button>
    </div>
    <el-form inline @submit.native.prevent="save">
      <el-form-item :label="editing.id ? '编辑姓名' : '添加乘机人'"
        ><el-input
          v-model="editing.name"
          maxlength="50"
          placeholder="请输入姓名"
          :disabled="saving"
      /></el-form-item>
      <el-form-item
        ><el-button
          type="primary"
          native-type="submit"
          :loading="saving"
          :disabled="!editing.name.trim()"
          >{{ editing.id ? "保存修改" : "添加" }}</el-button
        ><el-button v-if="editing.id" @click="editing = { name: '' }"
          >取消编辑</el-button
        ></el-form-item
      >
    </el-form>
    <el-table :data="list" empty-text="还没有常用乘机人">
      <el-table-column prop="name" label="姓名" />
      <el-table-column label="操作"
        ><template slot-scope="scope"
          ><el-button
            type="text"
            :disabled="saving"
            @click="editing = { ...scope.row }"
            >编辑</el-button
          ><el-button type="text" :disabled="saving" @click="remove(scope.row)"
            >删除</el-button
          ></template
        ></el-table-column
      >
    </el-table>
  </section>
</template>
<script>
import {
  getPassengers,
  savePassenger,
  deletePassenger,
} from "../api/passenger";
export default {
  data: () => ({
    list: [],
    editing: { name: "" },
    loading: false,
    saving: false,
    error: "",
  }),
  created() {
    this.load();
  },
  methods: {
    async load() {
      this.loading = true;
      this.error = "";
      try {
        this.list = (await getPassengers()).data || [];
      } catch (e) {
        this.error = "常用乘机人加载失败，请重试";
      } finally {
        this.loading = false;
      }
    },
    async save() {
      if (this.saving || !this.editing.name.trim()) return;
      this.saving = true;
      try {
        this.list = (
          await savePassenger({
            ...this.editing,
            name: this.editing.name.trim(),
          })
        ).data;
        this.editing = { name: "" };
        this.$message.success("乘机人已保存");
      } catch (e) {
      } finally {
        this.saving = false;
      }
    },
    async remove(row) {
      try {
        await this.$confirm(
          "删除常用乘机人“" + row.name + "”？历史订单不会改变。",
          "删除乘机人"
        );
        this.saving = true;
        await deletePassenger(row.id);
        if (this.editing.id === row.id) this.editing = { name: "" };
        await this.load();
      } catch (e) {
      } finally {
        this.saving = false;
      }
    },
  },
};
</script>
