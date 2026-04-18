/** 与 {@code ChatHttpHeaders.CLIENT_PLATFORM} 一致，由请求头发送 */
export const CLIENT_PLATFORM_HEADER = 'X-Jingu3-Client-Platform';

export interface ChatRequest {
  message: string;
  mode?: string;
  conversationId?: string;
  modePlan?: string[];
  workflowId?: string;
  requestId?: string;
  traceId?: string;
  clientPlatform?: string;
  /** 对上一轮输出的纠正或补充 */
  correctionNotes?: string;
  /** 丢弃 STM 最近一轮再生成 */
  undoLastStmTurn?: boolean;
  /** 将纠正写入服务端记忆（需配置开启） */
  persistUserCorrectionAsMemory?: boolean;
}

export interface PlanStepVo {
  mode?: string;
  reply?: string;
}

export interface ChatVo {
  userId?: string;
  username?: string;
  reply?: string;
  actionMode?: string;
  routingSource?: string;
  planSteps?: PlanStepVo[] | null;
}

export interface ApiResult<T> {
  success: boolean;
  code: string;
  message: string;
  data: T | null;
  timestamp: number;
}

export type StreamEventType =
  | 'META'
  | 'TOKEN'
  | 'STEP_BEGIN'
  | 'STEP_END'
  | 'BLOCK'
  | 'TOOL_RESULT'
  | 'DONE'
  | 'ERROR';

/** 与后端 {@link cn.lysoy.jingu3.stream.StreamEvent} JSON 对齐 */
export interface StreamEvent {
  type: StreamEventType;
  delta?: string;
  step?: number;
  label?: string;
  actionMode?: string;
  routingSource?: string;
  userId?: string;
  username?: string;
  block?: string;
  error?: string;
  toolId?: string;
  toolOutput?: string;
}
