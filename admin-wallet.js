#!/usr/bin/env node
/**
 * 钱包管理 CLI（调用 wallet-server.js 的 HTTP 管理接口，无需密码、零依赖）
 *
 * 前置：先启动服务  node wallet-server.js
 *
 * 交互模式（推荐）：
 *   node admin-wallet.js            直接运行，按数字选择操作
 *
 * 命令模式（用于脚本化调用）：
 *   node admin-wallet.js list                        列出所有用户钱包
 *   node admin-wallet.js query  <设备ID>             查询某设备
 *   node admin-wallet.js add    <设备ID> <金额>      充值
 *   node admin-wallet.js deduct <设备ID> <金额>      罚没
 *   node admin-wallet.js ban    <设备ID>             封禁账户
 *   node admin-wallet.js unban  <设备ID>             解禁账户
 *
 * 可选参数：
 *   --server http://127.0.0.1:5010   指定服务地址（默认本机 5010）
 *   --json                           以 JSON 输出（仅命令模式）
 */

const readline = require('readline');

const SERVER = getArg('--server') || 'http://127.0.0.1:5010';
const AS_JSON = process.argv.includes('--json');

function getArg(name) {
  const i = process.argv.indexOf(name);
  return i >= 0 ? process.argv[i + 1] : null;
}

async function api(method, path, body) {
  let res;
  try {
    res = await fetch(SERVER + path, {
      method,
      headers: { 'Content-Type': 'application/json' },
      body: body ? JSON.stringify(body) : undefined
    });
  } catch (e) {
    console.error(`无法连接钱包服务 ${SERVER} —— 请先运行: node wallet-server.js`);
    process.exit(1);
  }
  return res.json();
}

function fmtWallet(w) {
  const time = (t) => new Date(t).toLocaleString();
  return [
    `设备ID : ${w.deviceId}`,
    `余额   : ¥${Number(w.balance).toFixed(2)}`,
    `状态   : ${w.isBanned ? '已封禁 ⛔' : '正常 ✅'}`,
    `创建于 : ${time(w.createdAt)}`,
    `更新于 : ${time(w.updatedAt)}`
  ].join('\n');
}

function out(data, human) {
  console.log(AS_JSON ? JSON.stringify(data, null, 2) : human);
}

// ============================================================
// 业务操作（交互模式 / 命令模式共用）
// ============================================================

async function doList() {
  const r = await api('GET', '/admin/wallets');
  if (!r.ok) return fail(r);
  const list = r.wallets || [];
  out(r, list.length === 0
    ? '（暂无钱包记录）'
    : list.map(fmtWallet).join('\n' + '-'.repeat(40) + '\n') + `\n\n共 ${list.length} 个钱包`);
  return list;
}

async function doQuery(deviceId) {
  const r = await api('GET', `/api/wallet/${encodeURIComponent(deviceId)}`);
  if (!r.ok) return fail(r);
  out(r, fmtWallet(r));
}

async function doAmountOp(cmd, deviceId, amount) {
  const r = await api('POST', `/admin/wallet/${encodeURIComponent(deviceId)}/${cmd}`, { amount });
  if (!r.ok) return fail(r);
  out(r, `${cmd === 'add' ? '充值' : '罚没'} ¥${amount.toFixed(2)} 成功\n\n` + fmtWallet(r));
}

async function doBanOp(cmd, deviceId) {
  const r = await api('POST', `/admin/wallet/${encodeURIComponent(deviceId)}/${cmd}`);
  if (!r.ok) return fail(r);
  out(r, `${cmd === 'ban' ? '封禁' : '解禁'}成功\n\n` + fmtWallet(r));
}

// ============================================================
// 交互模式
// ============================================================

let rl = null;
let rlClosed = false;
const lineQueue = [];
let lineWaiters = [];

function setupReadline() {
  rl = readline.createInterface({ input: process.stdin, output: process.stdout });
  rl.on('line', (l) => {
    const w = lineWaiters.shift();
    if (w) w(l);
    else lineQueue.push(l);
  });
  rl.on('close', () => {
    rlClosed = true;
    lineWaiters.forEach((w) => w(null)); // EOF：唤醒所有等待者
    lineWaiters = [];
  });
}

/** 读取一行输入；返回 null 表示输入流已结束（Ctrl+C / EOF） */
function ask(question) {
  process.stdout.write(question);
  if (lineQueue.length > 0) {
    const l = lineQueue.shift();
    console.log(l); // 回显，保持输出可读
    return Promise.resolve(l.trim());
  }
  if (rlClosed) return Promise.resolve(null);
  return new Promise((resolve) => {
    lineWaiters.push((l) => resolve(l === null ? null : l.trim()));
  });
}

/**
 * 选择设备：拉取钱包列表，展示编号让用户选；也允许直接输入设备ID
 */
async function pickDevice() {
  const r = await api('GET', '/admin/wallets');
  const list = (r.ok && r.wallets) || [];

  if (list.length === 0) {
    const id = await ask('当前没有钱包记录，请直接输入设备ID（回车取消）: ');
    return id || null;
  }

  console.log('\n  请选择设备：');
  list.forEach((w, i) => {
    console.log(`  [${i + 1}] ${w.deviceId}  余额 ¥${Number(w.balance).toFixed(2)}  ${w.isBanned ? '⛔已封禁' : '✅正常'}`);
  });
  const input = await ask(`\n输入编号 1-${list.length}，或直接粘贴设备ID（回车取消）: `);
  if (!input) return null;

  const n = Number(input);
  if (Number.isInteger(n) && n >= 1 && n <= list.length) {
    return list[n - 1].deviceId;
  }
  return input; // 视为直接输入的设备ID
}

async function askAmount(label) {
  while (true) {
    const s = await ask(`请输入${label}金额（元，回车取消）: `);
    if (!s) return null;
    const amount = Number(s);
    if (amount > 0) return amount;
    console.log('  ⚠ 金额必须为正数，请重新输入');
  }
}

const MENU = `
========== 钱包管理 (${SERVER}) ==========
  [1] 列出所有用户钱包
  [2] 查询某设备
  [3] 充值
  [4] 罚没
  [5] 封禁账户
  [6] 解禁账户
  [7] 退出 (或 q)
==========================================`;

async function interactive() {
  setupReadline();

  // 启动时先探测服务是否可达
  await api('GET', '/admin/wallets');
  console.log(`已连接钱包服务: ${SERVER}`);

  while (true) {
    console.log(MENU);
    const choice = await ask('请选择操作 [1-7]: ');
    if (choice === null) { console.log('\n再见！'); return; } // 输入流结束

    try {
      switch (choice) {
        case '1':
          console.log('');
          await doList();
          break;
        case '2': {
          const id = await pickDevice();
          if (!id) break;
          console.log('');
          await doQuery(id);
          break;
        }
        case '3':
        case '4': {
          const cmd = choice === '3' ? 'add' : 'deduct';
          const id = await pickDevice();
          if (!id) break;
          const amount = await askAmount(cmd === 'add' ? '充值' : '罚没');
          if (amount == null) break;
          console.log('');
          await doAmountOp(cmd, id, amount);
          break;
        }
        case '5':
        case '6': {
          const cmd = choice === '5' ? 'ban' : 'unban';
          const id = await pickDevice();
          if (!id) break;
          if (cmd === 'ban') {
            const yes = await ask(`确认封禁 ${id} ？(y/N): `);
            if (!yes || yes.toLowerCase() !== 'y') { console.log('已取消'); break; }
          }
          console.log('');
          await doBanOp(cmd, id);
          break;
        }
        case '7':
        case 'q':
        case 'Q':
        case 'exit':
        case 'quit':
          console.log('再见！');
          rl.close();
          return;
        default:
          console.log('  ⚠ 无效选项，请输入 1-7');
      }
    } catch (e) {
      // fail() 在交互模式下抛出而不是退出进程
      console.error(`  ✗ ${e.message}`);
    }
  }
}

// ============================================================
// 命令模式（保持原有用法，便于脚本化）
// ============================================================

async function commandMode(cmd, deviceId, amountStr) {
  switch (cmd) {
    case 'list':
      await doList();
      break;
    case 'query':
      requireId(deviceId);
      await doQuery(deviceId);
      break;
    case 'add':
    case 'deduct': {
      requireId(deviceId);
      const amount = Number(amountStr);
      if (!(amount > 0)) die('金额必须为正数，例如: node admin-wallet.js add <设备ID> 10');
      await doAmountOp(cmd, deviceId, amount);
      break;
    }
    case 'ban':
    case 'unban':
      requireId(deviceId);
      await doBanOp(cmd, deviceId);
      break;
    case 'help':
    case '--help':
    case '-h':
      printUsage();
      break;
    default:
      console.error(`未知命令: ${cmd}\n`);
      printUsage();
      process.exit(1);
  }
}

function printUsage() {
  console.log(`用法:
  node admin-wallet.js                             交互模式（数字菜单）
  node admin-wallet.js list                        列出所有用户钱包
  node admin-wallet.js query  <设备ID>             查询某设备
  node admin-wallet.js add    <设备ID> <金额>      充值
  node admin-wallet.js deduct <设备ID> <金额>      罚没
  node admin-wallet.js ban    <设备ID>             封禁账户
  node admin-wallet.js unban  <设备ID>             解禁账户

可选: --server http://127.0.0.1:5010   --json`);
}

function requireId(id) {
  if (!id) die('缺少设备ID 参数');
}

function die(msg) {
  console.error(msg);
  process.exit(1);
}

let INTERACTIVE = false;

function fail(r) {
  const msg = `操作失败: ${r.message || JSON.stringify(r)}`;
  if (INTERACTIVE) throw new Error(msg);
  console.error(msg);
  process.exit(1);
}

async function main() {
  // 过滤掉 --server <url> / --json，剩下的才是命令参数
  const args = [];
  const argv = process.argv.slice(2);
  for (let i = 0; i < argv.length; i++) {
    if (argv[i] === '--server') { i++; continue; }
    if (argv[i] === '--json') continue;
    args.push(argv[i]);
  }

  if (args.length === 0) {
    INTERACTIVE = true;
    await interactive();
  } else {
    await commandMode(args[0], args[1], args[2]);
  }
}

main();
