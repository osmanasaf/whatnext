import { EventData } from './EventCard';

export const FPS = 30;
export const HOOK_FRAMES = 36; // ~1.2sn
export const FEATURED_FRAMES = 75; // ~2.5sn — kategori lideri (en yüksek skorlu)
export const QUICK_FRAMES = 35; // ~1.17sn — hızlı geçiş
export const OUTRO_FRAMES = 60; // ~2sn

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
