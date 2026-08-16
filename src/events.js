const GAP_HOURS = 6; // a pause of 6h+ between two shots starts a new "event"
const MAX_SPAN_DAYS = 3; // hard cap so a single event can't stretch forever

function fmtDate(d) {
  return d.toISOString().slice(0, 10);
}

/**
 * Cluster photos chronologically into "events" (like a photo-organizer would
 * group a day trip or a party). Photos without a date fall into a single
 * "Sans date" bucket. Returns { events: [{id,label,start,end}], assignments: Map(photoId -> eventId) }.
 */
export function clusterEvents(photos) {
  const dated = photos.filter((p) => p.takenAt).sort((a, b) => new Date(a.takenAt) - new Date(b.takenAt));
  const undated = photos.filter((p) => !p.takenAt);

  const events = [];
  const assignments = new Map();
  let current = null;

  for (const photo of dated) {
    const t = new Date(photo.takenAt);
    if (
      current &&
      (t - current.lastTime) / 36e5 <= GAP_HOURS &&
      (t - current.start) / 86400000 <= MAX_SPAN_DAYS
    ) {
      current.lastTime = t;
      current.photoIds.push(photo.id);
    } else {
      current = { id: `evt_${photo.id}`, start: t, lastTime: t, photoIds: [photo.id] };
      events.push(current);
    }
  }

  for (const e of events) {
    const startLabel = fmtDate(e.start);
    const endLabel = fmtDate(e.lastTime);
    e.label = startLabel === endLabel ? startLabel : `${startLabel} au ${endLabel}`;
    for (const pid of e.photoIds) assignments.set(pid, e.id);
  }

  if (undated.length) {
    const id = 'evt_sans_date';
    events.push({ id, start: null, lastTime: null, label: 'Sans date', photoIds: undated.map((p) => p.id) });
    for (const p of undated) assignments.set(p.id, id);
  }

  return { events, assignments };
}
