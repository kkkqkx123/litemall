# Litemall WeChat Backend and Frontend Startup Script
param(
    [string]$Profile = 'dev',
    [int]$BackendPort = 8080,
    [int]$WxApiPort = 8082,
    [switch]$SkipBuild,
    [switch]$SkipFrontend,
    [switch]$SkipBackend,
    [switch]$SkipDatabaseCheck
)

# 存储启动的进程ID
$global:StartedProcesses = @()

# 清理函数
function Cleanup-Processes {
    Write-Host "`n正在清理进程..." -ForegroundColor Yellow
    
    # 停止所有记录的进程
    foreach ($procId in $global:StartedProcesses) {
        $process = Get-Process -Id $procId -ErrorAction SilentlyContinue
        if ($process) {
            Stop-Process -Id $procId -Force -ErrorAction SilentlyContinue
            Write-Host "已停止进程: $($process.ProcessName) (PID: $procId)" -ForegroundColor Green
        }
    }
    
    Write-Host "清理完成" -ForegroundColor Green
}

Write-Host "========================================"
Write-Host "Litemall WeChat Backend and Frontend Startup Script"
Write-Host "========================================"

# Set variables
$ProjectRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$MavenCmd = "mvn"
$BackendJar = "litemall-all-0.1.0-exec.jar"
$BackendJarPath = Join-Path $ProjectRoot "litemall-all\target\$BackendJar"
$DatabaseConfigPath = Join-Path $ProjectRoot "litemall-db\src\main\resources\application-db.yml"
$WxProjectPath = Join-Path $ProjectRoot "litemall-wx"

Write-Host "Project root: $ProjectRoot"
Write-Host "Environment profile: $Profile"
Write-Host "Backend port: $BackendPort"
Write-Host "WeChat API port: $WxApiPort"
Write-Host "WeChat mini program path: $WxProjectPath"
Write-Host ""
Write-Host "Access URLs:"
Write-Host "- Backend: http://localhost:$BackendPort"
Write-Host "- WeChat API: http://localhost:$BackendPort/wx"
Write-Host "- Admin Panel: http://localhost:$BackendPort/admin"
Write-Host "- Swagger API Doc: http://localhost:$BackendPort/swagger-ui.html"
Write-Host "- H2 Console: http://localhost:$BackendPort/h2-console"
Write-Host "- WeChat Mini Program: Import $WxProjectPath in WeChat Developer Tools"
Write-Host ""

# Start backend service
if (-not $SkipBackend) {
    Write-Host "Preparing to start WeChat backend service..."
    
    if (-not (Get-Command $MavenCmd -ErrorAction SilentlyContinue)) {
        Write-Host "Error: Maven not found, please install Maven and configure environment variables" -ForegroundColor Red
        exit 1
    }
    
    # Check if MySQL is running (if not skipped)
    if (-not $SkipDatabaseCheck) {
        Write-Host "Checking MySQL database connection..."
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
    
    Write-Host "Starting WeChat backend service..."
    Write-Host "Access URL: http://localhost:$BackendPort"
    Write-Host "WeChat API: http://localhost:$BackendPort/wx"
    Write-Host "Admin panel: http://localhost:$BackendPort/admin"
    Write-Host "Swagger API Doc: http://localhost:$BackendPort/swagger-ui.html"
    Write-Host "H2 Console: http://localhost:$BackendPort/h2-console (JDBC URL: jdbc:h2:~/litemall)"
    
    $JavaArgs = "-jar $BackendJarPath --server.port=$BackendPort"
    if ($Profile -ne "dev") {
        $JavaArgs += " --spring.profiles.active=$Profile"
    }
    
    $backendProcess = Start-Process "java" -ArgumentList $JavaArgs -NoNewWindow -WorkingDirectory $ProjectRoot -PassThru
    $global:StartedProcesses += $backendProcess.Id
    Write-Host "WeChat backend service started successfully (PID: $($backendProcess.Id))" -ForegroundColor Green
}

# Start WeChat mini program
if (-not $SkipFrontend) {
    Write-Host "Preparing to start WeChat mini program..."
    
    $WxProjectPath = Join-Path $ProjectRoot "litemall-wx"
    if (-not (Test-Path $WxProjectPath)) {
        Write-Host "Error: WeChat mini program project directory not found: $WxProjectPath" -ForegroundColor Red
        exit 1
    }
    
    Write-Host "WeChat mini program project path: $WxProjectPath" -ForegroundColor Green
    Write-Host ""
    Write-Host "WeChat mini program startup instructions:"
    Write-Host "1. Open WeChat Developer Tools"
    Write-Host "2. Import project: $WxProjectPath"
    Write-Host "3. Configure project settings"
    Write-Host "4. Click compile and run"
    Write-Host ""
    Write-Host "Frontend access information:"
    Write-Host "- WeChat Mini Program: Use WeChat Developer Tools to open $WxProjectPath"
    Write-Host "- Backend API: http://localhost:$BackendPort/wx"
    Write-Host "- Admin Panel: http://localhost:$BackendPort/admin"
    Write-Host ""
    
    Write-Host "WeChat mini program project is ready" -ForegroundColor Green
}

Write-Host ""
Write-Host "========================================"
Write-Host "Press any key to stop all services..."
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")

# 调用清理函数
Cleanup-Processes

# 确保退出
exit 0