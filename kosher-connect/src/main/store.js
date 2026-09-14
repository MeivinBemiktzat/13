'use strict';

const fs = require('fs');
const path = require('path');

/**
 * מאגר נתונים פשוט ואמין מבוסס JSON.
 * שומר אנשי קשר, יומן שיחות, תא קולי, הגדרות וכו'.
 * ללא תלות חיצונית — עובד תמיד.
 */
class Store {
  constructor(dir) {
    this.file = path.join(dir, 'kosherconnect.json');
    this.data = this._load();
  }

  _load() {
    try {
      if (fs.existsSync(this.file)) {
        return JSON.parse(fs.readFileSync(this.file, 'utf8'));
      }
    } catch (_) {
      // אם הקובץ פגום — מתחילים נקי (וגיבוי לצד)
      try { fs.renameSync(this.file, this.file + '.bak'); } catch (_) {}
    }
    return this._defaults();
  }

  _defaults() {
    return {
      contacts: [],
      callLog: [],
      voicemail: [],
      settings: {
        autoRecord: false,
        babysitterEnabled: false,
        babysitterThreshold: 55,
        theme: 'dark',
        deviceName: 'KosherConnect PC',
        ringtone: 'classic'
      }
    };
  }

  _save() {
    try {
      const tmp = this.file + '.tmp';
      fs.writeFileSync(tmp, JSON.stringify(this.data, null, 2), 'utf8');
      fs.renameSync(tmp, this.file);
    } catch (_) {}
  }

  get(key, def) {
    const val = this._resolve(key);
    return val === undefined ? def : val;
  }

  set(key, val) {
    this._assign(key, val);
    this._save();
  }

  _resolve(key) {
    return key.split('.').reduce((o, k) => (o == null ? undefined : o[k]), this.data);
  }

  _assign(key, val) {
    const parts = key.split('.');
    let obj = this.data;
    for (let i = 0; i < parts.length - 1; i++) {
      if (typeof obj[parts[i]] !== 'object' || obj[parts[i]] === null) obj[parts[i]] = {};
      obj = obj[parts[i]];
    }
    obj[parts[parts.length - 1]] = val;
  }
}

module.exports = Store;
