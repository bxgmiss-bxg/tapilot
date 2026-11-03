import { randomUUID } from 'node:crypto';

/** Project real execution snapshots into small UI events; never forward model logs or input values. */
export function createStepProgress(emit, scope) {
  const seen = new Map();
  return (_serialized, execution) => {
    for (const task of execution?.tasks ?? []) {
      if (!task.taskId || !['running', 'finished', 'failed', 'cancelled'].includes(task.status)) continue;
      if (!['Planning', 'Insight', 'Action Space'].includes(task.type)) continue;
      const id = `${scope}:${task.taskId}`;
      const action = String(task.subType || task.type).slice(0, 60);
      // Locator descriptions describe the visible target, not the text being entered.
      const description = task.param?.locate?.description ?? task.param?.locate?.prompt;
      const target = task.type === 'Action Space' && !['Input', 'KeyboardPress'].includes(action) && typeof description === 'string'
        ? description.slice(0, 240) : '';
      const step = { id, action, target, status: task.status };
      const signature = JSON.stringify(step);
      if (seen.get(id) === signature) continue;
      seen.set(id, signature);
      emit({ type: 'execution.step', timestamp: new Date().toISOString(), ...step });
    }
  };
}

/** Direct workflow nodes bypass Agent snapshots but still have an actual lifecycle. */
export async function runDirectStep(action, execute, emit, target = '') {
  const step = { type: 'execution.step', id: randomUUID(), action, target };
  emit({ ...step, timestamp: new Date().toISOString(), status: 'running' });
  try {
    const result = await execute();
    emit({ ...step, timestamp: new Date().toISOString(), status: 'finished' });
    return result;
  } catch (error) {
    emit({ ...step, timestamp: new Date().toISOString(), status: error?.name === 'AbortError' ? 'cancelled' : 'failed' });
    throw error;
  }
}
