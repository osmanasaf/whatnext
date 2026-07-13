import React from 'react';
import { AbsoluteFill } from 'remotion';
import { FONT_BODY, FONT_TITLE } from '../fonts';
import { Footer, Palette, StillBackground, formatDateTr } from './shared';

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

      <AbsoluteFill style={{ justifyContent: 'center', alignItems: 'center', flexDirection: 'column' }}>
        <div
          style={{
            fontSize: 88,
            fontWeight: 900,
            color: '#ffffff',
            fontFamily: FONT_TITLE,
            textShadow: '0 4px 28px rgba(0,0,0,0.5)',
          }}
        >
          {formatDateTr(date)}
        </div>
        {eventCount > 0 && (
          <div
            style={{
              marginTop: 20,
              fontSize: 30,
              fontWeight: 700,
              color: 'rgba(255,255,255,0.88)',
              fontFamily: FONT_BODY,
              textShadow: '0 2px 16px rgba(0,0,0,0.45)',
            }}
          >
            Bugün {eventCount} etkinlik
          </div>
        )}
      </AbsoluteFill>

      <Footer color="rgba(255,255,255,0.6)" />
    </AbsoluteFill>
  );
};
