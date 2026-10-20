import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import languageData from '../../../assets/lang/data.json';

@Injectable({
  providedIn: 'root'
})
export class LanguageManagerService {

  public currentLanguage$: BehaviorSubject<any> = new BehaviorSubject<any>(null);
  public currentLanguageFlag = 'assets/flags/fr.png';
  public currentCode = 'FR';

  private readonly STORAGE_KEY = 'site_lang';

  public MASTERY_LEVELS = [
    { value: 1, labelKey: 'beginner', defaultLabel: 'Beginner' },
    { value: 2, labelKey: 'intermediate', defaultLabel: 'Intermediate' },
    { value: 3, labelKey: 'good', defaultLabel: 'Good' },
    { value: 4, labelKey: 'native', defaultLabel: 'Native' }
  ];

  // Liste des langues supportées
  public availableLanguages = [
    { code: 'FR', label: 'Français', flag: '/assets/flags/france.svg' },
    { code: 'EN', label: 'English', flag: '/assets/flags/united_kingdom.svg' },
    { code: 'DE', label: 'Deutsch', flag: '/assets/flags/germany.svg' },
    { code: 'ES', label: 'Español', flag: '/assets/flags/spain.svg' },
    { code: 'PT', label: 'Português', flag: '/assets/flags/portugal.svg'},
    { code: 'IT', label: 'Italiano', flag: '/assets/flags/italy.svg' },
    { code: 'RU', label: 'Русский', flag: '/assets/flags/russia.svg' },
    { code: 'UA', label: 'Українська', flag: '/assets/flags/ukraine.svg' },
    { code: 'AR', label: 'العربية', flag: '/assets/flags/saudi_arabia.svg' }
  ];

  constructor() {
    this.loadUserLanguage();
  }

  private loadUserLanguage() {
    const savedLang = localStorage.getItem(this.STORAGE_KEY) || 'FR';
    this.changeLanguageTo(savedLang);
  }

  public changeLanguageTo(langCode: string) {
    const lang = this.availableLanguages.find(l => l.code === langCode.toUpperCase()) || this.availableLanguages[0];

    this.currentCode = lang.code;
    this.currentLanguageFlag = lang.flag;

    // Extrait les mots correspondants du JSON
    const lowerKey = lang.code.toLowerCase() as keyof typeof languageData;
    const selectedTranslations = languageData[lowerKey] || languageData.fr;

    this.currentLanguage$.next(selectedTranslations);

    // Sauvegarde dans le cache
    localStorage.setItem(this.STORAGE_KEY, lang.code);
  }

  getLanguageMeta(code: string) {
    return this.availableLanguages.find(l => l.code === code) || {
      code,
      label: code,
      flag: '/assets/flags/france.svg'
    };
  }

  getMasteryLabel(level: number, t?: any): string {
    const item = this.MASTERY_LEVELS.find(m => m.value === level);
    if (!item) return '';
    if (t?.common?.mastery_levels) {
      return t.common.mastery_levels[item.labelKey] || item.defaultLabel;
    }
    return item.defaultLabel;
  }

  // Récupérer le libellé traduit d'un BodyType
  getBodyTypeLabel(key: string, t: any): string {
    if (!key) return '';
    const map: Record<string, string> = {
      SLIM: t?.common?.body_types?.slim || 'Slim',
      ATHLETIC: t?.common?.body_types?.athletic || 'Athletic',
      AVERAGE: t?.common?.body_types?.average || 'Average',
      CURVY: t?.common?.body_types?.curvy || 'Curvy',
      ROUND: t?.common?.body_types?.round || 'Round'
    };
    return map[key] || key;
  }

// Récupérer le libellé traduit d'une couleur d'yeux
  getEyeColorLabel(key: string, t: any): string {
    if (!key) return '';
    const map: Record<string, string> = {
      BROWN: t?.common?.eye_colors?.brown || 'Brown',
      HAZEL: t?.common?.eye_colors?.hazel || 'Hazel',
      GREEN: t?.common?.eye_colors?.green || 'Green',
      BLUE: t?.common?.eye_colors?.blue || 'Blue',
      GREY: t?.common?.eye_colors?.grey || 'Grey'
    };
    return map[key] || key;
  }

// Récupérer le libellé traduit d'une couleur de cheveux
  getHairColorLabel(key: string, t: any): string {
    if (!key) return '';
    const map: Record<string, string> = {
      BLACK: t?.common?.hair_colors?.black || 'Black',
      BROWN: t?.common?.hair_colors?.brown || 'Brown',
      FAIR: t?.common?.hair_colors?.fair || 'Fair',
      GINGER: t?.common?.hair_colors?.ginger || 'Ginger',
      BLOND: t?.common?.hair_colors?.blond || 'Blond',
      GREY: t?.common?.hair_colors?.grey || 'Grey',
      COLORED: t?.common?.hair_colors?.colored || 'Colored'
    };
    return map[key] || key;
  }
}
