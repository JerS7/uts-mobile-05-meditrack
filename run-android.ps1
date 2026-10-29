param(
    [string]$JavaHome,
    [string]$DeviceId,
    [switch]$BuildOnly
)

$ErrorActionPreference = 'Stop'
Push-Location $PSScriptRoot
$previousJavaHome = $env:JAVA_HOME
try {
    # Prefer Android Studio's JDK over an older Java on PATH.
    $candidates = @(
        $JavaHome,
        'D:\Aplikasi\AndroidStudi\jbr',
        "$env:ProgramFiles\Android\Android Studio\jbr",
        "$env:LOCALAPPDATA\Programs\Android Studio\jbr",
        $env:JAVA_HOME
    )
    $jdk = $candidates | Where-Object {
        $_ -and (Test-Path -LiteralPath (Join-Path $_ 'bin\java.exe'))
    } | Select-Object -First 1
    if (-not $jdk) {
        throw 'Java Android Studio tidak ditemukan. Jalankan dengan -JavaHome <folder JDK 17 atau lebih baru>.'
    }
    $env:JAVA_HOME = $jdk

    & .\gradlew.bat assembleDebug testDebugUnitTest
    if ($LASTEXITCODE -ne 0) { throw 'Build atau tes gagal. Periksa pesan Gradle di atas.' }
    if ($BuildOnly) { return }

    $sdk = $env:ANDROID_HOME
    if (Test-Path -LiteralPath 'local.properties') {
        $sdkLine = Get-Content 'local.properties' | Where-Object { $_ -match '^sdk\.dir=' } | Select-Object -First 1
        if ($sdkLine) { $sdk = $sdkLine.Substring(8).Replace('\\', '\').Replace('\:', ':') }
    }
    if (-not $sdk) { $sdk = "$env:LOCALAPPDATA\Android\Sdk" }
    $adb = Join-Path $sdk 'platform-tools\adb.exe'
    if (-not (Test-Path -LiteralPath $adb)) { throw "ADB tidak ditemukan: $adb" }

    $deviceLines = & $adb devices
    if ($LASTEXITCODE -ne 0) { throw 'Tidak dapat menghubungi ADB.' }
    $devices = @($deviceLines | ForEach-Object {
        if ($_ -match '^(\S+)\s+device$') { $Matches[1] }
    })
    if ($DeviceId) {
        if ($DeviceId -notin $devices) { throw "Perangkat $DeviceId belum siap. Periksa USB debugging dan izin pada HP." }
    } elseif ($devices.Count -eq 1) {
        $DeviceId = $devices[0]
    } elseif ($devices.Count -eq 0) {
        throw 'Hubungkan HP dengan USB debugging aktif dan izinkan koneksi, atau jalankan emulator Android Studio.'
    } else {
        throw 'Ada beberapa perangkat. Pilih dengan -DeviceId <serial dari adb devices>.'
    }

    & $adb -s $DeviceId install -r (Join-Path $PSScriptRoot 'app\build\outputs\apk\debug\app-debug.apk')
    if ($LASTEXITCODE -ne 0) { throw 'Pemasangan APK gagal.' }
    $launchResult = & $adb -s $DeviceId shell am start -W -n com.example.meditrack/.MainActivity 2>&1
    $launchResult | Write-Output
    if ($LASTEXITCODE -ne 0 -or ($launchResult -match 'Error:|Exception')) { throw 'Aplikasi gagal dibuka.' }
    Write-Host "MediTrack sudah dibuka pada $DeviceId."
} finally {
    $env:JAVA_HOME = $previousJavaHome
    Pop-Location
}
