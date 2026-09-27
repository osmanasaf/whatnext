import React from 'react';
import { AbsoluteFill } from 'remotion';
import { FONT_BODY, useBrandFonts } from '../fonts';
import { DateStamp, Poster, StillBackground, StillEventData, firstImage } from './shared';
import { ThemeContext, themeFor, withAlpha } from '../theme';

export interface CoverStillProps {
  monthName: string;
  date: string;
  eventCount: number;
  /** Günün en iyi etkinlikleri (story seçimi) — afiş mozaiği için; boşsa kapak tipografik kalır. */
  events?: StillEventData[];
  backgroundImageUrl?: string;
}

/** 1080×1350 tuvalde üst ~%58'e yayılan, hafif eğik ve üst üste binen 6 afiş yuvası. */
const SLOTS: { x: number; y: number; rot: number; size: number }[] = [
  { x: 40, y: 150, rot: -7, size: 360 },
  { x: 380, y: 70, rot: 4, size: 360 },
  { x: 720, y: 160, rot: -3, size: 360 },
  { x: 150, y: 500, rot: 5, size: 360 },
  { x: 500, y: 470, rot: -5, size: 360 },
  { x: 830, y: 540, rot: 7, size: 330 },
];

export const CoverStill: React.FC<CoverStillProps> = ({ date, eventCount, events = [], backgroundImageUrl }) => {
  useBrandFonts();
  const tiles = events.filter((e) => e.imageUrl).slice(0, SLOTS.length);
  // Az afişte ortadaki yuvaları tercih et (2-3 afiş tek köşede yığılmasın).
  const order = [1, 4, 0, 2, 3, 5];
  const placed = tiles.map((e, i) => ({ e, slot: SLOTS[order[i]] }));
  const count = eventCount > 0 ? eventCount : events.length;
  const theme = themeFor(date);

  return (
    <ThemeContext.Provider value={theme}>
    <AbsoluteFill>
      <StillBackground backgroundImageUrl={backgroundImageUrl} ambientImageUrl={firstImage(events)} />

      {/* Afiş mozaiği — z sırası yuva sırasına göre, sonrakiler öncekilerin üstüne biner */}
      <AbsoluteFill style={{ overflow: 'hidden' }}>
        {placed.map(({ e, slot }) => (
          <div
            key={e.id}
            style={{
              position: 'absolute',
              left: slot.x,
              top: slot.y,
              transform: `rotate(${slot.rot}deg)`,
            }}
          >
            <Poster imageUrl={e.imageUrl} title={e.title} eventType={e.eventType} width={slot.size} height={slot.size} radius={26} />
          </div>
        ))}
      </AbsoluteFill>

      {/* Alt panel ay renginde DOLU — profil ızgarasında her ay ayrı renk bandı olarak okunur.
          Eğik üst kenar mozaiği keser; tüm yazı panelin üstünde koyu (ink). */}
      <div
        style={{
          position: 'absolute',
          left: 0,
          right: 0,
          top: 820,
          bottom: 0,
          background: theme.accent,
          clipPath: 'polygon(0 14%, 100% 0, 100% 100%, 0 100%)',
          boxShadow: '0 -20px 60px rgba(0,0,0,0.5)',
        }}
      />

      {/* Ön plan: takvim damgası + sayı + kaydır ipucu */}
      <div style={{ position: 'absolute', left: 64, right: 64, bottom: 90, display: 'flex', flexDirection: 'column', gap: 26 }}>
        <div
          style={{
            fontSize: 20,
            fontWeight: 800,
            letterSpacing: 5,
            color: withAlpha(theme.base, 0.75),
            fontFamily: FONT_BODY,
          }}
        >
          BUGÜN İSTANBUL&apos;DA
        </div>
        <DateStamp date={date} scale={1.45} ink={theme.base} />
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <div
            style={{
              background: theme.base,
              borderRadius: 100,
              padding: '16px 32px',
              fontSize: 32,
              fontWeight: 700,
              lineHeight: 1,
              color: theme.accent,
              fontFamily: FONT_BODY,
              letterSpacing: 1,
            }}
          >
            {count} etkinlik
          </div>
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: 12,
              fontSize: 24,
              fontWeight: 800,
              letterSpacing: 2,
              color: theme.base,
              fontFamily: FONT_BODY,
            }}
          >
            <span>KAYDIR</span>
            <svg width="34" height="24" viewBox="0 0 34 24" fill="none">
              <path d="M2 12h28M20 3l10 9-10 9" stroke={theme.base} strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" />
            </svg>
          </div>
        </div>
      </div>

      <div
        style={{
          position: 'absolute',
          bottom: 30,
          left: 0,
          right: 0,
          textAlign: 'center',
          fontSize: 16,
          fontWeight: 700,
          letterSpacing: 2,
          color: withAlpha(theme.base, 0.6),
          fontFamily: FONT_BODY,
        }}
      >
        whatnext.tr
      </div>
    </AbsoluteFill>
    </ThemeContext.Provider>
  );
};
