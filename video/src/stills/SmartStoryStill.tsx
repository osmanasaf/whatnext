import React from 'react';
import { AbsoluteFill } from 'remotion';
import { FONT_BODY, FONT_TITLE } from '../fonts';
import { Footer, Palette, StillBackground, StillEventData, fontSizeForCount, dayNameTr, formatDateTr } from './shared';

export interface SmartStoryStillProps {
  monthName: string;
  palette: Palette;
  date: string;
  events: StillEventData[];
  backgroundImageUrl?: string;
}

export const SmartStoryStill: React.FC<SmartStoryStillProps> = ({ palette, date, events, backgroundImageUrl }) => {
  const shown = events.slice(0, 10);
  const titleSize = fontSizeForCount(shown.length, { few: 34, some: 28, many: 22 });
  const metaSize = Math.round(titleSize * 0.5);

  return (
    <AbsoluteFill>
      <StillBackground backgroundImageUrl={backgroundImageUrl} palette={palette} />

      <AbsoluteFill style={{ padding: '110px 68px 170px 68px', display: 'flex', flexDirection: 'column' }}>
        <div style={{ alignSelf: 'center', textAlign: 'center' }}>
          <div
            style={{
              fontSize: 30,
              fontWeight: 900,
              color: '#ffffff',
              fontFamily: FONT_TITLE,
              textShadow: '0 2px 16px rgba(0,0,0,0.45)',
            }}
          >
            Bugün İstanbul'da
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
            {dayNameTr(date)} • {formatDateTr(date)}
          </div>
        </div>

        <div
          style={{
            flex: 1,
            display: 'flex',
            flexDirection: 'column',
            justifyContent: 'center',
            gap: 22,
            marginTop: 24,
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
                  lineHeight: 1.2,
                  textShadow: '0 2px 16px rgba(0,0,0,0.5)',
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
                    marginTop: 3,
                    textShadow: '0 2px 8px rgba(0,0,0,0.4)',
                  }}
                >
                  {[e.startTime, e.venueName ? '@' + e.venueName : null].filter(Boolean).join(' • ')}
                </div>
              )}
            </div>
          ))}
        </div>
      </AbsoluteFill>

      {/* Instagram story yanıt çubuğu güvenli bölgesi — alttan ~%12 */}
      <div
        style={{
          position: 'absolute',
          left: 0,
          right: 0,
          bottom: 210,
          display: 'flex',
          justifyContent: 'center',
        }}
      >
        <div
          style={{
            background: 'rgba(20,20,20,0.55)',
            borderRadius: 100,
            padding: '12px 24px',
            fontSize: 22,
            fontWeight: 700,
            color: '#ffffff',
            fontFamily: FONT_BODY,
            letterSpacing: 1,
          }}
        >
          KAYDET • whatnext.tr
        </div>
      </div>

      <Footer color="rgba(255,255,255,0.6)" />
    </AbsoluteFill>
  );
};
