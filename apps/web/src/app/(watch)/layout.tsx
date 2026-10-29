// The player is full-bleed: no navbar or footer competing with the picture.
export default function WatchLayout({ children }: { children: React.ReactNode }) {
  return <main className="min-h-screen bg-black">{children}</main>;
}
