import React from 'react';
import { AbsoluteFill, useCurrentFrame, interpolate } from 'remotion';

const VARIANTS = [
  {
    gradient: 'linear-gradient(145deg, #0f0c29 0%, #302b63 50%, #24243e 100%)',
    accent: 'rgba(139,92,246,0.18)',
    accent2: 'rgba(99,102,241,0.12)',
    lineColor: 'rgba(167,139,250,0.07)',
  },
  {
    gradient: 'linear-gradient(145deg, #0a0a1a 0%, #1a1a4e 50%, #0d0d2b 100%)',
    accent: 'rgba(59,130,246,0.16)',
    accent2: 'rgba(168,85,247,0.12)',
    lineColor: 'rgba(99,102,241,0.07)',
  },
  {
    gradient: 'linear-gradient(145deg, #0f0c29 0%, #1e0a3c 50%, #0a0a1a 100%)',
    accent: 'rgba(124,58,237,0.18)',
    accent2: 'rgba(6,182,212,0.08)',
    lineColor: 'rgba(139,92,246,0.07)',
  },
  {
    gradient: 'linear-gradient(145deg, #050510 0%, #1a1040 50%, #0f0c29 100%)',
    accent: 'rgba(99,102,241,0.16)',
    accent2: 'rgba(139,92,246,0.1)',
    lineColor: 'rgba(167,139,250,0.06)',
  },
  {
    gradient: 'linear-gradient(145deg, #0a0a1a 0%, #2d1b4e 50%, #1a1a2e 100%)',
    accent: 'rgba(167,139,250,0.15)',
    accent2: 'rgba(99,102,241,0.1)',
    lineColor: 'rgba(139,92,246,0.07)',
  },
];

// SVG ikonlar - emoji yerine vektörel
const MusicNoteIcon: React.FC<{ size: number; color: string; opacity: number }> = ({ size, color, opacity }) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none" style={{ opacity }}>
    <path d="M9 18V5l12-2v13" stroke={color} strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/>
    <circle cx="6" cy="18" r="3" stroke={color} strokeWidth="1.5"/>
    <circle cx="18" cy="16" r="3" stroke={color} strokeWidth="1.5"/>
  </svg>
);

const StarIcon: React.FC<{ size: number; color: string; opacity: number }> = ({ size, color, opacity }) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none" style={{ opacity }}>
    <polygon points="12,2 15.09,8.26 22,9.27 17,14.14 18.18,21.02 12,17.77 5.82,21.02 7,14.14 2,9.27 8.91,8.26" stroke={color} strokeWidth="1.2" strokeLinejoin="round"/>
  </svg>
);

const DiamondIcon: React.FC<{ size: number; color: string; opacity: number }> = ({ size, color, opacity }) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none" style={{ opacity }}>
    <path d="M12 2L22 9L12 22L2 9Z" stroke={color} strokeWidth="1.2" strokeLinejoin="round"/>
    <path d="M2 9H22" stroke={color} strokeWidth="1.2"/>
  </svg>
);

const CircleIcon: React.FC<{ size: number; color: string; opacity: number }> = ({ size, color, opacity }) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none" style={{ opacity }}>
    <circle cx="12" cy="12" r="10" stroke={color} strokeWidth="1.2"/>
    <circle cx="12" cy="12" r="5" stroke={color} strokeWidth="1"/>
    <circle cx="12" cy="12" r="1.5" fill={color}/>
  </svg>
);

const TicketIcon: React.FC<{ size: number; color: string; opacity: number }> = ({ size, color, opacity }) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none" style={{ opacity }}>
    <path d="M2 9a1 1 0 0 1 1-1h18a1 1 0 0 1 1 1v2a2 2 0 0 0 0 4v2a1 1 0 0 1-1 1H3a1 1 0 0 1-1-1v-2a2 2 0 0 0 0-4V9Z" stroke={color} strokeWidth="1.2"/>
    <line x1="9" y1="8" x2="9" y2="16" stroke={color} strokeWidth="1.2" strokeDasharray="2 2"/>
  </svg>
);

export const ReelBackground: React.FC<{ variant?: number }> = ({ variant = 0 }) => {
  const frame = useCurrentFrame();
  const v = VARIANTS[Math.min(Math.max(0, variant), VARIANTS.length - 1)];

  const pulse = interpolate(Math.sin(frame * 0.03), [-1, 1], [0.85, 1.0]);
  const pulse2 = interpolate(Math.sin(frame * 0.02 + 1), [-1, 1], [0.9, 1.05]);
  const rotate = interpolate(frame, [0, 300], [0, 360], { extrapolateRight: 'wrap' });

  const iconColor = '#a78bfa';

  return (
    <AbsoluteFill style={{ background: v.gradient, overflow: 'hidden' }}>

      {/* Ana glow blob'lar */}
      <div style={{
        position: 'absolute', top: '-20%', right: '-15%',
        width: '65%', height: '65%', borderRadius: '50%',
        background: `radial-gradient(circle, ${v.accent} 0%, transparent 70%)`,
        transform: `scale(${pulse})`,
      }} />
      <div style={{
        position: 'absolute', bottom: '-25%', left: '-20%',
        width: '70%', height: '60%', borderRadius: '50%',
        background: `radial-gradient(circle, ${v.accent2} 0%, transparent 70%)`,
        transform: `scale(${pulse2})`,
      }} />
      <div style={{
        position: 'absolute', top: '35%', left: '30%',
        width: '55%', height: '55%', borderRadius: '50%',
        background: `radial-gradient(ellipse, ${v.accent} 0%, transparent 65%)`,
        transform: `scale(${pulse})`,
      }} />

      {/* Izgara çizgileri */}
      {[...Array(8)].map((_, i) => (
        <div key={`h${i}`} style={{
          position: 'absolute',
          left: 0, right: 0,
          top: `${i * 14}%`,
          height: 1,
          background: `linear-gradient(90deg, transparent 0%, ${v.lineColor} 30%, ${v.lineColor} 70%, transparent 100%)`,
        }} />
      ))}
      {[...Array(5)].map((_, i) => (
        <div key={`v${i}`} style={{
          position: 'absolute',
          top: 0, bottom: 0,
          left: `${i * 25}%`,
          width: 1,
          background: `linear-gradient(180deg, transparent 0%, ${v.lineColor} 30%, ${v.lineColor} 70%, transparent 100%)`,
        }} />
      ))}

      {/* SVG dekoratif ikonlar */}
      <div style={{ position: 'absolute', top: '12%', left: '8%', transform: `rotate(${rotate * 0.3}deg)` }}>
        <MusicNoteIcon size={72} color={iconColor} opacity={0.12} />
      </div>
      <div style={{ position: 'absolute', top: '18%', right: '10%', transform: `rotate(${-rotate * 0.2}deg)` }}>
        <StarIcon size={56} color={iconColor} opacity={0.1} />
      </div>
      <div style={{ position: 'absolute', bottom: '28%', left: '6%', transform: `rotate(${rotate * 0.15}deg)` }}>
        <DiamondIcon size={64} color={iconColor} opacity={0.1} />
      </div>
      <div style={{ position: 'absolute', bottom: '15%', right: '8%', transform: `rotate(${-rotate * 0.25}deg)` }}>
        <CircleIcon size={60} color={iconColor} opacity={0.09} />
      </div>
      <div style={{ position: 'absolute', top: '55%', left: '5%', transform: `rotate(${rotate * 0.1}deg)` }}>
        <TicketIcon size={52} color={iconColor} opacity={0.09} />
      </div>
      <div style={{ position: 'absolute', top: '42%', right: '6%', transform: `rotate(${-rotate * 0.18}deg)` }}>
        <MusicNoteIcon size={48} color={iconColor} opacity={0.08} />
      </div>

      {/* Köşe vurgu çizgileri */}
      <div style={{
        position: 'absolute', top: 0, left: 0,
        width: 120, height: 120,
        borderTop: '2px solid rgba(167,139,250,0.2)',
        borderLeft: '2px solid rgba(167,139,250,0.2)',
      }} />
      <div style={{
        position: 'absolute', bottom: 0, right: 0,
        width: 120, height: 120,
        borderBottom: '2px solid rgba(167,139,250,0.2)',
        borderRight: '2px solid rgba(167,139,250,0.2)',
      }} />
    </AbsoluteFill>
  );
};
