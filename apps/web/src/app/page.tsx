import Link from 'next/link';
import { Play, Sparkles, Tv, ShieldCheck, Zap } from 'lucide-react';

export default function LandingPage() {
  const plans = [
    {
      name: 'Free',
      price: '$0',
      period: 'forever',
      description: 'Standard definition streaming with community catalog access.',
      features: ['480p SD Resolution', '1 Concurrent Stream', 'Standard Audio', 'Ad-supported'],
      popular: false,
    },
    {
      name: 'Basic',
      price: '$5.99',
      period: 'per month',
      description: 'High-definition streaming without ads on any screen.',
      features: ['720p HD Resolution', '2 Concurrent Streams', 'Dolby Digital Audio', 'Watchlist & History Sync'],
      popular: false,
    },
    {
      name: 'Premium',
      price: '$9.99',
      period: 'per month',
      description: 'Crisp 1080p Full HD with unlimited device syncing.',
      features: ['1080p Full HD Resolution', '4 Concurrent Streams', 'Dolby Atmos Spatial Audio', 'Unlimited Watchlist', 'Priority Edge CDN'],
      popular: true,
    },
  ];

  return (
    <div className="min-h-screen pt-24 pb-16">
      {/* Hero Section */}
      <section className="relative px-4 sm:px-6 lg:px-8 py-20 lg:py-32 flex flex-col items-center text-center">
        <div className="inline-flex items-center space-x-2 px-3 py-1 rounded-full bg-primary/10 border border-primary/30 text-xs font-semibold text-purple-300 mb-6">
          <Sparkles className="w-3.5 h-3.5 text-pink-400" />
          <span>Next-Gen Cloud-Native VOD Platform</span>
        </div>

        <h1 className="font-heading font-extrabold text-4xl sm:text-6xl lg:text-7xl max-w-4xl tracking-tight leading-tight mb-6">
          Unlimited stories.{' '}
          <span className="gradient-text">One beautiful stream.</span>
        </h1>

        <p className="max-w-2xl text-text-secondary text-base sm:text-lg mb-10 leading-relaxed">
          Experience ultra-low latency, multi-rendition HLS streaming packaged with FFmpeg and delivered via Cloudflare edge CDN. No buffering, pure cinema.
        </p>

        <div className="flex flex-col sm:flex-row items-center gap-4">
          <Link
            href="/browse"
            className="flex items-center space-x-2 px-8 py-4 gradient-accent text-white font-bold rounded-full shadow-lg shadow-purple-600/30 hover:scale-105 transition-all text-base"
          >
            <Play className="w-5 h-5 fill-white" />
            <span>Start Watching Now</span>
          </Link>
          <Link
            href="/browse"
            className="px-8 py-4 glass-panel text-text-secondary hover:text-white rounded-full font-semibold text-base transition-colors border border-border"
          >
            Explore Catalog
          </Link>
        </div>
      </section>

      {/* Feature Highlights */}
      <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-16 grid grid-cols-1 md:grid-cols-3 gap-8">
        <div className="glass-panel p-8 rounded-card border border-border">
          <div className="w-12 h-12 rounded-xl gradient-accent flex items-center justify-center mb-5 text-white">
            <Zap className="w-6 h-6" />
          </div>
          <h3 className="font-heading font-bold text-xl text-white mb-2">Adaptive HLS Streaming</h3>
          <p className="text-text-secondary text-sm leading-relaxed">
            Multi-rendition encoding from 480p to 1080p with instant adaptive bitrate switching based on your network conditions.
          </p>
        </div>

        <div className="glass-panel p-8 rounded-card border border-border">
          <div className="w-12 h-12 rounded-xl gradient-accent flex items-center justify-center mb-5 text-white">
            <Tv className="w-6 h-6" />
          </div>
          <h3 className="font-heading font-bold text-xl text-white mb-2">Pick Up Where You Left Off</h3>
          <p className="text-text-secondary text-sm leading-relaxed">
            Continue Watching rail automatically syncs playback position across devices with an automated 90% completion trigger.
          </p>
        </div>

        <div className="glass-panel p-8 rounded-card border border-border">
          <div className="w-12 h-12 rounded-xl gradient-accent flex items-center justify-center mb-5 text-white">
            <ShieldCheck className="w-6 h-6" />
          </div>
          <h3 className="font-heading font-bold text-xl text-white mb-2">Edge CDN & Short-Lived Signed URLs</h3>
          <p className="text-text-secondary text-sm leading-relaxed">
            Zero video bytes pass through application servers. Direct Cloudflare R2 / MinIO playback authorized with short-lived tokens.
          </p>
        </div>
      </section>

      {/* Subscription Plans Section */}
      <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-20 text-center">
        <h2 className="font-heading font-extrabold text-3xl sm:text-4xl text-white mb-4">
          Choose Your Plan
        </h2>
        <p className="text-text-secondary max-w-xl mx-auto text-sm sm:text-base mb-12">
          Transparent, low-cost pricing designed for cinema enthusiasts and creators.
        </p>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-8 text-left">
          {plans.map((plan) => (
            <div
              key={plan.name}
              className={`relative glass-panel rounded-card p-8 flex flex-col justify-between border ${
                plan.popular
                  ? 'border-purple-500 shadow-2xl shadow-purple-900/30 bg-surface-elevated/90'
                  : 'border-border'
              }`}
            >
              {plan.popular && (
                <div className="absolute -top-3.5 right-6 px-3 py-1 rounded-full gradient-accent text-[11px] font-extrabold text-white tracking-wider uppercase">
                  Most Popular
                </div>
              )}

              <div>
                <h3 className="font-heading font-bold text-2xl text-white mb-2">{plan.name}</h3>
                <div className="flex items-baseline space-x-1.5 mb-4">
                  <span className="font-heading font-extrabold text-4xl text-white">{plan.price}</span>
                  <span className="text-xs text-text-muted">{plan.period}</span>
                </div>
                <p className="text-xs text-text-secondary mb-6">{plan.description}</p>

                <ul className="space-y-3 mb-8 text-xs text-text-secondary">
                  {plan.features.map((feature) => (
                    <li key={feature} className="flex items-center space-x-2">
                      <span className="w-1.5 h-1.5 rounded-full bg-accent-cyan" />
                      <span>{feature}</span>
                    </li>
                  ))}
                </ul>
              </div>

              <Link
                href="/browse"
                className={`w-full py-3 rounded-full text-center text-sm font-bold transition-all ${
                  plan.popular
                    ? 'gradient-accent text-white shadow-lg hover:scale-105'
                    : 'bg-surface-elevated border border-border text-white hover:border-white'
                }`}
              >
                Get Started
              </Link>
            </div>
          ))}
        </div>
      </section>
    </div>
  );
}
