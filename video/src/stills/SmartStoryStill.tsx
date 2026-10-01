import React from 'react';
import { AbsoluteFill } from 'remotion';
import { useBrandFonts } from '../fonts';
import { DateStamp, EventRow, MoreCta, SaveCta, StillBackground, StillEventData, firstImage } from './shared';
import { ThemeContext, themeFor } from '../theme';

export interface SmartStoryStillProps {
  monthName: string;
  date: string;
  events: StillEventData[];
  backgroundImageUrl?: string;
  /** Gösterilmeyen etkinlik sayısı — "+N etkinlik daha bugün". */
  moreCount?: number;
}

const PAD = 60;
const HEADER_H = 190;
/** Instagram story alt yanıt çubuğu güvenli bölgesi (~%12) + CTA + nefes payı. */
const CTA_ZONE = 340;
const CTA_BOTTOM = 250;
const ROW_GAP = 20;
/** En çok 10 satır (hafta sonu gün slaytıyla aynı seçki); kalabalıkta başlık tek satıra iner. Java da 10 seçer. */
const MAX_ROWS = 10;

const layoutFor = (count: number, canvasHeight: number) => {
  const available = canvasHeight - PAD - HEADER_H - 30 - CTA_ZONE;
  const rowH = Math.floor((available - (count - 1) * ROW_GAP) / count);
  const posterHeight = Math.min(200, Math.round(rowH * 0.8));
  const titleSize = count <= 4 ? 36 : count <= 6 ? 32 : 28;
  return { posterHeight, titleSize };
};

export const SmartStoryStill: React.FC<SmartStoryStillProps> = ({ date, events, backgroundImageUrl, moreCount }) => {
  useBrandFonts();
  const shown = events.slice(0, MAX_ROWS);
  const { posterHeight, titleSize } = layoutFor(Math.max(shown.length, 1), 1920);

  return (
    <ThemeContext.Provider value={themeFor(date)}>
    <AbsoluteFill>
      <StillBackground backgroundImageUrl={backgroundImageUrl} ambientImageUrl={firstImage(shown)} />

      <AbsoluteFill
        style={{ padding: `${PAD}px ${PAD}px ${CTA_ZONE}px ${PAD}px`, display: 'flex', flexDirection: 'column' }}
      >
        <div style={{ height: HEADER_H, display: 'flex', alignItems: 'center' }}>
          <DateStamp date={date} kicker="BUGÜN İSTANBUL'DA" subtitle={`günün en iyi ${shown.length}'${shown.length === 10 ? 'u' : 'i'}`} scale={1.12} />
        </div>

        <div style={{ flex: 1, display: 'flex', flexDirection: 'column', justifyContent: 'center', gap: ROW_GAP, paddingTop: 14 }}>
          {shown.map((e) => (
            <EventRow key={e.id} event={e} posterHeight={posterHeight} titleSize={titleSize} showType />
          ))}
        </div>
      </AbsoluteFill>

      {moreCount && moreCount > 0 ? (
        <MoreCta bottom={CTA_BOTTOM} count={moreCount} size={28} suffix="bugün İstanbul'da" />
      ) : (
        <SaveCta bottom={CTA_BOTTOM} size={24} />
      )}
    </AbsoluteFill>
    </ThemeContext.Provider>
  );
};
