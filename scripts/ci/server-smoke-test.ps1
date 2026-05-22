[CmdletBinding()]
param(
	[int] $TimeoutSeconds = 180,
	[int] $ShutdownTimeoutSeconds = 60,
	[string] $GradleTask = "runCiServer",
	[string] $RunDir = "run/ci-server-smoke"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$repoRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "../..")).Path
$serverRunDir = Join-Path $repoRoot $RunDir
$eulaPath = Join-Path $serverRunDir "eula.txt"
$isWindowsPlatform = if (Get-Variable -Name IsWindows -ErrorAction SilentlyContinue) {
	$IsWindows
} else {
	$env:OS -eq "Windows_NT"
}

if ($isWindowsPlatform) {
	throw "This smoke test is Linux-only. The GitHub workflow runs it only on Linux."
}

if ($GradleTask -notmatch '^[A-Za-z0-9_:.-]+$') {
	throw "Gradle task name contains unsupported characters: $GradleTask"
}

New-Item -ItemType Directory -Force -Path $serverRunDir | Out-Null
Set-Content -Path $eulaPath -Value "eula=true" -Encoding ascii

$gradlew = Join-Path $repoRoot "gradlew"
if (-not (Test-Path -LiteralPath $gradlew)) {
	throw "Gradle wrapper was not found at $gradlew"
}

& chmod +x $gradlew

$serverReady = $false
$readyPattern = 'Done \([^)]*\)! For help, type "help"'
$startCommand = "exec ./gradlew --no-daemon --console=plain $GradleTask 2>&1"

$startInfo = [System.Diagnostics.ProcessStartInfo]::new()
$startInfo.FileName = "bash"
[void] $startInfo.ArgumentList.Add("-lc")
[void] $startInfo.ArgumentList.Add($startCommand)
$startInfo.WorkingDirectory = $repoRoot
$startInfo.RedirectStandardInput = $true
$startInfo.RedirectStandardOutput = $true
$startInfo.UseShellExecute = $false

$process = [System.Diagnostics.Process]::new()
$process.StartInfo = $startInfo
$outputRead = $null

function Read-AvailableOutput {
	param(
		[System.Diagnostics.Process] $RunningProcess,
		[ref] $OutputRead,
		[ref] $ReadyFlag,
		[string] $ReadyRegex,
		[int] $WaitMilliseconds
	)

	if ($null -eq $OutputRead.Value) {
		return
	}

	if (-not $OutputRead.Value.Wait($WaitMilliseconds)) {
		return
	}

	$line = $OutputRead.Value.Result
	if ($null -eq $line) {
		$OutputRead.Value = $null
		return
	}

	Write-Host $line
	if ($line -match $ReadyRegex) {
		$ReadyFlag.Value = $true
	}

	if (-not $RunningProcess.HasExited) {
		$OutputRead.Value = $RunningProcess.StandardOutput.ReadLineAsync()
	} else {
		$OutputRead.Value = $null
	}
}

try {
	Write-Host "Starting Minecraft dedicated server smoke test with Gradle task '$GradleTask'."

	if (-not $process.Start()) {
		throw "Failed to start Gradle process."
	}

	$outputRead = $process.StandardOutput.ReadLineAsync()
	$startupDeadline = [DateTime]::UtcNow.AddSeconds($TimeoutSeconds)

	while (-not $serverReady) {
		Read-AvailableOutput -RunningProcess $process -OutputRead ([ref] $outputRead) -ReadyFlag ([ref] $serverReady) -ReadyRegex $readyPattern -WaitMilliseconds 500

		if ($serverReady) {
			break
		}

		if ($process.HasExited) {
			throw "Server exited before reaching the ready state. Exit code: $($process.ExitCode)"
		}

		if ([DateTime]::UtcNow -ge $startupDeadline) {
			throw "Server did not reach the ready state within $TimeoutSeconds seconds."
		}
	}

	Write-Host "Server reached the ready state. Stopping it cleanly."
	$process.StandardInput.WriteLine("stop")
	$process.StandardInput.Flush()

	$shutdownDeadline = [DateTime]::UtcNow.AddSeconds($ShutdownTimeoutSeconds)
	while (-not $process.HasExited) {
		Read-AvailableOutput -RunningProcess $process -OutputRead ([ref] $outputRead) -ReadyFlag ([ref] $serverReady) -ReadyRegex $readyPattern -WaitMilliseconds 500

		if ([DateTime]::UtcNow -ge $shutdownDeadline) {
			throw "Server did not stop within $ShutdownTimeoutSeconds seconds after receiving 'stop'."
		}
	}

	$drainDeadline = [DateTime]::UtcNow.AddSeconds(5)
	while ($null -ne $outputRead -and [DateTime]::UtcNow -lt $drainDeadline) {
		Read-AvailableOutput -RunningProcess $process -OutputRead ([ref] $outputRead) -ReadyFlag ([ref] $serverReady) -ReadyRegex $readyPattern -WaitMilliseconds 100
	}

	if ($process.ExitCode -ne 0) {
		throw "Server smoke test exited with code $($process.ExitCode)."
	}

	Write-Host "Server launch smoke test passed."
} finally {
	if ($null -ne $process -and -not $process.HasExited) {
		try {
			$process.StandardInput.WriteLine("stop")
			$process.StandardInput.Flush()
		} catch {
		}

		if (-not $process.WaitForExit(10000)) {
			$process.Kill()
			$process.WaitForExit()
		}
	}

	$process.Dispose()
}
