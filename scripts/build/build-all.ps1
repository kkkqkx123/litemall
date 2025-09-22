# litemall全量构建脚本 (PowerShell)
# 一键构建后端、Vue前端和微信小程序

param(
    [string]$Profile = "dev",
    [switch]$SkipBackend = $false,
    [switch]$SkipVue = $false,
    [switch]$SkipWx = $false,
    [switch]$SkipTests = $true,
    [switch]$Clean = $false,
    [switch]$Parallel = $false
)

Write-Host "========================================" -ForegroundColor Green
Write-Host "Litemall Full Build Script" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green

$ProjectRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$StartTime = Get-Date

# Display build plan
Write-Host "Build Plan:" -ForegroundColor Cyan
Write-Host "  Backend: $(if ($SkipBackend) {'Skip'} else {'Execute'})" -ForegroundColor $(if ($SkipBackend) {'Gray'} else {'Green'})
Write-Host "  Vue Frontend: $(if ($SkipVue) {'Skip'} else {'Execute'})" -ForegroundColor $(if ($SkipVue) {'Gray'} else {'Green'})
Write-Host "  WeChat Mini Program: $(if ($SkipWx) {'Skip'} else {'Execute'})" -ForegroundColor $(if ($SkipWx) {'Gray'} else {'Green'})
Write-Host "  Parallel Build: $Parallel" -ForegroundColor Cyan
Write-Host ""

# Build functions
function Build-Backend {
    Write-Host "📦 Starting backend build..." -ForegroundColor Yellow
    try {
        & "$PSScriptRoot\build-backend.ps1" -Profile $Profile -SkipTests:$SkipTests -Clean:$Clean
        return $true
    }
    catch {
        Write-Host "❌ Backend build failed: $_" -ForegroundColor Red
        return $false
    }
}

function Build-Vue {
    Write-Host "📱 Starting Vue frontend build..." -ForegroundColor Yellow
    try {
        & "$PSScriptRoot\build-vue.ps1" -Environment production -SkipInstall:$false
        return $true
    }
    catch {
        Write-Host "❌ Vue frontend build failed: $_" -ForegroundColor Red
        return $false
    }
}

function Build-Wx {
    Write-Host "🎯 Starting WeChat Mini Program check..." -ForegroundColor Yellow
    try {
        & "$PSScriptRoot\build-wx.ps1" -Mode check
        return $true
    }
    catch {
        Write-Host "❌ WeChat Mini Program check failed: $_" -ForegroundColor Red
        return $false
    }
}

# Execute build
$Success = $true

if ($Parallel -and -not $SkipBackend -and -not $SkipVue) {
    # Parallel build backend and Vue frontend
    Write-Host "🔄 Starting parallel build..." -ForegroundColor Cyan
    
    $BackendJob = Start-Job -ScriptBlock {
        param($ScriptPath, $Profile, $SkipTests, $Clean)
        & $ScriptPath -Profile $Profile -SkipTests:$SkipTests -Clean:$Clean
    } -ArgumentList "$PSScriptRoot\build-backend.ps1", $Profile, $SkipTests, $Clean
    
    $VueJob = Start-Job -ScriptBlock {
        param($ScriptPath)
        & $ScriptPath -Environment production -SkipInstall:$false
    } -ArgumentList "$PSScriptRoot\build-vue.ps1"
    
    # Wait for parallel tasks to complete
    $Jobs = @($BackendJob, $VueJob)
    foreach ($Job in $Jobs) {
        $Result = Wait-Job $Job
        if ($Result.State -eq "Failed") {
            $Success = $false
            Receive-Job $Job
        }
        Remove-Job $Job
    }
    
    # WeChat Mini Program cannot be parallelized as it may need CLI interaction
    if (-not $SkipWx -and $Success) {
        $Success = Build-Wx
    }
} else {
    # Sequential build
    if (-not $SkipBackend) {
        $Success = Build-Backend
    }
    
    if ($Success -and -not $SkipVue) {
        $Success = Build-Vue
    }
    
    if ($Success -and -not $SkipWx) {
        $Success = Build-Wx
    }
}

$EndTime = Get-Date
$Duration = $EndTime - $StartTime

Write-Host ""
Write-Host "========================================" -ForegroundColor Green

if ($Success) {
    Write-Host "✅ Full build completed successfully!" -ForegroundColor Green
    Write-Host "Total time: $($Duration.TotalSeconds.ToString('F2')) seconds" -ForegroundColor Cyan
    
    Write-Host ""
    Write-Host "Build Results:" -ForegroundColor Cyan
    
    if (-not $SkipBackend) {
        $JarPath = Join-Path $ProjectRoot "litemall-all\target\litemall-all-0.1.0.jar"
        if (Test-Path $JarPath) {
            $JarSize = (Get-Item $JarPath).Length / 1MB
            Write-Host "  Backend JAR: $([math]::Round($JarSize, 2)) MB" -ForegroundColor Green
        }
    }
    
    if (-not $SkipVue) {
        $VueDist = Join-Path $ProjectRoot "litemall-vue\dist"
        if (Test-Path $VueDist) {
            $VueFiles = Get-ChildItem $VueDist -Recurse -File
            $VueSize = ($VueFiles | Measure-Object -Property Length -Sum).Sum / 1MB
            Write-Host "  Vue Frontend: $([math]::Round($VueSize, 2)) MB ($($VueFiles.Count) files)" -ForegroundColor Green
        }
    }
    
    Write-Host ""
    Write-Host "Next steps:" -ForegroundColor Yellow
    Write-Host "  Start backend: java -jar litemall-all/target/litemall-all-0.1.0.jar"
    Write-Host "  Docker start: docker-compose up"
    Write-Host "  Vue dev: npm run serve (in litemall-vue directory)"
} else {
    Write-Host "❌ Full build failed!" -ForegroundColor Red
    Write-Host "Total time: $($Duration.TotalSeconds.ToString('F2')) seconds" -ForegroundColor Cyan
    exit 1
}