$ErrorActionPreference = 'Stop'
$workspacePath = Split-Path -Parent $PSScriptRoot
$environmentPath = Join-Path $workspacePath '.env'
if (-not (Test-Path -LiteralPath $environmentPath)) {
    Copy-Item -LiteralPath (Join-Path $workspacePath '.env.example') -Destination $environmentPath
}
$environmentContent = [System.IO.File]::ReadAllText($environmentPath)
if ($environmentContent -notmatch '(?m)^JWT_SECRET=\S+') {
    $jwtBytes = New-Object byte[] 32
    $jwtGenerator = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try {
        $jwtGenerator.GetBytes($jwtBytes)
    } finally {
        $jwtGenerator.Dispose()
    }
    $jwtSecret = [Convert]::ToBase64String($jwtBytes)
    if ($environmentContent -match '(?m)^JWT_SECRET=.*$') {
        $environmentContent = [regex]::Replace($environmentContent, '(?m)^JWT_SECRET=.*$', "JWT_SECRET=$jwtSecret")
    } else {
        $environmentContent = $environmentContent.TrimEnd() + [Environment]::NewLine + "JWT_SECRET=$jwtSecret" + [Environment]::NewLine
    }
    [System.IO.File]::WriteAllText($environmentPath, $environmentContent, [System.Text.UTF8Encoding]::new($false))
}
Write-Output 'Configuración local lista. La clave JWT permanece en .env.'
