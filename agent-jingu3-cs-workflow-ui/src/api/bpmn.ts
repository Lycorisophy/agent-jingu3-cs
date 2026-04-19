import { api } from './client'

export interface ProcessDefinitionItem {
  id: string
  key: string
  name: string | null
  version: number
}

export interface ProcessInstanceStart {
  processInstanceId: string
  processDefinitionId: string
  variables: Record<string, unknown>
}

export interface HistoricProcessInstanceItem {
  id: string
  processDefinitionId: string
  processDefinitionKey: string
  durationMs: number | null
  endTime: string | null
}

export interface TaskItem {
  id: string
  name: string | null
  processInstanceId: string
  assignee: string | null
}

export interface BpmnDeployResult {
  deploymentId: string
  deploymentName: string
  definitions: ProcessDefinitionItem[]
}

export function listProcessDefinitions() {
  return api.get<ProcessDefinitionItem[]>('/api/v1/bpmn/process-definitions')
}

export function startProcess(processDefinitionKey: string, variables: Record<string, unknown>) {
  return api.post<ProcessInstanceStart>('/api/v1/bpmn/process-instances/start', {
    processDefinitionKey,
    variables,
  })
}

export function listRunning() {
  return api.get<Array<Record<string, string>>>('/api/v1/bpmn/process-instances/running')
}

export function listTasks() {
  return api.get<TaskItem[]>('/api/v1/bpmn/tasks')
}

export function completeTask(taskId: string, variables?: Record<string, unknown>) {
  return api.post<void>(`/api/v1/bpmn/tasks/${encodeURIComponent(taskId)}/complete`, variables ?? {})
}

export function listHistory(limit = 20) {
  return api.get<HistoricProcessInstanceItem[]>('/api/v1/bpmn/history/process-instances', {
    params: { limit },
  })
}

export function deployBpmnFile(file: File, deploymentName?: string) {
  const fd = new FormData()
  fd.append('file', file)
  if (deploymentName) {
    fd.append('deploymentName', deploymentName)
  }
  return api.post<BpmnDeployResult>('/api/v1/bpmn/deploy', fd, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}
