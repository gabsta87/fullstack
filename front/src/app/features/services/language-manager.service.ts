import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import languageData from '../../../assets/lang/data.json';

@Injectable({
  providedIn: 'root'
})
class LanguageManagerService {
  // Contient l'objet de mots traduits pour la langue active
  currentLanguage$: BehaviorSubject<any> = new BehaviorSubject<any>(null);
  currentLanguageFlag = 'assets/flags/fr.png';
  currentCode = 'FR';

  private readonly STORAGE_KEY = 'site_lang';

  // Liste des langues supportées avec leur icône/drapeau local ou émoji
  public availableLanguages = [
    { code: 'FR', label: 'Français', flag: '/assets/flags/france.svg' },
    { code: 'EN', label: 'English', flag: '/assets/flags/united_kingdom.svg' },
    { code: 'DE', label: 'Deutsch', flag: '/assets/flags/germany.svg' },
    { code: 'ES', label: 'Español', flag: '/assets/flags/spain.svg' },
    { code: 'IT', label: 'Italiano', flag: '/assets/flags/italy.svg' },
    { code: 'RU', label: 'Русский', flag: '/assets/flags/russia.svg' },
    { code: 'UA', label: 'Українська', flag: '/assets/flags/ukraine.svg' },
    { code: 'AR', label: 'العربية', flag: '/assets/flags/saudi_arabia.svg' }
  ];

  constructor() {
    this.loadUserLanguage();
  }

  private loadUserLanguage() {
    // Récupère la langue du cache ou prend 'FR' par défaut
    const savedLang = localStorage.getItem(this.STORAGE_KEY) || 'FR';
    this.changeLanguageTo(savedLang);
  }

  changeLanguageTo(langCode: string) {
    const lang = this.availableLanguages.find(l => l.code === langCode.toUpperCase()) || this.availableLanguages[0];

    this.currentCode = lang.code;
    this.currentLanguageFlag = lang.flag;

    // Extrait les mots correspondants du JSON (ex: languageData.fr)
    const lowerKey = lang.code.toLowerCase() as keyof typeof languageData;
    this.currentLanguage$.next(languageData[lowerKey] || languageData.fr);

    // Sauvegarde immédiate dans le cache
    localStorage.setItem(this.STORAGE_KEY, lang.code);
  }
}

export default LanguageManagerService
