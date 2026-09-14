'use strict';

const { contextBridge, ipcRenderer } = require('electron');

// גשר מאובטח בין תהליך המסך (renderer) לבין תהליך הראשי (main).
// אין חשיפה של Node ישירות למסך — רק פונקציות מוגדרות מראש.
contextBridge.exposeInMainWorld('api', {
  // בלוטוס
  bt: {
    scan: () => ipcRenderer.invoke('bt:scan'),
    connect: (id) => ipcRenderer.invoke('bt:connect', id),
    disconnect: () => ipcRenderer.invoke('bt:disconnect'),
    getStatus: () => ipcRenderer.invoke('bt:getStatus')
  },
  // שיחות
  call: {
    dial: (number) => ipcRenderer.invoke('call:dial', number),
    answer: () => ipcRenderer.invoke('call:answer'),
    hangup: () => ipcRenderer.invoke('call:hangup'),
    sendDtmf: (d) => ipcRenderer.invoke('call:sendDtmf', d),
    toggleMute: () => ipcRenderer.invoke('call:toggleMute')
  },
  // הקלטות
  rec: {
    start: () => ipcRenderer.invoke('rec:start'),
    stop: () => ipcRenderer.invoke('rec:stop'),
    list: () => ipcRenderer.invoke('rec:list'),
    open: (p) => ipcRenderer.invoke('rec:open', p),
    delete: (p) => ipcRenderer.invoke('rec:delete', p)
  },
  // אחסון
  store: {
    get: (key, def) => ipcRenderer.invoke('store:get', key, def),
    set: (key, val) => ipcRenderer.invoke('store:set', key, val)
  },
  // מערכת
  sys: {
    info: () => ipcRenderer.invoke('sys:info'),
    openRecordingsFolder: () => ipcRenderer.invoke('sys:openRecordingsFolder')
  },
  // האזנה לאירועים מהתהליך הראשי
  on: (channel, cb) => {
    const valid = [
      'bt:status', 'bt:devices', 'bt:log', 'bt:battery',
      'call:update', 'call:incoming'
    ];
    if (valid.includes(channel)) {
      const listener = (_e, data) => cb(data);
      ipcRenderer.on(channel, listener);
      return () => ipcRenderer.removeListener(channel, listener);
    }
  }
});
