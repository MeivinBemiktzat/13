# ============================================================
#  bt-bridge.ps1 — גשר Serial אמיתי אל הפלאפון דרך הבלוטוס
# ============================================================
#  פותח את פורט ה-COM שהפלאפון חושף בבלוטוס (Standard Serial
#  over Bluetooth link), ומעביר פקודות AT הלוך ושוב.
#
#  - שורות שמגיעות מהפלאפון נכתבות ל-stdout בקידומת "SERIAL "
#  - פקודות שמגיעות מ-Node ב-stdin נשלחות אל הפלאפון
#  - אירועי פתיחה/שגיאה בקידומת "BRIDGE_"
# ============================================================
param(
  [Parameter(Mandatory = $true)][string]$Port,
  [int]$Baud = 115200
)

$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

try {
  $sp = New-Object System.IO.Ports.SerialPort $Port, $Baud, ([System.IO.Ports.Parity]::None), 8, ([System.IO.Ports.StopBits]::One)
  $sp.NewLine = "`r"
  $sp.ReadTimeout = 1000
  $sp.WriteTimeout = 1000
  $sp.DtrEnable = $true
  $sp.RtsEnable = $true
  $sp.Open()
} catch {
  [Console]::Out.WriteLine("BRIDGE_ERROR " + $_.Exception.Message)
  exit 1
}

[Console]::Out.WriteLine("BRIDGE_OPEN " + $Port)

# קבלת נתונים מהפלאפון (אירוע) -> stdout
$onData = {
  param($sender, $e)
  try {
    $chunk = $sender.ReadExisting()
    foreach ($ln in ($chunk -split "[`r`n]+")) {
      $t = $ln.Trim()
      if ($t.Length -gt 0) { [Console]::Out.WriteLine("SERIAL " + $t) }
    }
  } catch { }
}
$sp.add_DataReceived($onData)

# רצף אתחול תקני של מודם/פלאפון
Start-Sleep -Milliseconds 200
foreach ($init in @("ATZ", "ATE0", "AT+CMEE=1", "AT+CLIP=1", "AT+COLP=1")) {
  try { $sp.WriteLine($init); Start-Sleep -Milliseconds 120 } catch { }
}

# לולאה ראשית: קריאת פקודות מ-Node (stdin) ושליחתן לפלאפון
while ($true) {
  $cmd = [Console]::In.ReadLine()
  if ($null -eq $cmd) { break }
  if ($cmd -eq "__QUIT__") { break }
  try {
    $sp.WriteLine($cmd)
  } catch {
    [Console]::Out.WriteLine("BRIDGE_ERROR write_failed")
    break
  }
}

try { $sp.Close() } catch { }
[Console]::Out.WriteLine("BRIDGE_CLOSED")
