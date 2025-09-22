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
    
    Start-Process "java" -ArgumentList $JavaArgs -NoNewWindow -WorkingDirectory $ProjectRoot
    Write-Host "WeChat backend service started successfully" -ForegroundColor Green
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

# Cleanup processes
Write-Host "Stopping services..."
Get-Process | Where-Object { $_.ProcessName -eq "java" -and $_.CommandLine -like "*$BackendJar*" } | Stop-Process -Force
Write-Host "Services stopped"