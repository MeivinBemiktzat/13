'use strict';

const EventEmitter = require('events');
const { spawn, execFile } = require('child_process');
const fs = require('fs');
const os = require('os');
const path = require('path');

/**
 * ============================================================
 *  BluetoothService — חיבור אמיתי לפלאפון דרך הבלוטוס
 * ============================================================
 *  ללא הדגמות. שני חלקים אמיתיים:
 *
 *  1) זיהוי — דרך Windows (PowerShell):
 *     • אילו פורטי COM של בלוטוס (Serial/מודם) קיימים
 *     • אילו פלאפונים מותאמים ומה מצב החיבור שלהם
 *
 *  2) שליטה בשיחות — דרך פקודות AT על פורט ה-Serial שהפלאפון
 *     חושף בבלוטוס (המנגנון האמיתי לשליטה בפלאפון פשוט/כשר):
 *       ATD<מספר>;   → חיוג
 *       ATA          → מענה
 *       ATH / AT+CHUP→ ניתוק
 *       RING / +CLIP → שיחה נכנסת
 *       +CBC         → מצב סוללה
 *
 *  התקשורת עם הפורט מתבצעת דרך גשר PowerShell (bt-bridge.ps1)
 *  המשתמש ב-System.IO.Ports.SerialPort — ללא מודולים נייטיב.
 * ============================================================
 */
class BluetoothService extends EventEmitter {
  constructor({ recordingsDir, store, bridgeScript }) {
    super();
    this.recordingsDir = recordingsDir;
    this.store = store;
    this.bridgeScript = bridgeScript; // נתיב מלא ל-bt-bridge.ps1 (מחוץ ל-asar)
    this.isWindows = process.platform === 'win32';

    this.status = {
      state: 'disconnected', // disconnected | scanning | connecting | connected
      device: null,          // { id, name, port }
      battery: null,
      muted: false,
      recording: false,
      supported: this.isWindows
    };
    this.currentCall = null;
    this.bridge = null;      // תהליך PowerShell
    this._batteryTimer = null;
    this._pendingDial = null;
  }

  _log(msg) {
    this.emit('log', `[${new Date().toLocaleTimeString('he-IL')}] ${msg}`);
  }

  _pushStatus() { this.emit('status', { ...this.status }); }

  getStatus() { return { ...this.status, call: this.currentCall }; }

  // ---------- הרצת PowerShell וקבלת פלט ----------
  _ps(script) {
    return new Promise((resolve) => {
      if (!this.isWindows) return resolve({ ok: false, out: '', err: 'not_windows' });
      execFile('powershell.exe',
        ['-NoProfile', '-NonInteractive', '-ExecutionPolicy', 'Bypass', '-Command', script],
        { windowsHide: true, timeout: 15000, maxBuffer: 1024 * 1024 },
        (err, stdout, stderr) => resolve({ ok: !err, out: (stdout || '').trim(), err: stderr || (err && err.message) || '' })
      );
    });
  }

  // ================= סריקה / זיהוי אמיתי =================
  async scan() {
    this.status.state = 'scanning';
    this._pushStatus();
    this._log('סורק מכשירי בלוטוס במחשב...');

    if (!this.isWindows) {
      this._log('זיהוי בלוטוס אמיתי זמין רק ב-Windows.');
      this.status.state = this.status.device ? 'connected' : 'disconnected';
      this._pushStatus();
      this.emit('devices', []);
      return [];
    }

    const devices = await this._discover();
    this.status.state = this.status.device ? 'connected' : 'disconnected';
    this._pushStatus();
    this.emit('devices', devices);
    this._log(devices.length ? `נמצאו ${devices.length} מכשירים.` : 'לא נמצא פורט בלוטוס/Serial. ודאו שהפלאפון מותאם ושירות ה-Serial פעיל.');
    return devices;
  }

  async _discover() {
    // פורטי COM של בלוטוס (אלה שאפשר לשלוט דרכם בשיחות)
    const portScript = `
      $ports = @()
      try {
        $ports += Get-CimInstance Win32_SerialPort -ErrorAction SilentlyContinue |
          Where-Object { $_.Name -match 'Bluetooth' -or $_.Description -match 'Bluetooth' } |
          Select-Object @{n='port';e={$_.DeviceID}}, @{n='name';e={$_.Name}}
      } catch {}
      try {
        $ports += Get-CimInstance Win32_PnPEntity -ErrorAction SilentlyContinue |
          Where-Object { $_.Name -match 'Standard Serial over Bluetooth link \\(COM' } |
          Select-Object @{n='port';e={ if($_.Name -match '\\((COM\\d+)\\)'){$matches[1]} else {''} }}, @{n='name';e={$_.Name}}
      } catch {}
      $ports | Where-Object { $_.port } | Sort-Object port -Unique | ConvertTo-Json -Compress`;

    // פלאפונים מותאמים ומצב חיבור
    const pairedScript = `
      try {
        Get-PnpDevice -Class Bluetooth -ErrorAction SilentlyContinue |
          Where-Object { $_.InstanceId -match 'DEV_[0-9A-Fa-f]{12}' } |
          Select-Object @{n='name';e={$_.FriendlyName}}, @{n='status';e={$_.Status}} |
          ConvertTo-Json -Compress
      } catch {}`;

    const [pRes, pairRes] = await Promise.all([this._ps(portScript), this._ps(pairedScript)]);

    const ports = this._parseJson(pRes.out);
    const paired = this._parseJson(pairRes.out);
    const pairedNames = paired.map((d) => (d.name || '').trim()).filter(Boolean);
    const anyConnected = paired.some((d) => /ok/i.test(d.status || ''));

    // בונים רשימת מכשירים לשליטה = פורטי בלוטוס
    const devices = ports.map((p) => {
      const port = (p.port || '').toString().toUpperCase();
      // מנסים לשייך שם פלאפון מותאם
      const phoneName = pairedNames.find((n) => n && n.length < 40) || null;
      return {
        id: port,
        port,
        name: phoneName ? `${phoneName} (${port})` : `פלאפון בבלוטוס (${port})`,
        pairedConnected: anyConnected,
        connected: this.status.device && this.status.device.port === port
      };
    });
    return devices;
  }

  _parseJson(str) {
    if (!str) return [];
    try {
      const v = JSON.parse(str);
      return Array.isArray(v) ? v : [v];
    } catch (_) { return []; }
  }

  // ---------- זיהוי אוטומטי בעת עלייה ----------
  async autoDetect() {
    if (!this.isWindows) { this._pushStatus(); return; }
    const devices = await this._discover();
    this.emit('devices', devices);
    // אם קיים בדיוק פורט בלוטוס אחד — מתחברים אליו אוטומטית
    if (devices.length === 1) {
      this._log('זוהה פלאפון מחובר — מתחבר אוטומטית.');
      await this.connect(devices[0].port);
    } else if (devices.length > 1) {
      this._log(`זוהו ${devices.length} מכשירים — בחרו למי להתחבר.`);
      this._pushStatus();
    } else {
      this._pushStatus();
    }
  }

  // ================= חיבור אמיתי (פתיחת פורט) =================
  async connect(port) {
    if (!this.isWindows) {
      this._log('חיבור אמיתי זמין רק ב-Windows.');
      return { ok: false, reason: 'not_windows' };
    }
    if (this.bridge) await this.disconnect();

    port = String(port).toUpperCase();
    this.status.state = 'connecting';
    this._pushStatus();
    this._log(`פותח את פורט ${port}...`);

    return new Promise((resolve) => {
      let settled = false;
      const done = (res) => { if (!settled) { settled = true; resolve(res); } };

      let child;
      try {
        child = spawn('powershell.exe',
          ['-NoProfile', '-NonInteractive', '-ExecutionPolicy', 'Bypass', '-File', this.bridgeScript, '-Port', port],
          { windowsHide: true });
      } catch (e) {
        this.status.state = 'disconnected';
        this._pushStatus();
        this._log('כשל בהפעלת הגשר: ' + e.message);
        return done({ ok: false, reason: 'spawn_failed' });
      }

      this.bridge = child;
      let buf = '';

      child.stdout.on('data', (data) => {
        buf += data.toString('utf8');
        let idx;
        while ((idx = buf.indexOf('\n')) >= 0) {
          const line = buf.slice(0, idx).replace(/\r$/, '').trim();
          buf = buf.slice(idx + 1);
          if (line) this._handleBridgeLine(line, port, done);
        }
      });

      child.stderr.on('data', (d) => this._log('גשר: ' + d.toString().trim()));

      child.on('exit', (code) => {
        this._log(`הגשר נסגר (קוד ${code}).`);
        if (this._batteryTimer) { clearInterval(this._batteryTimer); this._batteryTimer = null; }
        this.bridge = null;
        if (this.status.state !== 'disconnected') {
          this.status.state = 'disconnected';
          this.status.device = null;
          this.status.battery = null;
          this.currentCall = null;
          this._pushStatus();
          this.emit('call', null);
        }
        done({ ok: false, reason: 'closed' });
      });

      // בטיחות: אם לא נפתח תוך 8 שניות
      setTimeout(() => {
        if (this.status.state === 'connecting') {
          this._log('הפורט לא נפתח בזמן — ודאו שהפלאפון מחובר.');
          this.disconnect();
          done({ ok: false, reason: 'timeout' });
        }
      }, 8000);
    });
  }

  _handleBridgeLine(line, port, done) {
    if (line.startsWith('BRIDGE_OPEN')) {
      this.status.state = 'connected';
      this.status.device = { id: port, port, name: `פלאפון בבלוטוס (${port})` };
      this._pushStatus();
      this._log(`מחובר לפלאפון דרך ${port}.`);
      this._startBatteryPolling();
      if (done) done({ ok: true });
      return;
    }
    if (line.startsWith('BRIDGE_ERROR')) {
      this._log('שגיאת חיבור: ' + line.replace('BRIDGE_ERROR', '').trim());
      return;
    }
    if (line.startsWith('BRIDGE_CLOSED')) return;
    if (line.startsWith('SERIAL ')) {
      this._handleAtLine(line.slice(7).trim());
    }
  }

  // ---------- פענוח תשובות AT מהפלאפון ----------
  _handleAtLine(ln) {
    if (!ln || ln === 'OK' || ln === 'ATZ' || /^ATE0/.test(ln)) return;

    // שיחה נכנסת
    if (ln === 'RING') {
      if (!this.currentCall) {
        this.currentCall = { number: '', name: '', direction: 'incoming', state: 'ringing', startedAt: Date.now() };
        this.emit('incoming', { ...this.currentCall });
        this.emit('call', { ...this.currentCall });
        this._log('שיחה נכנסת...');
      }
      return;
    }

    // זיהוי מתקשר: +CLIP: "0501234567",129,...
    let m = ln.match(/\+CLIP:\s*"([^"]*)"/);
    if (m && this.currentCall) {
      this.currentCall.number = m[1];
      this.currentCall.name = this._lookupName(m[1]) || m[1];
      this.emit('call', { ...this.currentCall });
      return;
    }

    // מספר יוצא שאושר: +COLP: "0501234567"
    m = ln.match(/\+COLP:\s*"([^"]*)"/);
    if (m && this.currentCall && this.currentCall.direction === 'outgoing') {
      if (!this.currentCall.number) this.currentCall.number = m[1];
      return;
    }

    // סוללה: +CBC: 0,85
    m = ln.match(/\+CBC:\s*\d+,\s*(\d+)/);
    if (m) {
      this.status.battery = parseInt(m[1], 10);
      this.emit('battery', this.status.battery);
      this._pushStatus();
      return;
    }

    // סיום שיחה
    if (/^(NO CARRIER|BUSY|NO ANSWER|NO DIALTONE)$/.test(ln)) {
      this._endCall(ln);
      return;
    }

    if (ln === 'ERROR' || /\+CME ERROR/.test(ln)) {
      this._log('הפלאפון החזיר שגיאה: ' + ln);
    }
  }

  // ================= שיחות אמיתיות =================
  _send(cmd) {
    if (this.bridge && this.bridge.stdin.writable) {
      this.bridge.stdin.write(cmd + '\n');
      return true;
    }
    return false;
  }

  async dial(number) {
    if (this.status.state !== 'connected') return { ok: false, reason: 'not_connected' };
    const clean = String(number).replace(/[^0-9+*#]/g, '');
    this.currentCall = {
      number: clean,
      name: this._lookupName(clean) || clean,
      direction: 'outgoing',
      state: 'dialing',
      startedAt: Date.now()
    };
    this.emit('call', { ...this.currentCall });
    this._log(`מחייג אל ${this.currentCall.name}...`);
    if (!this._send(`ATD${clean};`)) return { ok: false, reason: 'no_bridge' };
    // מעבר משוער למצב פעיל (הפלאפון לא תמיד מדווח)
    this._dialTimer = setTimeout(() => {
      if (this.currentCall && this.currentCall.state === 'dialing') {
        this.currentCall.state = 'active';
        this.currentCall.connectedAt = Date.now();
        this.emit('call', { ...this.currentCall });
      }
    }, 3000);
    return { ok: true };
  }

  async answer() {
    if (!this.currentCall) return { ok: false };
    this._send('ATA');
    this.currentCall.state = 'active';
    this.currentCall.connectedAt = Date.now();
    this.emit('call', { ...this.currentCall });
    this._log('שיחה נענתה.');
    return { ok: true };
  }

  async hangup() {
    if (!this.currentCall) { this._send('ATH'); return { ok: false }; }
    this._send('AT+CHUP');
    this._send('ATH');
    this._endCall('local');
    return { ok: true };
  }

  _endCall(reason) {
    if (this._dialTimer) { clearTimeout(this._dialTimer); this._dialTimer = null; }
    const call = this.currentCall;
    if (!call) return;
    const dur = call.connectedAt ? Math.round((Date.now() - call.connectedAt) / 1000) : 0;
    this._addToCallLog({
      number: call.number,
      name: call.name && call.name !== call.number ? call.name : null,
      direction: call.direction,
      missed: call.state === 'ringing' && call.direction === 'incoming',
      duration: dur,
      time: Date.now()
    });
    this.currentCall = null;
    this.status.muted = false;
    this.emit('call', null);
    this._log(`השיחה הסתיימה (${dur} שניות).`);
  }

  async sendDtmf(digit) {
    this._send(`AT+VTS=${digit}`);
    return { ok: true };
  }

  async toggleMute() {
    this.status.muted = !this.status.muted;
    this._send(this.status.muted ? 'AT+CMUT=1' : 'AT+CMUT=0');
    this._pushStatus();
    return { muted: this.status.muted };
  }

  // ================= סוללה =================
  _startBatteryPolling() {
    if (this._batteryTimer) clearInterval(this._batteryTimer);
    this._send('AT+CBC');
    this._batteryTimer = setInterval(() => this._send('AT+CBC'), 60000);
  }

  // ================= הקלטה (הקלטת מיקרופון המחשב) =================
  // הערה: הקלטת ערוץ השמע של שיחת HFP אינה זמינה דרך פקודות AT.
  // ההקלטה מתבצעת בצד המסך (Web Audio) של מיקרופון המחשב.

  // ================= אחסון =================
  _lookupName(number) {
    const contacts = this.store.get('contacts', []);
    const clean = String(number).replace(/[^0-9]/g, '');
    if (!clean) return null;
    const hit = contacts.find((c) => String(c.number).replace(/[^0-9]/g, '').endsWith(clean.slice(-7)));
    return hit ? hit.name : null;
  }

  _addToCallLog(entry) {
    if (!entry.number) return;
    const log = this.store.get('callLog', []);
    log.unshift(entry);
    this.store.set('callLog', log.slice(0, 500));
  }

  // ================= ניתוק / כיבוי =================
  async disconnect() {
    if (this._batteryTimer) { clearInterval(this._batteryTimer); this._batteryTimer = null; }
    if (this.bridge) {
      try { this.bridge.stdin.write('__QUIT__\n'); } catch (_) {}
      const b = this.bridge;
      this.bridge = null;
      setTimeout(() => { try { b.kill(); } catch (_) {} }, 500);
    }
    this.status.state = 'disconnected';
    this.status.device = null;
    this.status.battery = null;
    this.currentCall = null;
    this._pushStatus();
    this.emit('call', null);
    this._log('נותק מהפלאפון.');
    return this.getStatus();
  }

  // פתיחת הגדרות הבלוטוס של Windows
  openBluetoothSettings() {
    if (this.isWindows) {
      execFile('cmd.exe', ['/c', 'start', 'ms-settings:bluetooth'], { windowsHide: true }, () => {});
    }
    return true;
  }

  shutdown() {
    if (this._batteryTimer) clearInterval(this._batteryTimer);
    if (this.bridge) { try { this.bridge.stdin.write('__QUIT__\n'); this.bridge.kill(); } catch (_) {} }
  }
}

module.exports = BluetoothService;
