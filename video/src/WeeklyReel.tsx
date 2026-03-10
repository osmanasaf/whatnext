import React from 'react';
import { AbsoluteFill, Sequence, interpolate, useCurrentFrame, useVideoConfig, spring, Audio, staticFile } from 'remotion';
import { EventCard, EventData } from './EventCard';
import { ReelBackground } from './ReelBackground';

export interface WeeklyReelProps {
  events: EventData[];
  weekRange: string;
  backgroundVariant?: number;
}

const FRAMES_PER_CARD = 90;
const INTRO_FRAMES = 75;
const OUTRO_FRAMES = 60;

const LogoIcon = () => (
  <svg width="36" height="36" viewBox="0 0 24 24" fill="none">
    <polygon points="12,2 22,8.5 22,15.5 12,22 2,15.5 2,8.5" stroke="#a78bfa" strokeWidth="1.5" fill="rgba(139,92,246,0.15)" />
    <path d="M12 7v5l3 3" stroke="#a78bfa" strokeWidth="1.8" strokeLinecap="round" />
  </svg>
);

const IntroScreen: React.FC<{ weekRange: string }> = ({ weekRange }) => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();

  // Better entrance physics
  const titleDrop = spring({ frame, fps, config: { damping: 14, stiffness: 120 } });
  const titleY = interpolate(titleDrop, [0, 1], [50, 0]);
  const titleOpacity = interpolate(titleDrop, [0, 1], [0, 1]);

  const subDrop = spring({ frame: Math.max(0, frame - 10), fps, config: { damping: 14, stiffness: 100 } });
  const subOpacity = interpolate(subDrop, [0, 1], [0, 1]);
  const subY = interpolate(subDrop, [0, 1], [30, 0]);

  const lineScale = spring({ frame: Math.max(0, frame - 20), fps, config: { damping: 15 } });
  const badgeOpacity = interpolate(frame, [30, 50], [0, 1], { extrapolateRight: 'clamp' });

  return (
    <AbsoluteFill style={{
      justifyContent: 'center',
      alignItems: 'center',
      flexDirection: 'column',
      padding: 56,
    }}>
      {/* Üst logo + marka */}
      <div style={{
        position: 'absolute', top: 52, left: 0, right: 0,
        display: 'flex', justifyContent: 'center', alignItems: 'center', gap: 12,
        opacity: badgeOpacity,
      }}>
        <LogoIcon />
        <span style={{
          fontSize: 22,
          fontWeight: 700,
          color: '#a78bfa',
          fontFamily: 'system-ui, sans-serif',
          letterSpacing: 3,
          textTransform: 'uppercase',
        }}>
          whatnext
        </span>
      </div>

      {/* Ana başlık */}
      <div style={{
        opacity: titleOpacity,
        transform: `translateY(${titleY}px)`,
        textAlign: 'center',
      }}>
        <div style={{
          fontSize: 20,
          fontWeight: 600,
          color: '#a78bfa',
          letterSpacing: 6,
          textTransform: 'uppercase',
          fontFamily: 'system-ui, sans-serif',
          marginBottom: 16,
        }}>
          Bu Hafta
        </div>
        <h1 style={{
          fontSize: 72,
          fontWeight: 900,
          color: '#f8fafc',
          margin: 0,
          fontFamily: 'system-ui, sans-serif',
          lineHeight: 1.1,
          textShadow: '0 4px 30px rgba(139,92,246,0.4)',
        }}>
          İstanbul
        </h1>
      </div>

      {/* Ayırıcı çizgi */}
      <div style={{
        marginTop: 28,
        height: 2,
        width: 200,
        borderRadius: 1,
        background: 'linear-gradient(90deg, transparent, #a78bfa, transparent)',
        transform: `scaleX(${lineScale})`,
      }} />

      {/* Tarih aralığı */}
      <div style={{
        marginTop: 24,
        opacity: subOpacity,
        transform: `translateY(${subY}px)`,
        display: 'flex',
        alignItems: 'center',
        gap: 12,
      }}>
        <div style={{ width: 32, height: 1, background: 'rgba(167,139,250,0.4)' }} />
        <span style={{
          fontSize: 28,
          color: '#c4b5fd',
          fontFamily: 'system-ui, sans-serif',
          fontWeight: 500,
          letterSpacing: 1,
        }}>
          {weekRange}
        </span>
        <div style={{ width: 32, height: 1, background: 'rgba(167,139,250,0.4)' }} />
      </div>

      {/* Alt etiketler - reduced gap and padding to fit safe zone */}
      <div style={{
        position: 'absolute', bottom: 52,
        display: 'flex', gap: 12,
        opacity: badgeOpacity,
      }}>
        {[
          { label: 'KONSER', color: '#f9a8d4', border: 'rgba(244,114,182,0.35)' },
          { label: 'TİYATRO', color: '#c4b5fd', border: 'rgba(139,92,246,0.35)' },
          { label: 'STAND-UP', color: '#6ee7b7', border: 'rgba(52,211,153,0.35)' },
        ].map(t => (
          <div key={t.label} style={{
            padding: '8px 14px',
            borderRadius: 100,
            border: `1.5px solid ${t.border}`,
            background: `${t.border}`,
          }}>
            <span style={{
              fontSize: 14,
              fontWeight: 700,
              letterSpacing: 2,
              color: t.color,
              fontFamily: 'system-ui, sans-serif',
            }}>
              {t.label}
            </span>
          </div>
        ))}
      </div>
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
    <AbsoluteFill style={{
      justifyContent: 'center',
      alignItems: 'center',
      flexDirection: 'column',
      opacity,
      transform: `scale(${scale})`,
    }}>
      <LogoIcon />
      <div style={{
        marginTop: 20,
        fontSize: 42,
        fontWeight: 900,
        color: '#f8fafc',
        fontFamily: 'system-ui, sans-serif',
        letterSpacing: 2,
      }}>
        whatnext
      </div>
      <div style={{
        marginTop: 8,
        fontSize: 20,
        color: '#a78bfa',
        fontFamily: 'system-ui, sans-serif',
        letterSpacing: 1,
      }}>
        whatnext.tr
      </div>
      <div style={{
        marginTop: 32,
        height: 1.5,
        width: 120,
        background: 'linear-gradient(90deg, transparent, #a78bfa, transparent)',
      }} />
      <div style={{
        marginTop: 20,
        fontSize: 24,
        fontWeight: 700,
        color: '#f8fafc',
        fontFamily: 'system-ui, sans-serif',
      }}>
        Hemen Biletini Al!
      </div>

      <div style={{
        marginTop: 20,
        fontSize: 18,
        color: '#64748b',
        fontFamily: 'system-ui, sans-serif',
        letterSpacing: 1,
        background: 'rgba(139,92,246,0.15)',
        padding: '12px 24px',
        borderRadius: 100,
        border: '1px solid rgba(139,92,246,0.3)'
      }}>
        Detaylar profildeki linkte 🔗
      </div>
    </AbsoluteFill>
  );
};

export const WeeklyReel: React.FC<WeeklyReelProps> = ({ events, weekRange, backgroundVariant = 0 }) => {
  const { durationInFrames } = useVideoConfig();
  return (
    <AbsoluteFill>
      <Audio src={staticFile('bg-music.mp3')} loop />

      <ReelBackground variant={backgroundVariant} />

      {/* Intro */}
      <Sequence from={0} durationInFrames={INTRO_FRAMES}>
        <IntroScreen weekRange={weekRange} />
      </Sequence>

      {/* Etkinlik kartları */}
      {events.map((event, i) => (
        <Sequence
          key={event.id}
          from={INTRO_FRAMES + i * FRAMES_PER_CARD}
          durationInFrames={FRAMES_PER_CARD}
        >
          <EventCard event={event} index={i} />
        </Sequence>
      ))}

      {/* Outro */}
      <Sequence from={INTRO_FRAMES + events.length * FRAMES_PER_CARD} durationInFrames={OUTRO_FRAMES}>
        <OutroScreen />
      </Sequence>

      {/* Sabit footer — tüm video boyunca */}
      <div style={{
        position: 'absolute',
        bottom: 0, left: 0, right: 0,
        height: 56,
        background: 'linear-gradient(180deg, transparent, rgba(0,0,0,0.4))',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        gap: 8,
      }}>
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
          <polygon points="12,2 22,8.5 22,15.5 12,22 2,15.5 2,8.5" stroke="rgba(167,139,250,0.5)" strokeWidth="1.5" />
        </svg>
        <span style={{
          fontSize: 15,
          color: 'rgba(255,255,255,0.35)',
          fontFamily: 'system-ui, sans-serif',
          letterSpacing: 1,
        }}>
          whatnext.tr
        </span>
      </div>
    </AbsoluteFill>
  );
};
