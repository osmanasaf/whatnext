import React from 'react';
import { AbsoluteFill, Img, interpolate, useCurrentFrame, useVideoConfig } from 'remotion';

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

export const EventCard: React.FC<{ event: EventData; index: number }> = ({ event, index }) => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();

  const fadeIn = interpolate(frame, [0, FADE_DURATION], [0, 1], { extrapolateRight: 'clamp' });
  const fadeOut = interpolate(frame, [CARD_DURATION - FADE_DURATION, CARD_DURATION], [1, 0], { extrapolateLeft: 'clamp' });
  const opacity = Math.min(fadeIn, fadeOut);

  const slideY = interpolate(frame, [0, FADE_DURATION], [30, 0], { extrapolateRight: 'clamp' });

  const typeLabel = event.type === 'CONCERT' ? '🎵 Konser' : 
    event.type === 'THEATER' ? '🎭 Tiyatro' : '🎤 Stand-up';

  const formatDate = (d: string) => {
    const [y, m, day] = d.split('-');
    const months = ['Ocak', 'Şubat', 'Mart', 'Nisan', 'Mayıs', 'Haziran', 'Temmuz', 'Ağustos', 'Eylül', 'Ekim', 'Kasım', 'Aralık'];
    return `${parseInt(day)} ${months[parseInt(m) - 1]}`;
  };

  return (
    <AbsoluteFill style={{ 
      background: 'rgba(30,41,59,0.88)',
      opacity,
      transform: `translateY(${slideY}px)`,
    }}>
      <div style={{
        display: 'flex',
        flexDirection: 'column',
        height: '100%',
        padding: 48,
        justifyContent: 'center',
        alignItems: 'center',
      }}>
        <span style={{
          fontSize: 32,
          color: '#a5b4fc',
          marginBottom: 20,
          fontFamily: 'system-ui, sans-serif',
        }}>
          {typeLabel}
        </span>
        <h1 style={{
          fontSize: 52,
          color: '#fff',
          textAlign: 'center',
          margin: '0 0 24px 0',
          maxWidth: '92%',
          lineHeight: 1.25,
          fontFamily: 'system-ui, sans-serif',
        }}>
          {event.title}
        </h1>
        {event.venueName && (
          <p style={{
            fontSize: 30,
            color: '#c7d2fe',
            margin: '0 0 14px 0',
            fontFamily: 'system-ui, sans-serif',
          }}>
            {event.venueName}
          </p>
        )}
        <p style={{
          fontSize: 26,
          color: '#94a3b8',
          margin: 0,
          fontFamily: 'system-ui, sans-serif',
        }}>
          {formatDate(event.startDate)}
        </p>
        {event.imageUrl && (
          <div style={{
            marginTop: 28,
            width: 360,
            height: 360,
            borderRadius: 20,
            overflow: 'hidden',
            boxShadow: '0 12px 40px rgba(0,0,0,0.35), 0 0 0 2px rgba(165,180,252,0.25)',
          }}>
            <Img src={event.imageUrl} style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
          </div>
        )}
      </div>
    </AbsoluteFill>
  );
};
