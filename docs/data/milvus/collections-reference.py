"""
Milvus 集合参考（与 docs/设计/系统数据存储-物化清单与测试数据.md 一致）
依赖: pip install pymilvus
注意: dim 须与 Ollama/嵌入模型一致；换模型需新集合并全量重嵌入。
"""
from pymilvus import (
    Collection,
    CollectionSchema,
    DataType,
    FieldSchema,
    connections,
    utility,
)

# ---------- 1) 记忆向量（与 memory_entry.id 对齐，同 milvus-collection-design） ----------
def build_jingu3_memory_schema(dim: int) -> CollectionSchema:
    fields = [
        FieldSchema(
            name="memory_entry_id",
            dtype=DataType.INT64,
            is_primary=True,
            auto_id=False,
        ),
        FieldSchema(name="user_id", dtype=DataType.VARCHAR, max_length=64),
        FieldSchema(name="embedding", dtype=DataType.FLOAT_VECTOR, dim=dim),
    ]
    return CollectionSchema(fields, description="jingu3 memory vectors")


# ---------- 2) 事件语义向量（与 ES event_id 对齐） ----------
def build_jingu3_event_vectors_schema(dim: int) -> CollectionSchema:
    fields = [
        FieldSchema(
            name="event_id",
            dtype=DataType.VARCHAR,
            max_length=64,
            is_primary=True,
        ),
        FieldSchema(name="user_id", dtype=DataType.VARCHAR, max_length=64),
        FieldSchema(name="vector", dtype=DataType.FLOAT_VECTOR, dim=dim),
        FieldSchema(name="ts_ms", dtype=DataType.INT64),
    ]
    return CollectionSchema(fields, description="jingu3 event semantic vectors")


def example_create(uri: str = "http://127.0.0.1:19530", dim: int = 1024):
    connections.connect("default", uri=uri)
    for name, builder in (
        ("jingu3_memory", build_jingu3_memory_schema),
        ("jingu3_event_vectors", build_jingu3_event_vectors_schema),
    ):
        if utility.has_collection(name):
            utility.drop_collection(name)
        schema = builder(dim)
        col = Collection(name, schema)
        col.create_index(
            field_name="embedding" if "memory" in name else "vector",
            index_params={
                "metric_type": "COSINE" if "memory" in name else "IP",
                "index_type": "FLAT",
                "params": {},
            },
        )
        col.load()
        print(f"created: {name}")


if __name__ == "__main__":
    example_create()
