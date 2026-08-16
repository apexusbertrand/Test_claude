/**
 * Shared helpers for the optional on-device AI features (object detection,
 * face recognition). Both can stall on mobile — a model download that never
 * completes, a WebGL call that never returns — so everything here is built to
 * fail loudly and quickly rather than hang, and to surface what happened in
 * the UI, since a phone user has no console to read.
 */

const listeners = new Set();
const messages = [];

/** Reject if `promise` hasn't settled in time — a hang becomes a real error. */
export function withTimeout(promise, ms, label) {
  return Promise.race([
    promise,
    new Promise((_, reject) => setTimeout(() => reject(new Error(`Délai dépassé (${label})`)), ms)),
  ]);
}

/** Record a user-visible status/diagnostic message and notify the UI. */
export function reportAiStatus(message) {
  const entry = { message, at: new Date() };
  messages.push(entry);
  for (const fn of listeners) {
    try {
      fn(entry);
    } catch {
      // a broken listener must never break the pipeline
    }
  }
}

export function onAiStatus(fn) {
  listeners.add(fn);
  return () => listeners.delete(fn);
}

export function getAiStatusMessages() {
  return messages.slice();
}
