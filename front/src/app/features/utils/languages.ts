/** Spoken languages, best mastered first (level 4 = native); same level: original order kept. */
export function sortByMastery<T extends { level: number }>(languages: readonly T[] | null | undefined): T[] {
  return [...(languages ?? [])].sort((a, b) => b.level - a.level);
}
