<template>
  <el-card>
    <div style="margin-bottom:14px">
      <el-button type="primary" @click="load()">刷新</el-button>
      <span style="margin-left:10px; color:#909399; font-size:13px">提示：执行中批次会从 RUNNING 变为 FINISHED，手动刷新查看</span>
    </div>

    <el-table :data="rows" border stripe>
      <el-table-column prop="id" label="批次ID" width="80" />
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="row.status === 'FINISHED' ? 'success' : 'warning'">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="totalCount" label="总数" width="70" />
      <el-table-column prop="passedCount" label="通过" width="70" />
      <el-table-column prop="failedCount" label="失败" width="70" />
      <el-table-column prop="createBy" label="发起人" width="100" />
      <el-table-column prop="createTime" label="发起时间" width="170" />
      <el-table-column prop="finishTime" label="完成时间" width="170" />
      <el-table-column label="操作" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="primary" @click="showDetail(row)">查看明细</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination style="margin-top:14px; justify-content:flex-end"
      layout="total, prev, pager, next"
      :total="total" :page-size="query.pageSize" :current-page="query.pageNum"
      @current-change="load" />

    <el-dialog v-model="detailDialog" title="执行明细" width="860px">
      <!-- 接口用例明细 -->
      <template v-if="detailType === 'api'">
        <el-table :data="apiRows" border size="small">
          <el-table-column prop="caseName" label="用例" min-width="130" />
          <el-table-column label="结果" width="80">
            <template #default="{ row }">
              <el-tag :type="row.passed === 1 ? 'success' : 'danger'">{{ row.passed === 1 ? '通过' : '失败' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="httpStatus" label="状态码" width="80" />
          <el-table-column prop="assertSummary" label="断言摘要" min-width="180" show-overflow-tooltip />
          <el-table-column prop="costMs" label="耗时(ms)" width="90" />
          <el-table-column label="操作" width="110" fixed="right">
            <template #default="{ row }">
              <!-- 只有失败行给 AI 归因入口 -->
              <el-button v-if="row.passed === 0" size="small" type="warning"
                :loading="aiBusyId === row.id" @click="analyze(row)">AI 归因</el-button>
            </template>
          </el-table-column>
        </el-table>
      </template>
      <!-- UI 套件明细 -->
      <template v-else-if="detailType === 'ui'">
        <div v-for="r in uiRows" :key="r.id" style="margin-bottom:12px">
          <p><b>{{ r.suiteName }}</b>
            <el-tag :type="r.passed === 1 ? 'success' : 'danger'" size="small" style="margin:0 8px">
              {{ r.passed === 1 ? '全部通过' : '存在失败' }}
            </el-tag>
            <span style="color:#909399; font-size:12px">exit={{ r.exitCode }} · {{ r.summary }} · {{ r.costMs }}ms</span>
          </p>
          <el-input type="textarea" :model-value="r.output" :rows="10" readonly style="margin-top:6px" />
        </div>
      </template>
      <p v-else style="color:#909399">暂无明细数据</p>
    </el-dialog>

    <!-- AI 归因结果：两段式文本 -->
    <el-dialog v-model="aiOpen" title="AI 失败归因" width="680px">
      <div style="white-space:pre-wrap; line-height:1.7">{{ aiText }}</div>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import request from '../api/request'

const rows = ref([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 10 })
const detailDialog = ref(false)
const detailType = ref('api')
const apiRows = ref([])
const uiRows = ref([])

async function load(pageNum) {
  if (pageNum) query.pageNum = pageNum
  const page = await request.get('/execution/page', { params: query })
  rows.value = page.records
  total.value = Number(page.total)
}

async function showDetail(row) {
  // 先查接口用例明细；为空再查 UI 套件明细（批次表共用，按数据落在哪边判断类型）
  const api = await request.get('/execution/' + row.id + '/results')
  if (api && api.length > 0) {
    detailType.value = 'api'
    apiRows.value = api
    uiRows.value = []
  } else {
    const ui = await request.get('/ui-suite/results/' + row.id)
    detailType.value = 'ui'
    uiRows.value = ui
    apiRows.value = []
  }
  detailDialog.value = true
}

/** AI 归因：按单条失败明细调 /ai/analyze-failure/{resultId}（15s+，放宽超时） */
const aiText = ref('')
const aiOpen = ref(false)
const aiBusyId = ref(null)

async function analyze(row) {
  aiBusyId.value = row.id
  try {
    aiText.value = await request.post('/ai/analyze-failure/' + row.id, {}, { timeout: 60000 })
    aiOpen.value = true
  } finally {
    aiBusyId.value = null
  }
}

onMounted(() => load())
</script>
