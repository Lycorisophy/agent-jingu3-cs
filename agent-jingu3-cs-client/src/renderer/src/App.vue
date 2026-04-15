<script setup lang="ts">
import { computed, ref } from 'vue';
import { defaultClientPlatform, postChat, postChatStream } from './api/chatApi';
import type { ChatRequest, ChatVo, PlanStepVo, StreamEvent } from './api/types';

const MODES = ['ASK', 'REACT', 'PLAN_AND_EXECUTE', 'WORKFLOW', 'AGENT_TEAM'] as const;

const message = ref('你好');
const mode = ref<string>('ASK');
const workflowId = ref('');
const useStream = ref(true);
const sending = ref(false);
const errorText = ref('');

const reply = ref('');
const actionMode = ref<string | null>(null);
const routingSource = ref<string | null>(null);
const planSteps = ref<PlanStepVo[]>([]);

/** 流式编排时间线：STEP_BEGIN/END、TOOL_RESULT 摘要 */
const streamTimeline = ref<{ kind: string; text: string }[]>([]);

const clientPlatformPreview = computed(() => defaultClientPlatform());

const showWorkflowId = computed(() => mode.value === 'WORKFLOW');

function resetOutput(): void {
  reply.value = '';
  actionMode.value = null;
  routingSource.value = null;
  planSteps.value = [];
  streamTimeline.value = [];
  errorText.value = '';
}

function applyChatVo(vo: ChatVo): void {
  reply.value = vo.reply ?? '';
  actionMode.value = vo.actionMode ?? null;
  routingSource.value = vo.routingSource ?? null;
  planSteps.value = vo.planSteps ?? [];
}

function handleStreamEvent(ev: StreamEvent): void {
  switch (ev.type) {
    case 'META':
      actionMode.value = ev.actionMode ?? null;
      routingSource.value = ev.routingSource ?? null;
      break;
    case 'TOKEN':
      if (ev.delta) {
        reply.value += ev.delta;
      }
      break;
    case 'BLOCK':
      if (ev.block) {
        reply.value += ev.block;
      }
      break;
    case 'TOOL_RESULT':
      streamTimeline.value.push({
        kind: 'TOOL_RESULT',
        text: `[${ev.toolId ?? ''}] ${ev.toolOutput ?? ''}`,
      });
      break;
    case 'STEP_BEGIN':
      streamTimeline.value.push({
        kind: 'STEP',
        text: `开始 #${ev.step ?? ''} ${ev.label ?? ''}`,
      });
      break;
    case 'STEP_END':
      streamTimeline.value.push({
        kind: 'STEP',
        text: `结束 #${ev.step ?? ''}`,
      });
      break;
    case 'ERROR':
      errorText.value = ev.error ?? '流式错误';
      break;
    case 'DONE':
    default:
      break;
  }
}

function buildRequest(): { body: ChatRequest; platform: string } {
  const platform = defaultClientPlatform();
  const body: ChatRequest = {
    message: message.value.trim(),
    mode: mode.value,
    clientPlatform: platform,
  };
  if (mode.value === 'WORKFLOW' && workflowId.value.trim()) {
    body.workflowId = workflowId.value.trim();
  }
  return { body, platform };
}

let abort: AbortController | null = null;

async function onSend(): Promise<void> {
  if (!message.value.trim() || sending.value) {
    return;
  }
  abort?.abort();
  abort = new AbortController();
  resetOutput();
  sending.value = true;
  const { body, platform } = buildRequest();
  try {
    if (useStream.value) {
      await postChatStream(body, {
        signal: abort.signal,
        clientPlatformHeader: platform,
        onEvent: handleStreamEvent,
      });
    } else {
      const vo = await postChat(body, {
        signal: abort.signal,
        clientPlatformHeader: platform,
      });
      applyChatVo(vo);
    }
  } catch (e) {
    errorText.value = e instanceof Error ? e.message : String(e);
  } finally {
    sending.value = false;
  }
}
</script>

<template>
  <div class="layout">
    <header class="top">
      <div class="brand">JinGu3 Agent</div>
      <div class="badges" aria-label="服务端路由角标">
        <span v-if="actionMode != null" class="badge mode">actionMode: {{ actionMode }}</span>
        <span v-if="routingSource != null" class="badge route">routingSource: {{ routingSource }}</span>
        <span v-if="actionMode == null && routingSource == null && !sending" class="badge muted"
          >等待首包 META 或非流式响应</span
        >
      </div>
    </header>

    <main class="main">
      <section class="panel controls">
        <label class="row">
          <span>行动模式</span>
          <select v-model="mode" class="input">
            <option v-for="m in MODES" :key="m" :value="m">{{ m }}</option>
          </select>
        </label>
        <label v-if="showWorkflowId" class="row">
          <span>workflowId</span>
          <input v-model="workflowId" class="input" type="text" placeholder="WORKFLOW 时建议填写" />
        </label>
        <label class="row check">
          <input v-model="useStream" type="checkbox" />
          <span>流式（POST /api/v1/chat/stream，SSE）</span>
        </label>
        <div class="row hint">
          请求头 {{ clientPlatformPreview }}（与 JSON body.clientPlatform 一致）
        </div>
        <label class="row grow">
          <span>消息</span>
          <textarea v-model="message" class="input textarea" rows="4" placeholder="输入后发送"></textarea>
        </label>
        <button type="button" class="send" :disabled="sending" @click="onSend">
          {{ sending ? '处理中…' : '发送' }}
        </button>
        <p v-if="errorText" class="err">{{ errorText }}</p>
      </section>

      <section class="panel reply">
        <h2>回复</h2>
        <pre class="reply-body">{{ reply || '（空）' }}</pre>

        <h3 v-if="planSteps.length">编排结果 planSteps（非流式）</h3>
        <ol v-if="planSteps.length" class="timeline">
          <li v-for="(s, i) in planSteps" :key="i">
            <strong>{{ s.mode }}</strong>
            <pre class="step-reply">{{ s.reply }}</pre>
          </li>
        </ol>

        <h3 v-if="streamTimeline.length">流式时间线</h3>
        <ul v-if="streamTimeline.length" class="timeline stream">
          <li v-for="(t, i) in streamTimeline" :key="i">
            <span class="tl-kind">{{ t.kind }}</span>
            {{ t.text }}
          </li>
        </ul>
      </section>
    </main>
  </div>
</template>

<style scoped>
.layout {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}
.top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 16px;
  background: #1e293b;
  color: #f8fafc;
}
.brand {
  font-weight: 600;
}
.badges {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: flex-end;
}
.badge {
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 12px;
  background: #334155;
}
.badge.mode {
  background: #0f766e;
}
.badge.route {
  background: #6d28d9;
}
.badge.muted {
  background: #475569;
  color: #e2e8f0;
}
.main {
  display: grid;
  grid-template-columns: minmax(280px, 360px) 1fr;
  gap: 16px;
  padding: 16px;
  flex: 1;
  align-items: start;
}
@media (max-width: 720px) {
  .main {
    grid-template-columns: 1fr;
  }
}
.panel {
  background: #fff;
  border-radius: 10px;
  padding: 16px;
  box-shadow: 0 1px 3px rgb(0 0 0 / 8%);
}
.controls .row {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 12px;
}
.row.check {
  flex-direction: row;
  align-items: center;
}
.row.grow {
  flex: 1;
}
.hint {
  font-size: 12px;
  color: #64748b;
}
.input {
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  padding: 8px 10px;
  font: inherit;
}
.textarea {
  resize: vertical;
  min-height: 100px;
}
.send {
  width: 100%;
  padding: 10px;
  font-weight: 600;
  border: none;
  border-radius: 8px;
  background: #2563eb;
  color: #fff;
  cursor: pointer;
}
.send:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.err {
  color: #b91c1c;
  font-size: 14px;
}
.reply h2,
.reply h3 {
  margin: 0 0 8px;
  font-size: 1rem;
}
.reply-body {
  margin: 0 0 16px;
  white-space: pre-wrap;
  word-break: break-word;
  background: #f1f5f9;
  padding: 12px;
  border-radius: 8px;
  min-height: 120px;
}
.timeline {
  margin: 0;
  padding-left: 1.2rem;
}
.step-reply {
  margin: 4px 0 0;
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 13px;
  background: #f8fafc;
  padding: 8px;
  border-radius: 6px;
}
.stream {
  list-style: disc;
}
.tl-kind {
  font-size: 11px;
  color: #64748b;
  margin-right: 6px;
}
</style>
