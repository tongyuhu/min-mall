$ErrorActionPreference = 'Stop'
Set-Location (Split-Path $PSScriptRoot -Parent)
$modules = @('common','user','catalog','marketing','trade','admin','app')
$allowed = @{common=@();user=@('common');catalog=@('common');marketing=@('common');trade=@('common','user','catalog','marketing');admin=@('common','user','catalog','marketing','trade');app=@('common','user','catalog','marketing','trade','admin')}
foreach ($module in $modules) {
    [xml]$pom = Get-Content -LiteralPath "mall-$module/pom.xml" -Raw
    $nodes = $pom.SelectNodes("//*[local-name()='dependencies']/*[local-name()='dependency']")
    foreach ($node in $nodes) {
        if ($node.groupId -eq 'com.minimall' -and $node.artifactId -like 'mall-*') {
            $dependency = $node.artifactId.Substring(5)
            if ($dependency -notin $allowed[$module]) { throw "非法依赖：$module -> $dependency" }
        }
        if ($node.version -match 'SNAPSHOT|LATEST|RELEASE') { throw "禁止浮动依赖版本：$($node.artifactId)" }
    }
}
[xml]$rootPom = Get-Content -LiteralPath pom.xml -Raw
if ($rootPom.project.properties.'maven.compiler.release' -ne '21') { throw 'Java编译目标必须为21' }
$sources = Get-ChildItem -Recurse -Filter *.java | Where-Object FullName -NotMatch '\\.superpowers\\|\\target\\'
foreach ($source in $sources) {
    $content = Get-Content -LiteralPath $source.FullName -Raw -Encoding UTF8
    if ($content.Contains([char]0xFFFD)) { throw "编码错误：$($source.FullName)" }
    foreach ($dependencyImport in [regex]::Matches($content, 'import\s+com\.minimall\.(\w+)\.[^;]*\.persistence\.')) {
        $owner = $dependencyImport.Groups[1].Value
        if ($owner -ne 'common' -and $source.FullName -notmatch "mall-$owner[\\/]" -and $source.FullName -notmatch '[\\/]src[\\/]test[\\/]') { throw "跨模块持久化引用：$($source.FullName)" }
    }
}
$scriptErrors = @()
Get-ChildItem -LiteralPath scripts -Filter '*.ps1' | ForEach-Object {
    $tokens = $null; $problems = $null
    [void][System.Management.Automation.Language.Parser]::ParseFile($_.FullName, [ref]$tokens, [ref]$problems)
    $scriptErrors += $problems
}
if ($scriptErrors.Count -gt 0) { $scriptErrors | Format-List; throw 'PowerShell脚本语法检查失败' }
git diff --check
if ($LASTEXITCODE -ne 0) { throw 'Git空白检查失败' }
git diff --cached --check
if ($LASTEXITCODE -ne 0) { throw 'Git暂存区空白检查失败' }
Write-Output "静态检查通过：7个模块，$($sources.Count)个Java源文件；此脚本不执行编译或测试。"
