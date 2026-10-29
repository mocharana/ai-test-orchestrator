<template>
  <div class="glass-card">
    <div class="toolbar">
      <el-button type="primary" @click="load()">刷新</el-button>
      <span class="toolbar-tip">执行中批次会从 RUNNING 变为 FINISHED，手动刷新查看</span>
    </div>

    <el-table :data="rows">
      <el-table-column prop="id" label="批次ID" width="76" />
      <el-table-column label="类型" width="82">
        <template #default="{ row }">
          <el-tag :class="row.execType === 'UI' ? 'tag-ui' : 'tag-api'" size="small">
            {{ row.execType === 'UI' ? 'UI' : '接口' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="执行对象" min-width="200" show-overflow-tooltip>
        <template #default="{ row }">
          {{ row.targetName || '—' }}{{ row.totalCount > 1 ? ` 等 ${row.totalCount} 条` : '' }}
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :class="row.status === 'FINISHED' ? 'tag-mint' : 'tag-p1'">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="totalCount" label="总数" width="64" />
      <el-table-column prop="passedCount" label="通过" width="64" />
      <el-table-column prop="failedCount" label="失败" width="64" />
      <el-table-column prop="createBy" label="发起人" width="90" />
      <el-table-column label="发起时间" width="180">
        <template #default="{ row }">{{ fmtTime(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="完成时间" width="180">
        <template #default="{ row }">{{ fmtTime(row.finishTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" fixed="right" width="110">
        <template #default="{ row }">
          <el-button size="small" type="primary" @click="showDetail(row)">查看明细</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination style="margin-top:14px; justify-content:flex-end"
      layout="total, prev, pager, next"
      :total="total" :page-size="query.pageSize" :current-page="query.pageNum"
      @current-change="load" />

    <!-- append-to-body：glass-card 的 backdrop-filter 会创建包含块，弹窗不加此项会被裁在卡片内 -->
    <el-dialog v-model="detailDialog" title="执行明细" width="860px" append-to-body>
      <!-- 接口用例明细 -->
      <template v-if="detailType === 'api'">
        <el-table :data="apiRows" size="small" row-key="id" :expand-row-keys="expandedIds">
          <!-- 展开列：AI 归因结果内联展示在失败行下方（箭头隐藏，由「AI 归因」按钮控制） -->
          <el-table-column type="expand" width="1">
            <template #default="{ row }">
              <div class="ai-panel">
                <div class="ai-panel__head">
                  <span>AI 失败归因</span>
                  <el-button link type="primary" @click="toggleExpand(row.id)">收起</el-button>
                </div>
                <div class="ai-panel__body">{{ aiResults[row.id] }}</div>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="caseName" label="用例" min-width="130" />
          <el-table-column label="结果" width="80">
            <template #default="{ row }">
              <el-tag :class="row.passed === 1 ? 'tag-mint' : 'tag-p0'">{{ row.passed === 1 ? '通过' : '失败' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="httpStatus" label="状态码" width="80" />
          <el-table-column prop="assertSummary" label="断言摘要" min-width="180" show-overflow-tooltip />
          <el-table-column prop="costMs" label="耗时(ms)" width="90" />
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <!-- 只有失败行给 AI 归因入口；已有结果时按钮变为展开/收起，不再重复调接口 -->
              <el-button v-if="row.passed === 0" size="small" type="warning"
                :loading="aiBusyId === row.id" @click="analyze(row)">
                {{ !aiResults[row.id] ? 'AI 归因' : (isExpanded(row.id) ? '收起' : '查看归因') }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </template>
      <!-- UI 套件明细 -->
      <template v-else-if="detailType === 'ui'">
        <div v-for="r in uiRows" :key="r.id" class="ui-block">
          <p class="ui-head">
            <b>{{ r.suiteName }}</b>
            <el-tag :class="r.passed === 1 ? 'tag-mint' : 'tag-p0'" size="small">
              {{ r.passed === 1 ? '全部通过' : '存在失败' }}
            </el-tag>
            <span class="ui-meta">exit={{ r.exitCode }} · {{ r.summary }} · {{ r.costMs }}ms</span>
          </p>
          <el-input type="textarea" :model-value="r.output" :rows="10" readonly />
        </div>
      </template>
      <p v-else class="empty-tip">暂无明细数据</p>
    </el-dialog>
  </div>
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

/** 后端 LocalDateTime 序列化成 2026-10-21T16:16:18，展示时把 T 换成空格 */
function fmtTime(t) {
  return t ? String(t).replace('T', ' ') : '—'
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

/**
 * AI 归因：按单条失败明细调 /ai/analyze-failure/{resultId}（15s+，放宽超时）。
 * 结果常驻 aiResults：收起后仍保留，再点「查看归因」直接展开，不重复调接口。
 */
const aiResults = reactive({})
const expandedIds = ref([])
const aiBusyId = ref(null)

async function analyze(row) {
  // 已有结果：只切换展开状态
  if (aiResults[row.id]) {
    toggleExpand(row.id)
    return
  }
  aiBusyId.value = row.id
  try {
    aiResults[row.id] = await request.post('/ai/analyze-failure/' + row.id, {}, { timeout: 60000 })
    if (!expandedIds.value.includes(row.id)) expandedIds.value.push(row.id)
  } finally {
    aiBusyId.value = null
  }
}

function toggleExpand(id) {
  const i = expandedIds.value.indexOf(id)
  if (i >= 0) expandedIds.value.splice(i, 1)
  else expandedIds.value.push(id)
}

function isExpanded(id) {
  return expandedIds.value.includes(id)
}

onMounted(() => load())
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
}
.toolbar-tip {
  color: var(--text-faint);
  font-size: 12.5px;
}
.ui-block {
  margin-bottom: 14px;
}
.ui-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 0 0 8px;
}
.ui-meta {
  color: var(--text-faint);
  font-size: 12px;
}
.empty-tip {
  color: var(--text-faint);
  font-size: 13px;
}

/* ===== 接口明细：AI 归因内联面板 ===== */
/* 隐藏表格自带的展开箭头——展开动作统一由「AI 归因 / 收起」按钮驱动 */
:deep(.el-table__expand-icon) {
  display: none;
}
:deep(.el-table__expand-column) {
  padding: 0 !important;
}
:deep(.el-table__expanded-cell) {
  padding: 0 !important;
  background: transparent !important;
}
.ai-panel {
  margin: 4px 12px 10px;
  padding: 14px 16px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.7);
  border: 1px solid rgba(140, 160, 195, 0.16);
}
.ai-panel__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
  font-size: 12.5px;
  color: var(--text-sub);
}
.ai-panel__body {
  white-space: pre-wrap;
  line-height: 1.75;
  font-size: 13px;
  color: var(--text-main);
}
</style>
