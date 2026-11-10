export const BLOOD_GROUPS = [
  ['A_POS', 'A+'], ['A_NEG', 'A-'], ['B_POS', 'B+'], ['B_NEG', 'B-'],
  ['AB_POS', 'AB+'], ['AB_NEG', 'AB-'], ['O_POS', 'O+'], ['O_NEG', 'O-'],
];
export const groupLabel = (g) => (BLOOD_GROUPS.find(([k]) => k === g) || [g, g || '?'])[1];

export const COMPONENTS = [
  ['WHOLE_BLOOD', 'Whole blood'], ['RED_CELLS', 'Red cells'],
  ['PLASMA', 'Plasma'], ['PLATELETS', 'Platelets'],
];
export const componentLabel = (c) => (COMPONENTS.find(([k]) => k === c) || [c, c])[1];

export const URGENCIES = [['EMERGENCY', 'Emergency'], ['URGENT', 'Urgent'], ['ROUTINE', 'Routine']];

// Simulated locations (Chandigarh tricity) used instead of real GPS in the prototype.
export const PLACES = [
  { name: 'Chandigarh', lat: 30.7333, lng: 76.7794 },
  { name: 'Mohali', lat: 30.7046, lng: 76.7179 },
  { name: 'Panchkula', lat: 30.6942, lng: 76.8606 },
  { name: 'Zirakpur', lat: 30.6425, lng: 76.8173 },
  { name: 'Kharar', lat: 30.7469, lng: 76.6469 },
];

export const ROLES = ['DONOR', 'REQUESTER', 'MODERATOR', 'ADMIN'];
export const ROLE_LABEL = { DONOR: 'Donor', REQUESTER: 'Requester', MODERATOR: 'Coordinator', ADMIN: 'Administrator' };

export const fmt = (iso) => (iso ? new Date(iso).toLocaleString() : '-');

/** Permission check against the list the server sent with the user. The server enforces every rule again. */
export const can = (user, permission) =>
  !!user && Array.isArray(user.permissions) && user.permissions.includes(permission);
export const canAny = (user, list) => list.some((p) => can(user, p));

export const STAFF_PERMISSIONS = [
  'ANALYTICS_VIEW', 'QUEUE_VIEW', 'DELIVERY_RETRY', 'USER_VIEW', 'PERMISSION_MANAGE', 'AUDIT_VIEW',
  'INVENTORY_VIEW', 'DONATION_RECORD', 'REPORT_EXPORT',
];
export const URGENCY_COLORS = { EMERGENCY: '#c8102e', URGENT: '#e08e0b', ROUTINE: '#3b82a6' };

export function homePath(user) {
  if (!user) return '/';
  if (user.role === 'DONOR') return '/donor';
  if (user.role === 'REQUESTER') return '/requests';
  if (canAny(user, STAFF_PERMISSIONS)) return '/control';
  return '/notifications';
}

export const MIN_DONATION_INTERVAL_DAYS = 90; // indicative placeholder, mirrors the server default
