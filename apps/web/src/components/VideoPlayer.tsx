'use client';

import React, { useEffect, useRef, useState, useCallback } from 'react';
import Hls from 'hls.js';
import { useRouter } from 'next/navigation';
import {
  ArrowLeft,
  Play,
  Pause,
  RotateCcw,
  RotateCw,
  Volume2,
  VolumeX,
  Maximize,
  Minimize,
  Settings,
  Loader2,
} from 'lucide-react';
import { ApiError, createPlaybackSession, endPlaybackSession, saveProgress } from '../lib/api';
import { formatTime } from '../lib/utils';
import { Title } from '../types';

// Player chrome sits over video, so it uses fixed translucent booth tones instead of the page surfaces.
const controlButton =
  'flex items-center justify-center rounded-full border border-white/15 bg-black/55 text-paper transition-colors hover:border-projector hover:text-projector focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-projector';
const textButton =
  'text-paper transition-colors hover:text-projector focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-projector rounded';

interface VideoPlayerProps {
  title: Title;
  /** Playable id: the title id for movies, the episode id for episodes. Defaults to the title id. */
  contentId?: string;
  targetType?: 'MOVIE' | 'EPISODE';
  episodeTitle?: string;
}

export default function VideoPlayer({
  title,
  contentId: contentIdProp,
  targetType = 'MOVIE',
  episodeTitle,
}: VideoPlayerProps) {
  const router = useRouter();
  const contentId = contentIdProp ?? title.id;
  const durationRef = useRef(0);
  const [retryNonce, setRetryNonce] = useState(0);
  const videoRef = useRef<HTMLVideoElement>(null);
  const containerRef = useRef<HTMLDivElement>(null);
  const hlsRef = useRef<Hls | null>(null);
  const controlsTimeoutRef = useRef<NodeJS.Timeout | null>(null);
  const heartbeatIntervalRef = useRef<NodeJS.Timeout | null>(null);
  // The playback session this player currently holds; ended when it is replaced, unmounted or the page is left.
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
  const [qualityLevels, setQualityLevels] = useState<{ id: number; name: string }[]>([]);
  const [currentQuality, setCurrentQuality] = useState(-1); // -1 = Auto
  const [showQualityMenu, setShowQualityMenu] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  // Progress heartbeat. Reads everything through refs so its identity is stable: the player must NOT
  // re-initialise (new session + new hls.js instance) just because the duration became known.
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
        // Created but never used (the player unmounted first): release it.
        void endPlaybackSession(session.sessionId);
        return null;
      }
      // A refresh replaces the previous session; end it so it stops counting against the plan's stream limit.
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

        if (Hls.isSupported()) {
          hlsInstance = new Hls({
            enableWorker: true,
            lowLatencyMode: false,
            startPosition: session.resumePositionSeconds || -1,
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
                // An expired/rejected signed URL looks like a network error: ask for a fresh session
                // (a few times) rather than retrying the dead URL forever.
                if (sessionRefreshes >= MAX_SESSION_REFRESHES) {
                  hlsInstance?.destroy();
                  setErrorMsg('The stream could not be loaded. Please try again.');
                  return;
                }
                sessionRefreshes += 1;
                const resumeAt = video.currentTime;
                const wasPlaying = !video.paused;
                startSession()
                  .then((next) => {
                    if (!next || !hlsInstance) return;
                    hlsInstance.once(Hls.Events.MANIFEST_PARSED, () => {
                      video.currentTime = resumeAt;
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
          // Native Safari HLS
          video.src = manifestUrl;
          const onLoadedMetadata = () => {
            if (session.resumePositionSeconds > 0) {
              video.currentTime = session.resumePositionSeconds;
            }
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
                : err instanceof ApiError && err.status === 404
                  ? 'This title is not available to watch yet.'
                  : 'Could not authorize playback. Please try again.',
          );
        }
      }
    }

    initPlayer();

    // Progress heartbeat every 20 seconds while actively playing
    heartbeatIntervalRef.current = setInterval(() => {
      if (videoRef.current && !videoRef.current.paused) sendHeartbeat();
    }, 20000);

    // Final best-effort update when the tab is hidden or closed (unmount cleanup does not run on tab close)
    const flush = () => sendHeartbeat({ keepalive: true });
    const onVisibility = () => {
      if (document.visibilityState === 'hidden') flush();
    };
    // Leaving the page ends the session. Merely hiding the tab does not, because the viewer may come back.
    const onPageHide = () => {
      flush();
      endActiveSession();
    };
    window.addEventListener('pagehide', onPageHide);
    document.addEventListener('visibilitychange', onVisibility);

    return () => {
      isCancelled = true;
      if (heartbeatIntervalRef.current) {
        clearInterval(heartbeatIntervalRef.current);
      }
      window.removeEventListener('pagehide', onPageHide);
      document.removeEventListener('visibilitychange', onVisibility);
      sendHeartbeat({ keepalive: true }); // final best effort (navigation away)
      endActiveSession();
      if (hlsInstance) {
        hlsInstance.destroy();
      }
      hlsRef.current = null;
    };
  }, [contentId, targetType, sendHeartbeat, retryNonce]);

  // Video event handlers
  const handleTimeUpdate = () => {
    if (videoRef.current) {
      setCurrentTime(videoRef.current.currentTime);
      // Buffered range
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

  const handleSeek = (e: React.ChangeEvent<HTMLInputElement>) => {
    const time = parseFloat(e.target.value);
    if (videoRef.current) {
      videoRef.current.currentTime = time;
      setCurrentTime(time);
    }
  };

  const skipSeconds = (offset: number) => {
    if (videoRef.current) {
      videoRef.current.currentTime = Math.max(0, Math.min(duration, videoRef.current.currentTime + offset));
    }
  };

  const handleVolumeChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const val = parseFloat(e.target.value);
    setVolume(val);
    if (videoRef.current) {
      videoRef.current.volume = val;
      videoRef.current.muted = val === 0;
      setIsMuted(val === 0);
    }
  };

  const adjustVolume = (delta: number) => {
    const video = videoRef.current;
    if (!video) return;
    const next = Math.min(1, Math.max(0, Math.round((video.volume + delta) * 10) / 10));
    video.volume = next;
    video.muted = next === 0;
    setVolume(next);
    setIsMuted(next === 0);
  };

  const toggleMute = () => {
    if (videoRef.current) {
      const nextMuted = !isMuted;
      videoRef.current.muted = nextMuted;
      setIsMuted(nextMuted);
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
    const onFullscreenChange = () => setIsFullscreen(!!document.fullscreenElement);
    document.addEventListener('fullscreenchange', onFullscreenChange);
    return () => document.removeEventListener('fullscreenchange', onFullscreenChange);
  }, []);

  const handleQualitySelect = (levelId: number) => {
    if (hlsRef.current) {
      hlsRef.current.currentLevel = levelId;
      setCurrentQuality(levelId);
      setShowQualityMenu(false);
    }
  };

  // Auto-hide controls
  const handleMouseMove = () => {
    setShowControls(true);
    if (controlsTimeoutRef.current) {
      clearTimeout(controlsTimeoutRef.current);
    }
    controlsTimeoutRef.current = setTimeout(() => {
      if (isPlaying) {
        setShowControls(false);
      }
    }, 3000);
  };

  // Keyboard shortcut handlers
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (['INPUT', 'TEXTAREA'].includes((e.target as HTMLElement).tagName)) return;
      switch (e.code) {
        case 'Space':
        case 'KeyK':
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
          adjustVolume(0.1);
          break;
        case 'ArrowDown':
          e.preventDefault();
          adjustVolume(-0.1);
          break;
        case 'KeyM':
          toggleMute();
          break;
        case 'KeyF':
          toggleFullscreen();
          break;
        case 'Escape':
          if (document.fullscreenElement) {
            document.exitFullscreen().catch(() => {});
          }
          break;
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [togglePlay, skipSeconds, toggleMute, toggleFullscreen]);

  return (
    <div
      ref={containerRef}
      onMouseMove={handleMouseMove}
      className="relative w-full h-screen bg-black overflow-hidden select-none flex items-center justify-center"
    >
      {/* HTML5 Video Element */}
      <video
        ref={videoRef}
        onTimeUpdate={handleTimeUpdate}
        onLoadedMetadata={handleLoadedMetadata}
        onPlay={() => setIsPlaying(true)}
        onPause={() => setIsPlaying(false)}
        onEnded={() => {
          setIsPlaying(false);
          sendHeartbeat();
        }}
        onClick={togglePlay}
        className="w-full h-full object-contain cursor-pointer"
        playsInline
      />

      {/* Loading Spinner */}
      {loading && (
        <div className="absolute inset-0 flex flex-col items-center justify-center bg-black/70 z-30">
          <Loader2 className="w-10 h-10 text-projector animate-spin mb-3" aria-hidden="true" />
          <p className="text-sm text-muted-foreground" role="status">
            Preparing your stream…
          </p>
        </div>
      )}

      {/* Error Banner */}
      {errorMsg && (
        <div className="absolute inset-0 flex flex-col items-center justify-center bg-black/90 z-30 p-6 text-center">
          <p role="alert" className="text-danger font-medium mb-4 text-base">{errorMsg}</p>
          <button
            onClick={() => setRetryNonce((n) => n + 1)}
            className="px-6 py-2.5 rounded-pill bg-projector text-booth font-medium text-sm transition-colors hover:bg-projector/90 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-paper"
          >
            Try again
          </button>
        </div>
      )}

      {/* Custom Glass Controls Overlay */}
      <div
        className={`absolute inset-0 flex flex-col justify-between p-6 transition-opacity duration-300 pointer-events-none z-20 ${
          showControls ? 'opacity-100' : 'opacity-0'
        }`}
      >
        {/* Top Header Bar */}
        <div className="flex items-center justify-between pointer-events-auto">
          <div className="flex items-center space-x-4">
            <button
              onClick={() => router.back()}
              className={`${controlButton} w-10 h-10`}
              aria-label="Back"
            >
              <ArrowLeft className="w-5 h-5" />
            </button>
            <div>
              <h2 className="font-heading text-lg text-paper drop-shadow">
                {title.title}
              </h2>
              {episodeTitle && (
                <p className="text-xs text-muted-foreground drop-shadow">{episodeTitle}</p>
              )}
            </div>
          </div>
        </div>

        {/* Center Play/Pause & Skip Controls */}
        <div className="flex items-center justify-center space-x-8 pointer-events-auto">
          <button
            onClick={() => skipSeconds(-10)}
            className={`${controlButton} w-12 h-12`}
            aria-label="Rewind 10 seconds"
          >
            <RotateCcw className="w-5 h-5" />
          </button>

          {/* The play action is the one amber surface in the player. */}
          <button
            onClick={togglePlay}
            className="flex items-center justify-center w-16 h-16 rounded-full bg-projector text-booth transition-transform hover:scale-105 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-paper"
            aria-label={isPlaying ? 'Pause' : 'Play'}
          >
            {isPlaying ? (
              <Pause className="w-7 h-7 fill-booth" />
            ) : (
              <Play className="w-7 h-7 fill-booth ml-1" />
            )}
          </button>

          <button
            onClick={() => skipSeconds(10)}
            className={`${controlButton} w-12 h-12`}
            aria-label="Forward 10 seconds"
          >
            <RotateCw className="w-5 h-5" />
          </button>
        </div>

        {/* Bottom Bar: Seek, Time, Volume, Quality, Fullscreen */}
        <div className="flex flex-col space-y-3 pointer-events-auto rounded-card border border-white/10 bg-booth/80 p-4 backdrop-blur-md">
          {/* Progress / Seek bar */}
          <div className="relative w-full flex items-center group">
            {/* Buffered track */}
            <div
              className="absolute left-0 top-1/2 -translate-y-1/2 h-1 bg-white/25 rounded-full pointer-events-none"
              style={{ width: `${bufferedPercent}%` }}
            />
            <input
              type="range"
              aria-label="Seek"
              min={0}
              max={duration || 100}
              value={currentTime}
              onChange={handleSeek}
              className="w-full h-1 bg-white/15 appearance-none cursor-pointer rounded-full accent-projector focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-projector"
            />
          </div>

          {/* Controls Bar */}
          <div className="flex items-center justify-between text-xs text-film-grey">
            {/* Left: Play/Pause, Volume, Time */}
            <div className="flex items-center space-x-4">
              <button
                onClick={togglePlay}
                className={`${textButton} p-1`}
                aria-label={isPlaying ? 'Pause' : 'Play'}
              >
                {isPlaying ? <Pause className="w-4 h-4" /> : <Play className="w-4 h-4 fill-paper" />}
              </button>

              <div className="flex items-center space-x-2">
                <button
                  onClick={toggleMute}
                  className={`${textButton} p-1`}
                  aria-label="Mute"
                >
                  {isMuted || volume === 0 ? (
                    <VolumeX className="w-4 h-4 text-danger" />
                  ) : (
                    <Volume2 className="w-4 h-4" />
                  )}
                </button>
                <input
                  type="range"
                  aria-label="Volume"
                  min={0}
                  max={1}
                  step={0.05}
                  value={isMuted ? 0 : volume}
                  onChange={handleVolumeChange}
                  className="w-16 h-1 bg-white/15 accent-projector rounded-full cursor-pointer focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-projector"
                />
              </div>

              <span className="font-mono text-paper/90 tabular-nums">
                {formatTime(currentTime)} / {formatTime(duration)}
              </span>
            </div>

            {/* Right: Quality Selector & Fullscreen */}
            <div className="flex items-center space-x-4 relative">
              {/* Quality Settings */}
              {qualityLevels.length > 0 && (
                <div className="relative">
                  <button
                    onClick={() => setShowQualityMenu(!showQualityMenu)}
                    aria-haspopup="menu"
                    aria-expanded={showQualityMenu}
                    className="flex items-center gap-1.5 rounded border border-white/20 bg-black/50 px-2.5 py-1 text-xs text-paper transition-colors hover:border-projector focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-projector"
                  >
                    <Settings className="w-3.5 h-3.5" />
                    <span>
                      {currentQuality === -1
                        ? 'Auto'
                        : qualityLevels.find((q) => q.id === currentQuality)?.name || 'Auto'}
                    </span>
                  </button>

                  {showQualityMenu && (
                    <div
                      role="menu"
                      className="absolute bottom-9 right-0 z-30 min-w-[100px] rounded-card border border-gate bg-booth py-1.5 shadow-xl"
                    >
                      <button
                        role="menuitemradio"
                        aria-checked={currentQuality === -1}
                        onClick={() => handleQualitySelect(-1)}
                        className={`w-full text-left px-3 py-1 text-xs hover:bg-screen-raised transition-colors ${
                          currentQuality === -1 ? 'text-projector font-medium' : 'text-paper'
                        }`}
                      >
                        Auto
                      </button>
                      {qualityLevels.map((lvl) => (
                        <button
                          key={lvl.id}
                          role="menuitemradio"
                          aria-checked={currentQuality === lvl.id}
                          onClick={() => handleQualitySelect(lvl.id)}
                          className={`w-full text-left px-3 py-1 text-xs hover:bg-screen-raised transition-colors ${
                            currentQuality === lvl.id ? 'text-projector font-medium' : 'text-paper'
                          }`}
                        >
                          {lvl.name}
                        </button>
                      ))}
                    </div>
                  )}
                </div>
              )}

              {/* Fullscreen button */}
              <button
                onClick={toggleFullscreen}
                className={`${textButton} p-1`}
                aria-label="Toggle Fullscreen"
              >
                {isFullscreen ? <Minimize className="w-4 h-4" /> : <Maximize className="w-4 h-4" />}
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
