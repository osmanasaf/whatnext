import { EventData } from './EventCard';

export const FPS = 30;
export const HOOK_FRAMES = 45; // 1.5sn — afiş mozaiği + başlık okunabilsin
export const FEATURED_FRAMES = 90; // 3sn — kategori lideri (en yüksek skorlu)
// 2sn: 35 kare (~1.2sn) afişi, tarihi ve başlığı okumaya yetmiyordu (2026-09-15 geri bildirimi: "çok kötü").
export const QUICK_FRAMES = 60;
export const OUTRO_FRAMES = 60; // 2sn
/** "days" düzeninde bir günün ekranı: 4 satıra kadar liste, okumaya 3sn. */
export const DAY_FRAMES = 90;

/** Reel düzeni haftadan haftaya döner: "cards" = etkinlik başına kart, "days" = gün başına liste ekranı. */
export type ReelLayout = 'cards' | 'days';

export const frameDurationForEvent = (event: Pick<EventData, 'featured'>): number =>
  event.featured ? FEATURED_FRAMES : QUICK_FRAMES;

export interface CardRange {
  event: EventData;
  from: number;
  durationInFrames: number;
}

/** Hook'tan hemen sonra başlayan, her kartın kendi süresine göre art arda dizilmiş aralıkları hesaplar. */
export const computeCardRanges = (events: EventData[]): CardRange[] => {
  let cursor = HOOK_FRAMES;
  return events.map((event) => {
    const durationInFrames = frameDurationForEvent(event);
    const range: CardRange = { event, from: cursor, durationInFrames };
    cursor += durationInFrames;
    return range;
  });
};

/** Etkinlikleri (zaten tarih sıralı) gün gün gruplar; boş gün ekranı olmaz. */
export const groupByDay = (events: EventData[]): EventData[][] => {
  const days: EventData[][] = [];
  for (const e of events) {
    const last = days[days.length - 1];
    if (last && last[0].startDate === e.startDate) last.push(e);
    else days.push([e]);
  }
  return days;
};

export const computeTotalDuration = (events: EventData[], layout: ReelLayout = 'cards'): number => {
  const body =
    layout === 'days'
      ? groupByDay(events).length * DAY_FRAMES
      : events.reduce((acc, e) => acc + frameDurationForEvent(e), 0);
  return HOOK_FRAMES + body + OUTRO_FRAMES;
};
