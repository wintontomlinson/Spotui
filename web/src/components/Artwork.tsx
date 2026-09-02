import clsx from 'clsx';

interface ArtworkProps {
  url: string | null | undefined;
  alt: string;
  className?: string;
  rounded?: 'sm' | 'md' | 'lg' | 'full';
  /** Rendered when there is no image — GridBackground 0xFF2A2A2A in the app. */
  placeholderClassName?: string;
  priority?: boolean;
}

const ROUNDING = {
  sm: 'rounded-[4px]',
  md: 'rounded-[6px]',
  lg: 'rounded-[10px]',
  full: 'rounded-full',
} as const;

/**
 * Plain <img> rather than next/image: these URLs are already CDN-optimised by
 * Spotify at several sizes, so routing them through the Next image optimizer
 * only adds Vercel image-transform billing for no visual gain.
 */
export function Artwork({
  url,
  alt,
  className,
  rounded = 'md',
  placeholderClassName,
  priority = false,
}: ArtworkProps) {
  const shape = ROUNDING[rounded];

  if (!url) {
    return (
      <div
        className={clsx('bg-grid-bg', shape, className, placeholderClassName)}
        role="img"
        aria-label={alt}
      />
    );
  }

  return (
    // eslint-disable-next-line @next/next/no-img-element
    <img
      src={url}
      alt={alt}
      loading={priority ? 'eager' : 'lazy'}
      decoding="async"
      draggable={false}
      className={clsx('bg-grid-bg object-cover', shape, className)}
    />
  );
}
