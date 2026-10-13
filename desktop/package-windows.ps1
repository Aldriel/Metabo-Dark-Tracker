<#
.SYNOPSIS
    Builds the portable Windows distribution of Metabo-Dark Tracker.

.DESCRIPTION
    Produces target\dist\Metabo-Dark-Tracker-<version>-windows-x64.zip containing "MetaboDark.exe"
    and its own trimmed Java runtime. Users unzip the archive and run the executable; no Java
    installation is required.

    Requires JDK 21 (jlink and jpackage). Maven is run through the wrapper of the project. The JDK
    is taken from JAVA_HOME, otherwise from the PATH.

.PARAMETER SkipTests
    Packages without running the tests.

.EXAMPLE
    .\package-windows.ps1 -SkipTests
#>
param([switch]$SkipTests)

$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

function Invoke-Checked([string]$Tool, [string[]]$Arguments) {
    & $Tool @Arguments
    if ($LASTEXITCODE -ne 0) { throw "$Tool failed with exit code $LASTEXITCODE" }
}

function Find-JdkTool([string]$Name) {
    if ($env:JAVA_HOME -and (Test-Path "$env:JAVA_HOME\bin\$Name.exe")) {
        return "$env:JAVA_HOME\bin\$Name.exe"
    }
    return (Get-Command $Name -ErrorAction Stop).Source
}

$mvnw = Resolve-Path '..\mvnw.cmd'
$pomVersion = ([xml](Get-Content ..\pom.xml -Raw -Encoding UTF8)).project.version
# jpackage only accepts numeric versions: 0.1.0-SNAPSHOT becomes 0.1.0.
$version = $pomVersion -replace '-.*$', ''
$name = 'MetaboDark'
$zipName = 'Metabo-Dark-Tracker'
$work = 'target\jpackage'
$libs = "$work\libs"
$runtime = "$work\runtime"
$image = "$work\image"
$zip = "target\dist\$zipName-$version-windows-x64.zip"

Write-Host "Packaging $name $version"
if (Test-Path $work) { Remove-Item $work -Recurse -Force }

# 1. Build the modules, then collect the desktop jar and its runtime dependencies.
$mvnArgs = @('-q', '-f', '..\pom.xml', 'install')
if ($SkipTests) { $mvnArgs += @('-DskipTests', '-Djacoco.skip=true') }
Invoke-Checked $mvnw $mvnArgs
Invoke-Checked $mvnw @(
    '-q',
    'org.apache.maven.plugins:maven-dependency-plugin:3.8.1:copy-dependencies',
    '-DincludeScope=runtime', "-DoutputDirectory=$libs")
Copy-Item "target\metabodark-desktop-$pomVersion.jar" $libs
# JavaFX ships an empty jar plus a platform jar per module: keep the Windows ones only.
Get-ChildItem $libs -Filter 'javafx-*.jar' |
    Where-Object { $_.Name -notmatch '-win\.jar$' } |
    Remove-Item

# 2. Trimmed Java runtime. The application and JavaFX run from the class path.
$modules = @(
    'java.base', 'java.desktop', 'java.logging', 'java.net.http', 'java.prefs',
    'java.xml', 'java.naming', 'jdk.unsupported', 'jdk.localedata', 'jdk.charsets'
) -join ','
Invoke-Checked (Find-JdkTool 'jlink') @(
    '--add-modules', $modules, '--strip-debug', '--no-header-files', '--no-man-pages',
    '--compress=zip-6', '--output', $runtime)

# 3. Executable. No custom icon is shipped with this version.
Invoke-Checked (Find-JdkTool 'jpackage') @(
    '--type', 'app-image',
    '--name', $name,
    '--app-version', $version,
    '--vendor', 'Maxime Ethier',
    '--copyright', "(c) $((Get-Date).Year) Maxime Ethier",
    '--description', 'Untargeted LC-MS/MS annotation of the dark metabolome',
    '--runtime-image', $runtime,
    '--input', $libs,
    '--main-jar', "metabodark-desktop-$pomVersion.jar",
    '--main-class', 'com.bioinfo.metabodark.desktop.DesktopLauncher',
    '--java-options', '-Dfile.encoding=UTF-8',
    '--dest', $image)

# 4. Portable archive.
New-Item -ItemType Directory -Force (Split-Path $zip) | Out-Null
if (Test-Path $zip) { Remove-Item $zip }
Compress-Archive -Path "$image\$name" -DestinationPath $zip -CompressionLevel Optimal
$sizeMb = [math]::Round((Get-Item $zip).Length / 1MB, 1)
Write-Host "Created $zip ($sizeMb MB)"
