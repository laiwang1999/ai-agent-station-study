# Prometheus Metrics Simulation Script (Windows PowerShell version)
# Purpose: Generate simulated Prometheus-formatted metrics for Node Exporter's textfile collector
# Author: AI Agent
# Usage: .\generate-metrics-windows.ps1

# Metrics file path
$METRICS_FILE = "$env:TEMP\custom_metrics.prom"

# Function to generate a random integer
function Generate-Random {
    param(
        [int]$Min,
        [int]$Max
    )
    return Get-Random -Minimum $Min -Maximum ($Max + 1)
}

# Function to generate a random decimal number
function Generate-RandomDecimal {
    param(
        [int]$Min,
        [int]$Max,
        [int]$DecimalPlaces = 2
    )
    $randomInt = Generate-Random -Min ($Min * 100) -Max ($Max * 100)
    $result = [math]::Round($randomInt / 100, $DecimalPlaces)
    return $result
}

# Check dependencies (PowerShell has built-in support)
function Check-Dependencies {
    Write-Host "[INFO] Dependency check passed." -ForegroundColor Green
}

# Initialization
Write-Host "=== Prometheus Metrics Generator (Windows) ===" -ForegroundColor Cyan
Write-Host "Start time: $(Get-Date)" -ForegroundColor Yellow
Write-Host "Metrics file: $METRICS_FILE" -ForegroundColor Yellow
Check-Dependencies

# Generate simulated metrics
function Generate-Metrics {
    $metricsContent = @"
# HELP http_requests_total Total number of HTTP requests
# TYPE http_requests_total counter
http_requests_total{method="GET",status="200"} $(Generate-Random -Min 1000 -Max 9999)
http_requests_total{method="POST",status="200"} $(Generate-Random -Min 500 -Max 2000)
http_requests_total{method="GET",status="404"} $(Generate-Random -Min 10 -Max 100)

# HELP response_time_seconds Response time in seconds
# TYPE response_time_seconds histogram
response_time_seconds_bucket{le="0.1"} $(Generate-Random -Min 100 -Max 500)
response_time_seconds_bucket{le="0.5"} $(Generate-Random -Min 500 -Max 1000)
response_time_seconds_bucket{le="1.0"} $(Generate-Random -Min 1000 -Max 1500)
response_time_seconds_bucket{le="+Inf"} $(Generate-Random -Min 1500 -Max 2000)
response_time_seconds_sum $(Generate-RandomDecimal -Min 1 -Max 10)
response_time_seconds_count $(Generate-Random -Min 1000 -Max 5000)

# HELP active_users Current number of active users
# TYPE active_users gauge
active_users $(Generate-Random -Min 50 -Max 500)

# HELP cpu_usage_percent CPU usage percentage
# TYPE cpu_usage_percent gauge
cpu_usage_percent $(Generate-RandomDecimal -Min 10 -Max 90)

# HELP memory_usage_bytes Memory usage in bytes
# TYPE memory_usage_bytes gauge
memory_usage_bytes $(Generate-Random -Min 1000000000 -Max 8000000000)
"@

    try {
        # Write metrics to file
        $metricsContent | Out-File -FilePath $METRICS_FILE -Encoding UTF8 -Force

        # Verify file creation
        if (Test-Path $METRICS_FILE) {
            $fileSize = (Get-Item $METRICS_FILE).Length
            Write-Host "$(Get-Date): [OK] Metrics written to $METRICS_FILE ($fileSize bytes)" -ForegroundColor Green
            return $true
        } else {
            Write-Host "$(Get-Date): [ERROR] Failed to create metrics file." -ForegroundColor Red
            return $false
        }
    }
    catch {
        Write-Host "$(Get-Date): [ERROR] Failed to write metrics file: $($_.Exception.Message)" -ForegroundColor Red
        return $false
    }
}

# Cleanup function
function Cleanup {
    Write-Host ""
    Write-Host "$(Get-Date): [INFO] Termination signal received. Cleaning up..." -ForegroundColor Yellow
    if (Test-Path $METRICS_FILE) {
        Write-Host "$(Get-Date): [INFO] Removing metrics file: $METRICS_FILE" -ForegroundColor Yellow
        Remove-Item -Path $METRICS_FILE -Force -ErrorAction SilentlyContinue
    }
    Write-Host "$(Get-Date): [INFO] Cleanup complete. Script stopped." -ForegroundColor Green
    exit 0
}

# Register cleanup on PowerShell exit
$null = Register-EngineEvent -SourceIdentifier PowerShell.Exiting -Action {
    Cleanup
}

# Main loop
try {
    Write-Host ""
    Write-Host "[INFO] Starting metrics generation..." -ForegroundColor Cyan
    Write-Host "[INFO] Press Ctrl+C to stop." -ForegroundColor Yellow
    Write-Host "[INFO] Metrics update interval: 15 seconds" -ForegroundColor Yellow
    Write-Host ""

    while ($true) {
        if (Generate-Metrics) {
            Write-Host "$(Get-Date): [OK] Metrics updated successfully." -ForegroundColor Green
        } else {
            Write-Host "$(Get-Date): [WARNING] Metrics update failed." -ForegroundColor Red
        }
        Start-Sleep -Seconds 15
    }
}
catch [System.Management.Automation.PipelineStoppedException] {
    # Ctrl+C pressed
    Cleanup
}
finally {
    Cleanup
}