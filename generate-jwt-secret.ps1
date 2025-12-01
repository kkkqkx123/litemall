# 生成JWT密钥脚本
# 生成256位（32字节）的安全随机密钥

# 生成32字节的随机字节
$randomBytes = New-Object byte[] 32
$rng = New-Object System.Security.Cryptography.RNGCryptoServiceProvider
$rng.GetBytes($randomBytes)

# 将字节转换为Base64字符串
$jwtSecret = [Convert]::ToBase64String($randomBytes)

# 输出结果
Write-Host "生成的JWT密钥 (Base64): $jwtSecret"
Write-Host ""
Write-Host "密钥长度: $($jwtSecret.Length) 字符"
Write-Host "原始字节长度: 256 位 (32 字节)"
Write-Host ""

# 将密钥写入配置文件
$configFile = "litemall-admin-api\src\main\resources\application-admin.yml"
$configContent = Get-Content -Path $configFile -Raw

# 替换JWT密钥配置
$newConfigContent = $configContent -replace 'secret: \$\{JWT_SECRET:[^}]+\}', "secret: `$`{JWT_SECRET:$jwtSecret}"

# 写入配置文件
Set-Content -Path $configFile -Value $newConfigContent

Write-Host "JWT密钥已成功更新到配置文件: $configFile"
Write-Host ""
Write-Host "注意: 在生产环境中，建议通过环境变量 JWT_SECRET 来设置密钥"
Write-Host "当前配置支持环境变量覆盖，如需使用环境变量，请设置: JWT_SECRET=你的密钥"