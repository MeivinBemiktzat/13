'use strict';

/* =========================================================
   KosherConnect — לוגיקת המסך (Renderer)
   ניהול תצוגות, שיחות, אנשי קשר, יומן, תא קולי, הקלטות ועוד.
   ========================================================= */

const api = window.api;

const State = {
  status: { state: 'disconnected', device: null, mode: 'simulation', battery: null, muted: false, recording: false },
  call: null,
  callTimer: null,
  view: 'dashboard'
};

const $ = (sel, root = document) => root.querySelector(sel);
const $$ = (sel, root = document) => Array.from(root.querySelectorAll(sel));
const esc = (s) => String(s == null ? '' : s).replace(/[&<>"]/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c]));

const VIEW_TITLES = {
  dashboard: 'בית', dialer: 'חייגן', contacts: 'אנשי קשר', calllog: 'יומן שיחות',
  voicemail: 'תא קולי', recordings: 'הקלטות', babysitter: 'בייביסיטר',
  devices: 'חיבור בלוטוס', settings: 'הגדרות'
};

/* ==================== עזרי תצוגה ==================== */
function toast(msg, type = '') {
  const host = $('#toastHost');
  const el = document.createElement('div');
  el.className = 'toast ' + type;
  el.textContent = msg;
  host.appendChild(el);
  setTimeout(() => { el.style.opacity = '0'; el.style.transition = 'opacity .4s'; setTimeout(() => el.remove(), 400); }, 3200);
}

function fmtDuration(sec) {
  sec = Math.max(0, Math.floor(sec || 0));
  const m = String(Math.floor(sec / 60)).padStart(2, '0');
  const s = String(sec % 60).padStart(2, '0');
  return `${m}:${s}`;
}
function fmtTime(ts) {
  const d = new Date(ts);
  return d.toLocaleString('he-IL', { day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit' });
}
function initials(name) {
  if (!name) return '👤';
  const parts = String(name).trim().split(/\s+/);
  return (parts[0][0] || '') + (parts[1] ? parts[1][0] : '');
}
function avatarClass(seed) {
  const cls = ['', 'green', 'gold', 'red'];
  let h = 0; for (const c of String(seed)) h = (h * 31 + c.charCodeAt(0)) % 4;
  return cls[h];
}

/* ==================== ניווט ==================== */
function switchView(view) {
  State.view = view;
  $$('.nav-item').forEach((b) => b.classList.toggle('active', b.dataset.view === view));
  $('#viewTitle').textContent = VIEW_TITLES[view] || '';
  render(view);
}

async function render(view) {
  const host = $('#viewHost');
  host.classList.remove('view-enter');
  void host.offsetWidth;
  host.classList.add('view-enter');
  const fn = Views[view];
  host.innerHTML = fn ? await fn() : '';
  if (AfterRender[view]) await AfterRender[view]();
}

/* ==================== התצוגות ==================== */
const Views = {
  async dashboard() {
    const log = await api.store.get('callLog', []);
    const contacts = await api.store.get('contacts', []);
    const vm = await api.store.get('voicemail', []);
    const recs = await api.rec.list();
    const unheard = vm.filter((v) => !v.heard).length;
    const connected = State.status.state === 'connected';
    return `
      <div class="hero">
        <h2>שלום, ברוך הבא ל-KosherConnect 👋</h2>
        <p>המחשב שלך הופך לדיבורית בלוטוס חכמה. חברו את הפלאפון הכשר ונהלו שיחות,
           תא קולי, הקלטות ובייביסיטר — הכול ממסך אחד יפהפה.</p>
        <div class="hero-actions">
          <button class="btn" onclick="go('devices')">📶 ${connected ? 'מחובר' : 'חיבור פלאפון'}</button>
          <button class="btn ghost" onclick="go('dialer')">📞 חייגן</button>
        </div>
      </div>

      <div class="grid cols-3" style="margin-top:20px">
        <div class="card"><div class="stat"><span class="num brand">${log.length}</span><span class="lbl">שיחות ביומן</span></div></div>
        <div class="card"><div class="stat"><span class="num accent">${contacts.length}</span><span class="lbl">אנשי קשר</span></div></div>
        <div class="card"><div class="stat"><span class="num gold">${recs.length}</span><span class="lbl">הקלטות שמורות</span></div></div>
      </div>

      <div class="grid cols-2" style="margin-top:20px">
        <div class="card">
          <h3>שיחות אחרונות</h3>
          <div class="sub">הפעילות האחרונה שלך</div>
          <div class="list">${log.slice(0, 5).map(logRow).join('') || emptyMini('אין שיחות עדיין')}</div>
        </div>
        <div class="card">
          <h3>תא קולי ${unheard ? `· ${unheard} חדשות` : ''}</h3>
          <div class="sub">הודעות שממתינות לך</div>
          <div class="list">${vm.slice(0, 5).map(vmRow).join('') || emptyMini('התא הקולי ריק')}</div>
        </div>
      </div>`;
  },

  async dialer() {
    const keys = [
      ['1', ''], ['2', 'ABC'], ['3', 'DEF'],
      ['4', 'GHI'], ['5', 'JKL'], ['6', 'MNO'],
      ['7', 'PQRS'], ['8', 'TUV'], ['9', 'WXYZ'],
      ['*', ''], ['0', '+'], ['#', '']
    ];
    return `
      <div class="dialpad-wrap card">
        <div class="dial-display" id="dialDisplay"></div>
        <div class="dial-hint" id="dialHint">הקלידו מספר לחיוג</div>
        <div class="dialpad">
          ${keys.map(([n, s]) => `
            <button class="key" data-key="${n}">
              <span class="k-num">${n}</span><span class="k-sub">${s}</span>
            </button>`).join('')}
        </div>
        <div class="dial-controls">
          <button class="dial-back" id="dialBack" title="מחיקה">⌫</button>
          <button class="dial-call" id="dialCall" title="חיוג">📞</button>
          <button class="dial-back" id="dialAdd" title="שמירה כאיש קשר">➕</button>
        </div>
      </div>`;
  },

  async contacts() {
    const contacts = (await api.store.get('contacts', [])).sort((a, b) => a.name.localeCompare(b.name, 'he'));
    return `
      <div class="card" style="margin-bottom:18px">
        <div class="grid cols-2" style="gap:10px; grid-template-columns: 1fr 1fr auto;">
          <input class="input" id="cName" placeholder="שם איש הקשר" />
          <input class="input" id="cNum" placeholder="מספר טלפון" dir="ltr" />
          <button class="btn green" id="cAdd">הוספה</button>
        </div>
      </div>
      <input class="input" id="cSearch" placeholder="🔍 חיפוש איש קשר..." style="margin-bottom:14px" />
      <div class="list" id="contactList">
        ${contacts.map(contactRow).join('') || emptyBig('👥', 'אין אנשי קשר', 'הוסיפו איש קשר ראשון למעלה')}
      </div>`;
  },

  async calllog() {
    const log = await api.store.get('callLog', []);
    return `
      <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:14px">
        <div class="section-title" style="margin:0">${log.length} שיחות</div>
        ${log.length ? '<button class="btn ghost sm" id="clearLog">ניקוי יומן</button>' : ''}
      </div>
      <div class="list">${log.map(logRow).join('') || emptyBig('🕓', 'יומן השיחות ריק', 'שיחות שתבצעו יופיעו כאן')}</div>`;
  },

  async voicemail() {
    const vm = await api.store.get('voicemail', []);
    return `<div class="list">${vm.map(vmRow).join('') || emptyBig('📬', 'התא הקולי ריק', 'הודעות ממתינות יופיעו כאן')}</div>`;
  },

  async recordings() {
    const recs = await api.rec.list();
    return `
      <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:14px">
        <div class="section-title" style="margin:0">${recs.length} הקלטות</div>
        <button class="btn ghost sm" id="openRecFolder">📂 פתיחת התיקייה</button>
      </div>
      <div class="list">${recs.map(recRow).join('') || emptyBig('🎙️', 'אין הקלטות', 'הקלטות שיחה יישמרו כאן אוטומטית')}</div>`;
  },

  async babysitter() {
    const s = await api.store.get('settings', {});
    return `
      <div class="grid cols-2">
        <div class="baby-mon">
          <div class="section-title" style="margin:0 0 10px">ניטור חדר בזמן אמת</div>
          <div class="baby-level" id="babyLevel">
            <div class="inner"><div class="val" id="babyVal">0</div><div class="unit">רמת רעש</div></div>
          </div>
          <div class="baby-wave" id="babyWave">${Array.from({length:16}).map(()=>'<i style="height:6px"></i>').join('')}</div>
          <div style="margin-top:16px">
            <button class="btn green" id="babyStart">▶ הפעלת ניטור</button>
            <button class="btn ghost" id="babyStop" hidden>⏸ עצירה</button>
          </div>
        </div>
        <div class="card">
          <h3>איך זה עובד?</h3>
          <div class="sub">מצב בייביסיטר / ניטור חדר</div>
          <p style="color:var(--muted); line-height:1.7; font-size:13.5px">
            כשהפלאפון הכשר מונח בחדר הילד, המחשב מאזין דרך ערוץ השמע של הבלוטוס.
            כאשר רמת הרעש (בכי / קול) חוצה את הסף שהגדרתם — תקבלו התראה מיידית,
            והמערכת יכולה לחייג אליכם חזרה אוטומטית.
          </p>
          <div class="setting-row">
            <div class="s-main"><div class="t">סף התראה</div><div class="d">רמת רעש שמעליה תישלח התראה</div></div>
            <input class="input" type="number" id="babyThreshold" min="10" max="100" value="${s.babysitterThreshold || 55}" style="width:90px" />
          </div>
          <div class="setting-row">
            <div class="s-main"><div class="t">חיוג חזרה אוטומטי</div><div class="d">חיוג אליכם כשמתגלה רעש חריג</div></div>
            <label class="switch"><input type="checkbox" id="babyCallback" ${s.babysitterCallback ? 'checked' : ''}/><span class="track"></span></label>
          </div>
        </div>
      </div>`;
  },

  async devices() {
    return `
      <div class="grid cols-2">
        <div class="card">
          <h3>חיבור לפלאפון כשר</h3>
          <div class="sub">המחשב מתחזה לדיבורית בלוטוס — חברו כל טלפון, גם כשר</div>
          <div class="device-scan">
            <button class="btn" id="btnScan">🔍 חיפוש מכשירים</button>
            <div class="spinner" id="scanSpin" hidden></div>
          </div>
          <div class="list" id="deviceList">
            ${emptyMini('לחצו על "חיפוש מכשירים" כדי להתחיל')}
          </div>
        </div>
        <div class="card">
          <h3>יומן חיבור</h3>
          <div class="sub">מצב מחסנית הבלוטוס בזמן אמת</div>
          <div class="log-box" id="logBox"></div>
          <p style="color:var(--muted-2); font-size:11.5px; margin-top:12px; line-height:1.6">
            הערה: חיבור חי לחומרה דורש מחסנית בלוטוס נתמכת ב-Windows.
            אם לא מותקנת — התוכנה פועלת במצב הדגמה מלא.
          </p>
        </div>
      </div>`;
  },

  async settings() {
    const s = await api.store.get('settings', {});
    const info = await api.sys.info();
    return `
      <div class="card" style="margin-bottom:18px">
        <h3>הגדרות שיחה</h3>
        <div class="sub">התאמת התנהגות התוכנה</div>
        <div class="setting-row">
          <div class="s-main"><div class="t">הקלטה אוטומטית</div><div class="d">הקלטת כל שיחה באופן אוטומטי</div></div>
          <label class="switch"><input type="checkbox" id="setAutoRec" ${s.autoRecord ? 'checked' : ''}/><span class="track"></span></label>
        </div>
        <div class="setting-row">
          <div class="s-main"><div class="t">שם המכשיר בבלוטוס</div><div class="d">כך הפלאפון יזהה את המחשב</div></div>
          <input class="input" id="setDeviceName" value="${esc(s.deviceName || 'KosherConnect PC')}" style="width:220px" />
        </div>
        <div class="setting-row">
          <div class="s-main"><div class="t">צליל שיחה נכנסת</div><div class="d">בחירת רינגטון</div></div>
          <select class="select" id="setRing" style="width:180px">
            <option value="classic" ${s.ringtone==='classic'?'selected':''}>קלאסי</option>
            <option value="gentle" ${s.ringtone==='gentle'?'selected':''}>עדין</option>
            <option value="marimba" ${s.ringtone==='marimba'?'selected':''}>מרימבה</option>
          </select>
        </div>
      </div>
      <div class="card">
        <h3>אודות</h3>
        <div class="sub">מידע על התוכנה</div>
        <div class="setting-row"><div class="s-main"><div class="t">גרסה</div></div><span style="color:var(--muted)">${esc(info.version)}</span></div>
        <div class="setting-row"><div class="s-main"><div class="t">מצב עבודה</div></div><span style="color:var(--muted)">${State.status.mode === 'hardware' ? 'חומרה אמיתית' : 'הדגמה'}</span></div>
        <div class="setting-row"><div class="s-main"><div class="t">תיקיית הקלטות</div></div><button class="btn ghost sm" id="openRec2">פתיחה</button></div>
      </div>`;
  }
};

/* ==================== שורות רשימה ==================== */
function contactRow(c) {
  return `<div class="row" data-num="${esc(c.number)}" data-name="${esc(c.name)}">
    <div class="avatar ${avatarClass(c.name)}">${esc(initials(c.name))}</div>
    <div class="row-main"><div class="name">${esc(c.name)}</div><div class="meta" dir="ltr">${esc(c.number)}</div></div>
    <div class="row-actions">
      <button class="icon-btn call" data-act="call" title="חיוג">📞</button>
      <button class="icon-btn del" data-act="del" title="מחיקה">🗑️</button>
    </div>
  </div>`;
}
function logRow(l) {
  const arrow = l.missed ? '↙️' : (l.direction === 'incoming' ? '↘️' : '↗️');
  const tag = l.missed ? '<span class="tag missed">לא נענתה</span>' : (l.direction === 'incoming' ? '<span class="tag in">נכנסת</span>' : '<span class="tag out">יוצאת</span>');
  return `<div class="row" data-num="${esc(l.number)}">
    <div class="avatar ${l.missed ? 'red' : avatarClass(l.name || l.number)}">${esc(initials(l.name) )}</div>
    <div class="row-main">
      <div class="name">${esc(l.name || l.number)}</div>
      <div class="meta">${arrow} ${tag} <span>${fmtTime(l.time)}</span> ${l.duration ? `<span>· ${fmtDuration(l.duration)}</span>` : ''}</div>
    </div>
    <div class="row-actions"><button class="icon-btn call" data-act="call" title="חיוג">📞</button></div>
  </div>`;
}
function vmRow(v) {
  return `<div class="row">
    <div class="avatar ${v.heard ? '' : 'gold'}">${v.heard ? '📭' : '📬'}</div>
    <div class="row-main"><div class="name">${esc(v.name || v.number)}</div>
      <div class="meta">${fmtTime(v.time)} · ${fmtDuration(v.duration)} ${v.heard ? '' : '<span class="tag missed">חדש</span>'}</div></div>
    <div class="row-actions">
      <button class="icon-btn" data-act="play" title="השמעה">▶️</button>
      <button class="icon-btn call" data-act="callback" data-num="${esc(v.number)}" title="חיוג חזרה">📞</button>
    </div>
  </div>`;
}
function recRow(r) {
  const kb = (r.size / 1024).toFixed(0);
  return `<div class="row">
    <div class="avatar gold">🎙️</div>
    <div class="row-main"><div class="name">${esc(r.name)}</div>
      <div class="meta">${fmtTime(r.mtime)} · ${kb} KB</div></div>
    <div class="row-actions">
      <button class="icon-btn" data-act="open" data-path="${esc(r.path)}" title="פתיחה בתיקייה">📂</button>
      <button class="icon-btn del" data-act="del" data-path="${esc(r.path)}" title="מחיקה">🗑️</button>
    </div>
  </div>`;
}
function emptyMini(t) { return `<div class="empty" style="padding:24px">${esc(t)}</div>`; }
function emptyBig(icon, t, sub) { return `<div class="empty"><div class="big">${icon}</div><div style="font-size:15px;color:var(--text)">${esc(t)}</div><div style="margin-top:4px">${esc(sub)}</div></div>`; }

/* ==================== לאחר רינדור: מאזינים ==================== */
const AfterRender = {
  dialer() {
    let num = '';
    const disp = $('#dialDisplay');
    const upd = () => { disp.textContent = num; };
    $$('.key').forEach((k) => k.addEventListener('click', () => { num += k.dataset.key; upd(); beep(); }));
    $('#dialBack').addEventListener('click', () => { num = num.slice(0, -1); upd(); });
    $('#dialBack').addEventListener('dblclick', () => { num = ''; upd(); });
    $('#dialCall').addEventListener('click', () => { if (num) startDial(num); });
    $('#dialAdd').addEventListener('click', async () => {
      if (!num) return;
      const name = prompt('שם איש הקשר:');
      if (name) { await addContact(name, num); toast('איש הקשר נשמר', 'green'); }
    });
    document.onkeydown = (e) => {
      if (State.view !== 'dialer') return;
      if (/^[0-9*#]$/.test(e.key)) { num += e.key; upd(); }
      else if (e.key === 'Backspace') { num = num.slice(0, -1); upd(); }
      else if (e.key === 'Enter' && num) startDial(num);
    };
  },

  contacts() {
    $('#cAdd').addEventListener('click', async () => {
      const name = $('#cName').value.trim(), num = $('#cNum').value.trim();
      if (!name || !num) return toast('נא למלא שם ומספר', 'red');
      await addContact(name, num);
      toast('איש הקשר נוסף', 'green');
      render('contacts');
    });
    $('#cSearch').addEventListener('input', (e) => {
      const q = e.target.value.trim();
      $$('#contactList .row').forEach((r) => {
        const hit = r.dataset.name.includes(q) || r.dataset.num.includes(q);
        r.style.display = hit ? '' : 'none';
      });
    });
    bindRowActions('#contactList', async (act, row) => {
      if (act === 'call') startDial(row.dataset.num);
      if (act === 'del') { await removeContact(row.dataset.num); render('contacts'); toast('נמחק'); }
    });
  },

  calllog() {
    const clr = $('#clearLog');
    if (clr) clr.addEventListener('click', async () => { await api.store.set('callLog', []); render('calllog'); toast('היומן נוקה'); });
    bindRowActions('.view-host', (act, row) => { if (act === 'call') startDial(row.dataset.num); });
  },

  voicemail() {
    bindRowActions('.view-host', async (act, el) => {
      if (act === 'callback') startDial(el.dataset.num);
      if (act === 'play') { toast('משמיע הודעה קולית...'); await markVmHeard(); }
    });
  },

  recordings() {
    $('#openRecFolder').addEventListener('click', () => api.sys.openRecordingsFolder());
    bindRowActions('.view-host', async (act, el) => {
      if (act === 'open') api.rec.open(el.dataset.path);
      if (act === 'del') { await api.rec.delete(el.dataset.path); render('recordings'); toast('ההקלטה נמחקה'); }
    });
  },

  async babysitter() {
    let running = false, timer = null;
    const level = $('#babyLevel'), val = $('#babyVal'), wave = $('#babyWave');
    const bars = $$('#babyWave i');
    const thresholdInput = $('#babyThreshold');
    const tick = () => {
      const noise = Math.round(20 + Math.random() * 70);
      val.textContent = noise;
      const deg = (noise / 100) * 360;
      const color = noise > (thresholdInput.value || 55) ? 'var(--danger)' : 'var(--accent)';
      level.style.background = `conic-gradient(${color} ${deg}deg, rgba(255,255,255,.06) ${deg}deg)`;
      bars.forEach((b) => b.style.height = (6 + Math.random() * 48) + 'px');
      if (noise > (thresholdInput.value || 55)) {
        toast('🔔 זוהה רעש חריג בחדר!', 'red');
      }
    };
    $('#babyStart').addEventListener('click', () => {
      running = true; $('#babyStart').hidden = true; $('#babyStop').hidden = false;
      timer = setInterval(tick, 900); toast('ניטור בייביסיטר הופעל', 'green');
    });
    $('#babyStop').addEventListener('click', () => {
      running = false; $('#babyStart').hidden = false; $('#babyStop').hidden = true;
      clearInterval(timer); val.textContent = '0';
      level.style.background = 'conic-gradient(var(--accent) 0deg, rgba(255,255,255,.06) 0deg)';
    });
    thresholdInput.addEventListener('change', () => api.store.set('settings.babysitterThreshold', +thresholdInput.value));
    $('#babyCallback').addEventListener('change', (e) => api.store.set('settings.babysitterCallback', e.target.checked));
  },

  devices() {
    refreshLog();
    $('#btnScan').addEventListener('click', async () => {
      $('#scanSpin').hidden = false;
      const list = await api.bt.scan();
      $('#scanSpin').hidden = true;
      renderDevices(list);
    });
  },

  settings() {
    $('#setAutoRec').addEventListener('change', (e) => { api.store.set('settings.autoRecord', e.target.checked); toast('נשמר', 'green'); });
    $('#setDeviceName').addEventListener('change', (e) => api.store.set('settings.deviceName', e.target.value));
    $('#setRing').addEventListener('change', (e) => api.store.set('settings.ringtone', e.target.value));
    $('#openRec2').addEventListener('click', () => api.sys.openRecordingsFolder());
  }
};

/* ==================== פעולות נתונים ==================== */
async function addContact(name, number) {
  const list = await api.store.get('contacts', []);
  list.push({ name, number, id: Date.now() });
  await api.store.set('contacts', list);
}
async function removeContact(number) {
  let list = await api.store.get('contacts', []);
  list = list.filter((c) => c.number !== number);
  await api.store.set('contacts', list);
}
async function markVmHeard() {
  const vm = await api.store.get('voicemail', []);
  vm.forEach((v) => v.heard = true);
  await api.store.set('voicemail', vm);
  updateVmBadge();
}

/* ==================== מסך שיחה ==================== */
function startDial(number) {
  if (State.status.state !== 'connected') { toast('אין חיבור לפלאפון — עברו למסך "חיבור בלוטוס"', 'red'); go('devices'); return; }
  api.call.dial(number);
}

function renderCall(call) {
  State.call = call;
  const overlay = $('#callOverlay');
  if (!call) {
    overlay.hidden = true;
    if (State.callTimer) { clearInterval(State.callTimer); State.callTimer = null; }
    if (State.view) render(State.view);
    return;
  }
  overlay.hidden = false;
  $('#callAvatar').textContent = initials(call.name);
  $('#callAvatar').className = 'call-avatar' + (call.state === 'ringing' ? ' ringing' : '');
  $('#callName').textContent = call.name || 'לא מזוהה';
  $('#callNumber').textContent = call.number;

  const stateText = { dialing: 'מחייג...', ringing: 'שיחה נכנסת', active: 'בשיחה' }[call.state] || '';
  $('#callState').textContent = stateText;

  const actions = $('#callActions');
  const timer = $('#callTimer');
  if (call.state === 'ringing' && call.direction === 'incoming') {
    actions.innerHTML = `<button class="ca-btn ca-hangup" id="caHangup">📵</button><button class="ca-btn ca-answer" id="caAnswer">📞</button>`;
    $('#caAnswer').onclick = () => api.call.answer();
    $('#caHangup').onclick = () => api.call.hangup();
    timer.hidden = true;
  } else {
    actions.innerHTML = `<button class="ca-btn ca-hangup" id="caHangup">📵</button>`;
    $('#caHangup').onclick = () => api.call.hangup();
  }

  if (call.state === 'active') {
    timer.hidden = false;
    if (!State.callTimer) {
      const started = call.connectedAt || Date.now();
      State.callTimer = setInterval(() => { timer.textContent = fmtDuration((Date.now() - started) / 1000); }, 500);
    }
  }

  $('#btnMute').onclick = async () => { const r = await api.call.toggleMute(); $('#btnMute').classList.toggle('active', r.muted); };
  $('#btnRec').onclick = async () => {
    if (State.status.recording) { await api.rec.stop(); $('#btnRec').classList.remove('active'); toast('הקלטה נעצרה'); }
    else { await api.rec.start(); $('#btnRec').classList.add('active'); toast('הקלטה החלה', 'red'); }
  };
}

/* ==================== חיבור / מכשירים / לוג ==================== */
let deviceCache = [];
function renderDevices(list) {
  deviceCache = list;
  const host = $('#deviceList');
  if (!host) return;
  host.innerHTML = list.map((d) => `
    <div class="row">
      <div class="avatar ${State.status.device && State.status.device.id === d.id ? 'green' : ''}">📱</div>
      <div class="row-main"><div class="name">${esc(d.name)}</div>
        <div class="meta" dir="ltr">${esc(d.id)} ${d.paired ? '· מותאם' : ''}</div></div>
      <div class="row-actions">
        ${State.status.device && State.status.device.id === d.id
          ? `<button class="btn red sm" data-disc>ניתוק</button>`
          : `<button class="btn green sm" data-conn="${esc(d.id)}">חיבור</button>`}
      </div>
    </div>`).join('');
  $$('[data-conn]', host).forEach((b) => b.addEventListener('click', () => api.bt.connect(b.dataset.conn)));
  $$('[data-disc]', host).forEach((b) => b.addEventListener('click', () => api.bt.disconnect()));
}

const logLines = [];
function refreshLog() {
  const box = $('#logBox');
  if (box) box.innerHTML = logLines.map((l) => `<div>${esc(l)}</div>`).join('');
  if (box) box.scrollTop = box.scrollHeight;
}

/* ==================== סטטוס גלובלי ==================== */
function applyStatus(s) {
  State.status = { ...State.status, ...s };
  const connected = s.state === 'connected';
  const dot = $('#connDot'), title = $('#connTitle'), sub = $('#connSub');
  dot.className = 'conn-dot ' + (connected ? 'on' : (s.state === 'connecting' || s.state === 'scanning' ? 'mid' : ''));
  title.textContent = connected ? (s.device ? s.device.name : 'מחובר') : (s.state === 'connecting' ? 'מתחבר...' : (s.state === 'scanning' ? 'סורק...' : 'לא מחובר'));
  sub.textContent = connected ? 'הפלאפון מחובר ✓' : 'לחצו כדי לחבר פלאפון';

  const modePill = $('#modePill');
  modePill.textContent = s.mode === 'hardware' ? 'חיבור חומרה' : 'מצב הדגמה';
  modePill.classList.toggle('hw', s.mode === 'hardware');

  const bp = $('#batteryPill');
  if (s.battery != null) { bp.hidden = false; $('#batteryVal').textContent = s.battery; } else bp.hidden = true;

  if (State.view === 'devices') renderDevices(deviceCache);
}

function updateVmBadge() {
  api.store.get('voicemail', []).then((vm) => {
    const n = vm.filter((v) => !v.heard).length;
    const b = $('#vmBadge');
    b.hidden = n === 0; b.textContent = n;
  });
}

/* ==================== שונות ==================== */
function beep() { /* מקום לצליל מקש עתידי */ }
window.go = (v) => switchView(v);

function bindRowActions(rootSel, handler) {
  const root = $(rootSel);
  if (!root) return;
  root.addEventListener('click', (e) => {
    const btn = e.target.closest('[data-act]');
    if (!btn) return;
    const row = btn.closest('.row');
    handler(btn.dataset.act, Object.assign(btn, { dataset: { ...row.dataset, ...btn.dataset } }));
  });
}

/* ==================== אתחול ==================== */
function boot() {
  $$('.nav-item').forEach((b) => b.addEventListener('click', () => switchView(b.dataset.view)));
  $('#connCard').addEventListener('click', () => switchView('devices'));

  api.on('bt:status', applyStatus);
  api.on('call:update', renderCall);
  api.on('call:incoming', (c) => toast(`📞 שיחה נכנסת מ-${c.name || c.number}`, 'green'));
  api.on('bt:battery', (b) => applyStatus({ battery: b }));
  api.on('bt:devices', (list) => { if (State.view === 'devices') renderDevices(list); });
  api.on('bt:log', (line) => { logLines.push(line); if (logLines.length > 100) logLines.shift(); refreshLog(); });

  api.bt.getStatus().then((s) => applyStatus(s));
  updateVmBadge();
  switchView('dashboard');
}

document.addEventListener('DOMContentLoaded', boot);
