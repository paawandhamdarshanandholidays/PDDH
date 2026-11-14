export const USER_ROLES = ['CEO', 'ADMIN', 'MANAGER', 'STAFF', 'CUSTOMER'] as const;
export type UserRole = typeof USER_ROLES[number];

export interface UserProfile {
  id: string;
  name: string;
  username: string;
  email: string;
  mobile: string;
  role: UserRole;
  active: boolean;
  lastLoginAt?: string | null;
}

export interface CreateUserRequest {
  name: string;
  username: string;
  email: string;
  mobile: string;
  password: string;
  gender: string;
  role: UserRole;
}
