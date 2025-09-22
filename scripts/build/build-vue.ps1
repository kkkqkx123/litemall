# litemall Vue frontend build script (PowerShell)
# Build Vue mobile mall

param(
    [string]$Environment = "production",
    [switch]$Dev = $false,
    [switch]$SkipInstall = $false,
    [switch]$Analyze = $false
)

Write-Host "========================================" -ForegroundColor Green
Write-Host "Litemall Vue Frontend Build Script" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green

# Set variables
$ProjectRoot = Split-Path -Parent $PSScriptRoot
$VueProjectPath = Join-Path $ProjectRoot "litemall-vue"
$NodeCmd = "node"
$NpmCmd = "npm"

# Check if Node.js and npm are installed
if (-not (Get-Command $NodeCmd -ErrorAction SilentlyContinue)) {
    Write-Host "Error: Node.js not found, please install Node.js" -ForegroundColor Red
    exit 1
}

if (-not (Get-Command $NpmCmd -ErrorAction SilentlyContinue)) {
    Write-Host "Error: npm not found, please install npm" -ForegroundColor Red
    exit 1
}

# Check Vue project directory
if (-not (Test-Path $VueProjectPath)) {
    Write-Host "Error: Vue project directory not found: $VueProjectPath" -ForegroundColor Red
    exit 1
}

# Change to Vue project directory
Set-Location $VueProjectPath

# Display project information
Write-Host "Vue project directory: $VueProjectPath" -ForegroundColor Cyan
Write-Host "Node version: $(node --version)" -ForegroundColor Cyan
Write-Host "npm version: $(npm --version)" -ForegroundColor Cyan

# Install dependencies (unless skipped)
if (-not $SkipInstall) {
    Write-Host "Installing dependencies..." -ForegroundColor Yellow
    
    if (Test-Path "package-lock.json") {
        Write-Host "Found package-lock.json, using npm ci..." -ForegroundColor Cyan
        npm ci
    } else {
        Write-Host "Using npm install..." -ForegroundColor Cyan
        npm install
    }
    
    if ($LASTEXITCODE -ne 0) {
        Write-Host "❌ Dependency installation failed" -ForegroundColor Red
        exit 1
    }
} else {
    Write-Host "Skipping dependency installation" -ForegroundColor Yellow
}

# Build commands
$BuildArgs = @()
if ($Dev) {
    Write-Host "Starting development server..." -ForegroundColor Yellow
    Write-Host "Access URL: http://localhost:8080" -ForegroundColor Green
    npm run serve
    exit 0
} else {
    Write-Host "Building production environment..." -ForegroundColor Yellow
    
    # Clean old build files
    $DistPath = Join-Path $VueProjectPath "dist"
    if (Test-Path $DistPath) {
        Write-Host "Cleaning old build files..." -ForegroundColor Cyan
        Remove-Item -Recurse -Force $DistPath
    }
    
    # Execute build
    $BuildCommand = "npm run build"
    if ($Environment -eq "dep") {
        $BuildCommand = "npm run build:dep"
    }
    
    if ($Analyze) {
        Write-Host "Enabling build analysis..." -ForegroundColor Cyan
        $BuildCommand = "npm run build:prod"
    }
    
    Invoke-Expression $BuildCommand
    
    if ($LASTEXITCODE -ne 0) {
        Write-Host "❌ Vue build failed, please check error messages" -ForegroundColor Red
        exit 1
    }
}

Write-Host "✅ Vue build successful!" -ForegroundColor Green

# Display build results
$DistPath = Join-Path $VueProjectPath "dist"
if (Test-Path $DistPath) {
    $Files = Get-ChildItem $DistPath -Recurse -File
    $TotalSize = ($Files | Measure-Object -Property Length -Sum).Sum / 1MB
    Write-Host "Build file count: $($Files.Count)" -ForegroundColor Green
    Write-Host "Total size: $([math]::Round($TotalSize, 2)) MB" -ForegroundColor Green
    
    # Display main files
    Write-Host "Main files:" -ForegroundColor Cyan
    Get-ChildItem $DistPath -File | ForEach-Object {
        Write-Host "  $($_.Name): $([math]::Round($_.Length / 1KB, 2)) KB" -ForegroundColor Gray
    }
} else {
    Write-Host "Warning: Build directory not found" -ForegroundColor Yellow
}

Write-Host "Build completed!" -ForegroundColor Green
if (-not $DevMode) {
    Write-Host "Deploy path: $DistPath" -ForegroundColor Green
}