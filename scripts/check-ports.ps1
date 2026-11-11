param([int[]]$Ports = @(13306,16379,18080))
$ErrorActionPreference = 'Stop'
foreach ($port in $Ports) {
    $connections = @(Get-NetTCPConnection -State Listen -LocalPort $port -ErrorAction SilentlyContinue)
    if ($connections.Count -eq 0) { Write-Output "$port：空闲" }
    else { foreach ($connection in $connections) { Write-Output "$port：已监听，PID=$($connection.OwningProcess)，地址=$($connection.LocalAddress)" } }
}
