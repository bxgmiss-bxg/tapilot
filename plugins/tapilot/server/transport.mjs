import tls from 'node:tls';
import { createHash, randomUUID, timingSafeEqual } from 'node:crypto';
import { networkInterfaces } from 'node:os';
import { discoverPhones } from './discovery.mjs';
import { findPairedPhone, matchesPhone } from './lan-discovery.mjs';

// A VPN may route the phone's LAN range into its tunnel. Bind the source to a
// physical interface when unambiguous, preserving the pinned TLS identity.
export function localAddressForPhone(host, interfaces = networkInterfaces()) {
  if (host.startsWith('127.')) return undefined;
  const addresses = Object.entries(interfaces)
    .filter(([name]) => /^(en\d+|eth\d+|wlan\d+|wlp\w+)$/.test(name))
    .flatMap(([, entries]) => (entries ?? []).filter(a => a.family === 'IPv4' && !a.internal));
  const numeric = ip => ip.split('.').reduce((value, part) => (value << 8) | Number(part), 0);
  const sameSubnet = addresses.filter(a => (numeric(a.address) & numeric(a.netmask)) === (numeric(host) & numeric(a.netmask)));
  const candidates = sameSubnet.length ? sameSubnet : addresses;
  return candidates.length === 1 ? candidates[0].address : undefined;
}

export function physicalNetworkKey(interfaces = networkInterfaces()) {
  return JSON.stringify(Object.entries(interfaces)
    .filter(([name]) => /^(en\d+|eth\d+|wlan\d+|wlp\w+)$/.test(name))
    .flatMap(([name, entries]) => (entries ?? []).filter(a => a.family === 'IPv4' && !a.internal)
      .map(a => `${name}:${a.address}/${a.netmask}`)).sort());
}

const MAX_RESPONSE = 32 * 1024 * 1024;
export class PhoneConnection {
  #socket;
  #pending = new Map();
  #buffer = Buffer.alloc(0);
  #opening;
  #authenticated = false;
  #abortConnect;
  #networkKey;
  constructor(config, {networkState, discover = discoverPhones, find = findPairedPhone} = {}) {
    this.config = config;
    this.networkState = networkState ?? (() => this.config.host.startsWith('127.') ? 'loopback' : physicalNetworkKey());
    this.discover = discover; this.find = find;
  }
  #networkChanged() { return this.#networkKey !== undefined && this.networkState() !== this.#networkKey; }
  get connected() { return this.#authenticated && !!this.#socket && !this.#socket.destroyed && !this.#networkChanged(); }
  async open(pair = false, rediscover = true) {
    if (this.#networkChanged()) this.close(new Error('Network changed. Reconnect to the phone at its current address.'));
    if (this.#opening) return this.#opening;
    if (this.connected) return;
    const opening = this.#open(pair, rediscover);
    this.#opening = opening;
    try { return await opening; }
    finally { if (this.#opening === opening) this.#opening = undefined; }
  }
  async #open(pair, rediscover) {
    this.#networkKey = this.networkState();
    const socket = tls.connect({ host: this.config.host, port: this.config.port, localAddress: localAddressForPhone(this.config.host), minVersion: 'TLSv1.2', maxVersion: 'TLSv1.2', rejectUnauthorized: false });
    socket.setKeepAlive(true, 10000);
    socket.setNoDelay(true);
    this.#socket = socket;
    this.#buffer = Buffer.alloc(0);
    socket.on('data', data => { if (this.#socket === socket) this.#receive(data); });
    socket.on('error', () => { if (this.#socket === socket) this.close(new Error('Phone connection failed. Check Wi-Fi and enable wireless control on the phone.')); });
    socket.on('close', () => { if (this.#socket === socket) this.close(new Error('Phone disconnected. Action outcome may be unknown; inspect the screen before retrying.')); });
    let authenticatedTransport = false;
    try {
      await new Promise((resolve, reject) => {
        const finish = error => {
          clearTimeout(timer); this.#abortConnect = undefined;
          if (error) reject(error); else resolve();
        };
        const timer = setTimeout(() => { finish(new Error('Phone connection timed out')); socket.destroy(); }, 5000);
        this.#abortConnect = finish;
        socket.once('secureConnect', () => {
          const cert = socket.getPeerCertificate().raw;
          const actual = cert && createHash('sha256').update(cert).digest();
          const expected = Buffer.from(this.config.fingerprint, 'hex');
          if (!actual || actual.length !== expected.length || !timingSafeEqual(actual, expected)) {
            finish(new Error('Phone certificate changed. Pair again on the phone; no credential was sent.'));
            socket.destroy(); return;
          }
          finish();
        });
      });
      authenticatedTransport = true;
      const result = await this.#request(pair ? 'pair' : 'authenticate', {}, this.config.token);
      if (this.#socket !== socket || socket.destroyed) throw new Error('Phone connection closed');
      this.#authenticated = true;
      this.features = result?.features ?? {};
      return result;
    } catch (error) {
      // Only pre-authentication connection failures are safe to confirm again.
      if (!authenticatedTransport && /^(Phone connection timed out|Unable to connect to phone|Phone connection failed\.)/.test(error.message))
        error.code = 'phone_unreachable';
      this.close(error);
      if (!pair && rediscover && !this.config.host.startsWith('127.') && /Unable to connect|connection timed out|connection failed|certificate changed/i.test(error.message)) {
        const previous = this.config;
        const seen = new Set([`${previous.host}:${previous.port}`]);
        // Try every advertised endpoint: one stale mDNS answer must not hide a
        // reachable address on another interface. Fall back only after those fail.
        for (const discover of [
          async () => (await Promise.all((previous.endpoints ?? []).slice(0, 8).map(async target =>
            !seen.has(`${target.host}:${target.port}`) && await matchesPhone(
              {host:target.host,localAddress:localAddressForPhone(target.host)},previous.fingerprint,target.port,1000,
            ) ? target : null))).filter(Boolean),
          () => this.discover({fingerprint:previous.fingerprint,timeoutMs:2000}),
          () => this.find({fingerprint:previous.fingerprint,port:previous.port}),
        ]) {
          const candidates = await discover();
          for (const target of candidates) {
            const key = `${target.host}:${target.port}`;
            if (seen.has(key)) continue;
            seen.add(key);
            this.config = {...previous,host:target.host,port:target.port};
            try {
              const result = await this.#open(false,false);
              const {saveConfig} = await import('./config.mjs'); await saveConfig(this.config, undefined, {activate:false});
              return result;
            } catch (failure) {
              this.close(failure); this.config = previous;
              // A verified phone rejecting authentication/ownership is not an
              // address discovery failure; do not obscure it by scanning again.
              if (failure.code !== 'phone_unreachable' && !/certificate changed/i.test(failure.message)) throw failure;
            }
          }
        }
      }
      throw error;
    }
  }
  request(method, params = {}, token) {
    if (!this.connected) return Promise.reject(new Error('Phone disconnected. Capture a new screen before continuing.'));
    return this.#request(method, params, token);
  }
  #request(method, params, token) {
    if (!this.#socket || this.#socket.destroyed) return Promise.reject(new Error('Phone connection closed'));
    const id = randomUUID();
    return new Promise((resolve, reject) => {
      const timer = setTimeout(() => this.close(new Error('Phone timed out; action outcome is unknown. Do not repeat automatically.')), 30000);
      this.#pending.set(id, { resolve, reject, timer });
      this.#socket.write(JSON.stringify({ version: 1, id, method, params, ...(token ? { token } : {}) }) + '\n');
    });
  }
  #receive(data) {
    this.#buffer = Buffer.concat([this.#buffer, data]);
    if (this.#buffer.length > MAX_RESPONSE) { this.close(new Error('Phone response too large')); return; }
    let end;
    while ((end = this.#buffer.indexOf(10)) >= 0) {
      const line = this.#buffer.subarray(0, end); this.#buffer = this.#buffer.subarray(end + 1);
      try {
        const response = JSON.parse(line.toString('utf8'));
        const pending = this.#pending.get(response.id);
        if (!pending) throw new Error('Unexpected response');
        this.#pending.delete(response.id); clearTimeout(pending.timer);
        if (response.ok === true) pending.resolve(response.result);
        else pending.reject(new Error(typeof response.error === 'string' ? response.error : 'Phone rejected request'));
      } catch { this.close(new Error('Invalid phone response')); return; }
    }
  }
  close(error = new Error('Phone connection closed')) {
    const socket = this.#socket; this.#socket = undefined; this.features = {}; this.#authenticated = false;
    this.#abortConnect?.(error);
    socket?.destroy();
    for (const pending of this.#pending.values()) { clearTimeout(pending.timer); pending.reject(error); }
    this.#pending.clear(); this.#buffer = Buffer.alloc(0);
  }
}
