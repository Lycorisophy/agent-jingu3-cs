// 事件图种子（与 ES event_id / Milvus event_id 对齐）
// 在 Neo4j Browser 或 cypher-shell 中执行

MERGE (e1:Event {id: 'evt_user_001', type: 'user_query', userId: '001'})
MERGE (e2:Event {id: 'evt_demo_001', type: 'assistant_reply', userId: '001'})
MERGE (e1)-[:PRECEDES {confidence: 1.0}]->(e2);
