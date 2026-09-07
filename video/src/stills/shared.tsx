import React from 'react';
import { AbsoluteFill, Img, staticFile } from 'remotion';
import { FONT_BODY, FONT_TITLE } from '../fonts';

export interface Palette {
  bg1: string;
  bg2: string;
  accent: string;
  text: string;
}

export interface StillEventData {
  id: number;
  title: string;
  venueName?: string;
  startTime?: string;
  imageUrl?: string;
}

export const TYPE_LABELS: Record<string, string> = {
  concert: 'KONSER',
  theater: 'TİYATRO',
  standup: 'STAND-UP',
};

/** Reel'in EventCard tür renkleriyle aynı — hesap genelinde tek dil. */
export const TYPE_COLORS: Record<string, { color: string; border: string }> = {
  concert: { color: '#f9a8d4', border: 'rgba(244,114,182,0.55)' },
  theater: { color: '#c4b5fd', border: 'rgba(139,92,246,0.55)' },
  standup: { color: '#6ee7b7', border: 'rgba(52,211,153,0.55)' },
};

const MONTHS_TR = [
  'Ocak', 'Şubat', 'Mart', 'Nisan', 'Mayıs', 'Haziran',
  'Temmuz', 'Ağustos', 'Eylül', 'Ekim', 'Kasım', 'Aralık',
];
const DAYS_TR = ['Pazar', 'Pazartesi', 'Salı', 'Çarşamba', 'Perşembe', 'Cuma', 'Cumartesi'];

/** "2026-07-17" -> "17 Temmuz" (UTC ile parse edilir, yerel saat dilimi kaymasını önler). */
export const formatDateTr = (isoDate: string): string => {
  const parts = isoDate.split('-');
  if (parts.length < 3) return isoDate;
  const [, m, d] = parts;
  return `${parseInt(d, 10)} ${MONTHS_TR[parseInt(m, 10) - 1]}`;
};

/** "2026-07-17" -> "Cuma". */
export const dayNameTr = (isoDate: string): string => {
  const d = new Date(isoDate + 'T00:00:00Z');
  return DAYS_TR[d.getUTCDay()];
};

/** http(s) URL ise olduğu gibi, değilse staticFile-göreli yol olarak çözülür (AI arka plan katmanı). */
export const resolveBackgroundSrc = (url?: string): string | null => {
  if (!url) return null;
  if (url.startsWith('http://') || url.startsWith('https://')) return url;
  return staticFile(url);
};

const DARK = '#0b0a1a';

/**
 * Arka plan (üç kademe): 1) {@code backgroundImageUrl} (AI motoru) net görsel + koyu gradyan;
 * 2) yoksa listedeki ilk afişli etkinliğin görseli, güçlü blur + karartma ile SADECE doku/ışık
 * olarak (bulanık olduğu için "bu foto hangi etkinliğin" belirsizliği doğurmaz — asıl afişler
 * kartlarda net gösterilir); 3) o da yoksa ay paleti gradyanı.
 */
export const StillBackground: React.FC<{
  backgroundImageUrl?: string;
  palette: Palette;
  ambientImageUrl?: string;
}> = ({ backgroundImageUrl, palette, ambientImageUrl }) => {
  const src = resolveBackgroundSrc(backgroundImageUrl);
  if (src) {
    return (
      <AbsoluteFill style={{ background: DARK }}>
        <Img src={src} style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
        <AbsoluteFill
          style={{ background: 'linear-gradient(180deg, rgba(11,10,26,0.35) 0%, rgba(11,10,26,0.8) 100%)' }}
        />
      </AbsoluteFill>
    );
  }
  if (ambientImageUrl) {
    return (
      <AbsoluteFill style={{ background: DARK, overflow: 'hidden' }}>
        <Img
          src={ambientImageUrl}
          style={{
            width: '100%',
            height: '100%',
            objectFit: 'cover',
            transform: 'scale(1.25)',
            filter: 'blur(38px) saturate(1.35) brightness(0.55)',
          }}
        />
        <AbsoluteFill
          style={{
            background: `linear-gradient(180deg, rgba(11,10,26,0.55) 0%, rgba(11,10,26,0.35) 40%, rgba(11,10,26,0.9) 100%)`,
          }}
        />
        <Vignette />
      </AbsoluteFill>
    );
  }
  return (
    <AbsoluteFill style={{ background: `linear-gradient(160deg, ${DARK} 0%, ${palette.accent} 140%)` }}>
      <div
        style={{
          position: 'absolute',
          top: '-15%',
          right: '-15%',
          width: '70%',
          height: '70%',
          borderRadius: '50%',
          background: `radial-gradient(circle, ${palette.bg1}99 0%, transparent 68%)`,
        }}
      />
      <div
        style={{
          position: 'absolute',
          bottom: '-20%',
          left: '-20%',
          width: '65%',
          height: '65%',
          borderRadius: '50%',
          background: `radial-gradient(circle, ${palette.bg2}66 0%, transparent 68%)`,
        }}
      />
      <Vignette />
    </AbsoluteFill>
  );
};

const Vignette: React.FC = () => (
  <AbsoluteFill
    style={{ background: 'radial-gradient(ellipse at center, transparent 55%, rgba(0,0,0,0.45) 100%)' }}
  />
);

export const ClockIcon: React.FC<{ size?: number; color?: string }> = ({ size = 18, color = '#e5e7eb' }) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none">
    <circle cx="12" cy="12" r="9" stroke={color} strokeWidth="1.8" />
    <path d="M12 7v5l3.5 2" stroke={color} strokeWidth="1.8" strokeLinecap="round" />
  </svg>
);

export const LocationIcon: React.FC<{ size?: number; color?: string }> = ({ size = 18, color = '#e5e7eb' }) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none">
    <path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7z" stroke={color} strokeWidth="1.8" />
    <circle cx="12" cy="9" r="2.5" stroke={color} strokeWidth="1.8" />
  </svg>
);

/** Reel'deki tür rozetiyle aynı: yarı saydam koyu pill + tür renginde kenarlık. */
export const TypeBadge: React.FC<{ eventType: string; size?: number }> = ({ eventType, size = 17 }) => {
  const cfg = TYPE_COLORS[eventType] ?? TYPE_COLORS.concert;
  const label = TYPE_LABELS[eventType] ?? eventType.toUpperCase();
  return (
    <div
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        background: 'rgba(11,10,26,0.6)',
        border: `1.5px solid ${cfg.border}`,
        borderRadius: 100,
        padding: `${Math.round(size * 0.6)}px ${Math.round(size * 1.2)}px`,
      }}
    >
      <span style={{ fontSize: size, fontWeight: 800, letterSpacing: 2.5, color: cfg.color, fontFamily: FONT_BODY }}>
        {label}
      </span>
    </div>
  );
};

/** Sol üst gün/tarih bloğu — Reel kartındaki "SAL / 22 Eyl" hiyerarşisinin still karşılığı. */
export const DateBlock: React.FC<{ date: string; daySize?: number; subtitle?: string }> = ({
  date,
  daySize = 64,
  subtitle,
}) => (
  <div style={{ display: 'flex', flexDirection: 'column' }}>
    <div
      style={{
        fontSize: daySize,
        color: '#f8fafc',
        fontFamily: FONT_TITLE,
        lineHeight: 1,
        textShadow: '0 4px 24px rgba(0,0,0,0.6)',
      }}
    >
      {dayNameTr(date)}
    </div>
    <div
      style={{
        marginTop: 10,
        fontSize: Math.round(daySize * 0.44),
        color: 'rgba(255,255,255,0.85)',
        fontFamily: FONT_BODY,
        fontWeight: 600,
        letterSpacing: 1,
        textShadow: '0 2px 10px rgba(0,0,0,0.6)',
      }}
    >
      {formatDateTr(date)}
      {subtitle ? ` • ${subtitle}` : ''}
    </div>
  </div>
);

/** Afiş küçük resmi — yoksa tür renginde gradyan + baş harf (boş kutu bırakmaz). */
export const Poster: React.FC<{
  imageUrl?: string;
  title: string;
  eventType?: string;
  width: number;
  height: number;
  radius?: number;
}> = ({ imageUrl, title, eventType = 'concert', width, height, radius = 20 }) => {
  const cfg = TYPE_COLORS[eventType] ?? TYPE_COLORS.concert;
  const common: React.CSSProperties = {
    width,
    height,
    borderRadius: radius,
    overflow: 'hidden',
    flexShrink: 0,
    boxShadow: '0 14px 40px rgba(0,0,0,0.55)',
    border: '1px solid rgba(255,255,255,0.14)',
  };
  if (imageUrl) {
    return (
      <div style={common}>
        <Img src={imageUrl} style={{ width: '100%', height: '100%', objectFit: 'cover', display: 'block' }} />
      </div>
    );
  }
  return (
    <div
      style={{
        ...common,
        background: `linear-gradient(160deg, ${cfg.border} 0%, ${DARK} 100%)`,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
      }}
    >
      <span style={{ fontSize: Math.round(height * 0.4), color: cfg.color, fontFamily: FONT_TITLE }}>
        {(title.trim()[0] ?? '•').toUpperCase()}
      </span>
    </div>
  );
};

/** Saat • mekân satırı, ikonlu. */
export const MetaLine: React.FC<{ startTime?: string; venueName?: string; size: number }> = ({
  startTime,
  venueName,
  size,
}) => {
  if (!startTime && !venueName) return null;
  const color = 'rgba(255,255,255,0.86)';
  return (
    <div
      style={{
        display: 'flex',
        alignItems: 'center',
        gap: Math.round(size * 0.5),
        fontSize: size,
        color,
        fontFamily: FONT_BODY,
        fontWeight: 500,
        textShadow: '0 2px 10px rgba(0,0,0,0.6)',
        whiteSpace: 'nowrap',
        overflow: 'hidden',
        textOverflow: 'ellipsis',
      }}
    >
      {startTime && (
        <span style={{ display: 'inline-flex', alignItems: 'center', gap: Math.round(size * 0.3) }}>
          <ClockIcon size={size} />
          <span>{startTime.length > 5 ? startTime.slice(0, 5) : startTime}</span>
        </span>
      )}
      {venueName && (
        <span
          style={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: Math.round(size * 0.3),
            overflow: 'hidden',
            textOverflow: 'ellipsis',
            whiteSpace: 'nowrap',
          }}
        >
          <LocationIcon size={size} />
          <span style={{ overflow: 'hidden', textOverflow: 'ellipsis' }}>{venueName}</span>
        </span>
      )}
    </div>
  );
};

/** Yatay etkinlik kartı: afiş + başlık + meta, cam panel üzerinde. */
export const EventRow: React.FC<{
  event: StillEventData;
  eventType?: string;
  posterHeight: number;
  titleSize: number;
}> = ({ event, eventType, posterHeight, titleSize }) => {
  // Bilet sitelerinin afişleri ağırlıkla kare — kare küçük resim, afiş metnini kırpmaz.
  const posterWidth = posterHeight;
  return (
    <div
      style={{
        display: 'flex',
        alignItems: 'center',
        gap: Math.round(posterHeight * 0.16),
        background: 'rgba(255,255,255,0.07)',
        border: '1px solid rgba(255,255,255,0.12)',
        borderRadius: Math.round(posterHeight * 0.16),
        padding: Math.round(posterHeight * 0.09),
        backdropFilter: 'blur(10px)',
      }}
    >
      <Poster
        imageUrl={event.imageUrl}
        title={event.title}
        eventType={eventType}
        width={posterWidth}
        height={posterHeight}
        radius={Math.round(posterHeight * 0.1)}
      />
      <div style={{ display: 'flex', flexDirection: 'column', minWidth: 0, flex: 1, gap: Math.round(titleSize * 0.3) }}>
        <div
          style={{
            fontSize: titleSize,
            color: '#f8fafc',
            fontFamily: FONT_TITLE,
            lineHeight: 1.18,
            textShadow: '0 2px 18px rgba(0,0,0,0.6)',
            display: '-webkit-box',
            WebkitLineClamp: 2,
            WebkitBoxOrient: 'vertical' as const,
            overflow: 'hidden',
          }}
        >
          {event.title}
        </div>
        <MetaLine startTime={event.startTime} venueName={event.venueName} size={Math.round(titleSize * 0.58)} />
      </div>
    </div>
  );
};

export const SaveCta: React.FC<{ bottom: number; size?: number }> = ({ bottom, size = 22 }) => (
  <div style={{ position: 'absolute', left: 0, right: 0, bottom, display: 'flex', justifyContent: 'center' }}>
    <div
      style={{
        background: 'rgba(255,255,255,0.1)',
        border: '1px solid rgba(255,255,255,0.18)',
        borderRadius: 100,
        padding: `${Math.round(size * 0.55)}px ${Math.round(size * 1.3)}px`,
        fontSize: size,
        fontWeight: 700,
        lineHeight: 1,
        color: '#ffffff',
        fontFamily: FONT_BODY,
        letterSpacing: 1.5,
        backdropFilter: 'blur(10px)',
        boxShadow: '0 10px 30px rgba(0,0,0,0.35)',
        display: 'flex',
        alignItems: 'center',
        gap: Math.round(size * 0.5),
      }}
    >
      <svg width={size} height={size} viewBox="0 0 24 24" fill="none" style={{ display: 'block', flexShrink: 0 }}>
        <path d="M6 3h12v18l-6-4-6 4V3z" stroke="#ffffff" strokeWidth="2" strokeLinejoin="round" />
      </svg>
      <span style={{ display: 'block', paddingTop: 1 }}>KAYDET • whatnext.tr</span>
    </div>
  </div>
);

export const Footer: React.FC<{ color?: string }> = ({ color = 'rgba(255,255,255,0.55)' }) => (
  <div
    style={{
      position: 'absolute',
      bottom: 28,
      left: 0,
      right: 0,
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      gap: 8,
    }}
  >
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
      <polygon points="12,2 22,8.5 22,15.5 12,22 2,15.5 2,8.5" stroke={color} strokeWidth="1.5" />
    </svg>
    <span style={{ fontSize: 14, color, fontFamily: FONT_BODY, letterSpacing: 1 }}>whatnext.tr</span>
  </div>
);

/** Etkinlik sayısına göre kademeli font boyutu — CSS'in tek başına yapamadığı "N öğeyi sabit
 * yüksekliğe sığdır" işini basit bir tabloyla çözer; line-clamp ile birlikte taşma garantili önlenir. */
export const fontSizeForCount = (count: number, sizes: { few: number; some: number; many: number }): number => {
  if (count <= 4) return sizes.few;
  if (count <= 7) return sizes.some;
  return sizes.many;
};

/** Listedeki ilk afişli etkinlik — bulanık ambient arka plan kaynağı. */
export const firstImage = (events: StillEventData[]): string | undefined =>
  events.find((e) => e.imageUrl && e.imageUrl.length > 0)?.imageUrl;
