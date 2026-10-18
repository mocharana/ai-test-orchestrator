<template>
  <el-card>
    <div style="display:flex; gap:10px; margin-bottom:14px">
      <el-button type="success" @click="openAdd">新增套件</el-button>
    </div>

    <el-table :data="rows" border stripe>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="suiteName" label="套件名称" min-width="140" />
      <el-table-column prop="pyPath" label="pytest 路径" min-width="220" show-overflow-tooltip />
      <el-table-column prop="description" label="备注" min-width="140" />
      <el-table-column prop="createTime" label="创建时间" width="170" />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="success" @click="run(row)">执行</el-button>
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="del(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination style="margin-top:14px; justify-content:flex-end"
      layout="total, prev, pager, next"
      :total="total" :page-size="query.pageSize" :current-page="query.pageNum"
      @current-change="load" />

    <el-dialog v-model="dialog" :title="form.id ? '编辑套件' : '新增套件'" width="520px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="套件名称"><el-input v-model="form.suiteName" /></el-form-item>
        <el-form-item label="pytest 路径">
          <el-input v-model="form.pyPath" placeholder="相对项目根，如 testcases/test_smoke_ui.py" />
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="form.description" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
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
const query = reactive({ pageNum: 1, pageSize: 10 })
const dialog = ref(false)
const form = reactive({})

async function load(pageNum) {
  if (pageNum) query.pageNum = pageNum
  const page = await request.get('/ui-suite/page', { params: query })
  rows.value = page.records
  total.value = Number(page.total)
}

function openAdd() {
  Object.keys(form).forEach((k) => delete form[k])
  dialog.value = true
}

function openEdit(row) {
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, row)
  dialog.value = true
}

async function save() {
  if (form.id) {
    await request.put('/ui-suite/' + form.id, form)
  } else {
    await request.post('/ui-suite', form)
  }
  ElMessage.success('保存成功')
  dialog.value = false
  load()
}

async function del(row) {
  await ElMessageBox.confirm('确认删除套件「' + row.suiteName + '」？', '提示', { type: 'warning' })
  await request.delete('/ui-suite/' + row.id)
  ElMessage.success('已删除')
  load()
}

async function run(row) {
  const id = await request.post('/ui-suite/' + row.id + '/run')
  ElMessage.success('已发起执行，批次 ' + id + '，Selenium 浏览器将自动打开，去「执行记录」查看结果')
}

onMounted(() => load())
</script>
