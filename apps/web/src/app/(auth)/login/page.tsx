'use client';

import React, { useState, Suspense } from 'react';
import Link from 'next/link';
import { useRouter, useSearchParams } from 'next/navigation';
import { zodResolver } from '@hookform/resolvers/zod';
import { AlertCircle, Eye, EyeOff, Loader2 } from 'lucide-react';
import { useForm } from 'react-hook-form';
import { z } from 'zod';
import { AuthShell, Divider, FieldError, GoogleButton, inputClass } from '@/components/layout/AuthShell';
import { Button } from '@/components/ui/button';
import { Checkbox } from '@/components/ui/checkbox';
import { Spinner } from '@/components/common';
import { useAuth } from '@/lib/auth-context';

const schema = z.object({
  email: z.string().email('Please enter a valid email address'),
  password: z.string().min(6, 'Password must be at least 6 characters'),
  remember: z.boolean().optional(),
});

type FormData = z.infer<typeof schema>;

function LoginForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const requestedRedirect = searchParams.get('redirect') || '/browse';
  // Same-site relative paths only (prevents open redirects via ?redirect=https://evil.example).
  const redirect =
    requestedRedirect.startsWith('/') && !requestedRedirect.startsWith('//') ? requestedRedirect : '/browse';
  const { signInWithPassword, signInWithOAuth } = useAuth();

  const [showPassword, setShowPassword] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const form = useForm<FormData>({
    resolver: zodResolver(schema),
    defaultValues: { email: '', password: '', remember: true },
  });
  const { errors, isSubmitting } = form.formState;

  const onSubmit = form.handleSubmit(async (values) => {
    setErrorMessage(null);
    const { error } = await signInWithPassword(values.email, values.password);
    if (error) {
      setErrorMessage(error.message || 'Invalid email or password. Please try again.');
      return;
    }
    router.push(redirect);
    router.refresh();
  });

  const handleGoogleSignIn = async () => {
    setErrorMessage(null);
    const { error } = await signInWithOAuth('google');
    if (error) {
      setErrorMessage(error.message || 'Google sign-in failed. Please try again.');
    }
  };

  return (
    <>
      {errorMessage && (
        <div role="alert" className="mb-5 flex items-center gap-2 rounded-xl border border-danger/40 bg-danger/10 px-4 py-3 text-sm text-foreground">
          <AlertCircle className="h-4 w-4 text-danger shrink-0" />
          <span>{errorMessage}</span>
        </div>
      )}

      <form onSubmit={onSubmit} noValidate className="space-y-4">
        <div>
          <label htmlFor="email" className="mb-1.5 block text-sm font-medium text-foreground">
            Email
          </label>
          <input
            id="email"
            type="email"
            autoComplete="email"
            placeholder="name@domain.com"
            className={inputClass}
            aria-invalid={!!errors.email}
            {...form.register('email')}
          />
          <FieldError message={errors.email?.message} />
        </div>

        <div>
          <label htmlFor="password" className="mb-1.5 block text-sm font-medium text-foreground">
            Password
          </label>
          <div className="relative">
            <input
              id="password"
              type={showPassword ? 'text' : 'password'}
              autoComplete="current-password"
              placeholder="••••••••"
              className={inputClass}
              aria-invalid={!!errors.password}
              {...form.register('password')}
            />
            <button
              type="button"
              aria-label={showPassword ? 'Hide password' : 'Show password'}
              onClick={() => setShowPassword((s) => !s)}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground transition-colors"
            >
              {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
            </button>
          </div>
          <FieldError message={errors.password?.message} />
        </div>

        <div className="flex items-center justify-between text-sm pt-1">
          <label className="flex items-center gap-2 text-muted-foreground cursor-pointer">
            <Checkbox
              checked={form.watch('remember')}
              onCheckedChange={(c) => form.setValue('remember', !!c)}
            />
            <span>Remember me</span>
          </label>
        </div>

        <Button
          type="submit"
          variant="default"
          className="w-full mt-2 h-11 text-base font-semibold"
          disabled={isSubmitting}
        >
          {isSubmitting ? (
            <>
              <Loader2 className="h-4 w-4 animate-spin mr-2" />
              Signing in...
            </>
          ) : (
            'Sign in'
          )}
        </Button>
      </form>

      <Divider />

      <GoogleButton onClick={handleGoogleSignIn} disabled={isSubmitting} />

      <p className="mt-6 text-center text-sm text-muted-foreground">
        New to VyroFlix?{' '}
        <Link href="/register" className="font-semibold text-foreground hover:text-primary transition-colors">
          Create an account
        </Link>
      </p>
    </>
  );
}

export default function LoginPage() {
  return (
    <AuthShell
      title="Sign in"
      subtitle="Sign in to your VyroFlix account to keep watching your favorite titles."
    >
      <Suspense fallback={<Spinner label="Loading sign in..." />}>
        <LoginForm />
      </Suspense>
    </AuthShell>
  );
}
