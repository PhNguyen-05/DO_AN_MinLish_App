<#
.SYNOPSIS
Convert text files to UTF-8 (no BOM), with fallbacks for Vietnamese (CP1258) and CP1252.

.Usage
# Recurse current dir and make backups:
# powershell -ExecutionPolicy Bypass -File .\scripts\ensure-utf8-nobom.ps1 -Path . -Recurse -Backup

# Re-encode specific files:
# powershell -ExecutionPolicy Bypass -File .\scripts\ensure-utf8-nobom.ps1 -Path file1.kt,file2.java

.PARAMETER Path
Paths or files to process (default: .)
.PARAMETER Recurse
Recurse into directories when Path is a folder.
.PARAMETER Include
File patterns to include.
.PARAMETER Exclude
File patterns to exclude.
.PARAMETER Backup
Create a .bak backup for each file before overwriting.
.PARAMETER DryRun
Show what would be done without writing files.
#>

param(
    [Parameter(Position=0, ValueFromPipeline=$true)]
    [string[]]$Path = ".",
    [switch]$Recurse,
    [string[]]$Include = @("*.kt","*.java","*.xml","*.gradle","*.gradle.kts","*.properties","*.js","*.ts","*.json","*.md","*.txt","*.yml","*.yaml","*.html","*.css"),
    [string[]]$Exclude = @(),
    [switch]$Backup,
    [switch]$DryRun
)

Write-Host "Starting re-encoding to UTF-8 (no BOM)..."

$targets = @()
foreach ($p in $Path) {
    if (-not (Test-Path $p)) {
        Write-Warning "Path not found: $p"
        continue
    }
    $item = Get-Item $p -Force
    if ($item.PSIsContainer) {
        $targets += Get-ChildItem -Path $item.FullName -Recurse:$Recurse -Include $Include -File -ErrorAction SilentlyContinue
    } else {
        $targets += $item
    }
}

$targets = $targets | Sort-Object -Unique

foreach ($f in $targets) {
    $full = $f.FullName
    # Exclude patterns
    $skip = $false
    foreach ($ex in $Exclude) {
        if ($full -like $ex) { $skip = $true; break }
    }
    if ($skip) { Write-Host "Excluded: $full"; continue }
    try {
        $bytes = [System.IO.File]::ReadAllBytes($full)
    } catch {
        Write-Warning "Cannot read: $full"
        continue
    }
    if ($bytes.Length -eq 0) { Write-Host "Empty: $full"; continue }
    # detect binary (NUL in first 512 bytes)
    $isBinary = $false
    $checkLen = [Math]::Min(512, $bytes.Length)
    for ($i=0; $i -lt $checkLen; $i++) {
        if ($bytes[$i] -eq 0) { $isBinary = $true; break }
    }
    if ($isBinary) { Write-Host "Skipping (binary): $full"; continue }
    # Try strict UTF8 (throw on invalid sequences)
    $decoded = $null
    $utf8Strict = New-Object System.Text.UTF8Encoding($false,$true)
    try {
        $decoded = $utf8Strict.GetString($bytes)
    } catch {
        # try CP1258 (Vietnamese), then CP1252, then permissive UTF8
        try { $decoded = [System.Text.Encoding]::GetEncoding(1258).GetString($bytes) } catch { 
            try { $decoded = [System.Text.Encoding]::GetEncoding(1252).GetString($bytes) } catch {
                $decoded = [System.Text.Encoding]::UTF8.GetString($bytes)
            }
        }
    }
    # If decoded contains replacement character, try CP1258
    if ($decoded -match '�') {
        try {
            $decoded = [System.Text.Encoding]::GetEncoding(1258).GetString($bytes)
        } catch {}
    }
    if ($DryRun) {
        Write-Host "[DryRun] Would re-encode: $full"
        continue
    }
    if ($Backup) {
        $bak = "$full.bak"
        try { Copy-Item -Path $full -Destination $bak -ErrorAction Stop; Write-Host "Backup: $bak" } catch { Write-Warning "Backup failed: $full" }
    }
    try {
        [System.IO.File]::WriteAllText($full, $decoded, (New-Object System.Text.UTF8Encoding($false)))
        Write-Host "Re-encoded: $full"
    } catch {
        Write-Warning "Write failed: $full - $_"
    }
}

Write-Host "Done." 
