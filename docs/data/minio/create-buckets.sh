#!/usr/bin/env bash
# MinIO 桶初始化（需已配置 mc alias，例如: mc alias set local http://127.0.0.1:9000 minioadmin minioadmin）
set -euo pipefail

BUCKETS=(
  jingu3-skills
  jingu3-documents
  jingu3-generated-code
  jingu3-session-artifacts
  jingu3-embeddings-cache
)

for b in "${BUCKETS[@]}"; do
  mc mb -p "local/${b}" 2>/dev/null || true
  echo "bucket ready: ${b}"
done

echo "示例对象键（与 dev_seed 技能一致）:"
echo "  jingu3-skills/skills/<skillId>/<version>/SKILL.md"
