<template>
  <aside class="auth-story">
    <div class="auth-brand"><i class="el-icon-position" /> 云际航空</div>
    <span class="auth-eyebrow">A JOURNEY TO LOOK FORWARD TO</span>
    <h1>
      {{ register ? '每一次出发，' : '欢迎回来，' }}<br />{{
        register ? '都有新的期待。' : '下一程即将开启。'
      }}
    </h1>
    <p>从航班查询到行程管理，<br />让每一次飞行，都更从容。</p>
    <div class="auth-route">
      <span>此刻</span><i class="el-icon-position" /><span>下一站</span>
    </div>
    <div class="auth-services">
      <span><i class="el-icon-check" /> 在线预订</span
      ><span><i class="el-icon-check" /> 舱位选择</span
      ><span><i class="el-icon-check" /> 智能规划</span>
    </div>
    <router-link to="/">← 返回首页，继续探索</router-link>
  </aside>
</template>
<script>
export default { props: { register: Boolean } }
</script>
