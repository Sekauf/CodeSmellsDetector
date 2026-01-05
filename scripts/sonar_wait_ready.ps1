param(
    [string]$HostUrl = "http://localhost:9000",
    [int]$TimeoutSeconds = 120
)

$start = Get-Date
$statusUrl = ($HostUrl.TrimEnd("/") + "/api/system/status")

while ($true) {
    try {
        $response = Invoke-WebRequest -Uri $statusUrl -UseBasicParsing -TimeoutSec 10
        if ($response.StatusCode -eq 200 -and $response.Content -match '"status"\s*:\s*"(UP|GREEN)"') {
            Write-Host "SonarQube is ready."
            exit 0
        }
    } catch {
        # Ignore transient failures while waiting.
    }

    $elapsed = (Get-Date) - $start
    if ($elapsed.TotalSeconds -ge $TimeoutSeconds) {
        Write-Error "Timed out waiting for SonarQube after $TimeoutSeconds seconds."
        exit 1
    }
    Start-Sleep -Seconds 2
}
