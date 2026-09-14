import React from 'react';
import { AbsoluteFill } from 'remotion';
import { useBrandFonts } from '../fonts';
import { DateStamp, EventRow, Palette, SaveCta, StillBackground, StillEventData, firstImage } from './shared';

export interface SmartStoryStillProps {
  monthName: string;
  palette: Palette;
  date: string;
  events: StillEventData[];
  backgroundImageUrl?: string;
}

const PAD = 60;
const HEADER_H = 190;
/** Instagram story alt yanıt çubuğu güvenli bölgesi (~%12) + CTA + nefes payı. */
const CTA_ZONE = 340;
const CTA_BOTTOM = 250;
const ROW_GAP = 20;
/** En çok 8 satır — 10 kalabalık bulundu; Java tarafı da 8 seçer, burası emniyet. */
const MAX_ROWS = 8;

const layoutFor = (count: number, canvasHeight: number) => {
  const available = canvasHeight - PAD - HEADER_H - 30 - CTA_ZONE;
  const rowH = Math.floor((available - (count - 1) * ROW_GAP) / count);
  const posterHeight = Math.min(200, Math.round(rowH * 0.8));
  const titleSize = count <= 4 ? 36 : count <= 6 ? 32 : 28;
  return { posterHeight, titleSize };
};

export const SmartStoryStill: React.FC<SmartStoryStillProps> = ({ palette, date, events, backgroundImageUrl }) => {
  useBrandFonts();
  const shown = events.slice(0, MAX_ROWS);
  const { posterHeight, titleSize } = layoutFor(Math.max(shown.length, 1), 1920);

  return (
    <AbsoluteFill>
      <StillBackground backgroundImageUrl={backgroundImageUrl} palette={palette} ambientImageUrl={firstImage(shown)} />

      <AbsoluteFill
        style={{ padding: `${PAD}px ${PAD}px ${CTA_ZONE}px ${PAD}px`, display: 'flex', flexDirection: 'column' }}
      >
        <div style={{ height: HEADER_H, display: 'flex', alignItems: 'center' }}>
          <DateStamp date={date} kicker="BUGÜN İSTANBUL'DA" subtitle={`${shown.length} etkinlik`} scale={1.12} />
        </div>

        <div style={{ flex: 1, display: 'flex', flexDirection: 'column', justifyContent: 'center', gap: ROW_GAP, paddingTop: 14 }}>
          {shown.map((e) => (
            <EventRow key={e.id} event={e} posterHeight={posterHeight} titleSize={titleSize} showType />
          ))}
        </div>
      </AbsoluteFill>

      <SaveCta bottom={CTA_BOTTOM} size={24} />
    </AbsoluteFill>
  );
};
