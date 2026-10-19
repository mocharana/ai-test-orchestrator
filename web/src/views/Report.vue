<template>
  <div>
    <!-- 统计卡片行：总览四指标 -->
    <el-row :gutter="12">
      <el-col :span="6"><el-card><div class="stat"><div class="num">{{ s.batches }}</div><div class="label">执行批次</div></div></el-card></el-col>
      <el-col :span="6"><el-card><div class="stat"><div class="num">{{ s.cases }}</div><div class="label">用例执行次数</div></div></el-card></el-col>
      <el-col :span="6"><el-card><div class="stat"><div class="num" style="color:#67c23a">{{ s.passed }}</div><div class="label">通过</div></div></el-card></el-col>
      <el-col :span="6"><el-card><div class="stat"><div class="num" style="color:#f56c6c">{{ s.failed }}</div><div class="label">失败</div></div></el-card></el-col>
    </el-row>

    <!-- 通过率 -->
    <el-card style="margin-top:12px">
      <div style="display:flex; align-items:center; gap:16px">
        <span style="white-space:nowrap">用例通过率</span>
        <el-progress :percentage="s.passRate" style="flex:1" />
      </div>
    </el-card>

    <!-- 按天趋势 -->
    <el-card header="近 14 天执行趋势" style="margin-top:12px">
      <el-table :data="trend" border size="small">
        <el-table-column prop="day" label="日期" />
        <el-table-column prop="batches" label="批次" width="120" />
        <el-table-column prop="passed" label="通过" width="120" />
        <el-table-column prop="failed" label="失败" width="120" />
      </el-table>
    </el-card>

    <!-- 失败 Top -->
    <el-card header="失败 Top5 用例（接口用例）" style="margin-top:12px">
      <el-table :data="top" border size="small">
        <el-table-column prop="caseName" label="用例" />
        <el-table-column prop="failTimes" label="失败次数" width="140" />
      </el-table>
    </el-card>
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
.stat { text-align: center; }
.num { font-size: 28px; font-weight: bold; }
.label { color: #909399; font-size: 13px; margin-top: 4px; }
</style>
