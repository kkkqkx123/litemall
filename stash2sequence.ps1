# 检查是否在Git仓库中
if (-not (Test-Path -Path .git)) {
    Write-Error "当前目录不是Git仓库，请在Git仓库根目录运行此脚本"
    exit 1
}

# 获取暂存区文件列表（仅文件名）
try {
    # 捕获标准输出，忽略错误流
    $stagedFiles = git diff --cached --name-only 2>$null
}
catch {
    Write-Error "获取暂存文件失败：$_"
    exit 1
}

# 检查是否有暂存文件
if (-not $stagedFiles) {
    Write-Host "没有暂存的文件"
    exit 0
}

# 处理文件名（包含空格的添加双引号）
$processedFiles = @()
foreach ($file in $stagedFiles) {
    if ($file -match '\s') {
        $processedFiles += "`"$file`""  # 包含空格的文件名用双引号包裹
    }
    else {
        $processedFiles += $file
    }
}

# 转换为空格分隔的字符串
$filesAsString = $processedFiles -join ' '

# 输出结果
Write-Output $filesAsString