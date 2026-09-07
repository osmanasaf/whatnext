import React from 'react';
import { AbsoluteFill } from 'remotion';
import { FONT_BODY, FONT_TITLE } from '../fonts';
import { Palette, SaveCta, StillBackground, dayNameTr, formatDateTr } from './shared';

export interface CoverStillProps {
  monthName: string;
  palette: Palette;
  date: string;
  eventCount: number;
  backgroundImageUrl?: string;
}

export const CoverStill: React.FC<CoverStillProps> = ({ palette, date, eventCount, backgroundImageUrl }) => {
  return (
    <AbsoluteFill>
      <StillBackground backgroundImageUrl={backgroundImageUrl} palette={palette} />

      {/* Arka planda büyük, silik "İSTANBUL" — kapağı boş bırakmayan tipografik doku */}
      <AbsoluteFill style={{ justifyContent: 'center', alignItems: 'center', overflow: 'hidden' }}>
        <div
          style={{
            fontSize: 150,
            color: 'rgba(255,255,255,0.05)',
            fontFamily: FONT_TITLE,
            letterSpacing: 6,
            transform: 'rotate(-90deg)',
            whiteSpace: 'nowrap',
          }}
        >
          İSTANBUL
        </div>
      </AbsoluteFill>

      <AbsoluteFill style={{ justifyContent: 'center', alignItems: 'center', flexDirection: 'column' }}>
        <div
          style={{
            fontSize: 24,
            fontWeight: 700,
            letterSpacing: 5,
            color: 'rgba(255,255,255,0.75)',
            fontFamily: FONT_BODY,
            textShadow: '0 2px 10px rgba(0,0,0,0.6)',
          }}
        >
          BUGÜN İSTANBUL&apos;DA
        </div>
        <div
          style={{
            marginTop: 26,
            fontSize: 132,
            color: '#f8fafc',
            fontFamily: FONT_TITLE,
            lineHeight: 1,
            textShadow: '0 6px 36px rgba(0,0,0,0.6)',
          }}
        >
          {dayNameTr(date)}
        </div>
        <div
          style={{
            marginTop: 18,
            fontSize: 48,
            color: 'rgba(255,255,255,0.9)',
            fontFamily: FONT_TITLE,
            textShadow: '0 2px 20px rgba(0,0,0,0.6)',
          }}
        >
          {formatDateTr(date)}
        </div>
        {eventCount > 0 && (
          <div
            style={{
              marginTop: 44,
              background: 'rgba(255,255,255,0.1)',
              border: '1px solid rgba(255,255,255,0.18)',
              borderRadius: 100,
              padding: '16px 34px',
              fontSize: 30,
              fontWeight: 700,
              color: '#ffffff',
              fontFamily: FONT_BODY,
              letterSpacing: 1,
            }}
          >
            {eventCount} etkinlik
          </div>
        )}
      </AbsoluteFill>

      <SaveCta bottom={56} />
    </AbsoluteFill>
  );
};
