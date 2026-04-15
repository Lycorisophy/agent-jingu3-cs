import {
  CLIENT_PLATFORM_HEADER,
  type ApiResult,
  type ChatRequest,
  type ChatVo,
  type StreamEvent,
} from './types';

function getApiBase(): string {
  const fromEnv = (import.meta.env.VITE_API_BASE as string | undefined)?.trim();
  if (import.meta.env.DEV) {
    return '';
  }
  return fromEnv && fromEnv.length > 0 ? fromEnv : 'http://127.0.0.1:8080';
}

/** 与 README 约定一致：electron-win / electron-macos / electron-linux */
export function defaultClientPlatform(): string {
  const p = window.jingu3?.platform ?? '';
  const map: Record<string, string> = {
    win32: 'electron-win',
    darwin: 'electron-macos',
    linux: 'electron-linux',
  };
  return map[p] ?? 'electron-web';
}

function parseSseDataBlock(rawBlock: string): string | null {
  const normalized = rawBlock.replace(/\r\n/g, '\n');
  const lines = normalized.split('\n');
  const dataLines = lines.filter((l) => l.startsWith('data:'));
  if (dataLines.length === 0) {
    return null;
  }
  return dataLines.map((l) => l.slice(5).trimStart()).join('\n');
}

export async function postChat(
  body: ChatRequest,
  options?: { clientPlatformHeader?: string; signal?: AbortSignal },
): Promise<ChatVo> {
  const base = getApiBase();
  const res = await fetch(`${base}/api/v1/chat`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'application/json',
      ...(options?.clientPlatformHeader
        ? { [CLIENT_PLATFORM_HEADER]: options.clientPlatformHeader }
        : {}),
    },
    body: JSON.stringify(body),
    signal: options?.signal,
  });
  const json = (await res.json()) as ApiResult<ChatVo>;
  if (!res.ok) {
    throw new Error(json?.message || `HTTP ${res.status}`);
  }
  if (!json.success) {
    throw new Error(json.message || json.code || '请求失败');
  }
  if (json.data == null) {
    throw new Error('响应 data 为空');
  }
  return json.data;
}

/**
 * POST SSE：解析 {@code text/event-stream}，按事件回调。
 */
export async function postChatStream(
  body: ChatRequest,
  options: {
    onEvent: (event: StreamEvent) => void;
    clientPlatformHeader?: string;
    signal?: AbortSignal;
  },
): Promise<void> {
  const base = getApiBase();
  const res = await fetch(`${base}/api/v1/chat/stream`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'text/event-stream',
      ...(options.clientPlatformHeader
        ? { [CLIENT_PLATFORM_HEADER]: options.clientPlatformHeader }
        : {}),
    },
    body: JSON.stringify(body),
    signal: options.signal,
  });
  if (!res.ok) {
    const text = await res.text();
    let msg = text || `HTTP ${res.status}`;
    try {
      const j = JSON.parse(text) as ApiResult<unknown>;
      if (j?.message) {
        msg = j.message;
      }
    } catch {
      // 非 JSON
    }
    throw new Error(msg);
  }
  const reader = res.body?.getReader();
  if (!reader) {
    throw new Error('响应无 body');
  }
  const decoder = new TextDecoder();
  let buf = '';
  for (;;) {
    const { done, value } = await reader.read();
    if (done) {
      break;
    }
    buf += decoder.decode(value, { stream: true });
    buf = buf.replace(/\r\n/g, '\n');
    for (;;) {
      const sep = buf.indexOf('\n\n');
      if (sep === -1) {
        break;
      }
      const block = buf.slice(0, sep);
      buf = buf.slice(sep + 2);
      const data = parseSseDataBlock(block);
      if (data == null || data.length === 0 || data.startsWith(':')) {
        continue;
      }
      try {
        const ev = JSON.parse(data) as StreamEvent;
        options.onEvent(ev);
      } catch {
        // 忽略非 JSON 心跳等
      }
    }
  }
}
