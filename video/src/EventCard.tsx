import React from 'react';
import { AbsoluteFill, Img, interpolate, spring, useCurrentFrame, useVideoConfig } from 'remotion';
import { FONT_TITLE } from './fonts';
import { DateStamp, MetaLine, Palette, Poster, StillBackground, TypeBadge } from './stills/shared';

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

const REEL_PALETTE: Palette = { bg1: '#302b63', bg2: '#24243e', accent: '#a78bfa', text: '#f8fafc' };

/** Instagram Reels arayüzü üst ~200px ve alt ~380px'i (açıklama, butonlar) kapatır — bilgi bu bölgelerin dışında. */
const SAFE_TOP = 210;
const SAFE_BOTTOM = 380;
const SIDE = 72;
/** Afiş en fazla bu kutuya sığar, ASLA büyütülmez/kırpılmaz (kaynak afişler 600×800 – 1920×1080). */
const POSTER_MAX_W = 1080 - SIDE * 2;
const POSTER_MAX_H = 800;

/** Reel türü (CONCERT/THEATER/STANDUP/PERFORMING_ART) → still bileşenlerinin tür anahtarı. */
export const stillType = (type: string): string => {
  const t = (type || '').toUpperCase();
  if (t === 'STANDUP') return 'standup';
  if (t === 'CONCERT') return 'concert';
  return 'theater';
};

/**
 * Tek etkinlik kartı — statik görsellerin (post/story) diliyle aynı: afiş en-boy oranı korunarak ortada,
 * arkada aynı afişin bulanık hâli, üstte takvim damgası, altta büyük başlık + mekân.
 *
 * <p>Önceki sürüm (2026-09-15 geri bildirimi: "çok kötü") afişi tam ekran {@code objectFit: cover} ile
 * basıyordu: yatay Biletix afişleri (914×504) 3-4 kat büyüyüp ortadan dar şerit kalıyordu ("KEN DOĞ",
 * "engin utfağı"), başlığımız afişin kendi yazısının üstüne biniyor, her kart karanlığa inip çıkıyordu.
 */
export const EventCard: React.FC<{ event: EventData; durationInFrames: number }> = ({ event, durationInFrames }) => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();

  const enter = spring({ frame, fps, config: { damping: 18, stiffness: 170 } });
  const posterScale = interpolate(enter, [0, 1], [0.9, 1]);
  const drift = interpolate(frame, [0, durationInFrames], [1, 1.035]);
  const textY = interpolate(enter, [0, 1], [48, 0]);
  const textOpacity = interpolate(frame, [2, 10], [0, 1], { extrapolateLeft: 'clamp', extrapolateRight: 'clamp' });
  const type = stillType(event.type);

  return (
    <AbsoluteFill>
      <StillBackground palette={REEL_PALETTE} ambientImageUrl={event.imageUrl || undefined} />

      <AbsoluteFill
        style={{
          padding: `${SAFE_TOP}px ${SIDE}px ${SAFE_BOTTOM}px`,
          display: 'flex',
          flexDirection: 'column',
          gap: 44,
        }}
      >
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            opacity: textOpacity,
          }}
        >
          <DateStamp date={event.startDate} subtitle={event.startTime || undefined} scale={1.35} />
          <TypeBadge eventType={type} size={26} />
        </div>

        {/* Afiş + başlık tek grup, kalan alanda dikey ortalı: yatay afişte başlık afişin hemen altında kalır. */}
        <div style={{ flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column', justifyContent: 'center', gap: 52 }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          {event.imageUrl ? (
            <Img
              src={event.imageUrl}
              style={{
                maxWidth: POSTER_MAX_W,
                maxHeight: POSTER_MAX_H,
                width: 'auto',
                height: 'auto',
                display: 'block',
                borderRadius: 28,
                border: '1px solid rgba(255,255,255,0.16)',
                boxShadow: '0 30px 90px rgba(0,0,0,0.65)',
                transform: `scale(${posterScale * drift})`,
              }}
            />
          ) : (
            <div style={{ transform: `scale(${posterScale})` }}>
              <Poster title={event.title} eventType={type} width={620} height={620} radius={28} />
            </div>
          )}
        </div>

        <div
          style={{
            opacity: textOpacity,
            transform: `translateY(${textY}px)`,
            display: 'flex',
            flexDirection: 'column',
            gap: 18,
          }}
        >
          <div
            style={{
              fontSize: event.title.length > 30 ? 62 : 78,
              fontFamily: FONT_TITLE,
              color: '#f8fafc',
              lineHeight: 1.12,
              textShadow: '0 4px 28px rgba(0,0,0,0.7)',
              display: '-webkit-box',
              WebkitLineClamp: 2,
              WebkitBoxOrient: 'vertical' as const,
              overflow: 'hidden',
            }}
          >
            {event.title}
          </div>
          <MetaLine venueName={event.venueName} size={34} />
        </div>
        </div>
      </AbsoluteFill>
    </AbsoluteFill>
  );
};
