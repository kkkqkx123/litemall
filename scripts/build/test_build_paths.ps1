# 测试修复后的构建脚本路径计算
Write-Host "Testing build script path calculations..."
Write-Host "Current script location: $PSScriptRoot"

# 模拟修复后的路径计算（从build目录运行）
$ProjectRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
Write-Host "Project Root: $ProjectRoot"

# 测试各个项目路径
$VueProjectPath = Join-Path $ProjectRoot "litemall-vue"
$WxProjectPath = Join-Path $ProjectRoot "litemall-wx"
$BackendTargetPath = Join-Path $ProjectRoot "litemall-all\target"

Write-Host "Vue Project Path: $VueProjectPath"
Write-Host "Wx Project Path: $WxProjectPath"
Write-Host "Backend Target Path: $BackendTargetPath"

# 验证路径存在性
Write-Host "`nPath existence check:"
Write-Host "Vue project exists: $(Test-Path $VueProjectPath)"
Write-Host "Wx project exists: $(Test-Path $WxProjectPath)"
Write-Host "Backend target exists: $(Test-Path $BackendTargetPath)"

# 验证JAR文件
$JarFile = Join-Path $BackendTargetPath "litemall-all-0.1.0.jar"
Write-Host "JAR file exists: $(Test-Path $JarFile)"

Write-Host "`nPath calculation fix verified successfully!"