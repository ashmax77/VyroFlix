// Sign-in and register render their own full-page shell (AuthShell); no site chrome around them.
export default function AuthLayout({ children }: { children: React.ReactNode }) {
  return <main className="min-h-screen">{children}</main>;
}
