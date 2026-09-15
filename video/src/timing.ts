import { EventData } from './EventCard';

export const FPS = 30;
export const HOOK_FRAMES = 45; // 1.5sn — afiş mozaiği + başlık okunabilsin
export const FEATURED_FRAMES = 90; // 3sn — kategori lideri (en yüksek skorlu)
// 2sn: 35 kare (~1.2sn) afişi, tarihi ve başlığı okumaya yetmiyordu (2026-09-15 geri bildirimi: "çok kötü").
export const QUICK_FRAMES = 60;
export const OUTRO_FRAMES = 60; // 2sn

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

export const computeTotalDuration = (events: EventData[]): number => {
  const cardsTotal = events.reduce((acc, e) => acc + frameDurationForEvent(e), 0);
  return HOOK_FRAMES + cardsTotal + OUTRO_FRAMES;
};
