import React from 'react';
import { AbsoluteFill } from 'remotion';
import { FONT_BODY, FONT_TITLE } from '../fonts';
import { EventRow, Palette, SaveCta, StillBackground, StillEventData, dayNameTr, firstImage, formatDateTr } from './shared';

export interface SmartStoryStillProps {
  monthName: string;
  palette: Palette;
  date: string;
  events: StillEventData[];
  backgroundImageUrl?: string;
}

const PAD = 60;
const HEADER_H = 170;
/** Instagram story alt yanıt çubuğu güvenli bölgesi (~%12) + CTA. */
const CTA_ZONE = 340;
const CTA_BOTTOM = 250;
const ROW_GAP = 18;

const layoutFor = (count: number, canvasHeight: number) => {
  const available = canvasHeight - PAD - HEADER_H - 30 - CTA_ZONE;
  const rowH = Math.floor((available - (count - 1) * ROW_GAP) / count);
  const posterHeight = Math.min(200, Math.round(rowH * 0.82));
  const titleSize = count <= 4 ? 36 : count <= 6 ? 32 : count <= 8 ? 28 : 25;
  return { posterHeight, titleSize };
};

export const SmartStoryStill: React.FC<SmartStoryStillProps> = ({ palette, date, events, backgroundImageUrl }) => {
  const shown = events.slice(0, 10);
  const { posterHeight, titleSize } = layoutFor(Math.max(shown.length, 1), 1920);

  return (
    <AbsoluteFill>
      <StillBackground backgroundImageUrl={backgroundImageUrl} palette={palette} ambientImageUrl={firstImage(shown)} />

      <AbsoluteFill
        style={{ padding: `${PAD}px ${PAD}px ${CTA_ZONE}px ${PAD}px`, display: 'flex', flexDirection: 'column' }}
      >
        <div style={{ height: HEADER_H, display: 'flex', flexDirection: 'column', justifyContent: 'center' }}>
          <div
            style={{
              fontSize: 22,
              fontWeight: 700,
              letterSpacing: 4,
              color: 'rgba(255,255,255,0.75)',
              fontFamily: FONT_BODY,
              textShadow: '0 2px 10px rgba(0,0,0,0.6)',
            }}
          >
            BUGÜN İSTANBUL&apos;DA
          </div>
          <div
            style={{
              marginTop: 10,
              fontSize: 72,
              color: '#f8fafc',
              fontFamily: FONT_TITLE,
              lineHeight: 1,
              textShadow: '0 4px 24px rgba(0,0,0,0.6)',
            }}
          >
            {dayNameTr(date)}
          </div>
          <div
            style={{
              marginTop: 10,
              fontSize: 28,
              fontWeight: 600,
              color: 'rgba(255,255,255,0.85)',
              fontFamily: FONT_BODY,
              letterSpacing: 1,
              textShadow: '0 2px 10px rgba(0,0,0,0.6)',
            }}
          >
            {formatDateTr(date)} • {shown.length} etkinlik
          </div>
        </div>

        <div style={{ flex: 1, display: 'flex', flexDirection: 'column', justifyContent: 'center', gap: ROW_GAP, paddingTop: 14 }}>
          {shown.map((e) => (
            <EventRow key={e.id} event={e} posterHeight={posterHeight} titleSize={titleSize} />
          ))}
        </div>
      </AbsoluteFill>

      <SaveCta bottom={CTA_BOTTOM} size={24} />
    </AbsoluteFill>
  );
};
