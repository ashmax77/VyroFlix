'use client';

import React, { useState, type FormEvent } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { Check, ChevronRight, MonitorSmartphone, PlayCircle, ListPlus, ChevronDown } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { PageContainer } from '@/components/common';
import { useAuth } from '@/lib/auth-context';
import { MOCK_PLANS, MOCK_FAQ } from '@/lib/mock-data';
import { cn } from '@/lib/utils';

const POSTER_WALL = [
  'https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=400&q=80',
  'https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=400&q=80',
  'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=400&q=80',
  'https://images.unsplash.com/photo-1534447677768-be436bb09401?w=400&q=80',
  'https://images.unsplash.com/photo-1551244072-5d12893278ab?w=400&q=80',
  'https://images.unsplash.com/photo-1506703719100-a0f3a48c0f86?w=400&q=80',
  'https://images.unsplash.com/photo-1516339901601-2e1b62dc0c45?w=400&q=80',
  'https://images.unsplash.com/photo-1470071459604-3b5ec3a7fe05?w=400&q=80',
  'https://images.unsplash.com/photo-1483921020237-2ff51e8e4b22?w=400&q=80',
  'https://images.unsplash.com/photo-1509316975850-ff9c5deb0cd9?w=400&q=80',
  'https://images.unsplash.com/photo-1500382017468-9049fed747ef?w=400&q=80',
  'https://images.unsplash.com/photo-1519681393784-d120267933ba?w=400&q=80',
  'https://images.unsplash.com/photo-1446776811953-b23d57bd21aa?w=400&q=80',
  'https://images.unsplash.com/photo-1514565131-fce0801e5785?w=400&q=80',
  'https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=400&q=80',
  'https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=400&q=80',
  'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=400&q=80',
  'https://images.unsplash.com/photo-1534447677768-be436bb09401?w=400&q=80',
];

const FEATURES = [
  {
    icon: MonitorSmartphone,
    title: 'Watch anywhere',
    body: 'Stream on your TV, laptop, tablet and phone. Your library follows you from the couch to the commute.',
    image: 'https://images.unsplash.com/photo-1522869635100-9f4c5e86aa37?w=900&q=80',
  },
  {
    icon: PlayCircle,
    title: 'Pick up where you left off',
    body: 'Every second is remembered. Jump back into the exact scene on any device — no scrubbing required.',
    image: 'https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=900&q=80',
  },
  {
    icon: ListPlus,
    title: 'Build your watchlist',
    body: 'Save films and series for later with a single tap. Your curated library is always ready whenever you are.',
    image: 'https://images.unsplash.com/photo-1478760329108-5c3ed9d495a0?w=900&q=80',
  },
];

export default function LandingPage() {
  const router = useRouter();
  const { isAuthed } = useAuth();
  const [email, setEmail] = useState('');
  const [openFaq, setOpenFaq] = useState<number | null>(0);

  const start = (e: FormEvent) => {
    e.preventDefault();
    router.push(email ? `/register?email=${encodeURIComponent(email)}` : '/register');
  };

  return (
    <div className="overflow-hidden">
      {/* Hero Section with Angled Poster Wall */}
      <section className="relative flex min-h-[92vh] items-center justify-center overflow-hidden pt-12">
        {/* Background Angled Poster Wall Grid */}
        <div
          aria-hidden
          className="absolute inset-0 grid -rotate-6 scale-125 grid-cols-3 sm:grid-cols-6 gap-3 opacity-25 blur-[1.5px] pointer-events-none"
        >
          {POSTER_WALL.map((src, i) => (
            <div key={i} className="aspect-[2/3] w-full overflow-hidden rounded-xl bg-surface">
              <img src={src} alt="" className="h-full w-full object-cover" />
            </div>
          ))}
        </div>

        {/* Radial Dark Vignette & Fade Gradients */}
        <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_center,rgba(14,10,23,0.65)_10%,rgb(14,10,23)_78%)]" />
        <div className="absolute inset-x-0 bottom-0 h-40 bg-fade-bottom" />

        {/* Hero Foreground Content */}
        <PageContainer className="relative z-10 text-center py-12">
          <div className="animate-page-in max-w-4xl mx-auto">
            <h1 className="text-4xl font-extrabold tracking-tight sm:text-6xl lg:text-7xl leading-[1.08] text-foreground">
              Unlimited stories.{' '}
              <span className="text-gradient-brand">One beautiful stream.</span>
            </h1>

            <p className="mx-auto mt-6 max-w-xl text-base sm:text-lg text-muted-foreground leading-relaxed">
              Original films and series in stunning multi-bitrate quality. Start free — upgrade or cancel anytime.
            </p>

            {/* Email Registration CTA Form */}
            <form onSubmit={start} className="mx-auto mt-10 flex w-full max-w-xl flex-col gap-3 sm:flex-row">
              <input
                type="email"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="Enter your email address"
                className="h-14 flex-1 rounded-full border border-border bg-surface/80 px-6 text-base text-foreground backdrop-blur-md placeholder:text-subtle focus:border-cyan focus:outline-none transition-colors"
              />
              <Button type="submit" variant="gradient" size="lg" className="h-14 px-8 text-base shadow-glow">
                Get Started <ChevronRight className="ml-1 h-5 w-5" />
              </Button>
            </form>

            <div className="mt-6 flex items-center justify-center gap-4 text-xs text-subtle">
              <span>✓ Instant access</span>
              <span>✓ No credit card required for Free tier</span>
              <span>✓ 100% cloud-native</span>
            </div>
          </div>
        </PageContainer>
      </section>

      {/* Feature Spotlights */}
      <PageContainer className="space-y-24 py-20">
        {FEATURES.map((feature, i) => (
          <div
            key={feature.title}
            className={cn('grid items-center gap-10 md:grid-cols-2', i % 2 === 1 && 'md:[&>*:first-child]:order-2')}
          >
            <div>
              <div className="grid h-14 w-14 place-items-center rounded-2xl bg-surface-elevated text-cyan shadow-glow">
                <feature.icon className="h-7 w-7" />
              </div>
              <h2 className="mt-5 text-3xl font-extrabold sm:text-4xl text-foreground">{feature.title}</h2>
              <p className="mt-4 max-w-md text-base sm:text-lg leading-relaxed text-muted-foreground">{feature.body}</p>
            </div>
            <div className="relative">
              <div className="absolute -inset-6 rounded-3xl bg-gradient-brand opacity-20 blur-3xl pointer-events-none" />
              <div className="relative aspect-[16/10] w-full overflow-hidden rounded-2xl border border-border shadow-card bg-surface">
                <img src={feature.image} alt={feature.title} loading="lazy" className="h-full w-full object-cover" />
              </div>
            </div>
          </div>
        ))}
      </PageContainer>

      {/* Plans Section */}
      <section className="py-20 border-y border-border/60 bg-surface/30">
        <PageContainer>
          <div className="text-center max-w-xl mx-auto">
            <h2 className="text-3xl font-extrabold sm:text-4xl text-foreground">Choose your experience</h2>
            <p className="mt-3 text-muted-foreground">Select the plan that fits your screen.</p>
          </div>

          <div className="mx-auto mt-12 grid max-w-5xl gap-6 md:grid-cols-3">
            {MOCK_PLANS.map((plan) => (
              <div
                key={plan.id}
                className={cn(
                  'relative rounded-3xl p-8 transition-all duration-300',
                  plan.popular
                    ? 'gradient-border shadow-glow-lg bg-surface-elevated/90'
                    : 'border border-border bg-surface/80 hover:border-border-subtle'
                )}
              >
                {plan.popular && (
                  <span className="absolute -top-3 left-1/2 -translate-x-1/2 rounded-full bg-gradient-brand px-3.5 py-1 text-xs font-bold text-white shadow-md">
                    Most Popular
                  </span>
                )}
                <h3 className="text-xl font-bold text-foreground">{plan.name}</h3>
                <p className="mt-4 font-display text-4xl font-extrabold text-foreground">
                  {plan.price ? `$${plan.price}` : 'Free'}
                  {plan.price > 0 && <span className="text-base font-normal text-muted-foreground">/mo</span>}
                </p>

                <ul className="mt-6 space-y-3.5 text-sm text-foreground/90">
                  <li className="flex items-center gap-2.5">
                    <Check className="h-4 w-4 text-cyan" />
                    <span>{plan.resolution} Streaming Quality</span>
                  </li>
                  <li className="flex items-center gap-2.5">
                    <Check className="h-4 w-4 text-cyan" />
                    <span>{plan.streams} Simultaneous Screen{plan.streams > 1 ? 's' : ''}</span>
                  </li>
                  <li className="flex items-center gap-2.5">
                    <Check className="h-4 w-4 text-cyan" />
                    <span>{plan.ads ? 'Includes minimal ads' : 'Ad-free experience'}</span>
                  </li>
                </ul>

                <Button
                  variant={plan.popular ? 'gradient' : 'outline'}
                  size="lg"
                  className="mt-8 w-full font-bold"
                  asChild
                >
                  <Link href={`/register?plan=${plan.id}`}>
                    Get {plan.name}
                  </Link>
                </Button>
              </div>
            ))}
          </div>
        </PageContainer>
      </section>

      {/* Frequently Asked Questions Accordion */}
      <PageContainer className="max-w-3xl py-20">
        <h2 className="text-center text-3xl font-extrabold sm:text-4xl text-foreground">
          Frequently asked questions
        </h2>
        <div className="mt-10 space-y-3">
          {MOCK_FAQ.map((item, idx) => {
            const isOpen = openFaq === idx;
            return (
              <div
                key={idx}
                className="overflow-hidden rounded-2xl border border-border bg-surface transition-colors"
              >
                <button
                  type="button"
                  onClick={() => setOpenFaq(isOpen ? null : idx)}
                  className="flex w-full items-center justify-between p-6 text-left text-base font-semibold text-foreground hover:text-cyan transition-colors"
                >
                  <span>{item.q}</span>
                  <ChevronDown className={cn('h-5 w-5 text-subtle transition-transform duration-300', isOpen && 'rotate-180 text-cyan')} />
                </button>
                {isOpen && (
                  <div className="px-6 pb-6 text-sm leading-relaxed text-muted-foreground animate-page-in">
                    {item.a}
                  </div>
                )}
              </div>
            );
          })}
        </div>
      </PageContainer>

      {/* Bottom CTA Banner */}
      <section className="relative mx-4 my-12 overflow-hidden rounded-3xl sm:mx-8 lg:mx-12 shadow-glow-lg">
        <div className="absolute inset-0 bg-gradient-brand opacity-95" />
        <div className="relative px-6 py-16 text-center text-white">
          <h2 className="text-3xl font-extrabold sm:text-5xl">Ready to watch?</h2>
          <p className="mt-3 text-white/90 text-base sm:text-lg">Create your account and jump into thousands of stories in seconds.</p>
          <Button variant="light" size="lg" className="mt-8 px-9 shadow-card font-bold" asChild>
            <Link href="/register">Start streaming now</Link>
          </Button>
        </div>
      </section>
    </div>
  );
}
