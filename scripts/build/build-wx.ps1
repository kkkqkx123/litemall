# litemall WeChat Mini Program build script (PowerShell)
# Used for WeChat Mini Program code checking and preview

param(
    [string]$Mode = "build",
    [switch]$Upload = $false,
    [string]$Version = "",
    [string]$Desc = "",
    [switch]$Watch = $false
)

Write-Host "========================================" -ForegroundColor Green
Write-Host "Litemall WeChat Mini Program Build Script" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green

# Set variables
$ProjectRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$WxProjectPath = Join-Path $ProjectRoot "litemall-wx"

# Check WeChat Mini Program project directory
if (-not (Test-Path $WxProjectPath)) {
    Write-Host "Error: WeChat Mini Program project directory not found: $WxProjectPath" -ForegroundColor Red
    exit 1
}

# Check WeChat Developer Tools CLI
$CliPaths = @(
    "$env:PROGRAMFILES(x86)\Tencent\微信开发者工具\cli.bat",
    "$env:PROGRAMFILES\Tencent\微信开发者工具\cli.bat",
    "$env:LOCALAPPDATA\微信开发者工具\cli.bat"
)

$CliPath = $null
foreach ($Path in $CliPaths) {
    if (Test-Path $Path) {
        $CliPath = $Path
        break
    }
}

if (-not $CliPath) {
    Write-Host "Warning: WeChat Developer Tools CLI not found" -ForegroundColor Yellow
    Write-Host "Please ensure WeChat Developer Tools is installed and CLI is added to system PATH" -ForegroundColor Yellow
    Write-Host "Or use WeChat Developer Tools to manually upload" -ForegroundColor Yellow
} else {
    Write-Host "Found CLI: $CliPath" -ForegroundColor Green
}

# Switch to WeChat Mini Program project directory
Set-Location $WxProjectPath

# Display project information
Write-Host "WeChat Mini Program project directory: $WxProjectPath" -ForegroundColor Cyan

# Check project configuration file
$ProjectConfig = Join-Path $WxProjectPath "project.config.json"
if (-not (Test-Path $ProjectConfig)) {
    Write-Host "Error: Project configuration file not found: $ProjectConfig" -ForegroundColor Red
    exit 1
}

# Read project configuration
try {
    $ConfigContent = Get-Content $ProjectConfig -Raw -Encoding UTF8 | ConvertFrom-Json
    Write-Host "Project name: $($ConfigContent.projectname)" -ForegroundColor Cyan
    Write-Host "App ID: $($ConfigContent.appid)" -ForegroundColor Cyan
} catch {
    Write-Host "Warning: Could not read project configuration" -ForegroundColor Yellow
}

# Execute different mode operations
switch ($Mode.ToLower()) {
    "check" {
        Write-Host "Performing code check..." -ForegroundColor Yellow
        
        # Check app.json
        $AppJson = Join-Path $WxProjectPath "app.json"
        if (Test-Path $AppJson) {
            Write-Host "✅ app.json exists" -ForegroundColor Green
            try {
                $AppConfig = Get-Content $AppJson -Raw -Encoding UTF8 | ConvertFrom-Json
                $PageCount = if ($AppConfig.pages) { $AppConfig.pages.Count } else { 0 }
                Write-Host "  Page count: $PageCount" -ForegroundColor Gray
            } catch {
                Write-Host "  Could not parse app.json" -ForegroundColor Yellow
            }
        } else {
            Write-Host "❌ app.json not found" -ForegroundColor Red
        }
        
        # Check app.js
        if (Test-Path "app.js") {
            Write-Host "✅ app.js exists" -ForegroundColor Green
        } else {
            Write-Host "❌ app.js not found" -ForegroundColor Red
        }
        
        # Check app.wxss
        if (Test-Path "app.wxss") {
            Write-Host "✅ app.wxss exists" -ForegroundColor Green
        } else {
            Write-Host "❌ app.wxss not found" -ForegroundColor Red
        }
        
        # Check directory structure
        if (Test-Path "pages") {
            $PageCount = (Get-ChildItem "pages" -Directory -ErrorAction SilentlyContinue).Count
            Write-Host "✅ pages directory exists, sub-pages: $PageCount" -ForegroundColor Green
        } else {
            Write-Host "❌ pages directory not found" -ForegroundColor Red
        }
        
        if (Test-Path "utils") {
            Write-Host "✅ utils directory exists" -ForegroundColor Green
        } else {
            Write-Host "❌ utils directory not found" -ForegroundColor Red
        }
        
        # Check sitemap.json
        if (Test-Path "sitemap.json") {
            Write-Host "✅ sitemap.json exists" -ForegroundColor Green
        } else {
            Write-Host "❌ sitemap.json not found" -ForegroundColor Red
        }
        
        Write-Host "Code check completed!" -ForegroundColor Green
    }
    
    "build" {
        Write-Host "Building WeChat Mini Program..." -ForegroundColor Yellow
        
        # Check if upload is needed
        if ($Upload) {
            if ([string]::IsNullOrEmpty($Version) -or [string]::IsNullOrEmpty($Desc)) {
                Write-Host "Error: Upload mode requires version and description" -ForegroundColor Red
                Write-Host "Example: -Upload -Version \"1.0.0\" -Desc \"Update content\"" -ForegroundColor Yellow
                exit 1
            }
            
            if (-not $CliPath) {
                Write-Host "Error: CLI not found, cannot upload" -ForegroundColor Red
                exit 1
            }
            
            Write-Host "Uploading to WeChat Mini Program..." -ForegroundColor Yellow
            Write-Host "Version: $Version" -ForegroundColor Cyan
            Write-Host "Description: $Desc" -ForegroundColor Cyan
            
            # Execute upload command
            & $CliPath upload --project $WxProjectPath --version $Version --desc $Desc
            if ($LASTEXITCODE -eq 0) {
                Write-Host "✅ Upload successful!" -ForegroundColor Green
            } else {
                Write-Host "❌ Upload failed" -ForegroundColor Red
                exit 1
            }
        } else {
            Write-Host "Build mode: checking code only, no upload" -ForegroundColor Green
            Write-Host "Use -Upload parameter to perform upload" -ForegroundColor Yellow
            
            # Perform basic check
            & "$PSScriptRoot\build-wx.ps1" -Mode check
        }
    }
    
    "preview" {
        Write-Host "Generating WeChat Mini Program preview..." -ForegroundColor Yellow
        
        if (-not $CliPath) {
            Write-Host "Error: CLI not found, cannot generate preview" -ForegroundColor Red
            exit 1
        }
        
        # Generate preview QR code
        & $CliPath preview --project $WxProjectPath
        if ($LASTEXITCODE -eq 0) {
            Write-Host "✅ Preview QR code generated!" -ForegroundColor Green
        } else {
            Write-Host "❌ Preview generation failed" -ForegroundColor Red
            exit 1
        }
    }
    
    "watch" {
        Write-Host "Starting WeChat Mini Program auto-build..." -ForegroundColor Yellow
        Write-Host "Press Ctrl+C to stop watching" -ForegroundColor Cyan
        
        if (-not $CliPath) {
            Write-Host "Error: CLI not found, cannot start auto-build" -ForegroundColor Red
            exit 1
        }
        
        # Watch file changes and auto-upload
        & $CliPath auto --project $WxProjectPath
    }
    
    default {
        Write-Host "Unknown mode: $Mode" -ForegroundColor Red
        Write-Host "Available modes: check, build, preview, watch" -ForegroundColor Yellow
        exit 1
    }
}

Write-Host ""
Write-Host "WeChat Mini Program build completed!" -ForegroundColor Green