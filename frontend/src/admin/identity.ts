import { ref } from 'vue';
import { api } from '../shared/api';
import type { AdminProfile } from '../shared/types';
const profile = ref<AdminProfile | null>(null);
export const adminIdentity = {
  profile,
  async load() {
    profile.value = await api<AdminProfile>('admin', '/auth/me');
  },
  can(permission: string) {
    return profile.value?.permissions.includes(permission) ?? false;
  },
  clear() {
    profile.value = null;
  },
};
