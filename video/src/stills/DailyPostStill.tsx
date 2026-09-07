import React from 'react';
import { AbsoluteFill } from 'remotion';
import { FONT_TITLE } from '../fonts';
import {
  DateBlock,
  EventRow,
  MetaLine,
  Palette,
  Poster,
  SaveCta,
  StillBackground,
  StillEventData,
  TypeBadge,
  firstImage,
} from './shared';

export interface DailyPostStillProps {
  eventType: string;
  monthName: string;
  palette: Palette;
  date: string;
  events: StillEventData[];
  backgroundImageUrl?: string;
}

const PAD = 64;
const HEADER_H = 120;
/** Alt CTA bölgesi — liste bu alana girmez, CTA ile son kart arasında nefes payı kalır. */
const CTA_ZONE = 180;
const CTA_BOTTOM = 64;
const ROW_GAP = 22;

/** Satır yüksekliği — listedeki kart sayısına göre, kullanılabilir alana tam oturacak şekilde. */
const layoutFor = (count: number, canvasHeight: number) => {
  const available = canvasHeight - PAD - HEADER_H - 24 - CTA_ZONE;
  const rowH = Math.floor((available - (count - 1) * ROW_GAP) / count);
  const posterHeight = Math.min(240, Math.round(rowH * 0.82));
  const titleSize = count <= 3 ? 40 : count <= 4 ? 36 : count <= 5 ? 32 : 28;
  return { posterHeight, titleSize };
};

export const DailyPostStill: React.FC<DailyPostStillProps> = ({
  eventType,
  palette,
  date,
  events,
  backgroundImageUrl,
}) => {
  const shown = events.slice(0, 6);
  const ambient = firstImage(shown);
  const subtitle = shown.length > 1 ? `İstanbul • ${shown.length} etkinlik` : 'İstanbul';

  return (
    <AbsoluteFill>
      <StillBackground backgroundImageUrl={backgroundImageUrl} palette={palette} ambientImageUrl={ambient} />

      <AbsoluteFill
        style={{ padding: `${PAD}px ${PAD}px ${CTA_ZONE}px ${PAD}px`, display: 'flex', flexDirection: 'column' }}
      >
        <div style={{ height: HEADER_H, display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
          <DateBlock date={date} daySize={64} subtitle={subtitle} />
          <TypeBadge eventType={eventType} size={18} />
        </div>

        {shown.length === 1 ? (
          <Hero event={shown[0]} eventType={eventType} />
        ) : (
          <List events={shown} eventType={eventType} />
        )}
      </AbsoluteFill>

      <SaveCta bottom={CTA_BOTTOM} />
    </AbsoluteFill>
  );
};

/** Tek etkinlik: afiş büyük, başlık altında — tipik "bugün tek tiyatro var" günü. */
const Hero: React.FC<{ event: StillEventData; eventType: string }> = ({ event, eventType }) => (
  <div style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center' }}>
    <Poster imageUrl={event.imageUrl} title={event.title} eventType={eventType} width={680} height={680} radius={30} />
    <div
      style={{
        marginTop: 34,
        fontSize: event.title.length > 30 ? 42 : 50,
        color: '#f8fafc',
        fontFamily: FONT_TITLE,
        lineHeight: 1.15,
        textAlign: 'center',
        textShadow: '0 2px 24px rgba(0,0,0,0.7)',
        maxWidth: 900,
        display: '-webkit-box',
        WebkitLineClamp: 2,
        WebkitBoxOrient: 'vertical' as const,
        overflow: 'hidden',
      }}
    >
      {event.title}
    </div>
    <div style={{ marginTop: 14, maxWidth: 900 }}>
      <MetaLine startTime={event.startTime} venueName={event.venueName} size={26} />
    </div>
  </div>
);

const List: React.FC<{ events: StillEventData[]; eventType: string }> = ({ events, eventType }) => {
  const { posterHeight, titleSize } = layoutFor(events.length, 1350);
  return (
    <div style={{ flex: 1, display: 'flex', flexDirection: 'column', justifyContent: 'center', gap: ROW_GAP, paddingTop: 12 }}>
      {events.map((e) => (
        <EventRow key={e.id} event={e} eventType={eventType} posterHeight={posterHeight} titleSize={titleSize} />
      ))}
    </div>
  );
};
