# Litemall Web Backend and Frontend Startup Script
param(
    [string]$Profile = 'dev',
    [int]$Port = 8080,
    [switch]$SkipBuild,
    [switch]$SkipFrontend,
    [switch]$SkipBackend
)

Write-Host "========================================"
Write-Host "Litemall Web Backend and Frontend Startup Script"
Write-Host "========================================"

# Set variables
$ProjectRoot = Split-Path -Parent $PSScriptRoot
$MavenCmd = "mvn"
$BackendJar = "litemall-all-0.1.0-exec.jar"
$BackendJarPath = Join-Path $ProjectRoot "litemall-all\target\$BackendJar"
$FrontendPath = Join-Path $ProjectRoot "litemall-admin"

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
    Write-Host "Admin panel: http://localhost:$Port/admin"
    
    $JavaArgs = "-jar $BackendJarPath --server.port=$Port"
    if ($Profile -ne "dev") {
        $JavaArgs += " --spring.profiles.active=$Profile"
    }
    
    Start-Process "java" -ArgumentList $JavaArgs -NoNewWindow -WorkingDirectory $ProjectRoot
    Write-Host "Backend service started successfully" -ForegroundColor Green
}

# Start frontend service
if (-not $SkipFrontend) {
    Write-Host "Preparing to start frontend service..."
    
    if (-not (Test-Path $FrontendPath)) {
        Write-Host "Error: Frontend project directory not found: $FrontendPath" -ForegroundColor Red
        exit 1
    }
    
    Write-Host "Frontend project path: $FrontendPath"
    
    Set-Location $FrontendPath
    
    # Check if node_modules exists to avoid reinstalling dependencies
    if (-not (Test-Path "$FrontendPath\node_modules")) {
        Write-Host "Installing frontend dependencies..."
        Invoke-Expression "npm install"
    } else {
        Write-Host "Frontend dependencies already installed, skipping installation..."
    }
    
    Write-Host "Starting frontend development server..."
    Write-Host "Frontend access: http://localhost:9527"
    
    # Use cmd.exe to run npm properly
    Start-Process "cmd.exe" -ArgumentList "/c npm run dev" -NoNewWindow -WorkingDirectory $FrontendPath
    Write-Host "Frontend service started successfully" -ForegroundColor Green
}

Write-Host ""
Write-Host "========================================"
Write-Host "Press any key to stop all services..."
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")

# Cleanup processes
Write-Host "Stopping services..."
Get-Process | Where-Object { $_.ProcessName -eq "java" -and $_.CommandLine -like "*$BackendJar*" } | Stop-Process -Force
Get-Process | Where-Object { $_.ProcessName -eq "node" } | Stop-Process -Force
Write-Host "Services stopped"