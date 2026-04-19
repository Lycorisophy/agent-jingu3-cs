<template>
  <div class="design">
    <el-alert
      title="画布编辑 BPMN 2.0 XML，部署到本机 Flowable。ServiceTask 可绑定 delegateExpression，例如 ${jingu3LlmDelegate}（见服务端示例）。"
      type="info"
      show-icon
      :closable="false"
      style="margin-bottom: 0.75rem"
    />

    <div class="toolbar">
      <el-input v-model="deploymentName" placeholder="部署名称（可选）" style="width: 220px" clearable />
      <el-button type="primary" :loading="deploying" @click="deployFromCanvas">部署当前图</el-button>
      <el-button @click="downloadXml">导出 XML</el-button>
    </div>

    <div ref="containerRef" class="canvas" />

    <el-card shadow="never" class="hint">
      <template #header>变量与 Delegate（MVP 说明）</template>
      <ul>
        <li>示例流程 <code>demoLlm</code>：变量 <code>prompt</code> 传入 LLM；执行后变量 <code>llmOutput</code> / <code>llmError</code>。</li>
        <li>编排体验可参考「扣子」工作流指南心智；本实现为标准 BPMN + Flowable，不对接 Coze 云端 API。</li>
      </ul>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import BpmnModeler from 'bpmn-js/lib/Modeler'
import 'bpmn-js/dist/assets/diagram-js.css'
import 'bpmn-js/dist/assets/bpmn-font/css/bpmn.css'
import 'bpmn-js/dist/assets/bpmn-font/css/bpmn-embedded.css'
import { deployBpmnFile } from '@/api/bpmn'

type Modeler = InstanceType<typeof BpmnModeler>

const containerRef = ref<HTMLDivElement | null>(null)
const deploymentName = ref('')
const deploying = ref(false)
let modeler: Modeler | null = null

onMounted(async () => {
  if (!containerRef.value) {
    return
  }
  modeler = new BpmnModeler({ container: containerRef.value })
  await modeler.createDiagram()
})

onBeforeUnmount(() => {
  modeler?.destroy()
  modeler = null
})

async function deployFromCanvas() {
  if (!modeler) {
    ElMessage.error('编辑器未就绪')
    return
  }
  deploying.value = true
  try {
    const { xml } = await modeler.saveXML({ format: true })
    if (!xml) {
      throw new Error('无法生成 BPMN XML')
    }
    const file = new File([xml], 'diagram.bpmn20.xml', { type: 'application/xml' })
    const res = await deployBpmnFile(file, deploymentName.value.trim() || undefined)
    ElMessage.success(`部署成功：${res.deploymentId}，定义数 ${res.definitions?.length ?? 0}`)
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : String(e))
  } finally {
    deploying.value = false
  }
}

async function downloadXml() {
  if (!modeler) {
    return
  }
  const { xml } = await modeler.saveXML({ format: true })
  if (!xml) {
    return
  }
  const blob = new Blob([xml], { type: 'application/xml' })
  const a = document.createElement('a')
  a.href = URL.createObjectURL(blob)
  a.download = 'diagram.bpmn20.xml'
  a.click()
  URL.revokeObjectURL(a.href)
}
</script>

<style scoped>
.design {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}
.toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  align-items: center;
}
.canvas {
  height: min(70vh, 640px);
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  background: #fff;
}
.hint {
  margin-top: 0.5rem;
}
.hint ul {
  margin: 0;
  padding-left: 1.25rem;
  color: #606266;
  font-size: 14px;
  line-height: 1.6;
}
.hint code {
  font-size: 13px;
  background: #f4f4f5;
  padding: 0 4px;
  border-radius: 2px;
}
</style>
