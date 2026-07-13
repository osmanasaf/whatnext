import React from 'react';
import { AbsoluteFill, Img, staticFile } from 'remotion';
import { FONT_BODY } from '../fonts';

export interface Palette {
  bg1: string;
  bg2: string;
  accent: string;
  text: string;
}

export interface StillEventData {
  id: number;
  title: string;
  venueName?: string;
  startTime?: string;
  imageUrl?: string;
}

export const TYPE_LABELS: Record<string, string> = {
  concert: 'KONSER',
  theater: 'TİYATRO',
  standup: 'STAND-UP',
};

const MONTHS_TR = [
  'Ocak', 'Şubat', 'Mart', 'Nisan', 'Mayıs', 'Haziran',
  'Temmuz', 'Ağustos', 'Eylül', 'Ekim', 'Kasım', 'Aralık',
];
const DAYS_TR = ['Pazar', 'Pazartesi', 'Salı', 'Çarşamba', 'Perşembe', 'Cuma', 'Cumartesi'];

/** "2026-07-17" -> "17 Temmuz" (UTC ile parse edilir, yerel saat dilimi kaymasını önler). */
export const formatDateTr = (isoDate: string): string => {
  const parts = isoDate.split('-');
  if (parts.length < 3) return isoDate;
  const [, m, d] = parts;
  return `${parseInt(d, 10)} ${MONTHS_TR[parseInt(m, 10) - 1]}`;
};

/** "2026-07-17" -> "Cuma". */
export const dayNameTr = (isoDate: string): string => {
  const d = new Date(isoDate + 'T00:00:00Z');
  return DAYS_TR[d.getUTCDay()];
};

/** http(s) URL ise olduğu gibi, değilse staticFile-göreli yol olarak çözülür (AI arka plan katmanı). */
export const resolveBackgroundSrc = (url?: string): string | null => {
  if (!url) return null;
  if (url.startsWith('http://') || url.startsWith('https://')) return url;
  return staticFile(url);
};

/**
 * Arka plan: {@code backgroundImageUrl} varsa (AI motoru) o, yoksa ay paleti gradyanı + dekoratif
 * leke. Liste tipi ekranlar (post/story/kapak) tek bir etkinliğin afişini göstermez — Reel'in
 * EventCard'ından farklı olarak burada birden çok etkinlik aynı ekranda, tek bir foto hepsi için
 * temsili olmaz.
 */
export const StillBackground: React.FC<{ backgroundImageUrl?: string; palette: Palette }> = ({
  backgroundImageUrl,
  palette,
}) => {
  const src = resolveBackgroundSrc(backgroundImageUrl);
  if (src) {
    return (
      <AbsoluteFill>
        <Img src={src} style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
        <AbsoluteFill
          style={{ background: 'linear-gradient(180deg, rgba(10,8,25,0.15) 0%, rgba(10,8,25,0.55) 100%)' }}
        />
      </AbsoluteFill>
    );
  }
  return (
    <AbsoluteFill style={{ background: `linear-gradient(160deg, ${palette.bg1} 0%, ${palette.bg2} 100%)` }}>
      <div
        style={{
          position: 'absolute',
          top: '-12%',
          right: '-12%',
          width: '60%',
          height: '60%',
          borderRadius: '50%',
          background: `radial-gradient(circle, ${palette.accent}66 0%, transparent 70%)`,
        }}
      />
      <div
        style={{
          position: 'absolute',
          bottom: '-16%',
          left: '-16%',
          width: '55%',
          height: '55%',
          borderRadius: '50%',
          background: `radial-gradient(circle, ${palette.accent}33 0%, transparent 70%)`,
        }}
      />
    </AbsoluteFill>
  );
};

export const Footer: React.FC<{ color?: string }> = ({ color = 'rgba(255,255,255,0.55)' }) => (
  <div
    style={{
      position: 'absolute',
      bottom: 28,
      left: 0,
      right: 0,
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      gap: 8,
    }}
  >
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
      <polygon points="12,2 22,8.5 22,15.5 12,22 2,15.5 2,8.5" stroke={color} strokeWidth="1.5" />
    </svg>
    <span style={{ fontSize: 14, color, fontFamily: FONT_BODY, letterSpacing: 1 }}>whatnext.tr</span>
  </div>
);

/** Etkinlik sayısına göre kademeli font boyutu — CSS'in tek başına yapamadığı "N öğeyi sabit
 * yüksekliğe sığdır" işini basit bir tabloyla çözer; line-clamp ile birlikte taşma garantili önlenir. */
export const fontSizeForCount = (count: number, sizes: { few: number; some: number; many: number }): number => {
  if (count <= 4) return sizes.few;
  if (count <= 7) return sizes.some;
  return sizes.many;
};
