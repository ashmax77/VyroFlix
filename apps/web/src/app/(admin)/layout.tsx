// Studio pages. The admin shell (sidebar) is added in Phase 4; for now the page renders its own header.
export default function AdminLayout({ children }: { children: React.ReactNode }) {
  return <main className="min-h-screen">{children}</main>;
}
