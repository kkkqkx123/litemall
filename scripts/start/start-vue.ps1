# Litemall Vue Mobile Frontend Startup Script
param(
    [string]$Profile = 'dev',
    [int]$Port = 8080,
    [switch]$SkipBuild,
    [switch]$SkipBackend,
    [switch]$SkipFrontend,
    [switch]$SkipDatabaseCheck
)

# 设置错误处理和信号处理
try {
    # 注册Ctrl+C处理程序
    [Console]::TreatControlCAsInput = $false
    
    # 存储启动的进程ID
    $global:StartedProcesses = @()
    
    # 清理函数
    function Cleanup-Processes {
        Write-Host "`n正在清理进程..." -ForegroundColor Yellow
        
        # 停止所有记录的进程
        foreach ($procId in $global:StartedProcesses) {
            try {
                $process = Get-Process -Id $procId -ErrorAction SilentlyContinue
                if ($process) {
                    $process | Stop-Process -Force -ErrorAction SilentlyContinue
                    Write-Host "已停止进程: $($process.ProcessName) (PID: $procId)" -ForegroundColor Green
                }
            } catch {
                Write-Host "停止进程时出错: $_" -ForegroundColor Red
            }
        }
        
        # 额外的清理逻辑
        try {
            # 停止Java后端进程
            Get-Process -Name "java" -ErrorAction SilentlyContinue | Where-Object { $_.CommandLine -like "*$BackendJar*" } | ForEach-Object {
                Stop-Process -Id $_.Id -Force -ErrorAction SilentlyContinue
                Write-Host "已停止Java进程: $($_.Id)" -ForegroundColor Green
            }
            
            # 停止Node.js进程
            Get-Process -Name "node" -ErrorAction SilentlyContinue | Where-Object { $_.CommandLine -like "*npm*" -or $_.CommandLine -like "*vue*" } | ForEach-Object {
                Stop-Process -Id $_.Id -Force -ErrorAction SilentlyContinue
                Write-Host "已停止Node.js进程: $($_.Id)" -ForegroundColor Green
            }
            
            # 停止CMD进程
            Get-Process -Name "cmd" -ErrorAction SilentlyContinue | Where-Object { $_.CommandLine -like "*npm*" } | ForEach-Object {
                Stop-Process -Id $_.Id -Force -ErrorAction SilentlyContinue
                Write-Host "已停止CMD进程: $($_.Id)" -ForegroundColor Green
            }
        } catch {
            Write-Host "清理额外进程时出错: $_" -ForegroundColor Red
        }
        
        Write-Host "清理完成" -ForegroundColor Green
    }
    
    # 注册退出处理程序
    Register-EngineEvent -SourceIdentifier PowerShell.Exiting -Action { Cleanup-Processes } -ErrorAction SilentlyContinue
    
} catch {
    Write-Host "初始化错误处理时出错: $_" -ForegroundColor Red
}

Write-Host "========================================"
Write-Host "Litemall Vue Mobile Frontend Startup Script"
Write-Host "========================================"

# Set variables
$ProjectRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$MavenCmd = "mvn"
$BackendJar = "litemall-all-0.1.0-exec.jar"
$BackendJarPath = Join-Path $ProjectRoot "litemall-all\target\$BackendJar"
$FrontendPath = Join-Path $ProjectRoot "litemall-vue"

Write-Host "Project root: $ProjectRoot"
Write-Host "Environment profile: $Profile"
Write-Host "Port: $Port"
Write-Host ""

# Start backend service
if (-not $SkipBackend) {
    Write-Host "Preparing to start backend service..."
    
    if (-not (Get-Command $MavenCmd -ErrorAction SilentlyContinue)) {
        Write-Host "Error: Maven not found, please install Maven and configure environment variables" -ForegroundColor Red
        exit 1
    }
    
    # Check if MySQL is running
    if (-not $SkipDatabaseCheck) {
        Write-Host "Checking MySQL database connection..."
        try {
            $MySQLConnection = Test-NetConnection -ComputerName localhost -Port 3306 -WarningAction SilentlyContinue
            if (-not $MySQLConnection.TcpTestSucceeded) {
                Write-Host "❌ MySQL service is not running or port 3306 is not open" -ForegroundColor Red
                Write-Host "Please ensure MySQL is installed and running, database 'litemall' is created" -ForegroundColor Yellow
                Write-Host "Database connection URL: jdbc:mysql://127.0.0.1:3306/litemall" -ForegroundColor Yellow
                Write-Host "Username: root, Password: 1234567kk" -ForegroundColor Yellow
                pause
                exit 1
            }
            Write-Host "✅ MySQL connection is normal" -ForegroundColor Green
        } catch {
            Write-Host "Could not check MySQL connection, please ensure MySQL is running" -ForegroundColor Yellow
        }
    } else {
        Write-Host "⚠️ Skipping database connection check" -ForegroundColor Yellow
    }

    if (-not (Test-Path $BackendJarPath) -and -not $SkipBuild) {
        Write-Host "Backend JAR file not found, starting build..."
        Set-Location $ProjectRoot
        
        $MavenArgs = "clean install -DskipTests"
        if ($Profile -ne "dev") {
            $MavenArgs += " -P$Profile"
        }
        
        Write-Host "Executing command: $MavenCmd $MavenArgs"
        Invoke-Expression "$MavenCmd $MavenArgs"
        
        if ($LASTEXITCODE -ne 0) {
            Write-Host "Backend build failed" -ForegroundColor Red
            exit 1
        }
    }
    
    Write-Host "Starting backend service..."
    Write-Host "Access URL: http://localhost:$Port"
    
    $JavaArgs = "-jar $BackendJarPath --server.port=$Port"
    if ($Profile -ne "dev") {
        $JavaArgs += " --spring.profiles.active=$Profile"
    }
    
    try {
        $backendProcess = Start-Process -FilePath "java" -ArgumentList $JavaArgs -NoNewWindow -WorkingDirectory $ProjectRoot -PassThru
        $global:StartedProcesses += $backendProcess.Id
        Write-Host "Backend service started successfully (PID: $($backendProcess.Id))" -ForegroundColor Green
    } catch {
        Write-Host "Error starting backend service: $_" -ForegroundColor Red
        exit 1
    }
}

# Start mobile frontend service
if (-not $SkipFrontend) {
    Write-Host "Preparing to start mobile frontend service..."
    
    if (-not (Test-Path $FrontendPath)) {
        Write-Host "Error: Mobile frontend project directory not found: $FrontendPath" -ForegroundColor Red
        exit 1
    }
    
    Write-Host "Mobile frontend project path: $FrontendPath"
    
    Set-Location $FrontendPath
    
    # Check if node_modules exists to avoid reinstalling dependencies
    if (-not (Test-Path "$FrontendPath\node_modules")) {
        Write-Host "Installing mobile frontend dependencies..."
        Invoke-Expression "npm install"
    } else {
        Write-Host "Mobile frontend dependencies already installed, skipping installation..."
    }
    
    Write-Host "Starting mobile frontend development server..."
    Write-Host "Mobile frontend access: http://localhost:8081"
    
    # Use cmd.exe to run npm properly
    try {
        $frontendProcess = Start-Process "cmd.exe" -ArgumentList "/c npm run dev" -NoNewWindow -WorkingDirectory $FrontendPath -PassThru
        $global:StartedProcesses += $frontendProcess.Id
        Write-Host "Mobile frontend service started successfully (PID: $($frontendProcess.Id))" -ForegroundColor Green
    } catch {
        Write-Host "Error starting mobile frontend service: $_" -ForegroundColor Red
        exit 1
    }
}

Write-Host ""
Write-Host "========================================"
Write-Host "Press any key to stop all services..."
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")

# 调用清理函数
Cleanup-Processes

# 确保退出
exit 0