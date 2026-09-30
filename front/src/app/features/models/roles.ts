export const UserRole = {
  WORKER: 'WORKER',
  CLIENT: 'CLIENT',
  ADMIN: 'ADMIN',
  SUPER_ADMIN: 'SUPER_ADMIN',
} as const;

export type UserRole = typeof UserRole[keyof typeof UserRole];
