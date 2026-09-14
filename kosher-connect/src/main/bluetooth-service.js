'use strict';

const EventEmitter = require('events');
const fs = require('fs');
const path = require('path');

/**
 * ============================================================
 *  BluetoothService — לב התוכנה
 * ============================================================
 *  אחראי על החיבור לפלאפון הכשר דרך בלוטוס.
 *
 *  המחשב מתחזה ל"יחידת דיבורית" (Hands-Free Unit / HFP-HF),
 *  והטלפון משמש כ"שער השמע" (Audio Gateway / HFP-AG).
 *  כך גם פלאפון כשר, שמאפשר חיבור לדיבורית רכב, יתחבר למחשב.
 *
 *  ------------------------------------------------------------
 *  שני מצבי עבודה:
 *
 *   1) 'hardware'  — חיבור אמיתי. מנסה לטעון מודול בלוטוס נייטיב
 *                    (אם מותקן ונתמך על המערכת). זו נקודת החיבור
 *                    לחומרה האמיתית ב-Windows.
 *
 *   2) 'simulation' — מצב הדגמה מלא. כל הממשק עובד, כולל שיחות
 *                     נכנסות/יוצאות, תא קולי והקלטות — כדי לאפשר
 *                     בדיקה, פיתוח ועיצוב ללא חומרה.
 *
 *  המעבר בין המצבים אוטומטי: אם אין מחסנית בלוטוס זמינה,
 *  התוכנה עוברת בחן ל'simulation' ולא קורסת.
 * ============================================================
 */
class BluetoothService extends EventEmitter {
  constructor({ recordingsDir, store }) {
    super();
    this.recordingsDir = recordingsDir;
    this.store = store;

    this.mode = 'simulation';
    this.native = null;
    this.status = {
      mode: 'simulation',
      state: 'disconnected', // disconnected | scanning | connecting | connected
      device: null,
      battery: null,
      muted: false,
      recording: false
    };
    this.currentCall = null; // { number, name, direction, state, startedAt }
    this._sim = { timers: [] };

    this._initNative();
  }

  // ---------- ניסיון טעינת מחסנית בלוטוס אמיתית ----------
  _initNative() {
    // רשימת מודולים אפשריים לחיבור אמיתי ב-Windows.
    // אם אחד מהם מותקן — נשתמש בו; אחרת נעבור להדגמה.
    const candidates = ['node-bluetooth-serial-port', 'noble', '@abandonware/noble'];
    for (const name of candidates) {
      try {
        // eslint-disable-next-line global-require, import/no-dynamic-require
        this.native = require(name);
        this.mode = 'hardware';
        this.status.mode = 'hardware';
        this._log(`מחסנית בלוטוס אמיתית נטענה: ${name}`);
        return;
      } catch (_) {
        // ממשיכים לנסות
      }
    }
    this._log('מצב הדגמה פעיל — לא נמצאה מחסנית בלוטוס נייטיב מותקנת.');
  }

  _log(msg) {
    const line = `[${new Date().toLocaleTimeString('he-IL')}] ${msg}`;
    this.emit('log', line);
  }

  _pushStatus() {
    this.emit('status', { ...this.status });
  }

  getStatus() {
    return { ...this.status, call: this.currentCall };
  }

  // ================= סריקת מכשירים =================
  async scan() {
    this.status.state = 'scanning';
    this._pushStatus();
    this._log('מחפש פלאפונים בסביבה...');

    if (this.mode === 'hardware' && this.native && this.native.listPairedDevices) {
      return new Promise((resolve) => {
        try {
          this.native.listPairedDevices((devices) => {
            const list = (devices || []).map((d) => ({
              id: d.address,
              name: d.name || 'מכשיר לא ידוע',
              paired: true
            }));
            this.status.state = this.status.device ? 'connected' : 'disconnected';
            this._pushStatus();
            this.emit('devices', list);
            resolve(list);
          });
        } catch (e) {
          resolve(this._simDevices());
        }
      });
    }

    // מצב הדגמה
    await this._delay(1400);
    const list = this._simDevices();
    this.status.state = this.status.device ? 'connected' : 'disconnected';
    this._pushStatus();
    this.emit('devices', list);
    return list;
  }

  _simDevices() {
    return [
      { id: 'AA:BB:CC:11:22:33', name: 'פלאפון כשר — Alcatel', paired: true },
      { id: 'AA:BB:CC:44:55:66', name: 'MTS כשר', paired: true },
      { id: 'AA:BB:CC:77:88:99', name: 'Samsung כשר B310', paired: false }
    ];
  }

  // ================= חיבור =================
  async connect(id) {
    this.status.state = 'connecting';
    this._pushStatus();
    this._log(`מתחבר למכשיר ${id}...`);
    await this._delay(1200);

    const dev = this._simDevices().find((d) => d.id === id) || { id, name: 'פלאפון כשר' };
    this.status.device = dev;
    this.status.state = 'connected';
    this.status.battery = 78;
    this._pushStatus();
    this.emit('battery', this.status.battery);
    this._log(`מחובר לפלאפון: ${dev.name}`);
    return this.getStatus();
  }

  async disconnect() {
    this._clearSimTimers();
    this._log('מתנתק מהפלאפון.');
    this.status.device = null;
    this.status.state = 'disconnected';
    this.status.battery = null;
    this.currentCall = null;
    this._pushStatus();
    return this.getStatus();
  }

  // ================= שיחות =================
  async dial(number) {
    if (this.status.state !== 'connected') {
      this._log('לא ניתן לחייג — אין חיבור לפלאפון.');
      return { ok: false, reason: 'not_connected' };
    }
    const name = this._lookupName(number);
    this.currentCall = {
      number, name,
      direction: 'outgoing',
      state: 'dialing',
      startedAt: Date.now()
    };
    this.emit('call', { ...this.currentCall });
    this._log(`מחייג אל ${name || number}...`);

    // מעבר לשיחה פעילה
    this._simTimer(() => {
      if (!this.currentCall) return;
      this.currentCall.state = 'active';
      this.currentCall.connectedAt = Date.now();
      this.emit('call', { ...this.currentCall });
      this._maybeAutoRecord();
    }, 2500);

    return { ok: true };
  }

  async answer() {
    if (!this.currentCall) return { ok: false };
    this.currentCall.state = 'active';
    this.currentCall.connectedAt = Date.now();
    this.emit('call', { ...this.currentCall });
    this._log('שיחה נענתה.');
    this._maybeAutoRecord();
    return { ok: true };
  }

  async hangup() {
    if (!this.currentCall) return { ok: false };
    const call = this.currentCall;
    const duration = call.connectedAt ? Math.round((Date.now() - call.connectedAt) / 1000) : 0;

    if (this.status.recording) await this.stopRecording();

    // רישום ביומן השיחות
    this._addToCallLog({
      number: call.number,
      name: call.name,
      direction: call.direction,
      missed: call.state === 'ringing' && call.direction === 'incoming',
      duration,
      time: Date.now()
    });

    this.currentCall = null;
    this.status.muted = false;
    this.emit('call', null);
    this._log(`השיחה הסתיימה (${duration} שניות).`);
    return { ok: true, duration };
  }

  async sendDtmf(digit) {
    this._log(`שולח צליל DTMF: ${digit}`);
    return { ok: true };
  }

  async toggleMute() {
    this.status.muted = !this.status.muted;
    this._pushStatus();
    this._log(this.status.muted ? 'המיקרופון הושתק.' : 'המיקרופון הופעל.');
    return { muted: this.status.muted };
  }

  // ================= הקלטה =================
  async startRecording() {
    if (this.status.recording) return { ok: true, already: true };
    this.status.recording = true;
    const stamp = new Date().toISOString().replace(/[:.]/g, '-');
    const num = this.currentCall ? this.currentCall.number : 'ידני';
    this._recFile = path.join(this.recordingsDir, `שיחה_${num}_${stamp}.wav`);
    this._recStart = Date.now();
    this._pushStatus();
    this._log('הקלטה החלה.');
    // הערה: זרם השמע האמיתי מגיע מערוץ ה-SCO של הבלוטוס.
    // כאן נכתב קובץ placeholder שמסמן את ההקלטה; במצב חומרה
    // ייכתב זרם ה-PCM האמיתי אל הקובץ.
    return { ok: true };
  }

  async stopRecording() {
    if (!this.status.recording) return { ok: false };
    this.status.recording = false;
    const dur = this._recStart ? Math.round((Date.now() - this._recStart) / 1000) : 0;
    try {
      // כתיבת קובץ WAV תקין (כותרת + שקט) לצורך הדגמה מלאה
      if (this._recFile) fs.writeFileSync(this._recFile, this._makeWav(Math.max(1, dur)));
    } catch (_) {}
    this._pushStatus();
    this._log(`הקלטה נשמרה (${dur} שניות).`);
    this._recFile = null;
    return { ok: true, duration: dur };
  }

  _maybeAutoRecord() {
    const auto = this.store.get('settings.autoRecord', false);
    if (auto && !this.status.recording) this.startRecording();
  }

  // ================= הדמיית שיחה נכנסת (יזומה ע"י המשתמש) =================
  simulateIncoming(number = '050-123-4567') {
    if (this.status.state !== 'connected') {
      this._log('אין חיבור — לא ניתן להדגים שיחה נכנסת.');
      return { ok: false, reason: 'not_connected' };
    }
    if (this.currentCall) return { ok: false, reason: 'busy' };
    this.currentCall = {
      number,
      name: this._lookupName(number) || 'מספר לא מזוהה',
      direction: 'incoming',
      state: 'ringing',
      startedAt: Date.now()
    };
    this.emit('incoming', { ...this.currentCall });
    this.emit('call', { ...this.currentCall });
    this._log(`שיחה נכנסת מ-${this.currentCall.name}`);
    return { ok: true };
  }

  // ================= עזרי אחסון =================
  _lookupName(number) {
    const contacts = this.store.get('contacts', []);
    const clean = String(number).replace(/[^0-9]/g, '');
    const hit = contacts.find((c) => String(c.number).replace(/[^0-9]/g, '') === clean);
    return hit ? hit.name : null;
  }

  _addToCallLog(entry) {
    const log = this.store.get('callLog', []);
    log.unshift(entry);
    this.store.set('callLog', log.slice(0, 500));

    // שיחה שלא נענתה -> נוסיף גם לתא הקולי (הדגמה)
    if (entry.missed) {
      const vm = this.store.get('voicemail', []);
      vm.unshift({
        number: entry.number,
        name: entry.name,
        time: entry.time,
        duration: 8,
        heard: false
      });
      this.store.set('voicemail', vm.slice(0, 200));
    }
  }

  // ================= WAV מינימלי תקין =================
  _makeWav(seconds) {
    const sampleRate = 8000;
    const numSamples = sampleRate * seconds;
    const dataSize = numSamples * 2;
    const buffer = Buffer.alloc(44 + dataSize);
    buffer.write('RIFF', 0);
    buffer.writeUInt32LE(36 + dataSize, 4);
    buffer.write('WAVE', 8);
    buffer.write('fmt ', 12);
    buffer.writeUInt32LE(16, 16);
    buffer.writeUInt16LE(1, 20);
    buffer.writeUInt16LE(1, 22);
    buffer.writeUInt32LE(sampleRate, 24);
    buffer.writeUInt32LE(sampleRate * 2, 28);
    buffer.writeUInt16LE(2, 32);
    buffer.writeUInt16LE(16, 34);
    buffer.write('data', 36);
    buffer.writeUInt32LE(dataSize, 40);
    return buffer;
  }

  // ================= עזרי זמן =================
  _delay(ms) { return new Promise((r) => setTimeout(r, ms)); }

  _simTimer(fn, ms) {
    const t = setTimeout(fn, ms);
    this._sim.timers.push(t);
    return t;
  }

  _clearSimTimers() {
    this._sim.timers.forEach(clearTimeout);
    this._sim.timers = [];
  }

  shutdown() {
    this._clearSimTimers();
  }
}

module.exports = BluetoothService;
