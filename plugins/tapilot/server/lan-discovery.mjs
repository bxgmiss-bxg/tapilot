import tls from 'node:tls';
import { createHash } from 'node:crypto';
import { networkInterfaces } from 'node:os';

// Bound fallback discovery to at most one /24 slice per physical IPv4 interface.
export function lanCandidates(interfaces = networkInterfaces()) {
  const result = new Map();
  const numeric = ip => ip.split('.').reduce((n, part) => (n << 8) | Number(part), 0);
  for (const [name, entries] of Object.entries(interfaces)) {
    if (!/^(en\d+|eth\d+|wlan\d+|wlp\w+)$/.test(name)) continue;
    for (const entry of entries ?? []) {
      if (entry.family !== 'IPv4' || entry.internal) continue;
      const prefix = entry.address.split('.').slice(0, 3).join('.');
      const local = numeric(entry.address), mask = numeric(entry.netmask);
      for (let i = 1; i < 255; i++) {
        const host = `${prefix}.${i}`, address = numeric(host);
        if (host === entry.address || (address & mask) !== (local & mask) ||
            (address & ~mask) === 0 || (address | mask) === -1) continue;
        if (!result.has(host)) result.set(host, {host, localAddress:entry.address});
      }
    }
  }
  return [...result.values()].slice(0, 508);
}

export function matchesPhone({host, localAddress}, fingerprint, port, timeoutMs = 700) {
  return new Promise(resolve => {
    let socket, timer, finished = false;
    const finish = matched => {
      if (finished) return;
      finished = true; clearTimeout(timer); socket?.destroy(); resolve(matched);
    };
    try {
      socket = tls.connect({host, port, localAddress, minVersion:'TLSv1.2', maxVersion:'TLSv1.2', rejectUnauthorized:false});
      timer = setTimeout(() => finish(false), timeoutMs);
      socket.once('secureConnect', () => {
        const cert = socket.getPeerCertificate().raw;
        finish(!!cert && createHash('sha256').update(cert).digest('hex') === fingerprint);
      });
      socket.once('error', () => finish(false));
      socket.once('close', () => finish(false));
    } catch { finish(false); }
  });
}

/** Probe only TLS identity. No pairing token or phone action is sent to candidates. */
export async function findPairedPhone({fingerprint, port = 39877, candidates = lanCandidates(), probe = matchesPhone}) {
  if (!/^[a-f0-9]{64}$/.test(fingerprint)) return [];
  let index = 0, found;
  await Promise.all(Array.from({length:16}, async () => {
    while (!found && index < candidates.length) {
      const candidate = candidates[index++];
      if (await probe(candidate, fingerprint, port)) found = {...candidate, port, fingerprint};
    }
  }));
  return found ? [{host:found.host, port, fingerprint}] : [];
}
