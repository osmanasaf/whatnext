import React from 'react';
import { AbsoluteFill, Audio, Img, Sequence, interpolate, spring, staticFile, useCurrentFrame, useVideoConfig } from 'remotion';
import { EventCard, EventData } from './EventCard';
import { ReelBackground } from './ReelBackground';
import { FONT_BODY, FONT_TITLE, useBrandFonts } from './fonts';
import { HOOK_FRAMES, OUTRO_FRAMES, computeCardRanges } from './timing';
import { SaveCta } from './stills/shared';
import { ThemeContext, themeFor, useTheme, withAlpha } from './theme';

export interface WeeklyReelProps {
  events: EventData[];
  weekRange: string;
  backgroundVariant?: number;
}

/** Hook mozaiği: üst yarıya yayılan, hafif eğik afiş yuvaları (afişler kırpılmaz, kutuya sığdırılır). */
const HOOK_SLOTS: { x: number; y: number; rot: number }[] = [
  { x: 300, y: 250, rot: 4 },
  { x: 40, y: 430, rot: -8 },
  { x: 560, y: 520, rot: 7 },
  { x: 120, y: 820, rot: 5 },
  { x: 520, y: 880, rot: -5 },
];
const HOOK_TILE_MAX = 470;

const HookScreen: React.FC<{ events: EventData[]; weekRange: string; variant: number }> = ({ events, weekRange, variant }) => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  const tiles = events.filter((e) => e.imageUrl).slice(0, HOOK_SLOTS.length);
  const textIn = spring({ frame: frame - 6, fps, config: { damping: 16, stiffness: 150 } });
  const { accent, base } = useTheme();

  return (
    <AbsoluteFill>
      <ReelBackground variant={variant} />
      {tiles.map((e, i) => {
        const slot = HOOK_SLOTS[i];
        const pop = spring({ frame: frame - i * 3, fps, config: { damping: 14, stiffness: 160 } });
        return (
          <div
            key={e.id}
            style={{
              position: 'absolute',
              left: slot.x,
              top: slot.y,
              transform: `rotate(${slot.rot}deg) scale(${interpolate(pop, [0, 1], [0.6, 1])})`,
              opacity: interpolate(pop, [0, 1], [0, 1]),
            }}
          >
            <Img
              src={e.imageUrl as string}
              style={{
                maxWidth: HOOK_TILE_MAX,
                maxHeight: HOOK_TILE_MAX,
                width: 'auto',
                height: 'auto',
                display: 'block',
                borderRadius: 22,
                border: '1px solid rgba(255,255,255,0.18)',
                boxShadow: '0 24px 60px rgba(0,0,0,0.6)',
              }}
            />
          </div>
        );
      })}

      {/* Alt panel ay renginde DOLU (kapak görseliyle aynı dil) — Reel profil ızgarasında da ayın rengini taşır. */}
      <div
        style={{
          position: 'absolute',
          left: 0,
          right: 0,
          top: 1080,
          bottom: 0,
          background: accent,
          clipPath: 'polygon(0 10%, 100% 0, 100% 100%, 0 100%)',
        }}
      />

      <div
        style={{
          position: 'absolute',
          left: 72,
          right: 72,
          top: 1200,
          opacity: interpolate(textIn, [0, 1], [0, 1]),
          transform: `translateY(${interpolate(textIn, [0, 1], [40, 0])}px)`,
        }}
      >
        <div style={{ fontSize: 32, fontWeight: 700, letterSpacing: 6, color: withAlpha(base, 0.8), fontFamily: FONT_BODY }}>
          İSTANBUL • {weekRange.toLocaleUpperCase('tr')}
        </div>
        <div
          style={{
            marginTop: 18,
            fontSize: 120,
            lineHeight: 1.02,
            color: base,
            fontFamily: FONT_TITLE,
          }}
        >
          Bu hafta
          <br />
          kaçırma
        </div>
        <div style={{ marginTop: 22, fontSize: 40, fontWeight: 700, color: withAlpha(base, 0.85), fontFamily: FONT_BODY }}>
          {events.length} öne çıkan etkinlik
        </div>
      </div>
    </AbsoluteFill>
  );
};

const OutroScreen: React.FC<{ variant: number }> = ({ variant }) => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  const pop = spring({ frame, fps, config: { damping: 13, stiffness: 120 } });
  const { accent } = useTheme();

  return (
    <AbsoluteFill>
      <ReelBackground variant={variant} />
      <AbsoluteFill
        style={{
          justifyContent: 'center',
          alignItems: 'center',
          flexDirection: 'column',
          paddingBottom: 200,
          opacity: interpolate(pop, [0, 1], [0, 1]),
          transform: `scale(${interpolate(pop, [0, 1], [0.92, 1])})`,
        }}
      >
        <svg width={120} height={120} viewBox="0 0 24 24" fill="none">
          <polygon points="12,2 22,8.5 22,15.5 12,22 2,15.5 2,8.5" stroke={accent} strokeWidth="1.4" />
        </svg>
        <div style={{ marginTop: 28, fontSize: 110, color: '#f8fafc', fontFamily: FONT_TITLE, letterSpacing: 2 }}>whatnext</div>
        <div style={{ marginTop: 8, fontSize: 44, color: accent, fontFamily: FONT_BODY, fontWeight: 600, letterSpacing: 2 }}>
          whatnext.tr
        </div>
        <div
          style={{
            marginTop: 64,
            fontSize: 46,
            lineHeight: 1.3,
            textAlign: 'center',
            color: '#f8fafc',
            fontFamily: FONT_BODY,
            fontWeight: 700,
          }}
        >
          Kaydet,
          <br />
          hafta içinde lazım olacak
        </div>
      </AbsoluteFill>
      <SaveCta bottom={470} size={38} />
    </AbsoluteFill>
  );
};

export const WeeklyReel: React.FC<WeeklyReelProps> = ({ events, weekRange, backgroundVariant = 0 }) => {
  useBrandFonts();
  const { durationInFrames } = useVideoConfig();
  const cardRanges = computeCardRanges(events);
  const outroFrom = HOOK_FRAMES + cardRanges.reduce((acc, r) => acc + r.durationInFrames, 0);
  const theme = themeFor(events[0]?.startDate, false);

  return (
    <ThemeContext.Provider value={theme}>
    <AbsoluteFill style={{ background: theme.base }}>
      <Audio src={staticFile('bg-music.mp3')} loop />

      <Sequence from={0} durationInFrames={HOOK_FRAMES}>
        <HookScreen events={events} weekRange={weekRange} variant={backgroundVariant} />
      </Sequence>

      {cardRanges.map(({ event, from, durationInFrames: cardDuration }) => (
        <Sequence key={`${event.id}-${from}`} from={from} durationInFrames={cardDuration}>
          <EventCard event={event} durationInFrames={cardDuration} />
        </Sequence>
      ))}

      <Sequence from={outroFrom} durationInFrames={Math.max(OUTRO_FRAMES, durationInFrames - outroFrom)}>
        <OutroScreen variant={backgroundVariant} />
      </Sequence>
    </AbsoluteFill>
    </ThemeContext.Provider>
  );
};
