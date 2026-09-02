import Link from 'next/link';
import clsx from 'clsx';

import type { MediaCardItem } from '@/lib/types';

import { Artwork } from './Artwork';

interface MediaCardProps {
  item: MediaCardItem;
  /** Width of the card in the horizontal carousels. */
  width?: number;
  /** When there is no `href`, the card becomes a button that runs this. */
  onSelect?: (item: MediaCardItem) => void;
}

/** Equivalent of HomeFeedCard — cover, title, subtitle, artists are circular. */
export function MediaCard({ item, width = 150, onSelect }: MediaCardProps) {
  const body = (
    <>
      <Artwork
        url={item.imageUrl}
        alt={item.title}
        rounded={item.round ? 'full' : 'md'}
        className={clsx('aspect-square w-full', item.round && 'rounded-full')}
      />
      <p className="mt-2 line-clamp-2 text-[13px] font-semibold leading-tight text-white">
        {item.title}
      </p>
      {item.subtitle ? (
        <p className="mt-[2px] line-clamp-1 text-[12px] text-text-secondary">{item.subtitle}</p>
      ) : null}
    </>
  );

  if (!item.href) {
    if (onSelect) {
      return (
        <button
          type="button"
          onClick={() => onSelect(item)}
          className="shrink-0 text-left transition-opacity hover:opacity-80"
          style={{ width }}
        >
          {body}
        </button>
      );
    }
    return (
      <div className="shrink-0" style={{ width }}>
        {body}
      </div>
    );
  }

  return (
    <Link
      href={item.href}
      className="shrink-0 transition-opacity hover:opacity-80"
      style={{ width }}
    >
      {body}
    </Link>
  );
}

interface SectionRowProps {
  title: string;
  items: MediaCardItem[];
  cardWidth?: number;
  onSelect?: (item: MediaCardItem) => void;
}

/** Equivalent of HomeFeedSection — a titled horizontal carousel. */
export function SectionRow({ title, items, cardWidth = 150, onSelect }: SectionRowProps) {
  if (items.length === 0) return null;
  return (
    <section className="mt-6">
      <h2 className="mb-3 px-4 text-[20px] font-bold tracking-tightish text-white">{title}</h2>
      <div className="no-scrollbar snap-row flex gap-4 overflow-x-auto px-4 pb-1">
        {items.map((item, index) => (
          <MediaCard
            key={`${item.uri}-${item.id}-${index}`}
            item={item}
            width={cardWidth}
            onSelect={onSelect}
          />
        ))}
      </div>
    </section>
  );
}

/** The two-column tile grid at the top of Home — HomeShortcutGrid. */
export function ShortcutGrid({ items }: { items: MediaCardItem[] }) {
  if (items.length === 0) return null;
  return (
    <div className="grid grid-cols-2 gap-2 px-4">
      {items.slice(0, 8).map((item) => {
        const content = (
          <>
            <Artwork
              url={item.imageUrl}
              alt={item.title}
              rounded="sm"
              className="h-full w-[54px] shrink-0"
            />
            <span className="line-clamp-2 px-2 text-[12px] font-bold leading-tight text-white">
              {item.title}
            </span>
          </>
        );
        return item.href ? (
          <Link
            key={`${item.uri}-${item.id}`}
            href={item.href}
            className="flex h-[54px] items-center overflow-hidden rounded-[4px] bg-white/[0.09] transition-colors hover:bg-white/[0.15]"
          >
            {content}
          </Link>
        ) : (
          <div
            key={`${item.uri}-${item.id}`}
            className="flex h-[54px] items-center overflow-hidden rounded-[4px] bg-white/[0.09]"
          >
            {content}
          </div>
        );
      })}
    </div>
  );
}
