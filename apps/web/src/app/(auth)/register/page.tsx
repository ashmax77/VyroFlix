'use client';

import React, { useState, Suspense } from 'react';
import Link from 'next/link';
import { useRouter, useSearchParams } from 'next/navigation';
import { zodResolver } from '@hookform/resolvers/zod';
import { AlertCircle, CheckCircle, Loader2 } from 'lucide-react';
import { useForm } from 'react-hook-form';
import { z } from 'zod';
import { AuthShell, Divider, FieldError, GoogleButton, inputClass } from '@/components/layout/AuthShell';
import { Button } from '@/components/ui/button';
import { Checkbox } from '@/components/ui/checkbox';
import { Spinner } from '@/components/common';
import { useAuth } from '@/lib/auth-context';
import { cn } from '@/lib/utils';

const schema = z
  .object({
    name: z.string().min(2, 'Display name must be at least 2 characters'),
    email: z.string().email('Please enter a valid email address'),
    password: z.string().min(8, 'Password must be at least 8 characters'),
    confirm: z.string(),
    terms: z.literal(true, {
      errorMap: () => ({ message: 'You must accept the Terms of Service to continue' }),
    }),
  })
  .refine((v) => v.password === v.confirm, {
    path: ['confirm'],
    message: "Passwords don't match",
  });

type FormData = z.infer<typeof schema>;

function calculatePasswordStrength(password: string) {
  let score = 0;
  if (password.length >= 8) score++;
  if (/[A-Z]/.test(password) && /[a-z]/.test(password)) score++;
  if (/\d/.test(password)) score++;
  if (/[^A-Za-z0-9]/.test(password)) score++;
  return score;
}

const STRENGTH_LABELS = ['Too weak', 'Weak', 'Fair', 'Good', 'Strong'];
const STRENGTH_COLORS = ['bg-danger', 'bg-danger', 'bg-warning', 'bg-primary', 'bg-success'];

function RegisterForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const initialEmail = searchParams.get('email') || '';
  const { signUp, signInWithOAuth } = useAuth();

  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  const form = useForm<FormData>({
    resolver: zodResolver(schema),
    defaultValues: {
      name: '',
      email: initialEmail,
      password: '',
      confirm: '',
      terms: false as unknown as true,
    },
  });

  const { errors, isSubmitting } = form.formState;
  const password = form.watch('password');
  const strengthScore = calculatePasswordStrength(password || '');

  const onSubmit = form.handleSubmit(async (values) => {
    setErrorMessage(null);
    setSuccessMessage(null);

    const { error } = await signUp(values.email, values.password, values.name);
    if (error) {
      setErrorMessage(error.message || 'Registration failed. Please try again.');
      return;
    }

    setSuccessMessage('Account created successfully! Redirecting to browse...');
    setTimeout(() => {
      router.push('/browse');
      router.refresh();
    }, 1200);
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

      {successMessage && (
        <div role="alert" className="mb-5 flex items-center gap-2 rounded-xl border border-success/40 bg-success/10 px-4 py-3 text-sm text-foreground">
          <CheckCircle className="h-4 w-4 text-success shrink-0" />
          <span>{successMessage}</span>
        </div>
      )}

      <form onSubmit={onSubmit} noValidate className="space-y-4">
        <div>
          <label htmlFor="name" className="mb-1.5 block text-sm font-medium text-foreground">
            Display name
          </label>
          <input
            id="name"
            type="text"
            autoComplete="name"
            placeholder="Alex Mercer"
            className={inputClass}
            aria-invalid={!!errors.name}
            {...form.register('name')}
          />
          <FieldError message={errors.name?.message} />
        </div>

        <div>
          <label htmlFor="email" className="mb-1.5 block text-sm font-medium text-foreground">
            Email
          </label>
          <input
            id="email"
            type="email"
            autoComplete="email"
            placeholder="alex@domain.com"
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
          <input
            id="password"
            type="password"
            autoComplete="new-password"
            placeholder="••••••••"
            className={inputClass}
            aria-invalid={!!errors.password}
            {...form.register('password')}
          />
          {password && (
            <div className="mt-2.5">
              <div className="flex gap-1" aria-hidden>
                {[0, 1, 2, 3].map((i) => (
                  <span
                    key={i}
                    className={cn(
                      'h-1.5 flex-1 rounded-full transition-all',
                      i < strengthScore ? STRENGTH_COLORS[strengthScore] : 'bg-surface-elevated'
                    )}
                  />
                ))}
              </div>
              <p className="mt-1 text-xs text-muted-foreground font-medium">
                Password strength: <span className="text-foreground">{STRENGTH_LABELS[strengthScore]}</span>
              </p>
            </div>
          )}
          <FieldError message={errors.password?.message} />
        </div>

        <div>
          <label htmlFor="confirm" className="mb-1.5 block text-sm font-medium text-foreground">
            Confirm password
          </label>
          <input
            id="confirm"
            type="password"
            autoComplete="new-password"
            placeholder="••••••••"
            className={inputClass}
            aria-invalid={!!errors.confirm}
            {...form.register('confirm')}
          />
          <FieldError message={errors.confirm?.message} />
        </div>

        <div className="pt-1">
          <label className="flex items-start gap-2.5 text-sm text-muted-foreground cursor-pointer">
            <Checkbox
              className="mt-0.5"
              checked={!!form.watch('terms')}
              onCheckedChange={(c) => form.setValue('terms', (c === true) as true, { shouldValidate: true })}
            />
            <span>
              I agree to the <Link href="/browse" className="text-primary hover:underline">Terms of Use</Link> and <Link href="/browse" className="text-primary hover:underline">Privacy Policy</Link>.
            </span>
          </label>
          <FieldError message={errors.terms?.message} />
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
              Creating account...
            </>
          ) : (
            'Create account'
          )}
        </Button>
      </form>

      <Divider />

      <GoogleButton onClick={handleGoogleSignIn} disabled={isSubmitting} />

      <p className="mt-6 text-center text-sm text-muted-foreground">
        Already have a VyroFlix account?{' '}
        <Link href="/login" className="font-semibold text-foreground hover:text-primary transition-colors">
          Sign in
        </Link>
      </p>
    </>
  );
}

export default function RegisterPage() {
  return (
    <AuthShell
      title="Create account"
      subtitle="Start streaming ultra high-definition movies and series in under a minute."
    >
      <Suspense fallback={<Spinner label="Loading registration..." />}>
        <RegisterForm />
      </Suspense>
    </AuthShell>
  );
}
