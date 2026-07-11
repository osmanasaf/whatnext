import React from 'react';
import { AbsoluteFill, Img, interpolate, useCurrentFrame, useVideoConfig, spring } from 'remotion';
import { FONT_BODY, FONT_TITLE } from './fonts';

export interface EventData {
  id: number;
  title: string;
  startDate: string;
  dayName?: string;
  dayIndex?: number;
  startTime?: string;
  type: string;
  venueName?: string;
  imageUrl?: string | null;
  featured?: boolean;
}

const FADE_DURATION = 12;

const TYPE_CONFIG: Record<string, { label: string; color: string; border: string; icon: React.ReactNode }> = {
  CONCERT: {
    label: 'KONSER',
    color: '#f9a8d4',
    border: 'rgba(244,114,182,0.5)',
    icon: (
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
        <path d="M9 18V5l12-2v13" stroke="#f9a8d4" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
        <circle cx="6" cy="18" r="3" stroke="#f9a8d4" strokeWidth="2" />
        <circle cx="18" cy="16" r="3" stroke="#f9a8d4" strokeWidth="2" />
      </svg>
    ),
  },
  THEATER: {
    label: 'TİYATRO',
    color: '#c4b5fd',
    border: 'rgba(139,92,246,0.5)',
    icon: (
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
        <circle cx="9" cy="12" r="4" stroke="#c4b5fd" strokeWidth="2" />
        <circle cx="15" cy="12" r="4" stroke="#c4b5fd" strokeWidth="2" />
        <path d="M6 20c0-2.21 1.343-4 3-4h6c1.657 0 3 1.79 3 4" stroke="#c4b5fd" strokeWidth="2" strokeLinecap="round" />
      </svg>
    ),
  },
  STANDUP: {
    label: 'STAND-UP',
    color: '#6ee7b7',
    border: 'rgba(52,211,153,0.5)',
    icon: (
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
        <circle cx="12" cy="5" r="2.5" stroke="#6ee7b7" strokeWidth="2" />
        <path d="M12 8v5M9 11l-2 4M15 11l2 4M10 13h4" stroke="#6ee7b7" strokeWidth="2" strokeLinecap="round" />
        <path d="M8 21l2-4h4l2 4" stroke="#6ee7b7" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
      </svg>
    ),
  },
};

const ClockIcon = () => (
  <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
    <circle cx="12" cy="12" r="9" stroke="#e5e7eb" strokeWidth="1.8" />
    <path d="M12 7v5l3.5 2" stroke="#e5e7eb" strokeWidth="1.8" strokeLinecap="round" />
  </svg>
);

const LocationIcon = () => (
  <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
    <path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7z" stroke="#e5e7eb" strokeWidth="1.8" />
    <circle cx="12" cy="9" r="2.5" stroke="#e5e7eb" strokeWidth="1.8" />
  </svg>
);

const DAY_LABELS = ['PZT', 'SAL', 'ÇAR', 'PER', 'CUM', 'CMT', 'PAZ'];

export const EventCard: React.FC<{ event: EventData; index: number; durationInFrames: number }> = ({
  event,
  durationInFrames,
}) => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();

  const fadeIn = interpolate(frame, [0, FADE_DURATION], [0, 1], { extrapolateRight: 'clamp' });
  const fadeOut = interpolate(frame, [durationInFrames - FADE_DURATION, durationInFrames], [1, 0], {
    extrapolateLeft: 'clamp',
  });
  const opacity = Math.min(fadeIn, fadeOut);

  const pop = spring({ frame, fps, config: { damping: 16, stiffness: 140 } });
  const scale = interpolate(pop, [0, 1], [1.03, 1]);

  // Ken Burns: tüm kart boyunca yavaş zoom.
  const imageZoom = interpolate(frame, [0, durationInFrames], [1.05, 1.16], { extrapolateRight: 'clamp' });

  const cfg = TYPE_CONFIG[event.type] ?? TYPE_CONFIG.CONCERT;
  const dayLabel = typeof event.dayIndex === 'number' && event.dayIndex >= 0 ? DAY_LABELS[event.dayIndex] : null;

  const formatDate = (d: string) => {
    const parts = d.split('-');
    if (parts.length < 3) return d;
    const [, m, day] = parts;
    const months = ['Oca', 'Şub', 'Mar', 'Nis', 'May', 'Haz', 'Tem', 'Ağu', 'Eyl', 'Eki', 'Kas', 'Ara'];
    return `${parseInt(day, 10)} ${months[parseInt(m, 10) - 1]}`;
  };

  return (
    <AbsoluteFill style={{ opacity, transform: `scale(${scale})` }}>
      {/* Tam ekran görsel — yoksa tür renginde düz gradyan zemin */}
      {event.imageUrl ? (
        <AbsoluteFill>
          <Img
            src={event.imageUrl}
            style={{ width: '100%', height: '100%', objectFit: 'cover', transform: `scale(${imageZoom})` }}
          />
          <AbsoluteFill
            style={{
              background:
                'linear-gradient(180deg, rgba(10,8,25,0.55) 0%, rgba(10,8,25,0.15) 28%, rgba(10,8,25,0.35) 60%, rgba(10,8,25,0.92) 100%)',
            }}
          />
        </AbsoluteFill>
      ) : (
        <AbsoluteFill
          style={{
            background: `linear-gradient(160deg, rgba(15,12,41,1) 0%, ${cfg.border} 300%)`,
          }}
        />
      )}

      {/* Sol üst: gün rozeti */}
      <div style={{ position: 'absolute', top: 64, left: 56 }}>
        {dayLabel && (
          <div
            style={{
              fontSize: 72,
              fontWeight: 900,
              color: '#f8fafc',
              fontFamily: FONT_TITLE,
              lineHeight: 1,
              textShadow: '0 4px 24px rgba(0,0,0,0.6)',
            }}
          >
            {dayLabel}
          </div>
        )}
        <div
          style={{
            marginTop: 8,
            fontSize: 26,
            color: '#e5e7eb',
            fontFamily: FONT_BODY,
            fontWeight: 500,
            display: 'flex',
            alignItems: 'center',
            gap: 8,
            textShadow: '0 2px 10px rgba(0,0,0,0.6)',
          }}
        >
          <span>{formatDate(event.startDate)}</span>
          {event.startTime && (
            <>
              <ClockIcon />
              <span>{event.startTime}</span>
            </>
          )}
        </div>
      </div>

      {/* Sağ üst: tür rozeti */}
      <div
        style={{
          position: 'absolute',
          top: 64,
          right: 56,
          display: 'inline-flex',
          alignItems: 'center',
          gap: 8,
          background: 'rgba(15,12,41,0.55)',
          border: `1.5px solid ${cfg.border}`,
          borderRadius: 100,
          padding: '10px 18px',
        }}
      >
        {cfg.icon}
        <span
          style={{
            fontSize: 15,
            fontWeight: 800,
            letterSpacing: 2,
            color: cfg.color,
            fontFamily: FONT_BODY,
          }}
        >
          {cfg.label}
        </span>
      </div>

      {/* Alt: başlık + mekân */}
      <div style={{ position: 'absolute', left: 56, right: 56, bottom: 120 }}>
        <h1
          style={{
            fontSize: event.title.length > 34 ? 46 : 58,
            fontWeight: 900,
            color: '#f8fafc',
            margin: '0 0 16px 0',
            lineHeight: 1.15,
            fontFamily: FONT_TITLE,
            textShadow: '0 2px 24px rgba(0,0,0,0.7)',
          }}
        >
          {event.title}
        </h1>
        {event.venueName && (
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <LocationIcon />
            <span
              style={{
                fontSize: 24,
                color: '#e5e7eb',
                fontFamily: FONT_BODY,
                fontWeight: 500,
                textShadow: '0 2px 10px rgba(0,0,0,0.6)',
              }}
            >
              {event.venueName}
            </span>
          </div>
        )}
      </div>
    </AbsoluteFill>
  );
};
