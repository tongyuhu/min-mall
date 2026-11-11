param([string]$Path = (Join-Path (Split-Path $PSScriptRoot -Parent) '.env'))
$ErrorActionPreference = 'Stop'
foreach ($line in Get-Content -LiteralPath $Path -Encoding UTF8) {
    if ($line.Trim() -eq '' -or $line.TrimStart().StartsWith('#')) { continue }
    $index=$line.IndexOf('=')
    if ($index -lt 1) { throw '环境文件存在无效行' }
    $name=$line.Substring(0,$index).Trim()
    if ($name -notmatch '^[A-Z][A-Z0-9_]*$') { throw '环境变量名称不合法' }
    $value=$line.Substring($index+1)
    if ($value.Length -ge 2 -and (($value.StartsWith('"') -and $value.EndsWith('"')) -or ($value.StartsWith("'") -and $value.EndsWith("'")))) { $value=$value.Substring(1,$value.Length-2) }
    [Environment]::SetEnvironmentVariable($name,$value,'Process')
}
Write-Output '配置已导入当前 PowerShell 进程；未输出密码。'
