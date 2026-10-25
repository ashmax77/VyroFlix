import { createBrowserClient } from '@supabase/ssr';

export function createClient() {
  const supabaseUrl =
    process.env.NEXT_PUBLIC_SUPABASE_URL ||
    'https://sxyrwfjwdxsbiuqmvvac.supabase.co';
  const supabaseAnonKey =
    process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY ||
    'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InN4eXJ3Zmp3ZHhzYml1cW12dmFjIiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTI4Nzc1MTQsImV4cCI6MjEwODQ1MzUxNH0.rsgYJgciket_JhY352cY9vcELHQ9IMKP7C29CmjNmaE';

  return createBrowserClient(supabaseUrl, supabaseAnonKey);
}
