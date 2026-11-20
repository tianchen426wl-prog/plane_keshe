<template>
  <section class="seat-map" aria-label="选择座位">
    <p class="muted">座位布局 · 高亮为已选 · 灰色不可选 · 标有金额的座位额外收费</p>
    <p>已选 {{ value.length }}/{{ quantity }}：{{ value.join('、') || '未手选，将免费自动分配' }}</p>
    <div class="seat-scroll"><div class="seat-cabin"><div v-for="row in rows" :key="row.number" class="seat-row"><small>{{ row.number }}</small>
      <button v-for="seat in row.seats" :key="seat.seatNo" type="button" class="seat" :class="{chosen:value.includes(seat.seatNo),occupied:seat.state!=='AVAILABLE',aisle:seat.seatColumn==='D'}" :disabled="disabled||seat.state!=='AVAILABLE'" :data-testid="'seat-'+seat.seatNo" :aria-pressed="String(value.includes(seat.seatNo))" :aria-label="seat.seatNo+'选座费'+seat.seatFee+'元'" @click="toggle(seat.seatNo)">{{ seat.seatNo }}<small>{{ Number(seat.seatFee)>0?'¥'+seat.seatFee:'免费' }}</small></button>
    </div></div></div>
    <el-button type="text" :disabled="disabled" @click="$emit('input',[])">使用免费自动分配</el-button>
    <p class="muted">自动分配优先同排，可能分散；以报价座位号为准。提交占位前座位可能变化。</p>
  </section>
</template>
<script>
export default {props:{seats:{type:Array,default:()=>[]},value:{type:Array,default:()=>[]},quantity:Number,disabled:Boolean},computed:{rows(){const rows=new Map();this.seats.forEach(s=>{if(!rows.has(s.seatRow))rows.set(s.seatRow,[]);rows.get(s.seatRow).push(s)});return [...rows].map(([number,seats])=>({number,seats}))}},methods:{toggle(no){const selected=[...this.value];const i=selected.indexOf(no);if(i>=0)selected.splice(i,1);else if(selected.length<this.quantity)selected.push(no);else{this.$message.info('已选满，请先取消一个座位');return}this.$emit('input',selected)}}}
</script>
<style scoped>
.seat-map{margin:20px 0}.seat-scroll{max-height:310px;overflow:auto;border:1px solid var(--border);border-radius:12px;padding:16px;background:#f8fafc}.seat-cabin{width:max-content;margin:auto}.seat-row{display:flex;gap:6px;align-items:center;margin-bottom:6px}.seat-row>small{width:18px;color:var(--muted)}.seat{width:45px;min-height:44px;border:1px solid #b9c9db;border-radius:7px;background:white;font-size:12px;color:var(--ink);cursor:pointer}.seat small{display:block;font-size:10px}.seat.aisle{margin-left:18px}.seat.chosen{background:var(--brand);color:white;border-color:var(--brand)}.seat.occupied{background:#e2e8f0;color:#94a3b8;cursor:not-allowed}@media(max-width:500px){.seat-scroll{padding:8px}.seat{width:36px}.seat-row{gap:4px}.seat.aisle{margin-left:10px}}
</style>
