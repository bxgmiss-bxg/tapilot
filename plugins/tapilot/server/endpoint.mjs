import { isIP } from 'node:net';
import { matchesPhone } from './lan-discovery.mjs';
import { localAddressForPhone } from './transport.mjs';
import { loadDevices, saveConfig, configPath } from './config.mjs';

// Updating a remembered address never sends a pairing token to an unverified host.
export async function updatePhoneEndpoint(id, host, port, {path = configPath(), probe = matchesPhone} = {}) {
  const registry = await loadDevices(path);
  const device = registry.devices.find(d => d.fingerprint === id);
  if (!device) throw Error('Paired device not found. List devices first.');
  port ??= device.port;
  if (typeof host !== 'string' || isIP(host) !== 4 || !Number.isInteger(port) || port < 1024 || port > 65535)
    throw Error('Enter a valid phone IPv4 address and port.');
  if (!await probe({host, localAddress:localAddressForPhone(host)}, device.fingerprint, port, 3000))
    throw Error('Could not verify the paired phone at this address. Check the phone IP, wireless control, and network isolation.');
  await saveConfig({...device, host, port}, path, {activate:false});
}
