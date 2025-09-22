# litemall backend build script (PowerShell)
# Build all backend modules

param(
    [string]$Profile = "dev",
    [switch]$SkipTests = $true,
    [switch]$Clean = $false
)

Write-Host "========================================" -ForegroundColor Green
Write-Host "Litemall Backend Build Script" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green

# Set variables
$ProjectRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$MavenCmd = "mvn"

# Check if Maven is installed
if (-not (Get-Command $MavenCmd -ErrorAction SilentlyContinue)) {
    Write-Host "Error: Maven not found, please install Maven and configure environment variables" -ForegroundColor Red
    exit 1
}

# Change to project root directory
Set-Location $ProjectRoot

# Build arguments
$MavenArgs = @()
if ($Clean) {
    $MavenArgs += "clean"
}
$MavenArgs += "install"

if ($SkipTests) {
    $MavenArgs += "-DskipTests"
}

if ($Profile -ne "dev") {
    $MavenArgs += "-P$Profile"
}

# Display build information
Write-Host "Project root: $ProjectRoot" -ForegroundColor Cyan
Write-Host "Build command: $MavenCmd $($MavenArgs -join ' ')" -ForegroundColor Cyan
Write-Host "Skip tests: $SkipTests" -ForegroundColor Cyan
Write-Host "Build profile: $Profile" -ForegroundColor Cyan
Write-Host ""

# Start build
Write-Host "Starting backend build..." -ForegroundColor Yellow
$StartTime = Get-Date

try {
    & $MavenCmd $MavenArgs
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host ""
        Write-Host "✅ Backend build successful!" -ForegroundColor Green
        
        # Display build results
        $JarPath = Join-Path $ProjectRoot "litemall-all\target\litemall-all-0.1.0.jar"
        if (Test-Path $JarPath) {
            Write-Host "Generated JAR file: $JarPath" -ForegroundColor Green
            $JarSize = (Get-Item $JarPath).Length / 1MB
            Write-Host "File size: $([math]::Round($JarSize, 2)) MB" -ForegroundColor Green
        }
    } else {
        Write-Host ""
        Write-Host "❌ Backend build failed, please check error messages" -ForegroundColor Red
        exit 1
    }
}
catch {
    Write-Host ""
    Write-Host "❌ Build error occurred: $_" -ForegroundColor Red
    exit 1
}

$EndTime = Get-Date
$Duration = $EndTime - $StartTime
Write-Host "Build duration: $($Duration.TotalSeconds.ToString('F2')) seconds" -ForegroundColor Cyan

# Post-build options
Write-Host ""
Write-Host "Next steps after build:"
Write-Host "1. Run: java -jar $JarPath"
Write-Host "2. Docker: docker-compose up"
Write-Host "3. View logs: tail -f logs/litemall.log"