import { Injectable, inject } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';

const STORAGE_KEY = 'rmpd.lang';
const SUPPORTED = ['uk', 'pl', 'en'] as const;
export type AppLang = (typeof SUPPORTED)[number];

@Injectable({ providedIn: 'root' })
export class LocaleService {
  private readonly translate = inject(TranslateService);

  init(): void {
    const saved = localStorage.getItem(STORAGE_KEY) as AppLang | null;
    const lang = saved && SUPPORTED.includes(saved) ? saved : 'uk';
    this.translate.addLangs([...SUPPORTED]);
    this.translate.setDefaultLang('uk');
    this.translate.use(lang);
  }

  current(): AppLang {
    const lang = this.translate.currentLang as AppLang;
    return SUPPORTED.includes(lang) ? lang : 'uk';
  }

  set(lang: AppLang): void {
    this.translate.use(lang);
    localStorage.setItem(STORAGE_KEY, lang);
  }

  readonly languages: { code: AppLang; labelKey: string }[] = [
    { code: 'uk', labelKey: 'lang.uk' },
    { code: 'pl', labelKey: 'lang.pl' },
    { code: 'en', labelKey: 'lang.en' },
  ];
}
