'use client';

import React, { useEffect, useRef, useState, useCallback } from 'react';
import Hls from 'hls.js';
import { useRouter } from 'next/navigation';
import {
  ArrowLeft,
  Captions,
  FastForward,
  Gauge,
  Keyboard,
  Loader2,
  Maximize,
  Minimize,
  Pause,
  PictureInPicture2,
  Play,
  Rewind,
  Settings,
  SkipForward,
  Volume2,
  VolumeX,
} from 'lucide-react';
import { Button } from './ui/button';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuTrigger,
} from './ui/dropdown-menu';
import { ApiError, createPlaybackSession, endPlaybackSession, saveProgress } from '../lib/api';
import { formatTime, cn } from '../lib/utils';
import { Title } from '../types';

interface VideoPlayerProps {
  title: Title;
  /** Playable id: the title id for movies, the episode id for episodes. Defaults to the title id. */
  contentId?: string;
  targetType?: 'MOVIE' | 'EPISODE';
  episodeTitle?: string;
  nextEpisodeId?: string;
}

export default function VideoPlayer({
  title,
  contentId: contentIdProp,
  targetType = 'MOVIE',
  episodeTitle,
  nextEpisodeId,
}: VideoPlayerProps) {
  const router = useRouter();
  const contentId = contentIdProp ?? title.id;
  const durationRef = useRef(0);
  const videoRef = useRef<HTMLVideoElement>(null);
  const containerRef = useRef<HTMLDivElement>(null);
  const hlsRef = useRef<Hls | null>(null);
  const controlsTimeoutRef = useRef<NodeJS.Timeout | null>(null);
  const heartbeatIntervalRef = useRef<NodeJS.Timeout | null>(null);
  const activeSessionIdRef = useRef<string | null>(null);

  const [loading, setLoading] = useState(true);
  const [isPlaying, setIsPlaying] = useState(false);
  const [currentTime, setCurrentTime] = useState(0);
  const [duration, setDuration] = useState(0);
  const [volume, setVolume] = useState(1);
  const [isMuted, setIsMuted] = useState(false);
  const [isFullscreen, setIsFullscreen] = useState(false);
  const [showControls, setShowControls] = useState(true);
  const [bufferedPercent, setBufferedPercent] = useState(0);
  const [speed, setSpeed] = useState(1);
  const [qualityLevels, setQualityLevels] = useState<{ id: number; name: string }[]>([]);
  const [currentQuality, setCurrentQuality] = useState(-1); // -1 = Auto
  const [selectedSubtitles, setSelectedSubtitles] = useState('Off');
  const [showShortcutsHelp, setShowShortcutsHelp] = useState(false);
  const [hoverSeek, setHoverSeek] = useState<{ x: number; time: number } | null>(null);
  const [resumeNotice, setResumeNotice] = useState<number | null>(null);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  // Progress heartbeat
  const sendHeartbeat = useCallback(
    (options: { keepalive?: boolean } = {}) => {
      const video = videoRef.current;
      const dur = durationRef.current;
      if (!video || !(dur > 0)) return;
      void saveProgress(
        {
          contentId,
          targetType,
          positionSeconds: Math.round(video.currentTime),
          durationSeconds: Math.round(dur),
        },
        options,
      );
    },
    [contentId, targetType],
  );

  // Initialize stream and HLS
  useEffect(() => {
    let isCancelled = false;
    let hlsInstance: Hls | null = null;
    let sessionRefreshes = 0;
    const MAX_SESSION_REFRESHES = 2;

    function endActiveSession() {
      const id = activeSessionIdRef.current;
      activeSessionIdRef.current = null;
      if (id) void endPlaybackSession(id);
    }

    async function startSession() {
      const session = await createPlaybackSession(contentId, targetType);
      if (isCancelled || !videoRef.current) {
        void endPlaybackSession(session.sessionId);
        return null;
      }
      endActiveSession();
      activeSessionIdRef.current = session.sessionId;
      return session;
    }

    async function initPlayer() {
      setLoading(true);
      setErrorMsg(null);

      try {
        const session = await startSession();
        if (!session) return;
        const video = videoRef.current!;
        const manifestUrl = session.manifestUrl;

        const resumeAt = session.resumePositionSeconds || 0;
        if (resumeAt > 10) {
          setResumeNotice(resumeAt);
          setTimeout(() => setResumeNotice(null), 5000);
        }

        if (Hls.isSupported()) {
          hlsInstance = new Hls({
            enableWorker: true,
            lowLatencyMode: false,
            startPosition: resumeAt > 0 ? resumeAt : -1,
          });
          hlsRef.current = hlsInstance;

          hlsInstance.loadSource(manifestUrl);
          hlsInstance.attachMedia(video);

          hlsInstance.on(Hls.Events.MANIFEST_PARSED, (event, data) => {
            const levels = data.levels.map((level, idx) => ({
              id: idx,
              name: `${level.height}p`,
            }));
            setQualityLevels(levels);
            setLoading(false);
            video.play().catch(() => setIsPlaying(false));
          });

          hlsInstance.on(Hls.Events.ERROR, (event, data) => {
            if (!data.fatal) return;
            switch (data.type) {
              case Hls.ErrorTypes.NETWORK_ERROR: {
                if (sessionRefreshes >= MAX_SESSION_REFRESHES) {
                  hlsInstance?.destroy();
                  setErrorMsg('The stream could not be loaded. Please check your connection.');
                  return;
                }
                sessionRefreshes += 1;
                const resumePos = video.currentTime;
                const wasPlaying = !video.paused;
                startSession()
                  .then((next) => {
                    if (!next || !hlsInstance) return;
                    hlsInstance.once(Hls.Events.MANIFEST_PARSED, () => {
                      video.currentTime = resumePos;
                      if (wasPlaying) video.play().catch(() => setIsPlaying(false));
                    });
                    hlsInstance.loadSource(next.manifestUrl);
                  })
                  .catch(() => {
                    if (!isCancelled) setErrorMsg('Could not renew your playback session.');
                  });
                break;
              }
              case Hls.ErrorTypes.MEDIA_ERROR:
                hlsInstance?.recoverMediaError();
                break;
              default:
                hlsInstance?.destroy();
                setErrorMsg('Failed to play media stream.');
                break;
            }
          });
        } else if (video.canPlayType('application/vnd.apple.mpegurl')) {
          video.src = manifestUrl;
          const onLoadedMetadata = () => {
            if (resumeAt > 0) video.currentTime = resumeAt;
            setLoading(false);
            video.play().catch(() => setIsPlaying(false));
          };
          video.addEventListener('loadedmetadata', onLoadedMetadata, { once: true });
        } else {
          setLoading(false);
          setErrorMsg('HLS playback is not supported on this browser.');
        }
      } catch (err) {
        if (!isCancelled) {
          setLoading(false);
          setErrorMsg(
            err instanceof ApiError && err.status === 401
              ? 'Please sign in to watch.'
              : err instanceof ApiError && err.status === 403
                ? 'Your account is not allowed to watch this title.'
                : 'Could not authorize playback. Please try again.',
          );
        }
      }
    }

    initPlayer();

    heartbeatIntervalRef.current = setInterval(() => {
      if (videoRef.current && !videoRef.current.paused) sendHeartbeat();
    }, 20000);

    const flush = () => sendHeartbeat({ keepalive: true });
    const onVisibility = () => {
      if (document.visibilityState === 'hidden') flush();
    };
    const onPageHide = () => {
      flush();
      endActiveSession();
    };

    window.addEventListener('pagehide', onPageHide);
    document.addEventListener('visibilitychange', onVisibility);

    return () => {
      isCancelled = true;
      if (heartbeatIntervalRef.current) clearInterval(heartbeatIntervalRef.current);
      window.removeEventListener('pagehide', onPageHide);
      document.removeEventListener('visibilitychange', onVisibility);
      sendHeartbeat({ keepalive: true });
      endActiveSession();
      if (hlsInstance) hlsInstance.destroy();
      hlsRef.current = null;
    };
  }, [contentId, targetType, sendHeartbeat]);

  const handleTimeUpdate = () => {
    if (videoRef.current) {
      setCurrentTime(videoRef.current.currentTime);
      if (videoRef.current.buffered.length > 0 && duration > 0) {
        const bufferedEnd = videoRef.current.buffered.end(videoRef.current.buffered.length - 1);
        setBufferedPercent(Math.min(100, (bufferedEnd / duration) * 100));
      }
    }
  };

  const handleLoadedMetadata = () => {
    if (videoRef.current) {
      durationRef.current = videoRef.current.duration;
      setDuration(videoRef.current.duration);
    }
  };

  const togglePlay = () => {
    if (videoRef.current) {
      if (videoRef.current.paused) {
        videoRef.current.play();
        setIsPlaying(true);
      } else {
        videoRef.current.pause();
        setIsPlaying(false);
        sendHeartbeat();
      }
    }
  };

  const skipSeconds = (offset: number) => {
    if (videoRef.current) {
      videoRef.current.currentTime = Math.max(0, Math.min(duration, videoRef.current.currentTime + offset));
    }
  };

  const handleVolumeChange = (val: number) => {
    setVolume(val);
    if (videoRef.current) {
      videoRef.current.volume = val;
      videoRef.current.muted = val === 0;
      setIsMuted(val === 0);
    }
  };

  const toggleMute = () => {
    if (videoRef.current) {
      const next = !isMuted;
      videoRef.current.muted = next;
      setIsMuted(next);
    }
  };

  const toggleFullscreen = () => {
    if (!containerRef.current) return;
    if (!document.fullscreenElement) {
      containerRef.current.requestFullscreen().catch(() => {});
      setIsFullscreen(true);
    } else {
      document.exitFullscreen().catch(() => {});
      setIsFullscreen(false);
    }
  };

  useEffect(() => {
    const onFsChange = () => setIsFullscreen(!!document.fullscreenElement);
    document.addEventListener('fullscreenchange', onFsChange);
    return () => document.removeEventListener('fullscreenchange', onFsChange);
  }, []);

  const handleSpeedChange = (s: number) => {
    setSpeed(s);
    if (videoRef.current) {
      videoRef.current.playbackRate = s;
    }
  };

  const handleQualitySelect = (levelId: number) => {
    if (hlsRef.current) {
      hlsRef.current.currentLevel = levelId;
      setCurrentQuality(levelId);
    }
  };

  // Auto-hide controls
  const pokeControls = useCallback(() => {
    setShowControls(true);
    if (controlsTimeoutRef.current) clearTimeout(controlsTimeoutRef.current);
    controlsTimeoutRef.current = setTimeout(() => {
      if (videoRef.current && !videoRef.current.paused) {
        setShowControls(false);
      }
    }, 3200);
  }, []);

  // Keyboard shortcuts
  useEffect(() => {
    const onKeyDown = (e: KeyboardEvent) => {
      if (['INPUT', 'TEXTAREA'].includes((e.target as HTMLElement).tagName)) return;
      pokeControls();
      switch (e.key) {
        case ' ':
        case 'k':
        case 'K':
          e.preventDefault();
          togglePlay();
          break;
        case 'ArrowLeft':
          skipSeconds(-10);
          break;
        case 'ArrowRight':
          skipSeconds(10);
          break;
        case 'ArrowUp':
          e.preventDefault();
          handleVolumeChange(Math.min(1, volume + 0.1));
          break;
        case 'ArrowDown':
          e.preventDefault();
          handleVolumeChange(Math.max(0, volume - 0.1));
          break;
        case 'm':
        case 'M':
          toggleMute();
          break;
        case 'f':
        case 'F':
          toggleFullscreen();
          break;
        case '?':
          setShowShortcutsHelp((h) => !h);
          break;
        case 'Escape':
          if (showShortcutsHelp) setShowShortcutsHelp(false);
          break;
      }
    };
    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, [togglePlay, skipSeconds, toggleMute, toggleFullscreen, volume, showShortcutsHelp, pokeControls]);

  return (
    <div
      ref={containerRef}
      onMouseMove={pokeControls}
      onTouchStart={pokeControls}
      className={cn(
        'relative flex h-screen w-full select-none items-center justify-center overflow-hidden bg-black',
        !showControls && isPlaying && 'cursor-none',
      )}
    >
      <video
        ref={videoRef}
        onTimeUpdate={handleTimeUpdate}
        onLoadedMetadata={handleLoadedMetadata}
        onPlay={() => setIsPlaying(true)}
        onPause={() => setIsPlaying(false)}
        onEnded={() => {
          setIsPlaying(false);
          sendHeartbeat();
          if (nextEpisodeId) {
            router.push(`/watch/${title.id}?episode=${nextEpisodeId}`);
          }
        }}
        onClick={togglePlay}
        className="h-full w-full object-contain"
        playsInline
      />

      {/* Loading Overlay */}
      {loading && (
        <div className="absolute inset-0 z-30 flex flex-col items-center justify-center bg-background/80 backdrop-blur-sm">
          <Loader2 className="h-12 w-12 animate-spin text-cyan" />
          <p className="mt-4 text-base font-medium text-foreground">Preparing your stream…</p>
          <p className="mt-1 text-xs text-muted-foreground">Fetching signed HLS master playlist from Cloudflare edge</p>
        </div>
      )}

      {/* Error Overlay */}
      {errorMsg && (
        <div className="absolute inset-0 z-30 flex flex-col items-center justify-center bg-background/90 p-6 text-center">
          <div className="glass max-w-md rounded-2xl p-8 shadow-card">
            <h2 className="text-xl font-bold text-foreground">Playback Notice</h2>
            <p className="mt-2 text-sm text-muted-foreground">{errorMsg}</p>
            <div className="mt-6 flex justify-center gap-3">
              <Button variant="gradient" onClick={() => window.location.reload()}>
                Try again
              </Button>
              <Button variant="outline" onClick={() => router.push(`/title/${title.id}`)}>
                Back to title
              </Button>
            </div>
          </div>
        </div>
      )}

      {/* Resumed Playback Toast */}
      {resumeNotice && (
        <div className="glass fixed bottom-28 left-8 z-40 flex items-center gap-3 rounded-2xl border border-cyan/30 px-4 py-3 text-xs text-foreground shadow-glow animate-page-in">
          <span>Resumed from {formatTime(resumeNotice)}</span>
          <button
            type="button"
            onClick={() => {
              if (videoRef.current) videoRef.current.currentTime = 0;
              setResumeNotice(null);
            }}
            className="rounded-full bg-cyan/20 px-2.5 py-1 font-semibold text-cyan hover:bg-cyan/30"
          >
            Start over
          </button>
        </div>
      )}

      {/* Custom Glass Controls Overlay */}
      <div
        className={cn(
          'pointer-events-none absolute inset-0 z-20 flex flex-col justify-between p-4 transition-opacity duration-300 sm:p-6',
          showControls || !isPlaying ? 'opacity-100' : 'opacity-0',
        )}
      >
        {/* Top Header Bar */}
        <div className="pointer-events-auto flex items-center justify-between bg-gradient-to-b from-background/90 via-background/40 to-transparent p-2 sm:p-4">
          <div className="flex items-center gap-4">
            <button
              onClick={() => router.push(`/title/${title.id}`)}
              className="glass grid h-10 w-10 place-items-center rounded-full text-foreground transition-transform hover:scale-105"
              aria-label="Back to details"
            >
              <ArrowLeft className="h-5 w-5" />
            </button>
            <div>
              <h2 className="font-display text-base font-semibold text-foreground drop-shadow sm:text-lg">
                {title.title}
              </h2>
              {episodeTitle && (
                <p className="text-xs text-muted-foreground drop-shadow">{episodeTitle}</p>
              )}
            </div>
          </div>

          <button
            type="button"
            onClick={() => setShowShortcutsHelp(true)}
            aria-label="Keyboard shortcuts"
            className="glass grid h-10 w-10 place-items-center rounded-full text-muted-foreground transition-colors hover:text-foreground"
          >
            <Keyboard className="h-4 w-4" />
          </button>
        </div>

        {/* Center Rewind, Play/Pause, Forward Buttons */}
        <div className="pointer-events-auto flex items-center justify-center gap-6 sm:gap-10">
          <button
            type="button"
            onClick={() => skipSeconds(-10)}
            className="glass grid h-12 w-12 place-items-center rounded-full text-foreground transition-transform hover:scale-110 active:scale-95 sm:h-14 sm:w-14"
            aria-label="Rewind 10 seconds"
          >
            <Rewind className="h-5 w-5 fill-current" />
          </button>

          <button
            type="button"
            onClick={togglePlay}
            className="grid h-16 w-16 place-items-center rounded-full bg-gradient-brand text-white shadow-glow transition-transform hover:scale-105 active:scale-95 sm:h-20 sm:w-20"
            aria-label={isPlaying ? 'Pause' : 'Play'}
          >
            {isPlaying ? (
              <Pause className="h-8 w-8 fill-current" />
            ) : (
              <Play className="ml-1 h-8 w-8 fill-current" />
            )}
          </button>

          <button
            type="button"
            onClick={() => skipSeconds(10)}
            className="glass grid h-12 w-12 place-items-center rounded-full text-foreground transition-transform hover:scale-110 active:scale-95 sm:h-14 sm:w-14"
            aria-label="Forward 10 seconds"
          >
            <FastForward className="h-5 w-5 fill-current" />
          </button>
        </div>

        {/* Bottom Bar: Seek, Time, Volume, Quality, Fullscreen */}
        <div className="pointer-events-auto rounded-2xl border border-white/10 bg-background/80 p-4 shadow-card backdrop-blur-md">
          {/* Interactive Seek Bar */}
          <div
            className="group relative h-6 cursor-pointer"
            onMouseMove={(e) => {
              const rect = e.currentTarget.getBoundingClientRect();
              const x = e.clientX - rect.left;
              const t = (x / rect.width) * duration;
              setHoverSeek({ x, time: t });
            }}
            onMouseLeave={() => setHoverSeek(null)}
            onClick={(e) => {
              const rect = e.currentTarget.getBoundingClientRect();
              const pos = ((e.clientX - rect.left) / rect.width) * duration;
              if (videoRef.current) {
                videoRef.current.currentTime = pos;
                setCurrentTime(pos);
              }
            }}
          >
            <div className="absolute inset-x-0 top-1/2 h-1 -translate-y-1/2 rounded-full bg-white/20 transition-all group-hover:h-1.5">
              {/* Buffer track */}
              <div
                className="absolute inset-y-0 left-0 rounded-full bg-white/30"
                style={{ width: `${bufferedPercent}%` }}
              />
              {/* Play progress */}
              <div
                className="absolute inset-y-0 left-0 rounded-full bg-cyan shadow-[0_0_8px_rgba(6,182,212,0.8)]"
                style={{ width: `${duration ? (currentTime / duration) * 100 : 0}%` }}
              />
            </div>

            {/* Hover timestamp tooltip */}
            {hoverSeek && (
              <span
                className="absolute -top-7 -translate-x-1/2 rounded bg-surface-elevated px-2 py-0.5 text-[11px] font-mono text-cyan shadow-md"
                style={{ left: hoverSeek.x }}
              >
                {formatTime(hoverSeek.time)}
              </span>
            )}
          </div>

          {/* Controls Bar Row */}
          <div className="mt-2 flex items-center justify-between text-xs text-muted-foreground">
            {/* Left: Play/Pause, Volume, Time */}
            <div className="flex items-center gap-3">
              <button
                type="button"
                onClick={togglePlay}
                className="grid h-8 w-8 place-items-center rounded-full text-foreground hover:bg-surface-elevated"
                aria-label={isPlaying ? 'Pause' : 'Play'}
              >
                {isPlaying ? <Pause className="h-4 w-4" /> : <Play className="h-4 w-4 fill-current" />}
              </button>

              <div className="group/vol flex items-center gap-1.5">
                <button
                  type="button"
                  onClick={toggleMute}
                  className="grid h-8 w-8 place-items-center rounded-full text-foreground hover:bg-surface-elevated"
                  aria-label={isMuted ? 'Unmute' : 'Mute'}
                >
                  {isMuted || volume === 0 ? (
                    <VolumeX className="h-4 w-4 text-rose-400" />
                  ) : (
                    <Volume2 className="h-4 w-4" />
                  )}
                </button>
                <input
                  type="range"
                  min={0}
                  max={1}
                  step={0.05}
                  value={isMuted ? 0 : volume}
                  onChange={(e) => handleVolumeChange(Number(e.target.value))}
                  aria-label="Volume slider"
                  className="w-0 accent-cyan transition-all duration-200 group-hover/vol:w-20 focus:w-20"
                />
              </div>

              <span className="font-mono text-xs tabular-nums text-foreground/90">
                {formatTime(currentTime)} / {formatTime(duration)}
              </span>
            </div>

            {/* Right: Next Ep, Subtitles, Speed, Quality, PiP, Fullscreen */}
            <div className="flex items-center gap-1.5 sm:gap-2">
              {nextEpisodeId && (
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => router.push(`/watch/${title.id}?episode=${nextEpisodeId}`)}
                  className="hidden sm:inline-flex"
                >
                  <SkipForward className="h-3.5 w-3.5" />
                  Next episode
                </Button>
              )}

              {/* Subtitles Menu */}
              <DropdownMenu>
                <DropdownMenuTrigger asChild>
                  <button
                    type="button"
                    className="grid h-8 w-8 place-items-center rounded-full text-foreground hover:bg-surface-elevated"
                    aria-label="Subtitles"
                  >
                    <Captions className="h-4 w-4" />
                  </button>
                </DropdownMenuTrigger>
                <DropdownMenuContent align="end">
                  <DropdownMenuLabel>Subtitles</DropdownMenuLabel>
                  {['Off', 'English [CC]', 'Spanish', 'French', 'German'].map((s) => (
                    <DropdownMenuItem
                      key={s}
                      onSelect={() => setSelectedSubtitles(s)}
                      className={cn(selectedSubtitles === s && 'text-cyan font-semibold')}
                    >
                      {s}
                    </DropdownMenuItem>
                  ))}
                </DropdownMenuContent>
              </DropdownMenu>

              {/* Speed Menu */}
              <DropdownMenu>
                <DropdownMenuTrigger asChild>
                  <button
                    type="button"
                    className="grid h-8 w-8 place-items-center rounded-full text-foreground hover:bg-surface-elevated"
                    aria-label="Playback speed"
                  >
                    <Gauge className="h-4 w-4" />
                  </button>
                </DropdownMenuTrigger>
                <DropdownMenuContent align="end">
                  <DropdownMenuLabel>Speed</DropdownMenuLabel>
                  {[0.5, 0.75, 1, 1.25, 1.5, 2].map((s) => (
                    <DropdownMenuItem
                      key={s}
                      onSelect={() => handleSpeedChange(s)}
                      className={cn(speed === s && 'text-cyan font-semibold')}
                    >
                      {s}x
                    </DropdownMenuItem>
                  ))}
                </DropdownMenuContent>
              </DropdownMenu>

              {/* Quality Menu */}
              <DropdownMenu>
                <DropdownMenuTrigger asChild>
                  <button
                    type="button"
                    className="grid h-8 w-8 place-items-center rounded-full text-foreground hover:bg-surface-elevated"
                    aria-label="Quality settings"
                  >
                    <Settings className="h-4 w-4" />
                  </button>
                </DropdownMenuTrigger>
                <DropdownMenuContent align="end">
                  <DropdownMenuLabel>Quality</DropdownMenuLabel>
                  <DropdownMenuItem
                    onSelect={() => handleQualitySelect(-1)}
                    className={cn(currentQuality === -1 && 'text-cyan font-semibold')}
                  >
                    Auto
                  </DropdownMenuItem>
                  {qualityLevels.map((lvl) => (
                    <DropdownMenuItem
                      key={lvl.id}
                      onSelect={() => handleQualitySelect(lvl.id)}
                      className={cn(currentQuality === lvl.id && 'text-cyan font-semibold')}
                    >
                      {lvl.name}
                    </DropdownMenuItem>
                  ))}
                </DropdownMenuContent>
              </DropdownMenu>

              {/* Picture-in-Picture */}
              <button
                type="button"
                onClick={() =>
                  document.pictureInPictureElement
                    ? document.exitPictureInPicture()
                    : videoRef.current?.requestPictureInPicture()
                }
                className="hidden h-8 w-8 place-items-center rounded-full text-foreground hover:bg-surface-elevated sm:grid"
                aria-label="Picture-in-picture"
              >
                <PictureInPicture2 className="h-4 w-4" />
              </button>

              {/* Fullscreen */}
              <button
                type="button"
                onClick={toggleFullscreen}
                className="grid h-8 w-8 place-items-center rounded-full text-foreground hover:bg-surface-elevated"
                aria-label={isFullscreen ? 'Exit Fullscreen' : 'Enter Fullscreen'}
              >
                {isFullscreen ? <Minimize className="h-4 w-4" /> : <Maximize className="h-4 w-4" />}
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* Subtitles display sample */}
      {selectedSubtitles !== 'Off' && isPlaying && (
        <p className="pointer-events-none absolute inset-x-0 bottom-32 z-10 text-center">
          <span className="rounded-lg bg-black/75 px-3 py-1.5 text-sm font-medium text-white shadow-md">
            [{selectedSubtitles} subtitles active]
          </span>
        </p>
      )}

      {/* Keyboard Shortcuts Modal */}
      {showShortcutsHelp && (
        <div
          className="fixed inset-0 z-50 grid place-items-center bg-black/70 p-4 backdrop-blur-sm"
          onClick={() => setShowShortcutsHelp(false)}
        >
          <div
            className="glass w-full max-w-sm rounded-2xl p-6 shadow-glow"
            onClick={(e) => e.stopPropagation()}
            role="dialog"
            aria-label="Keyboard shortcuts"
          >
            <h2 className="mb-4 text-base font-bold text-foreground">Keyboard shortcuts</h2>
            <dl className="grid grid-cols-2 gap-y-2.5 text-xs">
              {[
                ['Space / K', 'Play / pause'],
                ['← / →', 'Seek 10 seconds'],
                ['↑ / ↓', 'Volume up / down'],
                ['M', 'Mute / unmute'],
                ['F', 'Toggle fullscreen'],
                ['?', 'Show this help'],
                ['Esc', 'Close / back'],
              ].map(([key, desc]) => (
                <React.Fragment key={key}>
                  <dt>
                    <kbd className="rounded-md border border-border bg-surface-elevated px-2 py-1 font-mono text-[11px] text-cyan">
                      {key}
                    </kbd>
                  </dt>
                  <dd className="text-muted-foreground">{desc}</dd>
                </React.Fragment>
              ))}
            </dl>
          </div>
        </div>
      )}
    </div>
  );
}
