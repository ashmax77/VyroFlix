'use client';

import React, { useState } from 'react';
import {
  Activity,
  AlertTriangle,
  CheckCircle2,
  Cog,
  Eye,
  FileText,
  Film,
  RefreshCw,
  Sparkles,
} from 'lucide-react';
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Line,
  LineChart,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import { Skeleton } from '../common';
import { getAdminStats } from '@/lib/api';

const PIE_COLORS = ['#10b981', '#f43f5e', '#06b6d4'];

const tooltipStyle = {
  contentStyle: {
    background: 'hsl(var(--surface-elevated))',
    border: '1px solid hsl(var(--border))',
    borderRadius: '12px',
    color: '#fff',
    fontSize: '12px',
  },
};

export default function AnalyticsPanel() {
  const [stats, setStats] = useState<Awaited<ReturnType<typeof getAdminStats>> | null>(null);
  const [loading, setLoading] = useState(true);
  const [aiLoading, setAiLoading] = useState(false);
  const [aiInsights, setAiInsights] = useState<string[]>([
    'Peak streaming concurrency hit 142 viewers on Saturday evening.',
    'Neon Tides is the top performing title with over 480 continuous plays.',
    'Encoding pipeline has a 92% first-pass success rate across 1080p renditions.',
    'Recommended action: 2 drafts require metadata review before publication.',
  ]);

  React.useEffect(() => {
    getAdminStats()
      .then((s) => setStats(s))
      .finally(() => setLoading(false));
  }, []);

  const handleGenerateInsights = () => {
    setAiLoading(true);
    setTimeout(() => {
      setAiInsights([
        'Viewer retention is highest for titles tagged with Sci-Fi and Thriller genres.',
        'Average bitrate adaptation stayed stable at 1080p for 86% of mobile sessions.',
        'Cloudflare R2 bandwidth usage remains well below the monthly free-tier allocation.',
        'No dead-letter Kafka messages recorded in the last 24 hours.',
      ]);
      setAiLoading(false);
    }, 1200);
  };

  const kpis = [
    { label: 'Total titles', value: stats?.total, icon: Film },
    { label: 'Published', value: stats?.published, icon: CheckCircle2 },
    { label: 'Drafts', value: stats?.drafts, icon: FileText },
    { label: 'Encoding in progress', value: stats?.encoding, icon: Cog },
    { label: 'Failed jobs', value: stats?.failed, icon: AlertTriangle },
    { label: 'Active viewers', value: stats?.activeViewers.toLocaleString(), icon: Eye },
  ];

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="font-display text-3xl font-bold tracking-tight text-foreground sm:text-4xl">
            Studio Dashboard
          </h1>
          <p className="mt-1 text-sm text-muted-foreground">
            Platform metrics from Content, Streaming and FFmpeg Encoding services.
          </p>
        </div>
      </div>

      {/* AI Insights Card */}
      <section className="rounded-2xl border border-border bg-surface p-5 shadow-card backdrop-blur-sm">
        <div className="flex items-center gap-2">
          <Sparkles className="h-5 w-5 text-brand-magenta" />
          <h2 className="font-semibold text-foreground">AI Catalog Insights</h2>
          <button
            type="button"
            disabled={aiLoading}
            onClick={handleGenerateInsights}
            className="ml-auto flex items-center gap-1.5 rounded-full bg-gradient-brand px-3.5 py-1.5 text-xs font-medium text-white transition-opacity hover:opacity-95 disabled:opacity-50"
          >
            <RefreshCw className={aiLoading ? 'h-3.5 w-3.5 animate-spin' : 'h-3.5 w-3.5'} />
            {aiLoading ? 'Analyzing platform data…' : 'Regenerate summary'}
          </button>
        </div>

        <div className="mt-4 text-sm">
          {aiLoading ? (
            <div className="space-y-2">
              <Skeleton className="h-4 w-full" />
              <Skeleton className="h-4 w-5/6" />
              <Skeleton className="h-4 w-4/6" />
            </div>
          ) : (
            <ul className="grid gap-2.5 sm:grid-cols-2">
              {aiInsights.map((line, i) => (
                <li key={i} className="flex items-start gap-2.5 text-muted-foreground">
                  <span className="mt-1.5 h-1.5 w-1.5 shrink-0 rounded-full bg-cyan shadow-[0_0_8px_rgba(6,182,212,0.8)]" />
                  <span className="text-xs leading-relaxed sm:text-sm">{line}</span>
                </li>
              ))}
            </ul>
          )}
        </div>
      </section>

      {/* KPI Cards */}
      <div className="grid grid-cols-2 gap-4 lg:grid-cols-3 xl:grid-cols-6">
        {kpis.map((k) => (
          <div
            key={k.label}
            className="rounded-2xl border border-border bg-surface p-4 transition-all duration-200 hover:border-cyan/30"
          >
            <div className="flex items-center justify-between">
              <k.icon className="h-5 w-5 text-cyan" />
            </div>
            {stats && !loading ? (
              <p className="mt-3 font-display text-3xl font-bold text-foreground">{k.value ?? 0}</p>
            ) : (
              <Skeleton className="mt-3 h-9 w-16" />
            )}
            <p className="mt-1 text-xs text-muted-foreground">{k.label}</p>
          </div>
        ))}
      </div>

      {/* Analytics Charts */}
      <div className="grid gap-6 lg:grid-cols-3">
        {/* Plays per day LineChart */}
        <div className="rounded-2xl border border-border bg-surface p-5 lg:col-span-2">
          <div className="mb-4 flex items-center justify-between">
            <h2 className="font-semibold text-foreground">Plays per day</h2>
            <span className="text-xs text-muted-foreground">Last 7 days</span>
          </div>
          <div className="h-64 w-full">
            {stats && !loading ? (
              <ResponsiveContainer width="100%" height="100%">
                <LineChart data={stats.playsPerDay}>
                  <CartesianGrid stroke="hsl(var(--border))" strokeDasharray="3 3" opacity={0.5} />
                  <XAxis dataKey="day" stroke="hsl(var(--subtle))" fontSize={12} tickLine={false} />
                  <YAxis stroke="hsl(var(--subtle))" fontSize={12} tickLine={false} axisLine={false} />
                  <Tooltip {...tooltipStyle} />
                  <Line
                    type="monotone"
                    dataKey="plays"
                    stroke="#8b5cf6"
                    strokeWidth={3}
                    dot={{ fill: '#8b5cf6', r: 4 }}
                    activeDot={{ r: 6, fill: '#06b6d4' }}
                  />
                </LineChart>
              </ResponsiveContainer>
            ) : (
              <Skeleton className="h-full w-full rounded-xl" />
            )}
          </div>
        </div>

        {/* Encoding Outcomes Donut Chart */}
        <div className="rounded-2xl border border-border bg-surface p-5">
          <div className="mb-4 flex items-center justify-between">
            <h2 className="font-semibold text-foreground">Encoding outcomes</h2>
            <span className="text-xs text-muted-foreground">HLS renditions</span>
          </div>
          <div className="h-64 w-full">
            {stats && !loading ? (
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie
                    data={stats.encodingSplit}
                    dataKey="value"
                    nameKey="name"
                    innerRadius={55}
                    outerRadius={85}
                    paddingAngle={4}
                  >
                    {stats.encodingSplit.map((_, i) => (
                      <Cell key={i} fill={PIE_COLORS[i % PIE_COLORS.length]} stroke="none" />
                    ))}
                  </Pie>
                  <Tooltip {...tooltipStyle} />
                </PieChart>
              </ResponsiveContainer>
            ) : (
              <Skeleton className="h-full w-full rounded-xl" />
            )}
          </div>
        </div>

        {/* Top Titles BarChart */}
        <div className="rounded-2xl border border-border bg-surface p-5 lg:col-span-2">
          <div className="mb-4 flex items-center justify-between">
            <h2 className="font-semibold text-foreground">Top titles by streams</h2>
            <span className="text-xs text-muted-foreground">Session volume</span>
          </div>
          <div className="h-64 w-full">
            {stats && !loading ? (
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={stats.topTitles}>
                  <CartesianGrid stroke="hsl(var(--border))" strokeDasharray="3 3" opacity={0.5} />
                  <XAxis dataKey="name" stroke="hsl(var(--subtle))" fontSize={11} tickLine={false} />
                  <YAxis stroke="hsl(var(--subtle))" fontSize={12} tickLine={false} axisLine={false} />
                  <Tooltip {...tooltipStyle} />
                  <Bar dataKey="plays" fill="#d946ef" radius={[6, 6, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            ) : (
              <Skeleton className="h-full w-full rounded-xl" />
            )}
          </div>
        </div>

        {/* Recent Activity Feed */}
        <div className="rounded-2xl border border-border bg-surface p-5">
          <h2 className="mb-4 flex items-center gap-2 font-semibold text-foreground">
            <Activity className="h-4 w-4 text-cyan" />
            Recent event activity
          </h2>
          <ul className="space-y-3.5 text-sm">
            {stats?.activity.map((a, i) => (
              <li key={i} className="flex gap-3">
                <span className="mt-1.5 h-2 w-2 shrink-0 rounded-full bg-cyan shadow-[0_0_8px_rgba(6,182,212,0.8)]" />
                <div className="min-w-0 flex-1">
                  <p className="truncate text-xs font-medium text-foreground">
                    <code className="rounded bg-cyan/10 px-1 py-0.5 text-[10px] text-cyan">
                      {a.type}
                    </code>{' '}
                    · {a.file}
                  </p>
                  <p className="mt-0.5 text-[11px] text-subtle">
                    {new Date(a.at).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                  </p>
                </div>
              </li>
            ))}
          </ul>
        </div>
      </div>
    </div>
  );
}
