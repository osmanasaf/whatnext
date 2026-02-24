import React from 'react';
import { AbsoluteFill, Img, interpolate, useCurrentFrame } from 'remotion';

export interface EventData {
  id: number;
  title: string;
  startDate: string;
  type: string;
  venueName?: string;
  imageUrl?: string | null;
}

const CARD_DURATION = 90;
const FADE_DURATION = 15;

const TYPE_CONFIG: Record<string, { label: string; color: string; bg: string; border: string; icon: React.ReactNode }> = {
  CONCERT: {
    label: 'KONSER',
    color: '#f9a8d4',
    bg: 'rgba(244,114,182,0.12)',
    border: 'rgba(244,114,182,0.4)',
    icon: (
      <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
        <path d="M9 18V5l12-2v13" stroke="#f9a8d4" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
        <circle cx="6" cy="18" r="3" stroke="#f9a8d4" strokeWidth="2"/>
        <circle cx="18" cy="16" r="3" stroke="#f9a8d4" strokeWidth="2"/>
      </svg>
    ),
  },
  THEATER: {
    label: 'TİYATRO',
    color: '#c4b5fd',
    bg: 'rgba(139,92,246,0.12)',
    border: 'rgba(139,92,246,0.4)',
    icon: (
      <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
        <circle cx="9" cy="12" r="4" stroke="#c4b5fd" strokeWidth="2"/>
        <circle cx="15" cy="12" r="4" stroke="#c4b5fd" strokeWidth="2"/>
        <path d="M6 20c0-2.21 1.343-4 3-4h6c1.657 0 3 1.79 3 4" stroke="#c4b5fd" strokeWidth="2" strokeLinecap="round"/>
      </svg>
    ),
  },
  STANDUP: {
    label: 'STAND-UP',
    color: '#6ee7b7',
    bg: 'rgba(52,211,153,0.12)',
    border: 'rgba(52,211,153,0.4)',
    icon: (
      <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
        <circle cx="12" cy="5" r="2.5" stroke="#6ee7b7" strokeWidth="2"/>
        <path d="M12 8v5M9 11l-2 4M15 11l2 4M10 13h4" stroke="#6ee7b7" strokeWidth="2" strokeLinecap="round"/>
        <path d="M8 21l2-4h4l2 4" stroke="#6ee7b7" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
      </svg>
    ),
  },
};

const CalendarIcon = () => (
  <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
    <rect x="3" y="4" width="18" height="18" rx="3" stroke="#94a3b8" strokeWidth="1.8"/>
    <path d="M3 9h18" stroke="#94a3b8" strokeWidth="1.8"/>
    <path d="M8 2v4M16 2v4" stroke="#94a3b8" strokeWidth="1.8" strokeLinecap="round"/>
  </svg>
);

const LocationIcon = () => (
  <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
    <path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7z" stroke="#94a3b8" strokeWidth="1.8"/>
    <circle cx="12" cy="9" r="2.5" stroke="#94a3b8" strokeWidth="1.8"/>
  </svg>
);

export const EventCard: React.FC<{ event: EventData; index: number }> = ({ event }) => {
  const frame = useCurrentFrame();

  const fadeIn = interpolate(frame, [0, FADE_DURATION], [0, 1], { extrapolateRight: 'clamp' });
  const fadeOut = interpolate(frame, [CARD_DURATION - FADE_DURATION, CARD_DURATION], [1, 0], { extrapolateLeft: 'clamp' });
  const opacity = Math.min(fadeIn, fadeOut);

  const slideY = interpolate(frame, [0, FADE_DURATION], [40, 0], { extrapolateRight: 'clamp' });
  const scale = interpolate(frame, [0, FADE_DURATION], [0.96, 1], { extrapolateRight: 'clamp' });

  const cfg = TYPE_CONFIG[event.type] ?? TYPE_CONFIG.CONCERT;

  const formatDate = (d: string) => {
    const [, m, day] = d.split('-');
    const months = ['Ocak', 'Şubat', 'Mart', 'Nisan', 'Mayıs', 'Haziran', 'Temmuz', 'Ağustos', 'Eylül', 'Ekim', 'Kasım', 'Aralık'];
    return `${parseInt(day)} ${months[parseInt(m) - 1]}`;
  };

  return (
    <AbsoluteFill style={{ opacity, transform: `translateY(${slideY}px) scale(${scale})` }}>
      <div style={{
        position: 'absolute', inset: 0,
        background: 'linear-gradient(180deg, rgba(15,12,41,0.75) 0%, rgba(15,12,41,0.95) 100%)',
        backdropFilter: 'blur(2px)',
      }} />

      <div style={{
        position: 'absolute', inset: 0,
        display: 'flex',
        flexDirection: event.imageUrl ? 'row' : 'column',
        alignItems: 'center',
        justifyContent: 'center',
        padding: event.imageUrl ? '48px 40px' : '60px 56px',
        gap: 40,
      }}>

        {/* Sol: metin içerik */}
        <div style={{
          flex: 1,
          display: 'flex',
          flexDirection: 'column',
          justifyContent: 'center',
          gap: 0,
        }}>

          {/* Tür badge */}
          <div style={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: 10,
            background: cfg.bg,
            border: `1.5px solid ${cfg.border}`,
            borderRadius: 100,
            padding: '10px 22px',
            marginBottom: 28,
            alignSelf: 'flex-start',
          }}>
            {cfg.icon}
            <span style={{
              fontSize: 18,
              fontWeight: 800,
              letterSpacing: 3,
              color: cfg.color,
              fontFamily: 'system-ui, sans-serif',
            }}>
              {cfg.label}
            </span>
          </div>

          {/* Başlık */}
          <h1 style={{
            fontSize: event.imageUrl ? 44 : 58,
            fontWeight: 900,
            color: '#f8fafc',
            margin: '0 0 24px 0',
            lineHeight: 1.2,
            fontFamily: 'system-ui, sans-serif',
            textShadow: '0 2px 20px rgba(0,0,0,0.5)',
          }}>
            {event.title}
          </h1>

          {/* Mekan */}
          {event.venueName && (
            <div style={{
              display: 'flex',
              alignItems: 'center',
              gap: 10,
              marginBottom: 16,
            }}>
              <LocationIcon />
              <span style={{
                fontSize: 24,
                color: '#cbd5e1',
                fontFamily: 'system-ui, sans-serif',
                fontWeight: 500,
              }}>
                {event.venueName}
              </span>
            </div>
          )}

          {/* Tarih */}
          <div style={{
            display: 'flex',
            alignItems: 'center',
            gap: 10,
          }}>
            <CalendarIcon />
            <span style={{
              fontSize: 22,
              color: '#94a3b8',
              fontFamily: 'system-ui, sans-serif',
            }}>
              {formatDate(event.startDate)}
            </span>
          </div>

          {/* Alt çizgi vurgusu */}
          <div style={{
            marginTop: 32,
            height: 3,
            width: 80,
            borderRadius: 2,
            background: `linear-gradient(90deg, ${cfg.color}, transparent)`,
          }} />
        </div>

        {/* Sağ: etkinlik görseli */}
        {event.imageUrl && (
          <div style={{
            width: 320,
            height: 420,
            borderRadius: 24,
            overflow: 'hidden',
            flexShrink: 0,
            boxShadow: `0 0 0 1.5px ${cfg.border}, 0 20px 60px rgba(0,0,0,0.5)`,
            position: 'relative',
          }}>
            <Img
              src={event.imageUrl}
              style={{ width: '100%', height: '100%', objectFit: 'cover' }}
            />
            {/* Görsel üzerine hafif gradient */}
            <div style={{
              position: 'absolute', inset: 0,
              background: `linear-gradient(180deg, transparent 50%, rgba(15,12,41,0.6) 100%)`,
            }} />
          </div>
        )}
      </div>

      {/* Üst ince çizgi */}
      <div style={{
        position: 'absolute', top: 0, left: 0, right: 0,
        height: 2,
        background: `linear-gradient(90deg, transparent, ${cfg.color}, transparent)`,
        opacity: 0.6,
      }} />
    </AbsoluteFill>
  );
};
