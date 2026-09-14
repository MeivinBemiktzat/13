'use strict';

const { app, BrowserWindow, ipcMain, Menu, shell, dialog } = require('electron');
const path = require('path');
const fs = require('fs');
const os = require('os');

const BluetoothService = require('./bluetooth-service');
const Store = require('./store');

const isDev = process.argv.includes('--dev');

let mainWindow = null;
let bluetooth = null;
let store = null;

// ---------- נתיבי אחסון ----------
function userDataDir() {
  const dir = path.join(app.getPath('userData'), 'data');
  if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true });
  return dir;
}

function recordingsDir() {
  const dir = path.join(app.getPath('userData'), 'recordings');
  if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true });
  return dir;
}

// ---------- יצירת החלון הראשי ----------
function createWindow() {
  mainWindow = new BrowserWindow({
    width: 1180,
    height: 760,
    minWidth: 940,
    minHeight: 620,
    backgroundColor: '#0d1117',
    show: false,
    autoHideMenuBar: true,
    title: 'KosherConnect',
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      contextIsolation: true,
      nodeIntegration: false,
      sandbox: false
    }
  });

  mainWindow.loadFile(path.join(__dirname, '..', 'renderer', 'index.html'));

  mainWindow.once('ready-to-show', () => {
    mainWindow.show();
    if (isDev) mainWindow.webContents.openDevTools({ mode: 'detach' });
  });

  mainWindow.webContents.setWindowOpenHandler(({ url }) => {
    shell.openExternal(url);
    return { action: 'deny' };
  });

  mainWindow.on('closed', () => {
    mainWindow = null;
  });

  Menu.setApplicationMenu(null);
}

// ---------- שידור אירועים למסך ----------
function emit(channel, payload) {
  if (mainWindow && !mainWindow.isDestroyed()) {
    mainWindow.webContents.send(channel, payload);
  }
}

// ---------- אתחול השירותים ----------
function initServices() {
  store = new Store(userDataDir());

  bluetooth = new BluetoothService({
    recordingsDir: recordingsDir(),
    store
  });

  // חיבור אירועי הבלוטוס אל המסך
  bluetooth.on('status', (s) => emit('bt:status', s));
  bluetooth.on('devices', (d) => emit('bt:devices', d));
  bluetooth.on('call', (c) => emit('call:update', c));
  bluetooth.on('incoming', (c) => emit('call:incoming', c));
  bluetooth.on('log', (m) => emit('bt:log', m));
  bluetooth.on('battery', (b) => emit('bt:battery', b));
}

// ================= IPC =================
function registerIpc() {
  // --- בלוטוס ---
  ipcMain.handle('bt:scan', async () => bluetooth.scan());
  ipcMain.handle('bt:connect', async (_e, id) => bluetooth.connect(id));
  ipcMain.handle('bt:disconnect', async () => bluetooth.disconnect());
  ipcMain.handle('bt:getStatus', async () => bluetooth.getStatus());

  // --- שיחות ---
  ipcMain.handle('call:dial', async (_e, number) => bluetooth.dial(number));
  ipcMain.handle('call:answer', async () => bluetooth.answer());
  ipcMain.handle('call:hangup', async () => bluetooth.hangup());
  ipcMain.handle('call:sendDtmf', async (_e, digit) => bluetooth.sendDtmf(digit));
  ipcMain.handle('call:toggleMute', async () => bluetooth.toggleMute());

  // --- הקלטות ---
  ipcMain.handle('rec:start', async () => bluetooth.startRecording());
  ipcMain.handle('rec:stop', async () => bluetooth.stopRecording());
  ipcMain.handle('rec:list', async () => {
    const dir = recordingsDir();
    const files = fs.existsSync(dir) ? fs.readdirSync(dir) : [];
    return files
      .filter((f) => /\.(wav|webm|mp3)$/i.test(f))
      .map((f) => {
        const st = fs.statSync(path.join(dir, f));
        return { name: f, size: st.size, mtime: st.mtimeMs, path: path.join(dir, f) };
      })
      .sort((a, b) => b.mtime - a.mtime);
  });
  ipcMain.handle('rec:open', async (_e, p) => {
    shell.showItemInFolder(p);
    return true;
  });
  ipcMain.handle('rec:delete', async (_e, p) => {
    try { fs.unlinkSync(p); return true; } catch (e) { return false; }
  });

  // --- אחסון כללי (אנשי קשר, יומן, תא קולי, הגדרות) ---
  ipcMain.handle('store:get', async (_e, key, def) => store.get(key, def));
  ipcMain.handle('store:set', async (_e, key, val) => { store.set(key, val); return true; });

  // --- מידע מערכת ---
  ipcMain.handle('sys:info', async () => ({
    version: app.getVersion(),
    platform: process.platform,
    hostname: os.hostname(),
    userData: app.getPath('userData'),
    recordings: recordingsDir()
  }));

  ipcMain.handle('sys:openRecordingsFolder', async () => {
    shell.openPath(recordingsDir());
    return true;
  });
}

// ================= מחזור חיים =================
app.whenReady().then(() => {
  initServices();
  registerIpc();
  createWindow();

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) createWindow();
  });
});

app.on('window-all-closed', () => {
  if (bluetooth) bluetooth.shutdown();
  if (process.platform !== 'darwin') app.quit();
});

process.on('uncaughtException', (err) => {
  // לא מפילים את התוכנה — רק מתעדים
  try { emit('bt:log', 'שגיאה: ' + err.message); } catch (_) {}
});
