import React from 'react';
import { Composition } from 'remotion';
import { WeeklyReel, WeeklyReelProps } from './WeeklyReel';

const FPS = 30;
const WIDTH = 1080;
const HEIGHT = 1920;
const INTRO = 60;
const OUTRO = 60;
const PER_CARD = 90;

export const RemotionRoot: React.FC = () => {
  const defaultProps: WeeklyReelProps = {
    events: [],
    weekRange: '02 Şubat - 08 Şubat',
    backgroundVariant: 0,
  };

  const duration = INTRO + 9 * PER_CARD + OUTRO;

  return (
    <>
      <Composition
        id="WeeklyReel"
        component={WeeklyReel}
        durationInFrames={duration}
        fps={FPS}
        width={WIDTH}
        height={HEIGHT}
        defaultProps={defaultProps}
        calculateMetadata={({ props }) => ({
          durationInFrames: INTRO + Math.max(props.events?.length ?? 9, 1) * PER_CARD + OUTRO,
        })}
      />
    </>
  );
};
