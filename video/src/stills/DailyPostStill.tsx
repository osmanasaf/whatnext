import React from 'react';
import { AbsoluteFill } from 'remotion';
import { FONT_BODY, FONT_TITLE } from '../fonts';
import { Footer, Palette, StillBackground, StillEventData, TYPE_LABELS, fontSizeForCount, formatDateTr } from './shared';

export interface DailyPostStillProps {
  eventType: string;
  monthName: string;
  palette: Palette;
  date: string;
  events: StillEventData[];
  backgroundImageUrl?: string;
}

export const DailyPostStill: React.FC<DailyPostStillProps> = ({
  eventType,
  palette,
  date,
  events,
  backgroundImageUrl,
}) => {
  const label = TYPE_LABELS[eventType] ?? eventType.toUpperCase();
  const shown = events.slice(0, 6);
  const titleSize = fontSizeForCount(shown.length, { few: 40, some: 34, many: 28 });
  const metaSize = Math.round(titleSize * 0.56);

  return (
    <AbsoluteFill>
      <StillBackground backgroundImageUrl={backgroundImageUrl} palette={palette} />

      <AbsoluteFill style={{ padding: '84px 76px 110px 76px', display: 'flex', flexDirection: 'column' }}>
        <div style={{ alignSelf: 'center', textAlign: 'center' }}>
          <div
            style={{
              fontSize: 28,
              fontWeight: 800,
              letterSpacing: 5,
              color: '#ffffff',
              fontFamily: FONT_BODY,
              textShadow: '0 2px 16px rgba(0,0,0,0.45)',
            }}
          >
            {label}
          </div>
          <div
            style={{
              fontSize: 20,
              color: 'rgba(255,255,255,0.8)',
              fontFamily: FONT_BODY,
              marginTop: 6,
              textShadow: '0 2px 12px rgba(0,0,0,0.4)',
            }}
          >
            {formatDateTr(date)}
          </div>
        </div>

        <div
          style={{
            flex: 1,
            display: 'flex',
            flexDirection: 'column',
            justifyContent: 'center',
            gap: 30,
            marginTop: 20,
          }}
        >
          {shown.map((e) => (
            <div key={e.id} style={{ display: 'flex', flexDirection: 'column' }}>
              <div
                style={{
                  fontSize: titleSize,
                  fontWeight: 900,
                  color: '#ffffff',
                  fontFamily: FONT_TITLE,
                  lineHeight: 1.22,
                  textShadow: '0 2px 18px rgba(0,0,0,0.5)',
                  display: '-webkit-box',
                  WebkitLineClamp: 2,
                  WebkitBoxOrient: 'vertical' as const,
                  overflow: 'hidden',
                }}
              >
                {e.title}
              </div>
              {(e.venueName || e.startTime) && (
                <div
                  style={{
                    fontSize: metaSize,
                    color: 'rgba(255,255,255,0.82)',
                    fontFamily: FONT_BODY,
                    marginTop: 4,
                    textShadow: '0 2px 10px rgba(0,0,0,0.4)',
                  }}
                >
                  {[e.startTime, e.venueName ? '@' + e.venueName : null].filter(Boolean).join(' • ')}
                </div>
              )}
            </div>
          ))}
        </div>
      </AbsoluteFill>

      <Footer color="rgba(255,255,255,0.6)" />
    </AbsoluteFill>
  );
};
