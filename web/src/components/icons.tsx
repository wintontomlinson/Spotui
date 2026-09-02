import type { SVGProps } from 'react';

type IconProps = SVGProps<SVGSVGElement> & { size?: number };

function Icon({ size = 24, children, ...rest }: IconProps) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="currentColor"
      aria-hidden="true"
      focusable="false"
      {...rest}
    >
      {children}
    </svg>
  );
}

export function HomeIcon(props: IconProps) {
  return (
    <Icon {...props}>
      <path d="M12 3 2 11h3v9h5v-6h4v6h5v-9h3z" />
    </Icon>
  );
}

export function SearchIcon(props: IconProps) {
  return (
    <Icon {...props}>
      <path d="M10.5 3a7.5 7.5 0 1 0 4.55 13.46l4.24 4.25 1.42-1.42-4.25-4.24A7.5 7.5 0 0 0 10.5 3Zm0 2a5.5 5.5 0 1 1 0 11 5.5 5.5 0 0 1 0-11Z" />
    </Icon>
  );
}

export function LibraryIcon(props: IconProps) {
  return (
    <Icon {...props}>
      <path d="M3 3h2v18H3zm4 0h2v18H7zm5.2.5 1.93-.52 4.66 17.39-1.93.52z" />
    </Icon>
  );
}

export function PlayIcon(props: IconProps) {
  return (
    <Icon {...props}>
      <path d="M7 4.5v15l12-7.5z" />
    </Icon>
  );
}

export function PauseIcon(props: IconProps) {
  return (
    <Icon {...props}>
      <path d="M6 4h4v16H6zm8 0h4v16h-4z" />
    </Icon>
  );
}

export function NextIcon(props: IconProps) {
  return (
    <Icon {...props}>
      <path d="M5 4.5 15 12 5 19.5zM17 4h2.5v16H17z" />
    </Icon>
  );
}

export function PreviousIcon(props: IconProps) {
  return (
    <Icon {...props}>
      <path d="M19 4.5 9 12l10 7.5zM4.5 4H7v16H4.5z" />
    </Icon>
  );
}

export function ShuffleIcon(props: IconProps) {
  return (
    <Icon {...props}>
      <path d="M16 3.5 21 8l-5 4.5V10h-1.6c-1.2 0-2 .4-2.9 1.6l-.9 1.2-1.3-1.7.8-1C10.4 8.4 11.9 8 13.4 8H16zM3 8h3.2c.9 0 1.6.2 2.2.7l-1.3 1.7C6.9 10.1 6.6 10 6.2 10H3zm13 7.5V13l5 4.5-5 4.5v-2.5h-2.6c-1.5 0-3-.4-4.3-2.1l-.8-1 1.3-1.7.9 1.2c.9 1.2 1.7 1.6 2.9 1.6zM3 14h3.2c.4 0 .7-.1 1-.4l1.3 1.7c-.6.5-1.3.7-2.2.7H3z" />
    </Icon>
  );
}

export function RepeatIcon(props: IconProps) {
  return (
    <Icon {...props}>
      <path d="M7 5h10a4 4 0 0 1 4 4v2h-2V9a2 2 0 0 0-2-2H7v2.5L3 6l4-3.5zm10 14H7a4 4 0 0 1-4-4v-2h2v2a2 2 0 0 0 2 2h10v-2.5l4 3.5-4 3.5z" />
    </Icon>
  );
}

export function HeartIcon(props: IconProps) {
  return (
    <Icon {...props}>
      <path
        d="M12 20.5 4.6 13a4.8 4.8 0 0 1 0-6.8 4.8 4.8 0 0 1 6.8 0l.6.6.6-.6a4.8 4.8 0 0 1 6.8 6.8z"
        fill="none"
        stroke="currentColor"
        strokeWidth="2"
      />
    </Icon>
  );
}

export function HeartFilledIcon(props: IconProps) {
  return (
    <Icon {...props}>
      <path d="M12 20.8 4.3 13a5.2 5.2 0 0 1 7.4-7.3l.3.3.3-.3A5.2 5.2 0 0 1 19.7 13z" />
    </Icon>
  );
}

export function PlusIcon(props: IconProps) {
  return (
    <Icon {...props}>
      <path d="M11 4h2v7h7v2h-7v7h-2v-7H4v-2h7z" />
    </Icon>
  );
}

export function CheckCircleIcon(props: IconProps) {
  return (
    <Icon {...props}>
      <path d="M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20zm-1 14.4-4-4 1.4-1.4L11 13.6l5.6-5.6L18 9.4z" />
    </Icon>
  );
}

export function ChevronDownIcon(props: IconProps) {
  return (
    <Icon {...props}>
      <path d="M6.7 8.3 12 13.6l5.3-5.3 1.4 1.4L12 16.4 5.3 9.7z" />
    </Icon>
  );
}

export function ChevronLeftIcon(props: IconProps) {
  return (
    <Icon {...props}>
      <path d="M15.7 5.3 9 12l6.7 6.7-1.4 1.4L6.2 12l8.1-8.1z" />
    </Icon>
  );
}

export function LyricsIcon(props: IconProps) {
  return (
    <Icon {...props}>
      <path d="M12 2a3 3 0 0 0-3 3v6a3 3 0 0 0 6 0V5a3 3 0 0 0-3-3zM6 10H4v1a8 8 0 0 0 7 7.94V22h2v-3.06A8 8 0 0 0 20 11v-1h-2v1a6 6 0 0 1-12 0z" />
    </Icon>
  );
}

export function DevicesIcon(props: IconProps) {
  return (
    <Icon {...props}>
      <path d="M3 5h13v2H5v8h6v2H3zm15 3h3v11h-8V8zm-3 2v7h6v-7z" />
    </Icon>
  );
}

export function ExplicitBadge() {
  return (
    <span
      className="ml-1 inline-flex h-[15px] w-[15px] shrink-0 items-center justify-center rounded-[2px] bg-white/60 text-[10px] font-bold leading-none text-black"
      title="Explicit"
      aria-label="Explicit"
    >
      E
    </span>
  );
}
