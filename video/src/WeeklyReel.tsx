import React from 'react';
import { AbsoluteFill, Img, Sequence, interpolate, useCurrentFrame, useVideoConfig, spring, Audio, staticFile } from 'remotion';
import { EventCard, EventData } from './EventCard';
import { ReelBackground } from './ReelBackground';
import { FONT_BODY, FONT_TITLE } from './fonts';
import { HOOK_FRAMES, OUTRO_FRAMES, computeCardRanges } from './timing';

export interface WeeklyReelProps {
  events: EventData[];
  weekRange: string;
  backgroundVariant?: number;
}

const LogoIcon: React.FC<{ size?: number; color?: string }> = ({ size = 14, color = 'rgba(167,139,250,0.5)' }) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none">
    <polygon points="12,2 22,8.5 22,15.5 12,22 2,15.5 2,8.5" stroke={color} strokeWidth="1.5" />
  </svg>
);

const DAY_LABELS = ['P', 'S', 'Ç', 'P', 'C', 'C', 'P'];

/**
 * Hafta ilerleme çubuğu: 7 nokta (Pzt-Paz), o an gösterilen kartın günü dolu. Kartların üstünde
 * sabit kalan bir overlay — "haftanı planla" vaadinin sürekli görsel karşılığı.
 */
const WeekProgressBar: React.FC<{ ranges: ReturnType<typeof computeCardRanges>; weekRange: string }> = ({
  ranges,
  weekRange,
}) => {
  const frame = useCurrentFrame();
  const active = ranges.find((r) => frame >= r.from && frame < r.from + r.durationInFrames);
  const activeDayIndex = active?.event.dayIndex ?? -1;

  return (
    <div
      style={{
        position: 'absolute',
        top: 24,
        left: 0,
        right: 0,
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        gap: 8,
        zIndex: 5,
      }}
    >
      <div style={{ display: 'flex', justifyContent: 'center', gap: 14 }}>
        {DAY_LABELS.map((label, i) => {
          const isActive = i === activeDayIndex;
          return (
            <div
              key={i}
              style={{
                width: isActive ? 12 : 8,
                height: isActive ? 12 : 8,
                borderRadius: '50%',
                background: isActive ? '#f9a8d4' : 'rgba(255,255,255,0.35)',
                boxShadow: isActive ? '0 0 12px rgba(249,168,212,0.8)' : 'none',
              }}
            />
          );
        })}
      </div>
      <span style={{ fontSize: 14, color: 'rgba(255,255,255,0.45)', fontFamily: FONT_BODY, letterSpacing: 1 }}>
        {weekRange}
      </span>
    </div>
  );
};

const HookScreen: React.FC<{ event: EventData | undefined; eventCount: number }> = ({ event, eventCount }) => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();

  const zoom = interpolate(frame, [0, HOOK_FRAMES], [1.0, 1.08], { extrapolateRight: 'clamp' });
  const textIn = spring({ frame, fps, config: { damping: 14, stiffness: 160 } });
  const textOpacity = interpolate(textIn, [0, 1], [0, 1]);
  const textY = interpolate(textIn, [0, 1], [24, 0]);
  const fadeOut = interpolate(frame, [HOOK_FRAMES - 8, HOOK_FRAMES], [1, 0], { extrapolateLeft: 'clamp' });

  return (
    <AbsoluteFill style={{ opacity: fadeOut }}>
      {event?.imageUrl ? (
        <AbsoluteFill>
          <Img
            src={event.imageUrl}
            style={{ width: '100%', height: '100%', objectFit: 'cover', transform: `scale(${zoom})` }}
          />
          <AbsoluteFill style={{ background: 'linear-gradient(180deg, rgba(10,8,25,0.35) 0%, rgba(10,8,25,0.75) 100%)' }} />
        </AbsoluteFill>
      ) : (
        <ReelBackground variant={0} />
      )}
      <AbsoluteFill style={{ justifyContent: 'flex-end', alignItems: 'center', paddingBottom: 140 }}>
        <div style={{ opacity: textOpacity, transform: `translateY(${textY}px)`, textAlign: 'center', padding: '0 56px' }}>
          <div
            style={{
              fontSize: 44,
              fontWeight: 900,
              color: '#f8fafc',
              fontFamily: FONT_TITLE,
              lineHeight: 1.2,
              textShadow: '0 4px 30px rgba(0,0,0,0.6)',
            }}
          >
            İstanbul'da bu hafta {eventCount} etkinlik
          </div>
          <div style={{ marginTop: 14, fontSize: 40 }}>👇</div>
        </div>
      </AbsoluteFill>
    </AbsoluteFill>
  );
};

const OutroScreen: React.FC = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();

  const pop = spring({ frame, fps, config: { damping: 12, stiffness: 100 } });
  const opacity = interpolate(pop, [0, 1], [0, 1]);
  const scale = interpolate(pop, [0, 1], [0.92, 1]);

  return (
    <AbsoluteFill
      style={{
        justifyContent: 'center',
        alignItems: 'center',
        flexDirection: 'column',
        opacity,
        transform: `scale(${scale})`,
      }}
    >
      <LogoIcon size={36} color="#a78bfa" />
      <div style={{ marginTop: 20, fontSize: 42, fontWeight: 900, color: '#f8fafc', fontFamily: FONT_TITLE, letterSpacing: 2 }}>
        whatnext
      </div>
      <div style={{ marginTop: 8, fontSize: 20, color: '#a78bfa', fontFamily: FONT_BODY, letterSpacing: 1 }}>
        whatnext.tr
      </div>
      <div style={{ marginTop: 32, height: 1.5, width: 120, background: 'linear-gradient(90deg, transparent, #a78bfa, transparent)' }} />
      <div
        style={{
          marginTop: 28,
          fontSize: 20,
          color: '#f8fafc',
          fontFamily: FONT_BODY,
          letterSpacing: 1,
          background: 'rgba(139,92,246,0.18)',
          padding: '14px 28px',
          borderRadius: 100,
          border: '1px solid rgba(139,92,246,0.35)',
        }}
      >
        🔖 Kaydet — hafta içinde lazım olacak
      </div>
    </AbsoluteFill>
  );
};

export const WeeklyReel: React.FC<WeeklyReelProps> = ({ events, weekRange, backgroundVariant = 0 }) => {
  const { durationInFrames } = useVideoConfig();
  const cardRanges = computeCardRanges(events);
  const outroFrom = HOOK_FRAMES + cardRanges.reduce((acc, r) => acc + r.durationInFrames, 0);
  const hookEvent = events.find((e) => e.featured && e.imageUrl) ?? events.find((e) => e.imageUrl) ?? events[0];

  return (
    <AbsoluteFill>
      <Audio src={staticFile('bg-music.mp3')} loop />

      <ReelBackground variant={backgroundVariant} />

      <Sequence from={0} durationInFrames={HOOK_FRAMES}>
        <HookScreen event={hookEvent} eventCount={events.length} />
      </Sequence>

      {cardRanges.map(({ event, from, durationInFrames: cardDuration }) => (
        <Sequence key={event.id} from={from} durationInFrames={cardDuration}>
          <EventCard event={event} index={0} durationInFrames={cardDuration} />
        </Sequence>
      ))}

      <Sequence from={outroFrom} durationInFrames={Math.max(OUTRO_FRAMES, durationInFrames - outroFrom)}>
        <OutroScreen />
      </Sequence>

      {/* Kartlar boyunca sabit: hafta ilerleme çubuğu */}
      <Sequence from={HOOK_FRAMES} durationInFrames={outroFrom - HOOK_FRAMES}>
        <WeekProgressBar ranges={cardRanges} weekRange={weekRange} />
      </Sequence>

      {/* Sabit footer — tüm video boyunca */}
      <div
        style={{
          position: 'absolute',
          bottom: 0,
          left: 0,
          right: 0,
          height: 56,
          background: 'linear-gradient(180deg, transparent, rgba(0,0,0,0.4))',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          gap: 8,
        }}
      >
        <LogoIcon />
        <span style={{ fontSize: 15, color: 'rgba(255,255,255,0.35)', fontFamily: FONT_BODY, letterSpacing: 1 }}>
          whatnext.tr
        </span>
      </div>
    </AbsoluteFill>
  );
};
