import Link from 'next/link';

// Shown for any URL that no route group matches.
export default function NotFound() {
  return (
    <main className="grid min-h-screen place-items-center px-6">
      <div className="max-w-md text-center">
        <p className="font-heading text-6xl text-primary">404</p>
        <h1 className="mt-4 text-2xl">This page isn&apos;t on the schedule.</h1>
        <p className="mt-2 text-muted-foreground">The link may be old, or the title may have been removed.</p>
        <Link href="/browse" className="mt-8 inline-flex h-10 items-center rounded-card bg-primary px-5 text-sm font-semibold text-primary-foreground">
          Back to browse
        </Link>
      </div>
    </main>
  );
}
