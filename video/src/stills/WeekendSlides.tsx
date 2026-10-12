import React from 'react';
import { AbsoluteFill } from 'remotion';
import { FONT_BODY, FONT_TITLE, useBrandFonts } from '../fonts';
import { ThemeContext, themeFor, useTheme, withAlpha } from '../theme';
import { MetaLine, Poster, StillBackground, StillEventData, TypeTag, firstImage } from './shared';

/** "Bu hafta sonu ne yapsak?" karuselinin son iki slaytı (1080×1350, date = Cuma). */

const PAD = 64;

const Kicker: React.FC<{ text: string }> = ({ text }) => {
  const { accent } = useTheme();
  return (
    <div style={{ fontSize: 22, fontWeight: 800, letterSpacing: 5, color: accent, fontFamily: FONT_BODY }}>{text}</div>
  );
};

export interface PicksStillProps {
  date: string;
  events: (StillEventData & { dayName?: string })[];
  kicker?: string;
  headline?: string;
}

/** "Kaçırma": bir günün en iyi 4 etkinliği, 2×2 büyük afiş (Perşembe karuselinde gün başına bir slayt). */
export const PicksStill: React.FC<PicksStillProps> = ({ date, events, kicker = 'BU HAFTA SONU', headline = 'Kaçırma' }) => {
  useBrandFonts();
  const shown = events.slice(0, 4);
  return (
    <ThemeContext.Provider value={themeFor(date)}>
      <AbsoluteFill>
        <StillBackground ambientImageUrl={firstImage(shown)} />
        <div style={{ position: 'absolute', left: PAD, right: PAD, top: PAD }}>
          <Kicker text={kicker} />
          <div style={{ marginTop: 6, fontSize: 92, lineHeight: 1, color: '#f8fafc', fontFamily: FONT_TITLE }}>
            {headline}
          </div>
        </div>
        <div
          style={{
            position: 'absolute',
            left: PAD,
            right: PAD,
            top: 250,
            bottom: 70,
            display: 'grid',
            gridTemplateColumns: '1fr 1fr',
            gap: '28px 32px',
          }}
        >
          {shown.map((e) => (
            <div key={e.id} style={{ display: 'flex', flexDirection: 'column', gap: 10, minWidth: 0 }}>
              <Poster imageUrl={e.imageUrl} title={e.title} eventType={e.eventType} width={444} height={330} radius={22} />
              <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                <TypeTag eventType={e.eventType} size={20} />
                <span style={{ fontSize: 20, fontWeight: 700, color: 'rgba(255,255,255,0.85)', fontFamily: FONT_BODY }}>
                  {e.dayName}
                </span>
              </div>
              <div
                style={{
                  fontSize: 32,
                  lineHeight: 1.1,
                  color: '#f8fafc',
                  fontFamily: FONT_TITLE,
                  whiteSpace: 'nowrap',
                  overflow: 'hidden',
                  textOverflow: 'ellipsis',
                }}
              >
                {e.title}
              </div>
              <MetaLine startTime={e.startTime} venueName={e.venueName} size={19} />
            </div>
          ))}
        </div>
      </AbsoluteFill>
    </ThemeContext.Provider>
  );
};

/** Kapanış: kaydet + arkadaşını etiketle çağrısı, ay renginde dolu zemin. */
export const CtaStill: React.FC<{ date: string }> = ({ date }) => {
  useBrandFonts();
  const theme = themeFor(date);
  const ink = theme.base;
  const line = (icon: string, text: string, sub: string) => (
    <div style={{ display: 'flex', alignItems: 'center', gap: 36 }}>
      <div
        style={{
          width: 120,
          height: 120,
          borderRadius: 32,
          background: ink,
          color: theme.accent,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          fontSize: 62,
          flexShrink: 0,
        }}
      >
        {icon}
      </div>
      <div>
        <div style={{ fontSize: 58, lineHeight: 1.05, color: ink, fontFamily: FONT_TITLE }}>{text}</div>
        <div style={{ marginTop: 8, fontSize: 28, fontWeight: 700, color: withAlpha(ink, 0.75), fontFamily: FONT_BODY }}>
          {sub}
        </div>
      </div>
    </div>
  );
  return (
    <AbsoluteFill style={{ background: theme.accent, padding: '120px 90px', display: 'flex', flexDirection: 'column' }}>
      <div style={{ fontSize: 26, fontWeight: 800, letterSpacing: 6, color: withAlpha(ink, 0.7), fontFamily: FONT_BODY }}>
        HAFTA SONU PLANIN HAZIR
      </div>
      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', justifyContent: 'center', gap: 70 }}>
        {line('🔖', 'Kaydet', 'Hafta sonu lazım olacak')}
        {line('👯', 'Etiketle', 'Kiminle gidiyorsun?')}
        {line('📲', 'Takip et', "Her sabah 09:00'da günün tam listesi")}
      </div>
      <div style={{ fontSize: 30, fontWeight: 800, color: ink, fontFamily: FONT_BODY, letterSpacing: 1 }}>@whatnext.event</div>
    </AbsoluteFill>
  );
};
