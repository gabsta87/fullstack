import { sortByMastery } from './languages';

describe('sortByMastery', () => {
  it('puts the best mastered languages first and keeps the order of equal levels', () => {
    const langs = [{ language: 'EN', level: 2 }, { language: 'FR', level: 4 }, { language: 'DE', level: 2 }, { language: 'IT', level: 3 }];
    expect(sortByMastery(langs).map(l => l.language)).toEqual(['FR', 'IT', 'EN', 'DE']);
    expect(langs[0].language).toBe('EN'); // input untouched
  });

  it('accepts a missing list', () => {
    expect(sortByMastery(undefined)).toEqual([]);
  });
});
