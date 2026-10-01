import React from 'react';
import { AbsoluteFill, Img, staticFile } from 'remotion';
import { FONT_BODY, FONT_TITLE } from '../fonts';
import { themeFor, useTheme, withAlpha } from '../theme';

export interface StillEventData {
  id: number;
  title: string;
  venueName?: string;
  startTime?: string;
  imageUrl?: string;
  /** "concert" | "theater" | "standup" — karışık listelerde (story) satır etiketi için. */
  eventType?: string;
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

export const dayOfMonth = (isoDate: string): string => String(parseInt(isoDate.split('-')[2] ?? '0', 10));
export const monthNameTr = (isoDate: string): string => MONTHS_TR[parseInt(isoDate.split('-')[1] ?? '1', 10) - 1] ?? '';

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
 * kartlarda net gösterilir); 3) o da yoksa düz ay gradyanı. Karartma ve ışıma ay temasının
 * renginde — afiş hangi renkte olursa olsun ayın tonu görünür.
 */
export const StillBackground: React.FC<{
  backgroundImageUrl?: string;
  ambientImageUrl?: string;
}> = ({ backgroundImageUrl, ambientImageUrl }) => {
  const { accent, base } = useTheme();
  const src = resolveBackgroundSrc(backgroundImageUrl);
  if (src) {
    return (
      <AbsoluteFill style={{ background: base }}>
        <Img src={src} style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
        <AbsoluteFill
          style={{ background: `linear-gradient(180deg, ${withAlpha(base, 0.35)} 0%, ${withAlpha(base, 0.8)} 100%)` }}
        />
        <AccentGlow accent={accent} />
      </AbsoluteFill>
    );
  }
  if (ambientImageUrl) {
    return (
      <AbsoluteFill style={{ background: base, overflow: 'hidden' }}>
        <Img
          src={ambientImageUrl}
          style={{
            width: '100%',
            height: '100%',
            objectFit: 'cover',
            transform: 'scale(1.25)',
            filter: 'blur(38px) saturate(0.6) brightness(0.5)',
          }}
        />
        <AbsoluteFill
          style={{
            background: `linear-gradient(180deg, ${withAlpha(base, 0.55)} 0%, ${withAlpha(base, 0.35)} 40%, ${withAlpha(base, 0.92)} 100%)`,
          }}
        />
        <AccentGlow accent={accent} />
        <Vignette />
      </AbsoluteFill>
    );
  }
  return (
    <AbsoluteFill style={{ background: `linear-gradient(160deg, ${base} 0%, ${withAlpha(accent, 0.45)} 140%)` }}>
      <AccentGlow accent={accent} />
      <Vignette />
    </AbsoluteFill>
  );
};

/** Sağ üstten ve sol alttan ay renginde iki ışıma — temanın arka plandaki ana taşıyıcısı. */
const AccentGlow: React.FC<{ accent: string }> = ({ accent }) => (
  <>
    <div
      style={{
        position: 'absolute',
        top: '-25%',
        right: '-25%',
        width: '85%',
        height: '75%',
        borderRadius: '50%',
        background: `radial-gradient(circle, ${withAlpha(accent, 0.42)} 0%, transparent 68%)`,
      }}
    />
    <div
      style={{
        position: 'absolute',
        bottom: '-25%',
        left: '-25%',
        width: '80%',
        height: '65%',
        borderRadius: '50%',
        background: `radial-gradient(circle, ${withAlpha(accent, 0.24)} 0%, transparent 68%)`,
      }}
    />
  </>
);

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

/**
 * Takvim damgası başlık: büyük gün sayısı + yanında ay / gün adı sütunu.
 * "Cumartesi" tek başına büyük yazmak yerine tarihi bir bakışta okutur (26 | EYLÜL / Cumartesi).
 */
export const DateStamp: React.FC<{
  date: string;
  kicker?: string;
  subtitle?: string;
  scale?: number;
  /** Ay renginde dolu zemin üstünde: tüm yazı bu koyu renkte, gölgesiz (kapak/Reel girişi). */
  ink?: string;
}> = ({ date, kicker, subtitle, scale = 1, ink }) => {
  const theme = useTheme();
  const accent = ink ?? theme.accent;
  const light = ink ?? '#f8fafc';
  const soft = ink ? withAlpha(ink, 0.75) : 'rgba(255,255,255,0.7)';
  const shadow = (v: string) => (ink ? 'none' : v);
  // Özel gün etiketi damganın kendi tarihinden — Reel'de her kart kendi gününü taşır.
  const special = themeFor(date).special;
  return (
  <div style={{ display: 'flex', alignItems: 'center', gap: Math.round(20 * scale) }}>
    <div
      style={{
        fontSize: Math.round(118 * scale),
        fontFamily: FONT_TITLE,
        lineHeight: 0.85,
        color: light,
        textShadow: shadow('0 4px 24px rgba(0,0,0,0.6)'),
        letterSpacing: -2,
      }}
    >
      {dayOfMonth(date)}
    </div>
    <div style={{ width: 3, alignSelf: 'stretch', background: accent, borderRadius: 2 }} />
    <div style={{ display: 'flex', flexDirection: 'column', justifyContent: 'center' }}>
      {special ? (
        <div
          style={{
            alignSelf: 'flex-start',
            fontSize: Math.round(17 * scale),
            fontWeight: 800,
            letterSpacing: 3,
            color: ink ? theme.accent : DARK,
            background: accent,
            borderRadius: 100,
            padding: `${Math.round(4 * scale)}px ${Math.round(14 * scale)}px`,
            fontFamily: FONT_BODY,
            marginBottom: Math.round(8 * scale),
          }}
        >
          {special}
        </div>
      ) : kicker && (
        <div
          style={{
            fontSize: Math.round(17 * scale),
            fontWeight: 700,
            letterSpacing: 4,
            color: soft,
            fontFamily: FONT_BODY,
            marginBottom: Math.round(4 * scale),
          }}
        >
          {kicker}
        </div>
      )}
      <div
        style={{
          fontSize: Math.round(22 * scale),
          fontWeight: 700,
          letterSpacing: 5,
          color: accent,
          fontFamily: FONT_BODY,
          textShadow: shadow('0 2px 10px rgba(0,0,0,0.6)'),
        }}
      >
        {monthNameTr(date).toLocaleUpperCase('tr')}
      </div>
      <div
        style={{
          fontSize: Math.round(46 * scale),
          fontFamily: FONT_TITLE,
          color: light,
          lineHeight: 1.05,
          textShadow: shadow('0 4px 20px rgba(0,0,0,0.6)'),
        }}
      >
        {dayNameTr(date)}
      </div>
      {subtitle && (
        <div
          style={{
            marginTop: Math.round(4 * scale),
            fontSize: Math.round(20 * scale),
            fontWeight: 600,
            color: soft,
            fontFamily: FONT_BODY,
            letterSpacing: 1,
          }}
        >
          {subtitle}
        </div>
      )}
    </div>
  </div>
  );
};

/**
 * Sol kenarda dikey tür rayı: aşağıdan yukarıya okunan "KONSER" + üstüne doğru sönen çizgi.
 * Post'ta sağ üst rozetin yerini alır; kartlar rayın sağından başlar.
 */
export const TypeRail: React.FC<{ eventType: string; left: number; top: number; bottom: number }> = ({
  eventType,
  left,
  top,
  bottom,
}) => {
  const cfg = TYPE_COLORS[eventType] ?? TYPE_COLORS.concert;
  const label = TYPE_LABELS[eventType] ?? eventType.toUpperCase();
  return (
    <div
      style={{
        position: 'absolute',
        left,
        top,
        bottom,
        width: 44,
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        gap: 18,
      }}
    >
      <div style={{ flex: 1, width: 3, borderRadius: 2, background: `linear-gradient(to top, ${cfg.color}, transparent)` }} />
      <div
        style={{
          writingMode: 'vertical-rl' as const,
          transform: 'rotate(180deg)',
          fontSize: 34,
          fontWeight: 800,
          letterSpacing: 9,
          color: cfg.color,
          fontFamily: FONT_BODY,
          textShadow: '0 2px 14px rgba(0,0,0,0.6)',
          whiteSpace: 'nowrap',
        }}
      >
        {label}
      </div>
    </div>
  );
};

/** Karışık listelerde satır başına küçük tür etiketi (story). */
export const TypeTag: React.FC<{ eventType?: string; size: number }> = ({ eventType, size }) => {
  if (!eventType) return null;
  const cfg = TYPE_COLORS[eventType] ?? TYPE_COLORS.concert;
  const label = TYPE_LABELS[eventType] ?? eventType.toUpperCase();
  return (
    <span
      style={{
        display: 'inline-block',
        alignSelf: 'flex-start',
        fontSize: Math.round(size * 0.78),
        fontWeight: 800,
        letterSpacing: 2,
        color: cfg.color,
        fontFamily: FONT_BODY,
        border: `1px solid ${cfg.border}`,
        borderRadius: 6,
        padding: `2px ${Math.round(size * 0.45)}px`,
        lineHeight: 1.25,
        flexShrink: 0,
      }}
    >
      {label}
    </span>
  );
};

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
  /** Karışık listede satır başına tür etiketi göster (post'ta ray zaten var, gerekmez). */
  showType?: boolean;
}> = ({ event, eventType, posterHeight, titleSize, showType }) => {
  const rowType = eventType ?? event.eventType;
  const { accent } = useTheme();
  // Bilet sitelerinin afişleri ağırlıkla kare — kare küçük resim, afiş metnini kırpmaz.
  const posterWidth = posterHeight;
  // Kalabalık liste (10 satırlık post): başlık tek satır, tür etiketi saat/mekân satırına iner — taşmaz.
  const compact = posterHeight < 90;
  return (
    <div
      style={{
        display: 'flex',
        alignItems: 'center',
        gap: Math.round(posterHeight * 0.16),
        background: `linear-gradient(90deg, ${withAlpha(accent, 0.12)} 0%, rgba(255,255,255,0.05) 60%)`,
        border: `1px solid ${withAlpha(accent, 0.3)}`,
        borderRadius: Math.round(posterHeight * 0.16),
        padding: Math.round(posterHeight * 0.09),
        backdropFilter: 'blur(10px)',
      }}
    >
      <Poster
        imageUrl={event.imageUrl}
        title={event.title}
        eventType={rowType}
        width={posterWidth}
        height={posterHeight}
        radius={Math.round(posterHeight * 0.1)}
      />
      <div style={{ display: 'flex', flexDirection: 'column', minWidth: 0, flex: 1, gap: Math.round(titleSize * 0.3) }}>
        {showType && !compact && <TypeTag eventType={rowType} size={Math.round(titleSize * 0.5)} />}
        <div
          style={{
            fontSize: titleSize,
            color: '#f8fafc',
            fontFamily: FONT_TITLE,
            lineHeight: 1.18,
            textShadow: '0 2px 18px rgba(0,0,0,0.6)',
            display: '-webkit-box',
            WebkitLineClamp: compact ? 1 : 2,
            WebkitBoxOrient: 'vertical' as const,
            overflow: 'hidden',
          }}
        >
          {event.title}
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: Math.round(titleSize * 0.35), minWidth: 0 }}>
          {showType && compact && <TypeTag eventType={rowType} size={Math.round(titleSize * 0.55)} />}
          <MetaLine startTime={event.startTime} venueName={event.venueName} size={Math.round(titleSize * 0.58)} />
        </div>
      </div>
    </div>
  );
};

/**
 * Gün slaytının altı: "+142 etkinlik daha • her gün story'lerde". Seçki 10 etkinlik; şehrin bolluğunu gösterir,
 * geri kalanı günlük story'lere yönlendirir (2026-10-01 kullanıcı: "çok etkinlik kaçmış gibi"; whatnext.tr
 * şimdilik kapalı olduğu için siteye yönlendirme yok).
 */
export const MoreCta: React.FC<{ bottom: number; count: number; size?: number; suffix?: string }> = ({
  bottom,
  count,
  size = 26,
  suffix = "her gün story'lerde",
}) => {
  const { accent, base } = useTheme();
  return (
    <div style={{ position: 'absolute', left: 0, right: 0, bottom, display: 'flex', justifyContent: 'center' }}>
      <div
        style={{
          background: accent,
          borderRadius: 100,
          padding: `${Math.round(size * 0.6)}px ${Math.round(size * 1.3)}px`,
          fontSize: size,
          fontWeight: 800,
          lineHeight: 1,
          color: base,
          fontFamily: FONT_BODY,
          boxShadow: '0 10px 30px rgba(0,0,0,0.4)',
          display: 'flex',
          alignItems: 'center',
          gap: Math.round(size * 0.45),
        }}
      >
        <span style={{ fontFamily: FONT_TITLE, fontSize: Math.round(size * 1.15) }}>+{count}</span>
        <span>etkinlik daha</span>
        <span style={{ opacity: 0.6 }}>•</span>
        <span>{suffix}</span>
      </div>
    </div>
  );
};

export const SaveCta: React.FC<{ bottom: number; size?: number }> = ({ bottom, size = 22 }) => {
  const { accent } = useTheme();
  return (
  <div style={{ position: 'absolute', left: 0, right: 0, bottom, display: 'flex', justifyContent: 'center' }}>
    <div
      style={{
        background: withAlpha(accent, 0.16),
        border: `1.5px solid ${withAlpha(accent, 0.6)}`,
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
        <path d="M6 3h12v18l-6-4-6 4V3z" stroke={accent} strokeWidth="2" strokeLinejoin="round" />
      </svg>
      <span style={{ display: 'block', paddingTop: 1 }}>KAYDET • @whatnext.event</span>
    </div>
  </div>
  );
};

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
    <span style={{ fontSize: 14, color, fontFamily: FONT_BODY, letterSpacing: 1 }}>@whatnext.event</span>
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
