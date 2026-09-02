'use client';

import { useEffect, useState } from 'react';

import { formatDuration } from '@/lib/format';

interface SeekbarProps {
  positionMs: number;
  durationMs: number;
  onSeek: (positionMs: number) => void;
  /** Compact variant used by the mini player — no timestamps, thinner track. */
  compact?: boolean;
  disabled?: boolean;
}

/** Stands in for the app's CustomSlider. */
export function Seekbar({
  positionMs,
  durationMs,
  onSeek,
  compact = false,
  disabled = false,
}: SeekbarProps) {
  const [dragValue, setDragValue] = useState<number | null>(null);

  // Drop the local drag value once the reported position catches up.
  useEffect(() => {
    if (dragValue === null) return;
    if (Math.abs(positionMs - dragValue) < 900) setDragValue(null);
  }, [positionMs, dragValue]);

  const max = Math.max(durationMs, 1);
  const value = Math.min(dragValue ?? positionMs, max);
  const percent = (value / max) * 100;

  if (compact) {
    return (
      <div className="h-[3px] w-full overflow-hidden rounded-full bg-white/25" aria-hidden="true">
        <div
          className="h-full rounded-full bg-white/90 transition-[width] duration-300 ease-linear"
          style={{ width: `${percent}%` }}
        />
      </div>
    );
  }

  return (
    <div className="w-full">
      <div className="relative flex items-center">
        <div className="pointer-events-none absolute inset-x-0 h-1 overflow-hidden rounded-full bg-white/30">
          <div className="h-full rounded-full bg-white" style={{ width: `${percent}%` }} />
        </div>
        <input
          type="range"
          className="spotui-slider relative z-10 h-1 w-full"
          min={0}
          max={max}
          step={1000}
          value={value}
          disabled={disabled}
          aria-label="Seek"
          onChange={(event) => setDragValue(Number(event.target.value))}
          onPointerUp={(event) => onSeek(Number((event.target as HTMLInputElement).value))}
          onKeyUp={(event) => {
            if (['ArrowLeft', 'ArrowRight', 'Home', 'End'].includes(event.key)) {
              onSeek(Number((event.target as HTMLInputElement).value));
            }
          }}
        />
      </div>
      <div className="mt-1 flex justify-between text-[11px] tabular-nums text-white/60">
        <span>{formatDuration(value)}</span>
        <span>{formatDuration(durationMs)}</span>
      </div>
    </div>
  );
}
