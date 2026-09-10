'use client';

import { useRouter } from 'next/navigation';
import clsx from 'clsx';

import { Artwork } from './Artwork';
import { ChevronLeftIcon, PlayIcon, ShuffleIcon } from './icons';

interface DetailHeaderProps {
  title: string;
  subtitle?: string;
  meta?: string;
  imageUrl: string | null;
  round?: boolean;
  onPlay?: () => void;
  onShuffle?: () => void;
  /** Gradient tint behind the header, from the artwork palette. */
  accent?: string;
}

export function DetailHeader({
  title,
  subtitle,
  meta,
  imageUrl,
  round = false,
  onPlay,
  onShuffle,
  accent = '#2A2A2A',
}: DetailHeaderProps) {
  const router = useRouter();

  return (
    <header
      className="relative px-4 pb-4 pt-[max(env(safe-area-inset-top),12px)]"
      style={{ backgroundImage: `linear-gradient(to bottom, ${accent}, #130824)` }}
    >
      <button
        type="button"
        onClick={() => router.back()}
        aria-label="Go back"
        className="mb-4 p-1 text-white transition-opacity hover:opacity-70"
      >
        <ChevronLeftIcon size={26} />
      </button>

      <div className="flex flex-col items-center text-center">
        <Artwork
          url={imageUrl}
          alt={title}
          rounded={round ? 'full' : 'lg'}
          priority
          className={clsx('h-[200px] w-[200px] shadow-2xl', round && 'rounded-full')}
        />
        <h1 className="mt-4 text-balance text-[26px] font-extrabold leading-tight text-white">
          {title}
        </h1>
        {subtitle ? <p className="mt-1 text-[14px] text-white/80">{subtitle}</p> : null}
        {meta ? <p className="mt-1 text-[12px] text-text-secondary">{meta}</p> : null}
      </div>

      {onPlay || onShuffle ? (
        <div className="mt-5 flex items-center justify-center gap-3">
          {onShuffle ? (
            <button
              type="button"
              onClick={onShuffle}
              className="flex items-center gap-2 rounded-full border border-gold-500/40 px-4 py-2 text-[13px] font-bold text-gold-300 transition-colors hover:border-gold-400 hover:text-gold-200"
            >
              <ShuffleIcon size={18} />
              Shuffle
            </button>
          ) : null}
          {onPlay ? (
            <button
              type="button"
              onClick={onPlay}
              className="flex items-center gap-2 rounded-full btn-gold px-6 py-2.5 text-[14px] font-bold transition-transform hover:scale-[1.03]"
            >
              <PlayIcon size={18} />
              Play
            </button>
          ) : null}
        </div>
      ) : null}
    </header>
  );
}
