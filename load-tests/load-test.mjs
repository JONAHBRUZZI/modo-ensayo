import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const BASE_URL = process.env.BASE_URL || 'http://localhost:8080';
const CONCURRENCY = parseInt(process.env.CONCURRENCY || '50', 10);
const ITERATIONS = parseInt(process.env.ITERATIONS || '5', 10);

const csvPath = path.join(__dirname, '..', 'jmeter', 'test-users.csv');
const users = fs.readFileSync(csvPath, 'utf-8')
  .split('\n')
  .slice(1)
  .filter(line => line.trim())
  .map(line => {
    const [email, password] = line.split(',');
    return { email: email.trim(), password: password.trim() };
  });

const results = [];

function record(label, success, elapsed) {
  results.push({ label, success, elapsed });
}

async function request(method, url, body = null, headers = {}) {
  const res = await fetch(url, {
    method: method.toUpperCase(),
    headers: {
      'Content-Type': 'application/json',
      Accept: 'application/json',
      ...headers,
    },
    body: body ? JSON.stringify(body) : null,
  });
  const text = await res.text();
  let data = null;
  try {
    data = text ? JSON.parse(text) : null;
  } catch {
    data = text;
  }
  return { status: res.status, data };
}

async function runFlow(user) {
  const startLogin = Date.now();
  let token = null;
  try {
    const loginRes = await request('post', `${BASE_URL}/api/auth/login`, {
      email: user.email,
      password: user.password,
    });
    const loginElapsed = Date.now() - startLogin;
    const loginOk = loginRes.status === 200 && loginRes.data?.token;
    record('01 - Login', loginOk, loginElapsed);
    if (!loginOk) {
      throw new Error(`Login failed: ${loginRes.status} ${JSON.stringify(loginRes.data)}`);
    }
    token = loginRes.data.token;
  } catch (err) {
    if (results.filter(r => r.label === '01 - Login' && !r.success).length < 3) {
      console.error('Login error:', err.message);
    }
    record('01 - Login', false, Date.now() - startLogin);
    throw err;
  }

  const authHeaders = { Authorization: `Bearer ${token}` };

  const requests = [
    { label: '02 - Listar Clases', method: 'get', url: `${BASE_URL}/api/classes` },
    { label: '03 - Listar Sedes', method: 'get', url: `${BASE_URL}/api/venues` },
    { label: '04 - Mi Perfil', method: 'get', url: `${BASE_URL}/api/users/me` },
    { label: '05 - Mi Carrito', method: 'get', url: `${BASE_URL}/api/payments/cart` },
  ];

  for (const req of requests) {
    const start = Date.now();
    try {
      const res = await request(req.method, req.url, null, authHeaders);
      const ok = res.status >= 200 && res.status < 300;
      record(req.label, ok, Date.now() - start);
      if (!ok) {
        throw new Error(`${req.label} failed: ${res.status}`);
      }
    } catch (err) {
      record(req.label, false, Date.now() - start);
      throw err;
    }
  }
}

async function worker(id) {
  const user = users[id % users.length];
  for (let i = 0; i < ITERATIONS; i++) {
    try {
      await runFlow(user);
    } catch {
      // errors already recorded
    }
  }
}

async function main() {
  console.log(`Load test: ${CONCURRENCY} concurrent users, ${ITERATIONS} iterations each, base URL ${BASE_URL}`);
  const start = Date.now();
  const workers = [];
  for (let i = 0; i < CONCURRENCY; i++) {
    workers.push(worker(i));
  }
  await Promise.all(workers);
  const totalElapsed = Date.now() - start;

  const grouped = {};
  for (const r of results) {
    if (!grouped[r.label]) grouped[r.label] = [];
    grouped[r.label].push(r);
  }

  console.log('\n=== RESULTADOS ===');
  console.log(`Total requests: ${results.length}`);
  console.log(`Total errors: ${results.filter(r => !r.success).length}`);
  console.log(`Total duration: ${(totalElapsed / 1000).toFixed(2)}s`);
  console.log(`Throughput: ${(results.length / (totalElapsed / 1000)).toFixed(2)} req/s`);
  console.log('\nBy endpoint:');
  for (const [label, data] of Object.entries(grouped)) {
    const count = data.length;
    const errors = data.filter(r => !r.success).length;
    const times = data.map(r => r.elapsed);
    const avg = times.reduce((a, b) => a + b, 0) / count;
    const min = Math.min(...times);
    const max = Math.max(...times);
    const sorted = [...times].sort((a, b) => a - b);
    const p95 = sorted[Math.floor(sorted.length * 0.95)];
    console.log(
      `${label.padEnd(22)} | count=${count.toString().padStart(4)} err=${errors.toString().padStart(4)} ` +
      `avg=${avg.toFixed(1).padStart(6)}ms min=${min.toString().padStart(4)}ms max=${max.toString().padStart(4)}ms p95=${p95}ms`
    );
  }
}

main().catch(console.error);
