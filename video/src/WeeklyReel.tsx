import React from 'react';
import { AbsoluteFill, Sequence } from 'remotion';
import { EventCard, EventData } from './EventCard';
import { ReelBackground } from './ReelBackground';

export interface WeeklyReelProps {
  events: EventData[];
  weekRange: string;
  backgroundVariant?: number;
}

const FRAMES_PER_CARD = 90;
const INTRO_FRAMES = 60;
const OUTRO_FRAMES = 60;

const FOOTER_STYLE: React.CSSProperties = {
  position: 'absolute',
  bottom: 24,
  left: 0,
  right: 0,
  display: 'flex',
  justifyContent: 'center',
  gap: 24,
  fontSize: 18,
  color: 'rgba(255,255,255,0.5)',
  fontFamily: 'system-ui, sans-serif',
};

export const WeeklyReel: React.FC<WeeklyReelProps> = ({ events, weekRange, backgroundVariant = 0 }) => {
  return (
    <AbsoluteFill>
      <ReelBackground variant={backgroundVariant} />
      {events.map((event, i) => (
        <Sequence
          key={event.id}
          from={INTRO_FRAMES + i * FRAMES_PER_CARD}
          durationInFrames={FRAMES_PER_CARD}
        >
          <EventCard event={event} index={i} />
        </Sequence>
      ))}
      <Sequence from={0} durationInFrames={INTRO_FRAMES}>
        <AbsoluteFill style={{
          justifyContent: 'center',
          alignItems: 'center',
          padding: 48,
        }}>
          <h1 style={{
            fontSize: 48,
            color: '#f8fafc',
            textAlign: 'center',
            margin: 0,
            fontFamily: 'system-ui, sans-serif',
          }}>
            Bu Hafta İstanbul
          </h1>
          <p style={{
            fontSize: 28,
            color: '#a5b4fc',
            marginTop: 16,
            fontFamily: 'system-ui, sans-serif',
          }}>
            {weekRange}
          </p>
        </AbsoluteFill>
      </Sequence>
      <Sequence from={INTRO_FRAMES + events.length * FRAMES_PER_CARD} durationInFrames={OUTRO_FRAMES}>
        <AbsoluteFill style={{
          justifyContent: 'center',
          alignItems: 'center',
        }}>
          <p style={{
            fontSize: 32,
            color: '#f8fafc',
            fontFamily: 'system-ui, sans-serif',
          }}>
            Detaylar profilde 🔗
          </p>
        </AbsoluteFill>
      </Sequence>
      <AbsoluteFill style={{ pointerEvents: 'none' }}>
        <div style={FOOTER_STYLE}>
          <span>instagram.com/whatnext.event</span>
          <span>whatnext.tr</span>
        </div>
      </AbsoluteFill>
    </AbsoluteFill>
  );
};
