// 事件图种子（与 ES event_id 对齐；边语义见 docs/设计/事件模型与关系类型.md）
// 使用单一关系类型 EVENT_LINK + rel_kind

MERGE (e1:Event {id: 'evt_user_001', type: 'user_query', user_id: '001'})
MERGE (e2:Event {id: 'evt_demo_001', type: 'assistant_reply', user_id: '001'})
MERGE (e1)-[r:EVENT_LINK {rel_kind: 'TEMPORAL_BEFORE', confidence: 1.0}]->(e2);
