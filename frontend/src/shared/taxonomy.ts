import type { Entry } from './types';
export function parentChoices(kind: string, currentId: string, parents: Entry[]): Entry[] {
  return kind === 'categories' ? parents.filter((parent) => parent.id !== currentId) : parents;
}
