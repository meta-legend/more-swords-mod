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

New-Item -ItemType Directory -Force -Path $serverRunDir | Out-Null
Set-Content -Path $eulaPath -Value "eula=true" -Encoding ascii

$gradlew = Join-Path $repoRoot "gradlew"

if (-not (Test-Path -LiteralPath $gradlew)) {
	throw "Gradle wrapper was not found at $gradlew"
}

& chmod +x $gradlew

$script:serverReady = $false
$script:readyPattern = 'Done \([^)]*\)! For help, type "help"'

$startInfo = [System.Diagnostics.ProcessStartInfo]::new()
$startInfo.FileName = $gradlew
$startInfo.Arguments = "--no-daemon --console=plain $GradleTask"
$startInfo.WorkingDirectory = $repoRoot
$startInfo.RedirectStandardInput = $true
$startInfo.RedirectStandardOutput = $true
$startInfo.RedirectStandardError = $true
$startInfo.UseShellExecute = $false

$process = [System.Diagnostics.Process]::new()
$process.StartInfo = $startInfo

$lineHandler = [System.Diagnostics.DataReceivedEventHandler] {
	param($sender, $eventArgs)

	if ([string]::IsNullOrWhiteSpace($eventArgs.Data)) {
		return
	}

	Write-Host $eventArgs.Data
	if ($eventArgs.Data -match $script:readyPattern) {
		$script:serverReady = $true
	}
}

$process.add_OutputDataReceived($lineHandler)
$process.add_ErrorDataReceived($lineHandler)

try {
	Write-Host "Starting Minecraft dedicated server smoke test with Gradle task '$GradleTask'."

	if (-not $process.Start()) {
		throw "Failed to start Gradle process."
	}

	$process.BeginOutputReadLine()
	$process.BeginErrorReadLine()

	$deadline = [DateTime]::UtcNow.AddSeconds($TimeoutSeconds)
	while (-not $script:serverReady) {
		if ($process.HasExited) {
			throw "Server exited before reaching the ready state. Exit code: $($process.ExitCode)"
		}

		if ([DateTime]::UtcNow -ge $deadline) {
			throw "Server did not reach the ready state within $TimeoutSeconds seconds."
		}

		Start-Sleep -Milliseconds 500
	}

	Write-Host "Server reached the ready state. Stopping it cleanly."
	$process.StandardInput.WriteLine("stop")
	$process.StandardInput.Flush()

	if (-not $process.WaitForExit($ShutdownTimeoutSeconds * 1000)) {
		throw "Server did not stop within $ShutdownTimeoutSeconds seconds after receiving 'stop'."
	}

	$process.WaitForExit()

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

	$process.remove_OutputDataReceived($lineHandler)
	$process.remove_ErrorDataReceived($lineHandler)
	$process.Dispose()
}
