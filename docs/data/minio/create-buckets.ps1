# MinIO 桶初始化（需已安装 mc 并配置 alias，例如: mc alias set local http://127.0.0.1:9000 minioadmin minioadmin）
$ErrorActionPreference = "Stop"
$buckets = @(
  "jingu3-skills",
  "jingu3-documents",
  "jingu3-generated-code",
  "jingu3-session-artifacts",
  "jingu3-embeddings-cache"
)
foreach ($b in $buckets) {
  mc mb -p "local/$b" 2>$null
  Write-Host "bucket: $b"
}
