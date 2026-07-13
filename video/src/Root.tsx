import React from 'react';
import { Composition, Still } from 'remotion';
import { WeeklyReel, WeeklyReelProps } from './WeeklyReel';
import { EventData } from './EventCard';
import { FPS, computeTotalDuration } from './timing';
import { DailyPostStill, DailyPostStillProps } from './stills/DailyPostStill';
import { SmartStoryStill, SmartStoryStillProps } from './stills/SmartStoryStill';
import { CoverStill, CoverStillProps } from './stills/CoverStill';

const WIDTH = 1080;
const HEIGHT = 1920;

const STILL_WIDTH = 1080;
const STILL_HEIGHT = 1350; // 4:5 — el yapımı şablonların 1:1 kısıtı (kırpma sorunu) burada geçerli değil
const STORY_STILL_HEIGHT = 1920; // 9:16, statik story şablonlarıyla aynı oran (Faz 3.1)

const DEFAULT_PALETTE = { bg1: '#0f0c29', bg2: '#302b63', accent: '#a78bfa', text: '#f8fafc' };

// Stüdyo önizlemesinde calculateMetadata henüz gerçek props'u görmeden önceki tahmini süre
// (tipik kullanım: 3 featured + 6 quick). Gerçek render'da her zaman calculateMetadata geçerli olur.
const PREVIEW_EVENTS: Pick<EventData, 'featured'>[] = [
  ...Array(3).fill({ featured: true }),
  ...Array(6).fill({ featured: false }),
];

export const RemotionRoot: React.FC = () => {
  const defaultProps: WeeklyReelProps = {
    events: [],
    weekRange: '02 Şubat - 08 Şubat',
    backgroundVariant: 0,
  };

  const dailyPostDefaults: DailyPostStillProps = {
    eventType: 'concert',
    monthName: 'july',
    palette: DEFAULT_PALETTE,
    date: '2026-07-17',
    events: [],
  };

  const smartStoryDefaults: SmartStoryStillProps = {
    monthName: 'july',
    palette: DEFAULT_PALETTE,
    date: '2026-07-17',
    events: [],
  };

  const coverDefaults: CoverStillProps = {
    monthName: 'july',
    palette: DEFAULT_PALETTE,
    date: '2026-07-17',
    eventCount: 0,
  };

  return (
    <>
      <Composition
        id="WeeklyReel"
        component={WeeklyReel as unknown as React.ComponentType<Record<string, unknown>>}
        durationInFrames={computeTotalDuration(PREVIEW_EVENTS as EventData[])}
        fps={FPS}
        width={WIDTH}
        height={HEIGHT}
        defaultProps={defaultProps}
        calculateMetadata={({ props }) => {
          const p = props as unknown as WeeklyReelProps;
          return {
            durationInFrames: computeTotalDuration(p.events ?? []),
          };
        }}
      />

      <Still
        id="DailyPostStill"
        component={DailyPostStill as unknown as React.ComponentType<Record<string, unknown>>}
        width={STILL_WIDTH}
        height={STILL_HEIGHT}
        defaultProps={dailyPostDefaults}
      />

      <Still
        id="SmartStoryStill"
        component={SmartStoryStill as unknown as React.ComponentType<Record<string, unknown>>}
        width={STILL_WIDTH}
        height={STORY_STILL_HEIGHT}
        defaultProps={smartStoryDefaults}
      />

      <Still
        id="CoverStill"
        component={CoverStill as unknown as React.ComponentType<Record<string, unknown>>}
        width={STILL_WIDTH}
        height={STILL_HEIGHT}
        defaultProps={coverDefaults}
      />
    </>
  );
};
