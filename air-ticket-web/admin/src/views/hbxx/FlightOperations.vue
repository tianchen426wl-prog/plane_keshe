<template>
 <el-collapse class="flight-operations" @change="load"><el-collapse-item title="每日航班、班期与票价规则" name="operations">
  <el-alert title="每日00:05补齐未来30天，启动时自动补漏；补齐任务不会重置已售库存。可在下方查看生成记录与班期设置。" type="info" :closable="false"/>
  <div class="operations-actions"><el-button type="primary" :loading="generating" @click="generate">补齐缺失航班</el-button><el-button @click="load">刷新任务状态</el-button></div>
  <el-tabs><el-tab-pane label="生成记录"><el-table :data="runs" size="small"><el-table-column prop="id" label="任务" width="70"/><el-table-column prop="windowStart" label="起始日期"/><el-table-column prop="windowEnd" label="结束日期"/><el-table-column prop="status" label="状态"/><el-table-column prop="createdCount" label="新增"/><el-table-column prop="skippedCount" label="跳过"/><el-table-column prop="failedCount" label="失败"/></el-table></el-tab-pane>
   <el-tab-pane label="班期模板"><p class="muted">启停仅影响尚未生成的日期；已有每日实例请在航班列表停售。已有实例不自动改期或取消。</p><el-table :data="schedules" max-height="360" size="small"><el-table-column prop="flightNo" label="航班号" width="130"/><el-table-column prop="companyName" label="航司" width="140"/><el-table-column label="航线" min-width="130"><template slot-scope="s">{{ s.row.departureCity }} → {{ s.row.arrivalCity }}</template></el-table-column><el-table-column prop="departureTime" label="起飞" width="95"/><el-table-column prop="weekdays" label="星期" width="95"/><el-table-column prop="basePrice" label="基础价" width="90"/><el-table-column label="启用" width="95"><template slot-scope="s"><el-switch :value="s.row.enabled===1" @change="toggle(s.row,$event)"/></template></el-table-column></el-table></el-tab-pane>
   <el-tab-pane label="产品规则"><article v-for="p in policies" :key="p.code"><h3>{{ p.name }} · {{ p.code }}</h3><p>{{ p.description }}</p><p>币种 {{ p.currency }} · 有效期 {{ p.effectiveFrom }} 至 {{ p.effectiveTo }}</p></article></el-tab-pane>
  </el-tabs>
 </el-collapse-item></el-collapse>
</template>
<script>
import request from '@/api/request'
export default{data(){return{runs:[],schedules:[],policies:[],generating:false}},methods:{async load(){try{const [r,s,p]=await Promise.all([request.get('/admin/hbxx/generation-runs'),request.get('/admin/hbxx/schedules'),request.get('/admin/hbxx/fare-policies')]);this.runs=r.data;this.schedules=s.data;this.policies=p.data}catch(e){}},async generate(){this.generating=true;try{await request.post('/admin/hbxx/generate',null,{timeout:180000});this.$message.success('任务已完成，请查看新增、跳过和失败数量');await this.load()}catch(e){}finally{this.generating=false}},async toggle(row,enabled){try{await request.put('/admin/hbxx/schedules/'+row.id+'/enabled',{enabled});await this.load()}catch(e){}}}}
</script>
<style scoped>.flight-operations{margin-bottom:24px}.operations-actions{display:flex;gap:12px;margin:18px 0}article{padding:12px 0;border-bottom:1px solid #e8edf3}</style>
