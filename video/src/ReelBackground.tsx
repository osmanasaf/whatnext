import React from 'react';
import { AbsoluteFill } from 'remotion';

const VARIANTS: Array<{ gradient: string; blobs: Array<React.CSSProperties>; icons: Array<{ emoji: string; style: React.CSSProperties }> }> = [
  {
    gradient: 'linear-gradient(165deg, #2d3748 0%, #1a202c 35%, #2d1b4e 70%, #1a1a2e 100%)',
    blobs: [
      { top: '-15%', right: '-10%', width: '50%', height: '50%', background: 'radial-gradient(circle, rgba(99,102,241,0.12) 0%, transparent 70%)' },
      { bottom: '-20%', left: '-15%', width: '60%', height: '50%', background: 'radial-gradient(circle, rgba(139,92,246,0.1) 0%, transparent 70%)' },
      { top: '40%', left: '50%', transform: 'translate(-50%, -50%)', width: '80%', height: '60%', borderRadius: '40%', background: 'radial-gradient(ellipse, rgba(67,56,202,0.08) 0%, transparent 70%)' },
    ],
    icons: [{ emoji: '♪', style: { top: '20%', left: '10%', fontSize: 80, opacity: 0.06 } }, { emoji: '🎭', style: { top: '60%', right: '15%', fontSize: 70, opacity: 0.05 } }, { emoji: '🎤', style: { bottom: '25%', left: '20%', fontSize: 65, opacity: 0.05 } }],
  },
  {
    gradient: 'linear-gradient(150deg, #1e3a5f 0%, #0f172a 40%, #312e81 80%, #1e1b4b 100%)',
    blobs: [
      { top: '-10%', left: '-5%', width: '45%', height: '45%', background: 'radial-gradient(circle, rgba(59,130,246,0.1) 0%, transparent 70%)' },
      { bottom: '-15%', right: '-8%', width: '55%', height: '45%', background: 'radial-gradient(circle, rgba(168,85,247,0.09) 0%, transparent 70%)' },
    ],
    icons: [{ emoji: '🎵', style: { top: '15%', right: '12%', fontSize: 72, opacity: 0.05 } }, { emoji: '🎬', style: { bottom: '30%', right: '18%', fontSize: 68, opacity: 0.05 } }],
  },
  {
    gradient: 'linear-gradient(180deg, #334155 0%, #1e293b 30%, #4c1d95 65%, #0f172a 100%)',
    blobs: [
      { top: '30%', right: '-12%', width: '50%', height: '50%', background: 'radial-gradient(circle, rgba(124,58,237,0.11) 0%, transparent 70%)' },
      { bottom: '20%', left: '-10%', width: '48%', height: '48%', background: 'radial-gradient(circle, rgba(6,182,212,0.08) 0%, transparent 70%)' },
    ],
    icons: [{ emoji: '🎪', style: { top: '70%', left: '12%', fontSize: 75, opacity: 0.05 } }, { emoji: '♫', style: { top: '25%', left: '15%', fontSize: 70, opacity: 0.06 } }],
  },
  {
    gradient: 'linear-gradient(170deg, #0f172a 0%, #1e293b 25%, #312e81 60%, #1e1b4b 100%)',
    blobs: [
      { top: '-8%', left: '50%', transform: 'translateX(-50%)', width: '70%', height: '40%', borderRadius: '50%', background: 'radial-gradient(ellipse, rgba(99,102,241,0.09) 0%, transparent 70%)' },
      { bottom: '-12%', left: '20%', width: '45%', height: '45%', background: 'radial-gradient(circle, rgba(139,92,246,0.08) 0%, transparent 70%)' },
    ],
    icons: [{ emoji: '🎸', style: { top: '55%', right: '10%', fontSize: 68, opacity: 0.05 } }, { emoji: '🎭', style: { top: '18%', right: '22%', fontSize: 65, opacity: 0.05 } }],
  },
  {
    gradient: 'linear-gradient(160deg, #1a202c 0%, #2d3748 35%, #553c9a 70%, #1a1a2e 100%)',
    blobs: [
      { top: '-5%', right: '-8%', width: '55%', height: '50%', background: 'radial-gradient(circle, rgba(167,139,250,0.1) 0%, transparent 70%)' },
      { bottom: '-18%', left: '-12%', width: '52%', height: '48%', background: 'radial-gradient(circle, rgba(99,102,241,0.09) 0%, transparent 70%)' },
      { top: '45%', left: '45%', width: '50%', height: '50%', background: 'radial-gradient(circle, rgba(67,56,202,0.06) 0%, transparent 70%)' },
    ],
    icons: [{ emoji: '🎤', style: { top: '22%', left: '8%', fontSize: 78, opacity: 0.05 } }, { emoji: '🎶', style: { bottom: '22%', right: '12%', fontSize: 70, opacity: 0.05 } }],
  },
];

export const ReelBackground: React.FC<{ variant?: number }> = ({ variant = 0 }) => {
  const v = VARIANTS[Math.min(Math.max(0, variant), VARIANTS.length - 1)];

  return (
    <AbsoluteFill style={{ background: v.gradient }}>
      {v.blobs.map((blob, i) => (
        <div
          key={i}
          style={{
            position: 'absolute',
            borderRadius: '50%',
            ...blob,
          }}
        />
      ))}
      {v.icons.map((icon, i) => (
        <div key={i} style={{ position: 'absolute', ...icon.style }}>
          {icon.emoji}
        </div>
      ))}
    </AbsoluteFill>
  );
};
