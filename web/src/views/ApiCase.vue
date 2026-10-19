<template>
  <el-card>
    <div style="display:flex; gap:10px; margin-bottom:14px">
      <el-input v-model="query.module" placeholder="模块" style="width:160px" clearable />
      <el-input v-model="query.caseName" placeholder="用例名称" style="width:200px" clearable />
      <el-button type="primary" @click="load(1)">查询</el-button>
      <el-button @click="reset">重置</el-button>
      <el-button type="warning" @click="aiDialog = true">AI 生成用例</el-button>
      <el-button type="success" style="margin-left:auto" @click="openAdd">新增用例</el-button>
    </div>

    <el-table :data="rows" border stripe>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="caseName" label="用例名称" min-width="140" />
      <el-table-column prop="module" label="模块" width="100" />
      <el-table-column label="优先级" width="80">
        <template #default="{ row }">
          <el-tag :type="['danger', 'warning', 'info'][row.priority]">{{ ['P0', 'P1', 'P2'][row.priority] }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="method" label="方法" width="80" />
      <el-table-column prop="url" label="URL" min-width="220" show-overflow-tooltip />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="success" @click="runCase(row)">执行</el-button>
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="del(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination style="margin-top:14px; justify-content:flex-end"
      layout="total, prev, pager, next"
      :total="total" :page-size="query.pageSize" :current-page="query.pageNum"
      @current-change="load" />

    <el-dialog v-model="dialog" :title="form.id ? '编辑用例' : '新增用例'" width="640px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="用例名称"><el-input v-model="form.caseName" /></el-form-item>
        <el-form-item label="模块"><el-input v-model="form.module" placeholder="不填默认：默认" /></el-form-item>
        <el-form-item label="优先级">
          <el-select v-model="form.priority" style="width:120px">
            <el-option label="P0" :value="0" /><el-option label="P1" :value="1" /><el-option label="P2" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="请求方法">
          <el-select v-model="form.method" style="width:120px">
            <el-option v-for="m in ['GET', 'POST', 'PUT', 'DELETE']" :key="m" :label="m" :value="m" />
          </el-select>
        </el-form-item>
        <el-form-item label="URL"><el-input v-model="form.url" /></el-form-item>
        <el-form-item label="请求头"><el-input v-model="form.headers" type="textarea" :rows="2" placeholder='JSON 数组，如 [{"key":"Authorization","value":"Bearer {{token}}"}]' /></el-form-item>
        <el-form-item label="查询参数"><el-input v-model="form.params" type="textarea" :rows="2" placeholder='JSON 对象，如 {"pageNum":1}' /></el-form-item>
        <el-form-item label="请求体"><el-input v-model="form.body" type="textarea" :rows="2" placeholder='JSON 对象（POST/PUT 用）' /></el-form-item>
        <el-form-item label="断言规则"><el-input v-model="form.asserts" type="textarea" :rows="2" placeholder='JSON 数组，如 [{"type":"http_status","expect":200}]' /></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <!-- AI 生成：输入需求 → 调 /ai/generate-cases（耗时 15s+，单独放宽超时） -->
    <el-dialog v-model="aiDialog" title="AI 生成接口用例" width="640px">
      <el-input v-model="aiRequirement" type="textarea" :rows="5"
        placeholder="描述要测的功能。例：测试若依登录接口，地址 http://localhost:8080/login，POST，body 含 username/password，正确账号返回 code 200 和 token，密码错误返回错误提示" />
      <template #footer>
        <el-button @click="aiDialog = false">取消</el-button>
        <el-button type="primary" :loading="aiLoading" @click="aiGenerate">
          {{ aiLoading ? 'AI 思考中（约 15 秒）...' : '生成' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- AI 生成结果报告：总数/入库/被拦原因 -->
    <el-dialog v-model="aiResultDialog" title="AI 生成结果报告" width="640px">
      <el-descriptions :column="3" border size="small">
        <el-descriptions-item label="AI 总产出">{{ aiResult.total }}</el-descriptions-item>
        <el-descriptions-item label="校验通过入库">{{ aiResult.accepted }}</el-descriptions-item>
        <el-descriptions-item label="被拦截">{{ (aiResult.rejected || []).length }}</el-descriptions-item>
      </el-descriptions>
      <div v-if="aiResult.savedNames && aiResult.savedNames.length" style="margin-top:10px; font-size:13px">
        已入库：{{ aiResult.savedNames.join('、') }}
      </div>
      <div v-for="r in aiResult.rejected || []" :key="r" style="color:#f56c6c; margin-top:6px; font-size:13px">
        ✗ {{ r }}
      </div>
      <template #footer>
        <el-button type="primary" @click="aiResultDialog = false">知道了</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../api/request'

const rows = ref([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 10, module: '', caseName: '' })
const dialog = ref(false)
const form = reactive({})

// AI 生成相关状态
const aiDialog = ref(false)
const aiResultDialog = ref(false)
const aiRequirement = ref('')
const aiLoading = ref(false)
const aiResult = ref({})

async function load(pageNum) {
  if (pageNum) query.pageNum = pageNum
  const page = await request.get('/api-case/page', { params: query })
  rows.value = page.records
  total.value = Number(page.total)
}

function reset() {
  query.module = ''
  query.caseName = ''
  load(1)
}

function openAdd() {
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, { priority: 2, method: 'GET' })
  dialog.value = true
}

function openEdit(row) {
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, row)
  dialog.value = true
}

async function save() {
  if (form.id) {
    await request.put('/api-case/' + form.id, form)
  } else {
    await request.post('/api-case', form)
  }
  ElMessage.success('保存成功')
  dialog.value = false
  load()
}

async function del(row) {
  await ElMessageBox.confirm('确认删除用例「' + row.caseName + '」？', '提示', { type: 'warning' })
  await request.delete('/api-case/' + row.id)
  ElMessage.success('已删除')
  load()
}

async function runCase(row) {
  const id = await request.post('/execution/run', { caseIds: [row.id] })
  ElMessage.success('已发起执行，批次 ' + id + '，去「执行记录」查看结果')
}

/** AI 生成：全局 axios 超时 15s 不够（实测 15~17s），单请求放宽到 60s */
async function aiGenerate() {
  if (!aiRequirement.value.trim()) {
    ElMessage.warning('先描述要测的功能')
    return
  }
  aiLoading.value = true
  try {
    aiResult.value = await request.post('/ai/generate-cases',
      { requirement: aiRequirement.value }, { timeout: 60000 })
    aiDialog.value = false
    aiResultDialog.value = true
    load() // 刷新列表，新用例立刻可见
  } finally {
    aiLoading.value = false
  }
}

onMounted(() => load())
</script>
