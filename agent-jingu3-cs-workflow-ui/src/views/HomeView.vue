<template>
  <div class="page">
    <el-alert
      title="开发期无鉴权：请仅在内网使用；与对话 JSON 工作流（workflowId）并存，互不替代。"
      type="warning"
      show-icon
      :closable="false"
      style="margin-bottom: 1rem"
    />

    <el-card shadow="never">
      <template #header>
        <span>流程定义（最新版本）</span>
        <el-button type="primary" link style="float: right" @click="refreshAll">刷新</el-button>
      </template>
      <el-table v-loading="loadingDefs" :data="definitions" stripe size="small" style="width: 100%">
        <el-table-column prop="key" label="Key" width="180" />
        <el-table-column prop="name" label="名称" />
        <el-table-column prop="version" label="版本" width="80" />
        <el-table-column prop="id" label="定义 ID" min-width="280" show-overflow-tooltip />
      </el-table>
    </el-card>

    <el-card shadow="never" style="margin-top: 1rem">
      <template #header>试运行（流程变量 JSON）</template>
      <el-form label-width="140px" @submit.prevent>
        <el-form-item label="processDefinitionKey">
          <el-input v-model="startKey" placeholder="例如 demoLlm（classpath 示例）" />
        </el-form-item>
        <el-form-item label="variables">
          <el-input
            v-model="variablesJson"
            type="textarea"
            :rows="6"
            placeholder='示例：{"prompt":"用一句话介绍 Flowable"}'
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="starting" @click="runStart">启动实例</el-button>
        </el-form-item>
      </el-form>
      <el-alert v-if="startResult" type="success" :closable="false">
        <pre class="json">{{ pretty(startResult) }}</pre>
      </el-alert>
    </el-card>

    <el-row :gutter="16" style="margin-top: 1rem">
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>运行中实例</template>
          <el-table :data="running" size="small">
            <el-table-column prop="processInstanceId" label="实例 ID" show-overflow-tooltip />
            <el-table-column prop="processDefinitionId" label="定义 ID" show-overflow-tooltip />
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>待办任务</template>
          <el-table :data="tasks" size="small">
            <el-table-column prop="name" label="名称" width="120" />
            <el-table-column prop="id" label="任务 ID" show-overflow-tooltip />
            <el-table-column label="操作" width="100">
              <template #default="{ row }">
                <el-button type="primary" link size="small" @click="openComplete(row.id)">完成</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" style="margin-top: 1rem">
      <template #header>最近已完成（历史）</template>
      <el-table :data="history" size="small">
        <el-table-column prop="processDefinitionKey" label="Key" width="140" />
        <el-table-column prop="id" label="实例 ID" show-overflow-tooltip />
        <el-table-column prop="durationMs" label="耗时(ms)" width="110" />
        <el-table-column prop="endTime" label="结束时间(UTC)" width="200" />
      </el-table>
    </el-card>

    <el-dialog v-model="completeVisible" title="完成任务" width="480px" @closed="completeTaskId = ''">
      <p class="muted">任务 ID：{{ completeTaskId }}</p>
      <el-input
        v-model="completeVarsJson"
        type="textarea"
        :rows="4"
        placeholder="可选：完成变量 JSON，例如 {}"
      />
      <template #footer>
        <el-button @click="completeVisible = false">取消</el-button>
        <el-button type="primary" :loading="completing" @click="submitComplete">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  completeTask,
  listHistory,
  listProcessDefinitions,
  listRunning,
  listTasks,
  startProcess,
  type ProcessDefinitionItem,
  type HistoricProcessInstanceItem,
  type ProcessInstanceStart,
  type TaskItem,
} from '@/api/bpmn'

const loadingDefs = ref(false)
const definitions = ref<ProcessDefinitionItem[]>([])
const running = ref<Array<Record<string, string>>>([])
const tasks = ref<TaskItem[]>([])
const history = ref<HistoricProcessInstanceItem[]>([])

const startKey = ref('demoLlm')
const variablesJson = ref('{"prompt":"用一句话说明 BPMN ServiceTask 的作用"}')
const starting = ref(false)
const startResult = ref<ProcessInstanceStart | null>(null)

const completeVisible = ref(false)
const completeTaskId = ref('')
const completeVarsJson = ref('{}')
const completing = ref(false)

function pretty(v: unknown) {
  return JSON.stringify(v, null, 2)
}

async function refreshAll() {
  loadingDefs.value = true
  try {
    definitions.value = await listProcessDefinitions()
    running.value = await listRunning()
    tasks.value = await listTasks()
    history.value = await listHistory(30)
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : String(e))
  } finally {
    loadingDefs.value = false
  }
}

async function runStart() {
  let vars: Record<string, unknown> = {}
  try {
    vars = JSON.parse(variablesJson.value || '{}') as Record<string, unknown>
  } catch {
    ElMessage.error('variables 不是合法 JSON')
    return
  }
  starting.value = true
  startResult.value = null
  try {
    startResult.value = await startProcess(startKey.value.trim(), vars)
    await refreshAll()
    ElMessage.success('已启动')
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : String(e))
  } finally {
    starting.value = false
  }
}

function openComplete(taskId: string) {
  completeTaskId.value = taskId
  completeVarsJson.value = '{}'
  completeVisible.value = true
}

async function submitComplete() {
  let vars: Record<string, unknown> | undefined
  try {
    const o = JSON.parse(completeVarsJson.value || '{}') as Record<string, unknown>
    vars = Object.keys(o).length ? o : undefined
  } catch {
    ElMessage.error('完成变量不是合法 JSON')
    return
  }
  completing.value = true
  try {
    await completeTask(completeTaskId.value, vars)
    completeVisible.value = false
    await refreshAll()
    ElMessage.success('任务已完成')
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : String(e))
  } finally {
    completing.value = false
  }
}

onMounted(() => {
  void refreshAll()
})
</script>

<style scoped>
.json {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
  font-size: 12px;
}
.muted {
  color: #909399;
  font-size: 13px;
}
</style>
