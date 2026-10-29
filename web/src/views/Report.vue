<template>
  <div class="report">
    <!-- 统计卡片行：总览四指标 -->
    <div class="stat-grid">
      <div class="glass-card stat">
        <div class="num">{{ s.batches }}</div>
        <div class="label">执行批次</div>
      </div>
      <div class="glass-card stat">
        <div class="num">{{ s.cases }}</div>
        <div class="label">用例执行次数</div>
      </div>
      <div class="glass-card stat">
        <div class="num num-pass">{{ s.passed }}</div>
        <div class="label">通过</div>
      </div>
      <div class="glass-card stat">
        <div class="num num-fail">{{ s.failed }}</div>
        <div class="label">失败</div>
      </div>
    </div>

    <!-- 通过率 -->
    <div class="glass-card rate-card">
      <div class="rate-row">
        <span class="rate-label">用例通过率</span>
        <el-progress :percentage="s.passRate" :stroke-width="10" style="flex:1" />
      </div>
    </div>

    <!-- 按天趋势 -->
    <div class="glass-card section">
      <div class="glass-card__title">近 14 天执行趋势</div>
      <el-table :data="trend" size="small">
        <el-table-column prop="day" label="日期" min-width="240" />
        <el-table-column prop="batches" label="批次" width="120" />
        <el-table-column prop="passed" label="通过" width="120" />
        <el-table-column prop="failed" label="失败" width="120" />
      </el-table>
    </div>

    <!-- 失败 Top -->
    <div class="glass-card section">
      <div class="glass-card__title">失败 Top5 用例（接口用例）</div>
      <el-table :data="top" size="small">
        <el-table-column prop="caseName" label="用例" />
        <el-table-column prop="failTimes" label="失败次数" width="140" />
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import request from '../api/request'

const s = reactive({ batches: 0, cases: 0, passed: 0, failed: 0, passRate: 0 })
const trend = ref([])
const top = ref([])

onMounted(async () => {
  // 三个统计接口并行拉取，互不阻塞
  const [summary, trendData, topData] = await Promise.all([
    request.get('/report/summary'),
    request.get('/report/trend'),
    request.get('/report/top-failed')
  ])
  Object.assign(s, summary)
  trend.value = trendData
  top.value = topData
})
</script>

<style scoped>
.report {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
}
.stat {
  padding: 22px 20px;
  text-align: center;
}
.num {
  font-size: 30px;
  font-weight: 600;
  letter-spacing: -0.01em;
  color: #33517c;
}
.num-pass {
  color: #2c7057;
}
.num-fail {
  color: #8a4747;
}
.label {
  margin-top: 6px;
  font-size: 12.5px;
  letter-spacing: 0.02em;
  color: var(--text-sub);
}
.rate-card {
  padding: 18px 22px;
}
.rate-row {
  display: flex;
  align-items: center;
  gap: 18px;
}
.rate-label {
  flex-shrink: 0;
  font-size: 13.5px;
  color: var(--text-sub);
}
.section {
  padding: 18px 22px 10px;
}

@media (max-width: 1100px) {
  .stat-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
