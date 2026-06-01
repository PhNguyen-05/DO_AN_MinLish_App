<#
.SYNOPSIS
    Convert text files to UTF-8 without BOM.

.DESCRIPTION
    Tìm các file theo phần mở rộng, phát hiện BOM/encoding, và ghi lại bằng UTF-8 (no BOM).
    Tạo bản backup nếu dùng -Backup.
    Hỗ trợ -WhatIf để chạy thử mà không ghi file.

.EXAMPLE
    pwsh -File .\scripts\convert-to-utf8-no-bom.ps1 -Root . -Recurse -Backup
#>

param(
    [Parameter(Position=0)]
    [string]$Root = ".",

    [switch]
    $Recurse,

    [string[]]
    $Include = @("*.kt","*.java","*.js","*.ts","*.json","*.md","*.xml","*.kts","*.gradle","*.properties","*.txt","*.html","*.css","*.yml","*.yaml","*.sql"),

    [switch]
    $Backup,

    [int]
    $FallbackCodePage = 1258,

    [switch]
    $WhatIf
)

$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
$utf8Throw = New-Object System.Text.UTF8Encoding($false,$true)

try {
    $encodingFallback = [System.Text.Encoding]::GetEncoding($FallbackCodePage)
} catch {
    Write-Warning "Fallback code page $FallbackCodePage not available. Using system default."
    $encodingFallback = [System.Text.Encoding]::Default
}

$processed = 0
$converted = 0
$skipped = 0

$files = @()
foreach ($pattern in $Include) {
    try {
        if ($Recurse) {
            $files += Get-ChildItem -Path $Root -Filter $pattern -File -Recurse -ErrorAction SilentlyContinue
        } else {
            $files += Get-ChildItem -Path $Root -Filter $pattern -File -ErrorAction SilentlyContinue
        }
    } catch {
        Write-Warning "Failed to enumerate pattern $pattern: $_"
    }
}

# exclude common build/system folders
$excludeDirs = @(".git","node_modules","build",".gradle")
$files = $files | Sort-Object -Property FullName -Unique | Where-Object {
    $n = $_.FullName.ToLower()
    $skip = $false
    foreach ($ed in $excludeDirs) {
        if ($n -like "*\$ed*") { $skip = $true; break }
    }
    -not $skip
}

if ($files.Count -eq 0) {
    Write-Host "No matching files found under $Root"
    exit 0
}

foreach ($file in $files) {
    $processed++
    $full = $file.FullName
    Write-Host "Processing: $full"
    try {
        $bytes = [System.IO.File]::ReadAllBytes($full)
    } catch {
        Write-Warning "Cannot read $full: $_"
        $skipped++
        continue
    }
    if ($bytes.Length -eq 0) {
        Write-Host "  Empty file, skip."
        $skipped++
        continue
    }
    $text = $null
    $changed = $false

    # BOM detection
    if ($bytes.Length -ge 3 -and $bytes[0] -eq 0xEF -and $bytes[1] -eq 0xBB -and $bytes[2] -eq 0xBF) {
        $text = [System.Text.Encoding]::UTF8.GetString($bytes, 3, $bytes.Length - 3)
        $changed = $true
        Write-Host "  Detected UTF-8 BOM -> will rewrite without BOM"
    } elseif ($bytes.Length -ge 2 -and $bytes[0] -eq 0xFF -and $bytes[1] -eq 0xFE) {
        $text = [System.Text.Encoding]::Unicode.GetString($bytes, 2, $bytes.Length - 2)
        $changed = $true
        Write-Host "  Detected UTF-16 LE -> convert to UTF-8 no-BOM"
    } elseif ($bytes.Length -ge 2 -and $bytes[0] -eq 0xFE -and $bytes[1] -eq 0xFF) {
        $text = [System.Text.Encoding]::BigEndianUnicode.GetString($bytes, 2, $bytes.Length - 2)
        $changed = $true
        Write-Host "  Detected UTF-16 BE -> convert to UTF-8 no-BOM"
    } elseif ($bytes.Length -ge 4 -and $bytes[0] -eq 0x00 -and $bytes[1] -eq 0x00 -and $bytes[2] -eq 0xFE -and $bytes[3] -eq 0xFF) {
        try {
            $text = [System.Text.Encoding]::UTF32.GetString($bytes, 4, $bytes.Length - 4)
            $changed = $true
            Write-Host "  Detected UTF-32 BE -> convert to UTF-8 no-BOM"
        } catch {
            Write-Warning "  Failed to decode UTF-32 BE: $_"
        }
    } else {
        # try utf8 strict
        try {
            $text = $utf8Throw.GetString($bytes)
            Write-Host "  No BOM, valid UTF-8 detected"
        } catch {
            # fallback to ANSI codepage
            try {
                $text = $encodingFallback.GetString($bytes)
                $changed = $true
                Write-Host "  No BOM and invalid UTF-8 -> decoded using codepage $FallbackCodePage"
            } catch {
                Write-Warning "  Failed to decode $full"
            }
        }
    }

    if ($null -eq $text) {
        Write-Warning "  Skipping (could not decode)"
        $skipped++
        continue
    }

    if ($WhatIf) {
        Write-Host "  WhatIf: would write UTF-8 (no BOM)"
        $converted++
        continue
    }

    if ($Backup) {
        $bak = "$full.bak"
        try {
            Copy-Item -Path $full -Destination $bak -Force
            Write-Host "  Backup created: $bak"
        } catch {
            Write-Warning "  Backup failed: $_"
        }
    }

    try {
        [System.IO.File]::WriteAllText($full, $text, $utf8NoBom)
        Write-Host "  Rewritten as UTF-8 (no BOM)"
        $converted++
    } catch {
        Write-Warning "  Failed to write $full: $_"
        $skipped++
    }
}

Write-Host ""
Write-Host "Processed: $processed  Converted: $converted  Skipped: $skipped"
