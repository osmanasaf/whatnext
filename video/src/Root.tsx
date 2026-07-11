import React from 'react';
import { Composition } from 'remotion';
import { WeeklyReel, WeeklyReelProps } from './WeeklyReel';
import { EventData } from './EventCard';
import { FPS, computeTotalDuration } from './timing';

const WIDTH = 1080;
const HEIGHT = 1920;

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
    </>
  );
};
