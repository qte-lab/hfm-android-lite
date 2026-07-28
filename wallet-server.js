#!/usr/bin/env node
/**
 * 钱包/扣费后端服务（零依赖，Node.js >= 16）
 *
 * 启动：node wallet-server.js
 * 监听：0.0.0.0:5010（局域网内 App 可直接访问）
 * 存储：同目录下 wallet-data.json（无需数据库、无需密码）
 *
 * ===== App 接口 =====
 *   GET  /api/wallet/:deviceId          查询钱包（不存在则自动创建，余额 0）
 *   POST /api/wallet/:deviceId/charge   扣除一次识别费用 ¥0.1
 *        成功: { ok:true, balance:2.9 }
 *        失败: { ok:false, code:"INSUFFICIENT"|"BANNED", balance:0, message:"..." }
 *
 * ===== 管理接口（同样无密码，仅限局域网使用）=====
 *   GET  /admin/wallets                       列出所有钱包
 *   POST /admin/wallet/:deviceId/add          body: {"amount":10}   充值
 *   POST /admin/wallet/:deviceId/deduct       body: {"amount":5}    罚没
 *   POST /admin/wallet/:deviceId/ban          封禁
 *   POST /admin/wallet/:deviceId/unban        解禁
 */

const http = require('http');
const fs = require('fs');
const path = require('path');

const PORT = 5010;
const HOST = '0.0.0.0';
const DATA_FILE = path.join(__dirname, 'wallet-data.json');
const RECOGNITION_COST = 0.1;

// ---------- 存储 ----------
let wallets = {};

function load() {
  try {
    wallets = JSON.parse(fs.readFileSync(DATA_FILE, 'utf8'));
  } catch (e) {
    wallets = {};
  }
}

function save() {
  const tmp = DATA_FILE + '.tmp';
  fs.writeFileSync(tmp, JSON.stringify(wallets, null, 2), 'utf8');
  fs.renameSync(tmp, DATA_FILE);
}

function getOrCreate(deviceId) {
  if (!wallets[deviceId]) {
    const now = Date.now();
    wallets[deviceId] = {
      deviceId,
      balance: 0,
      isBanned: false,
      createdAt: now,
      updatedAt: now
    };
    save();
    console.log(`[wallet] 新建钱包: ${deviceId}`);
  }
  return wallets[deviceId];
}

// 金额统一保留 2 位小数，避免浮点误差累积
function round2(n) {
  return Math.round(n * 100) / 100;
}

// ---------- HTTP ----------
function json(res, status, obj) {
  const body = JSON.stringify(obj);
  res.writeHead(status, {
    'Content-Type': 'application/json; charset=utf-8',
    'Content-Length': Buffer.byteLength(body)
  });
  res.end(body);
}

function readBody(req) {
  return new Promise((resolve) => {
    let data = '';
    req.on('data', (c) => (data += c));
    req.on('end', () => {
      try {
        resolve(data ? JSON.parse(data) : {});
      } catch {
        resolve({});
      }
    });
  });
}

const server = http.createServer(async (req, res) => {
  const url = new URL(req.url, `http://${req.headers.host}`);
  const parts = url.pathname.split('/').filter(Boolean); // 例: ['api','wallet','xxx','charge']
  console.log(`[${new Date().toLocaleString()}] ${req.method} ${url.pathname}`);

  try {
    // ---- App: GET /api/wallet/:deviceId ----
    if (req.method === 'GET' && parts[0] === 'api' && parts[1] === 'wallet' && parts.length === 3) {
      const w = getOrCreate(decodeURIComponent(parts[2]));
      return json(res, 200, { ok: true, ...w });
    }

    // ---- App: POST /api/wallet/:deviceId/charge ----
    if (req.method === 'POST' && parts[0] === 'api' && parts[1] === 'wallet' && parts[3] === 'charge') {
      const w = getOrCreate(decodeURIComponent(parts[2]));
      if (w.isBanned) {
        return json(res, 200, { ok: false, code: 'BANNED', balance: w.balance, message: '账户已被封禁，无法使用 AI 识别功能' });
      }
      if (w.balance < RECOGNITION_COST) {
        return json(res, 200, {
          ok: false, code: 'INSUFFICIENT', balance: w.balance,
          message: `余额不足（当前 ¥${w.balance.toFixed(2)}，需要 ¥${RECOGNITION_COST.toFixed(2)}）`
        });
      }
      w.balance = round2(w.balance - RECOGNITION_COST);
      w.updatedAt = Date.now();
      save();
      console.log(`[wallet] 扣费 ¥${RECOGNITION_COST} <- ${w.deviceId}，余额 ¥${w.balance}`);
      return json(res, 200, { ok: true, balance: w.balance });
    }

    // ---- Admin: GET /admin/wallets ----
    if (req.method === 'GET' && parts[0] === 'admin' && parts[1] === 'wallets') {
      return json(res, 200, { ok: true, wallets: Object.values(wallets) });
    }

    // ---- Admin: POST /admin/wallet/:deviceId/(add|deduct|ban|unban) ----
    if (req.method === 'POST' && parts[0] === 'admin' && parts[1] === 'wallet' && parts.length === 4) {
      const deviceId = decodeURIComponent(parts[2]);
      const action = parts[3];
      const body = await readBody(req);

      if (action === 'add') {
        const amount = Number(body.amount);
        if (!(amount > 0)) return json(res, 400, { ok: false, message: 'amount 必须为正数' });
        const w = getOrCreate(deviceId);
        w.balance = round2(w.balance + amount);
        w.updatedAt = Date.now();
        save();
        console.log(`[admin] 充值 ¥${amount} -> ${deviceId}，余额 ¥${w.balance}`);
        return json(res, 200, { ok: true, ...w });
      }

      if (action === 'deduct') {
        const amount = Number(body.amount);
        if (!(amount > 0)) return json(res, 400, { ok: false, message: 'amount 必须为正数' });
        const w = wallets[deviceId];
        if (!w) return json(res, 404, { ok: false, message: '钱包不存在' });
        w.balance = round2(Math.max(0, w.balance - amount));
        w.updatedAt = Date.now();
        save();
        console.log(`[admin] 罚没 ¥${amount} <- ${deviceId}，余额 ¥${w.balance}`);
        return json(res, 200, { ok: true, ...w });
      }

      if (action === 'ban' || action === 'unban') {
        const w = wallets[deviceId];
        if (!w) return json(res, 404, { ok: false, message: '钱包不存在' });
        w.isBanned = action === 'ban';
        w.updatedAt = Date.now();
        save();
        console.log(`[admin] ${action === 'ban' ? '封禁' : '解禁'} ${deviceId}`);
        return json(res, 200, { ok: true, ...w });
      }
    }

    json(res, 404, { ok: false, message: 'Not Found' });
  } catch (e) {
    console.error('[error]', e);
    json(res, 500, { ok: false, message: e.message });
  }
});

load();
server.listen(PORT, HOST, () => {
  console.log('==============================================');
  console.log(`  钱包服务已启动: http://${HOST}:${PORT}`);
  console.log(`  数据文件: ${DATA_FILE}`);
  console.log(`  每次 AI 识别扣费: ¥${RECOGNITION_COST}`);
  console.log('==============================================');
});
