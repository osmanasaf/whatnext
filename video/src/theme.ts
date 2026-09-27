import { createContext, useContext } from 'react';

/**
 * Ay teması: her ayın kendi vurgu rengi (tarih, çerçeveler, CTA, arka plan tonu) + özel günlerde
 * ayrı renk ve etiket. Tarih ({@code date} prop'u) zaten her kompozisyona geliyor, Java tarafı değişmez.
 *
 * <p>Önceki ay paleti yalnız afişsiz günlerde görünen arka plan gradyanına bağlıydı; afiş varken
 * (neredeyse her gün) bulanık afiş onu tamamen örtüyordu, bu yüzden paylaşımlar her ay aynıydı.
 */
export interface Theme {
  accent: string;
  /** Vurgunun koyu zemine karışmış hâli — gradyan ve karartma katmanlarının rengi. */
  base: string;
  /** Özel gün etiketi ("CUMHURİYET BAYRAMI"); sıradan günlerde yok. */
  special?: string;
}

/** Komşu aylar belirgin farklı tonlarda; İstanbul mevsimlerinden esinli (Nisan lale, Mayıs erguvan...). */
const MONTH_ACCENTS = [
  '#a5d8ff', // Ocak — buz mavisi
  '#fb7185', // Şubat — gül
  '#86efac', // Mart — nane
  '#ff5c7a', // Nisan — lale
  '#d8a4ff', // Mayıs — erguvan
  '#fde047', // Haziran — limon
  '#22d3ee', // Temmuz — turkuaz
  '#fb923c', // Ağustos — gün batımı
  '#e9c46a', // Eylül — hasat altını
  '#f4845f', // Ekim — kızıl yaprak
  '#ff6f91', // Kasım — şarap
  '#34d399', // Aralık — zümrüt
];

const NATIONAL_RED = '#ef4444';

/** MM-DD → etiket (+ isteğe bağlı renk). ponytail: yalnız sabit tarihli günler; Ramazan/Kurban
 * Bayramı her yıl kaydığı için yok, gerekirse yıl-tarih tablosu eklenir. */
const SPECIAL_DAYS: Record<string, { label: string; accent?: string }> = {
  '01-01': { label: 'YENİ YIL', accent: '#fde047' },
  '02-14': { label: 'SEVGİLİLER GÜNÜ', accent: '#fb7185' },
  '03-08': { label: 'DÜNYA KADINLAR GÜNÜ', accent: '#c084fc' },
  '03-27': { label: 'DÜNYA TİYATRO GÜNÜ' },
  '04-23': { label: '23 NİSAN', accent: NATIONAL_RED },
  '05-19': { label: '19 MAYIS', accent: NATIONAL_RED },
  '06-21': { label: 'DÜNYA MÜZİK GÜNÜ' },
  '08-30': { label: 'ZAFER BAYRAMI', accent: NATIONAL_RED },
  '10-29': { label: 'CUMHURİYET BAYRAMI', accent: NATIONAL_RED },
  '10-31': { label: 'CADILAR BAYRAMI', accent: '#fb923c' },
  '12-31': { label: 'YILBAŞI GECESİ', accent: '#fde047' },
};

const DARK = '#0b0a1a';

const hexToRgb = (hex: string): [number, number, number] => {
  const n = parseInt(hex.slice(1), 16);
  return [(n >> 16) & 255, (n >> 8) & 255, n & 255];
};

/** "#rrggbb" + alfa → "rgba(...)". */
export const withAlpha = (hex: string, alpha: number): string => {
  const [r, g, b] = hexToRgb(hex);
  return `rgba(${r},${g},${b},${alpha})`;
};

/** İki rengi karıştırır (t: 0 → a, 1 → b). */
const mix = (a: string, b: string, t: number): string => {
  const ca = hexToRgb(a);
  const cb = hexToRgb(b);
  return '#' + ca.map((v, i) => Math.round(v + (cb[i] - v) * t).toString(16).padStart(2, '0')).join('');
};

/**
 * "2026-10-29" → tema. Geçersiz tarihte Ocak'a düşmez, varsayılan mor döner.
 * {@code withSpecial=false}: yalnız ay rengi (haftalık Reel tek güne göre boyanmasın).
 */
export const themeFor = (isoDate?: string, withSpecial = true): Theme => {
  const [, mm, dd] = (isoDate ?? '').split('-');
  const month = parseInt(mm, 10);
  if (!month || month < 1 || month > 12) return DEFAULT_THEME;
  const special = !withSpecial ? undefined : SPECIAL_DAYS[`${mm}-${(dd ?? '').slice(0, 2)}`];
  const accent = special?.accent ?? MONTH_ACCENTS[month - 1];
  return { accent, base: mix(DARK, accent, 0.1), special: special?.label };
};

export const DEFAULT_THEME: Theme = { accent: '#a78bfa', base: mix(DARK, '#a78bfa', 0.1) };

export const ThemeContext = createContext<Theme>(DEFAULT_THEME);
export const useTheme = (): Theme => useContext(ThemeContext);
