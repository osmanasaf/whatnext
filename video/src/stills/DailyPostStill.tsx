import React from 'react';
import { AbsoluteFill } from 'remotion';
import { FONT_TITLE, useBrandFonts } from '../fonts';
import {
  DateStamp,
  EventRow,
  MetaLine,
  Poster,
  SaveCta,
  StillBackground,
  StillEventData,
  TypeRail,
  firstImage,
} from './shared';
import { ThemeContext, themeFor } from '../theme';

export interface DailyPostStillProps {
  eventType: string;
  monthName: string;
  date: string;
  events: StillEventData[];
  backgroundImageUrl?: string;
}

const PAD = 64;
const HEADER_H = 130;
/** Alt CTA bölgesi — liste bu alana girmez, CTA ile son kart arasında nefes payı kalır. */
const CTA_ZONE = 180;
const CTA_BOTTOM = 64;
const ROW_GAP = 22;
/** Sol dikey tür rayı için kartların soldan kayma payı. */
const RAIL_W = 62;

/** Satır yüksekliği — listedeki kart sayısına göre, kullanılabilir alana tam oturacak şekilde. */
const layoutFor = (count: number, canvasHeight: number) => {
  // 9-10 satırda boşluk daralır, afiş okunur boyda kalır.
  const gap = count > 8 ? 12 : ROW_GAP;
  const available = canvasHeight - PAD - HEADER_H - 24 - CTA_ZONE;
  const rowH = Math.floor((available - (count - 1) * gap) / count);
  const posterHeight = Math.min(240, Math.round(rowH * 0.82));
  const titleSize = count <= 3 ? 40 : count <= 4 ? 36 : count <= 5 ? 32 : 28;
  return { posterHeight, titleSize, gap };
};

export const DailyPostStill: React.FC<DailyPostStillProps> = ({
  eventType,
  date,
  events,
  backgroundImageUrl,
}) => {
  useBrandFonts();
  const shown = events.slice(0, 10);
  const ambient = firstImage(shown);
  const subtitle = shown.length > 1 ? `İstanbul • ${shown.length} etkinlik` : 'İstanbul';

  return (
    <ThemeContext.Provider value={themeFor(date)}>
    <AbsoluteFill>
      <StillBackground backgroundImageUrl={backgroundImageUrl} ambientImageUrl={ambient} />

      <AbsoluteFill
        style={{ padding: `${PAD}px ${PAD}px ${CTA_ZONE}px ${PAD}px`, display: 'flex', flexDirection: 'column' }}
      >
        <div style={{ height: HEADER_H, display: 'flex', alignItems: 'center' }}>
          <DateStamp date={date} subtitle={subtitle} />
        </div>

        {shown.length === 1 ? (
          <Hero event={shown[0]} eventType={eventType} />
        ) : (
          <List events={shown} eventType={eventType} />
        )}
      </AbsoluteFill>

      {/* Sol kenar: aşağıdan yukarı "KONSER" rayı — kart listesiyle aynı dikey aralık */}
      <TypeRail eventType={eventType} left={PAD - 10} top={PAD + HEADER_H + 24} bottom={CTA_ZONE} />

      <SaveCta bottom={CTA_BOTTOM} />
    </AbsoluteFill>
    </ThemeContext.Provider>
  );
};

/** Tek etkinlik: afiş büyük, başlık altında — tipik "bugün tek tiyatro var" günü. */
const Hero: React.FC<{ event: StillEventData; eventType: string }> = ({ event, eventType }) => (
  <div
    style={{
      flex: 1,
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
      justifyContent: 'center',
      paddingLeft: RAIL_W,
    }}
  >
    <Poster imageUrl={event.imageUrl} title={event.title} eventType={eventType} width={660} height={660} radius={30} />
    <div
      style={{
        marginTop: 34,
        fontSize: event.title.length > 30 ? 42 : 50,
        color: '#f8fafc',
        fontFamily: FONT_TITLE,
        lineHeight: 1.15,
        textAlign: 'center',
        textShadow: '0 2px 24px rgba(0,0,0,0.7)',
        maxWidth: 860,
        display: '-webkit-box',
        WebkitLineClamp: 2,
        WebkitBoxOrient: 'vertical' as const,
        overflow: 'hidden',
      }}
    >
      {event.title}
    </div>
    <div style={{ marginTop: 14, maxWidth: 860 }}>
      <MetaLine startTime={event.startTime} venueName={event.venueName} size={26} />
    </div>
  </div>
);

const List: React.FC<{ events: StillEventData[]; eventType: string }> = ({ events, eventType }) => {
  const { posterHeight, titleSize, gap } = layoutFor(events.length, 1350);
  return (
    <div
      style={{
        flex: 1,
        display: 'flex',
        flexDirection: 'column',
        justifyContent: 'center',
        gap,
        paddingTop: 12,
        paddingLeft: RAIL_W,
      }}
    >
      {events.map((e) => (
        <EventRow key={e.id} event={e} eventType={eventType} posterHeight={posterHeight} titleSize={titleSize} />
      ))}
    </div>
  );
};
