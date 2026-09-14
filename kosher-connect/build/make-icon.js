'use strict';
// מחולל אייקון: יוצר PNG בגודל 256x256 עם רקע מדורג וסמל טלפון, ואורז ל-ICO.
const zlib = require('zlib');
const fs = require('fs');
const path = require('path');

const SIZE = 256;

// ---- CRC32 ----
const crcTable = (() => {
  const t = new Uint32Array(256);
  for (let n = 0; n < 256; n++) {
    let c = n;
    for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1;
    t[n] = c >>> 0;
  }
  return t;
})();
function crc32(buf) {
  let c = 0xffffffff;
  for (let i = 0; i < buf.length; i++) c = crcTable[(c ^ buf[i]) & 0xff] ^ (c >>> 8);
  return (c ^ 0xffffffff) >>> 0;
}
function chunk(type, data) {
  const len = Buffer.alloc(4); len.writeUInt32BE(data.length, 0);
  const typeBuf = Buffer.from(type, 'ascii');
  const crcBuf = Buffer.alloc(4);
  crcBuf.writeUInt32BE(crc32(Buffer.concat([typeBuf, data])), 0);
  return Buffer.concat([len, typeBuf, data, crcBuf]);
}

// ---- ציור לתוך מאגר RGBA ----
const px = Buffer.alloc(SIZE * SIZE * 4);
function set(x, y, r, g, b, a) {
  if (x < 0 || y < 0 || x >= SIZE || y >= SIZE) return;
  const i = (y * SIZE + x) * 4;
  const na = a / 255;
  px[i] = Math.round(px[i] * (1 - na) + r * na);
  px[i + 1] = Math.round(px[i + 1] * (1 - na) + g * na);
  px[i + 2] = Math.round(px[i + 2] * (1 - na) + b * na);
  px[i + 3] = Math.max(px[i + 3], a);
}
function lerp(a, b, t) { return a + (b - a) * t; }

// רקע: פינה מעוגלת עם מדרג טורקיז->כחול
const radius = 52;
for (let y = 0; y < SIZE; y++) {
  for (let x = 0; x < SIZE; x++) {
    // מסכת פינות מעוגלות
    let inside = true;
    const cx = Math.min(x, SIZE - 1 - x), cy = Math.min(y, SIZE - 1 - y);
    if (cx < radius && cy < radius) {
      const dx = radius - cx, dy = radius - cy;
      if (dx * dx + dy * dy > radius * radius) inside = false;
    }
    if (!inside) continue;
    const t = (x + y) / (2 * SIZE);
    const r = Math.round(lerp(34, 79, t));
    const g = Math.round(lerp(211, 130, t));
    const b = Math.round(lerp(238, 246, t));
    set(x, y, r, g, b, 255);
  }
}

// סמל טלפון (מקבילון פשוט) במרכז, בצבע כהה
function fillCircle(cx, cy, rad, r, g, b, a) {
  for (let y = cy - rad; y <= cy + rad; y++)
    for (let x = cx - rad; x <= cx + rad; x++) {
      const d = (x - cx) ** 2 + (y - cy) ** 2;
      if (d <= rad * rad) {
        const edge = rad * rad - d;
        const aa = edge < rad ? Math.min(a, (edge / rad) * a + 60) : a;
        set(x, y, r, g, b, Math.min(255, aa));
      }
    }
}
// גוף השפופרת: שתי אליפסות מחוברות באלכסון
const dark = [6, 18, 31];
for (let s = 0; s <= 120; s++) {
  const t = s / 120;
  const x = Math.round(lerp(78, 178, t));
  const y = Math.round(lerp(88, 168, t));
  fillCircle(x, y, 16, dark[0], dark[1], dark[2], 255);
}
fillCircle(80, 92, 30, dark[0], dark[1], dark[2], 255);
fillCircle(176, 164, 30, dark[0], dark[1], dark[2], 255);
// "חורים" בהירים בקצוות השפופרת
fillCircle(80, 92, 13, 34, 211, 238, 255);
fillCircle(176, 164, 13, 79, 130, 246, 255);

// ---- קידוד PNG ----
function encodePng() {
  const raw = Buffer.alloc((SIZE * 4 + 1) * SIZE);
  for (let y = 0; y < SIZE; y++) {
    raw[y * (SIZE * 4 + 1)] = 0; // filter type 0
    px.copy(raw, y * (SIZE * 4 + 1) + 1, y * SIZE * 4, (y + 1) * SIZE * 4);
  }
  const ihdr = Buffer.alloc(13);
  ihdr.writeUInt32BE(SIZE, 0); ihdr.writeUInt32BE(SIZE, 4);
  ihdr[8] = 8; ihdr[9] = 6; ihdr[10] = 0; ihdr[11] = 0; ihdr[12] = 0;
  const sig = Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]);
  return Buffer.concat([
    sig,
    chunk('IHDR', ihdr),
    chunk('IDAT', zlib.deflateSync(raw, { level: 9 })),
    chunk('IEND', Buffer.alloc(0))
  ]);
}

const png = encodePng();
fs.writeFileSync(path.join(__dirname, 'icon.png'), png);

// ---- אריזת ICO (PNG מוטמע) ----
const ico = Buffer.alloc(6 + 16 + png.length);
ico.writeUInt16LE(0, 0); ico.writeUInt16LE(1, 2); ico.writeUInt16LE(1, 4); // reserved, type=1, count=1
ico.writeUInt8(0, 6); ico.writeUInt8(0, 7); // 256 => 0
ico.writeUInt8(0, 8); ico.writeUInt8(0, 9);
ico.writeUInt16LE(1, 10); ico.writeUInt16LE(32, 12);
ico.writeUInt32LE(png.length, 14);
ico.writeUInt32LE(22, 18);
png.copy(ico, 22);
fs.writeFileSync(path.join(__dirname, 'icon.ico'), ico);

console.log('icon.png + icon.ico נוצרו בהצלחה (' + png.length + ' bytes)');
