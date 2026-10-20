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
import { createPlaybackSession, saveProgress } from '../lib/api';
import { formatTime } from '../lib/utils';
import { Title } from '../types';

interface VideoPlayerProps {
  title: Title;
  targetType?: 'MOVIE' | 'EPISODE';
  episodeTitle?: string;
}

export default function VideoPlayer({
  title,
  targetType = 'MOVIE',
  episodeTitle,
}: VideoPlayerProps) {
  const router = useRouter();
  const videoRef = useRef<HTMLVideoElement>(null);
  const containerRef = useRef<HTMLDivElement>(null);
  const hlsRef = useRef<Hls | null>(null);
  const controlsTimeoutRef = useRef<NodeJS.Timeout | null>(null);
  const heartbeatIntervalRef = useRef<NodeJS.Timeout | null>(null);

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

  // Send progress heartbeat
  const sendHeartbeat = useCallback(() => {
    if (videoRef.current && duration > 0) {
      const pos = Math.round(videoRef.current.currentTime);
      const dur = Math.round(duration);
      saveProgress({
        contentId: title.id,
        targetType,
        positionSeconds: pos,
        durationSeconds: dur,
      });
    }
  }, [title.id, targetType, duration]);

  // Initialize stream and HLS
  useEffect(() => {
    let isCancelled = false;
    let hlsInstance: Hls | null = null;

    async function initPlayer() {
      setLoading(true);
      setErrorMsg(null);

      try {
        const session = await createPlaybackSession(title.id, targetType);

        if (isCancelled || !videoRef.current) return;

        const video = videoRef.current;
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
            if (data.fatal) {
              switch (data.type) {
                case Hls.ErrorTypes.NETWORK_ERROR:
                  hlsInstance?.startLoad();
                  break;
                case Hls.ErrorTypes.MEDIA_ERROR:
                  hlsInstance?.recoverMediaError();
                  break;
                default:
                  hlsInstance?.destroy();
                  setErrorMsg('Failed to play media stream. Reconnecting...');
                  break;
              }
            }
          });
        } else if (video.canPlayType('application/vnd.apple.mpegurl')) {
          // Native Safari HLS
          video.src = manifestUrl;
          video.addEventListener('loadedmetadata', () => {
            if (session.resumePositionSeconds > 0) {
              video.currentTime = session.resumePositionSeconds;
            }
            setLoading(false);
            video.play().catch(() => setIsPlaying(false));
          });
        } else {
          setErrorMsg('HLS playback is not supported on this browser.');
        }
      } catch (err) {
        if (!isCancelled) {
          setErrorMsg('Could not authorize playback session.');
        }
      }
    }

    initPlayer();

    // Start progress heartbeat every 20 seconds
    heartbeatIntervalRef.current = setInterval(sendHeartbeat, 20000);

    return () => {
      isCancelled = true;
      if (heartbeatIntervalRef.current) {
        clearInterval(heartbeatIntervalRef.current);
      }
      sendHeartbeat(); // final best effort
      if (hlsInstance) {
        hlsInstance.destroy();
      }
      hlsRef.current = null;
    };
  }, [title.id, targetType, sendHeartbeat]);

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
          setVolume((v) => Math.min(1, v + 0.1));
          break;
        case 'ArrowDown':
          e.preventDefault();
          setVolume((v) => Math.max(0, v - 0.1));
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
        onClick={togglePlay}
        className="w-full h-full object-contain cursor-pointer"
        playsInline
      />

      {/* Loading Spinner */}
      {loading && (
        <div className="absolute inset-0 flex flex-col items-center justify-center bg-black/70 z-30">
          <Loader2 className="w-12 h-12 text-primary animate-spin mb-3" />
          <p className="text-sm font-medium text-text-secondary tracking-wide">
            Preparing your stream...
          </p>
        </div>
      )}

      {/* Error Banner */}
      {errorMsg && (
        <div className="absolute inset-0 flex flex-col items-center justify-center bg-black/90 z-30 p-6 text-center">
          <p className="text-red-400 font-semibold mb-4 text-base">{errorMsg}</p>
          <button
            onClick={() => window.location.reload()}
            className="px-6 py-2.5 rounded-full bg-white text-black font-semibold text-sm hover:bg-gray-200 transition-colors"
          >
            Retry Stream
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
              className="w-10 h-10 rounded-full glass-panel flex items-center justify-center text-white hover:scale-105 transition-transform"
              aria-label="Back"
            >
              <ArrowLeft className="w-5 h-5" />
            </button>
            <div>
              <h2 className="font-heading font-bold text-lg text-white drop-shadow">
                {title.title}
              </h2>
              {episodeTitle && (
                <p className="text-xs text-text-secondary drop-shadow">{episodeTitle}</p>
              )}
            </div>
          </div>
        </div>

        {/* Center Play/Pause & Skip Controls */}
        <div className="flex items-center justify-center space-x-8 pointer-events-auto">
          <button
            onClick={() => skipSeconds(-10)}
            className="w-12 h-12 rounded-full glass-panel flex items-center justify-center text-white hover:scale-110 transition-transform"
            aria-label="Rewind 10 seconds"
          >
            <RotateCcw className="w-5 h-5" />
          </button>

          <button
            onClick={togglePlay}
            className="w-16 h-16 rounded-full gradient-accent flex items-center justify-center text-white shadow-2xl shadow-purple-600/40 hover:scale-110 transition-transform"
            aria-label={isPlaying ? 'Pause' : 'Play'}
          >
            {isPlaying ? (
              <Pause className="w-7 h-7 fill-white" />
            ) : (
              <Play className="w-7 h-7 fill-white ml-1" />
            )}
          </button>

          <button
            onClick={() => skipSeconds(10)}
            className="w-12 h-12 rounded-full glass-panel flex items-center justify-center text-white hover:scale-110 transition-transform"
            aria-label="Forward 10 seconds"
          >
            <RotateCw className="w-5 h-5" />
          </button>
        </div>

        {/* Bottom Bar: Seek, Time, Volume, Quality, Fullscreen */}
        <div className="flex flex-col space-y-3 pointer-events-auto glass-panel p-4 rounded-card backdrop-blur-md">
          {/* Progress / Seek bar */}
          <div className="relative w-full flex items-center group">
            {/* Buffered track */}
            <div
              className="absolute left-0 top-1/2 -translate-y-1/2 h-1.5 bg-white/20 rounded-full pointer-events-none"
              style={{ width: `${bufferedPercent}%` }}
            />
            <input
              type="range"
              min={0}
              max={duration || 100}
              value={currentTime}
              onChange={handleSeek}
              className="w-full h-1.5 bg-surface-elevated appearance-none cursor-pointer rounded-full accent-cyan-400 focus:outline-none"
            />
          </div>

          {/* Controls Bar */}
          <div className="flex items-center justify-between text-xs text-text-secondary">
            {/* Left: Play/Pause, Volume, Time */}
            <div className="flex items-center space-x-4">
              <button
                onClick={togglePlay}
                className="text-white hover:text-purple-400 transition-colors"
                aria-label={isPlaying ? 'Pause' : 'Play'}
              >
                {isPlaying ? <Pause className="w-4 h-4" /> : <Play className="w-4 h-4 fill-white" />}
              </button>

              <div className="flex items-center space-x-2">
                <button
                  onClick={toggleMute}
                  className="text-white hover:text-purple-400 transition-colors"
                  aria-label="Mute"
                >
                  {isMuted || volume === 0 ? (
                    <VolumeX className="w-4 h-4 text-red-400" />
                  ) : (
                    <Volume2 className="w-4 h-4" />
                  )}
                </button>
                <input
                  type="range"
                  min={0}
                  max={1}
                  step={0.05}
                  value={isMuted ? 0 : volume}
                  onChange={handleVolumeChange}
                  className="w-16 h-1 bg-surface-hover accent-purple-500 rounded-full cursor-pointer"
                />
              </div>

              <span className="font-mono text-white/90">
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
                    className="flex items-center space-x-1 px-2.5 py-1 rounded border border-border/80 bg-surface/80 text-white hover:border-purple-400 text-xs transition-colors"
                  >
                    <Settings className="w-3.5 h-3.5 mr-1" />
                    <span>
                      {currentQuality === -1
                        ? 'Auto'
                        : qualityLevels.find((q) => q.id === currentQuality)?.name || 'Auto'}
                    </span>
                  </button>

                  {showQualityMenu && (
                    <div className="absolute bottom-9 right-0 glass-panel rounded-lg py-1.5 shadow-xl min-w-[100px] z-30">
                      <button
                        onClick={() => handleQualitySelect(-1)}
                        className={`w-full text-left px-3 py-1 text-xs hover:bg-surface-elevated transition-colors ${
                          currentQuality === -1 ? 'text-accent-cyan font-bold' : 'text-white'
                        }`}
                      >
                        Auto
                      </button>
                      {qualityLevels.map((lvl) => (
                        <button
                          key={lvl.id}
                          onClick={() => handleQualitySelect(lvl.id)}
                          className={`w-full text-left px-3 py-1 text-xs hover:bg-surface-elevated transition-colors ${
                            currentQuality === lvl.id ? 'text-accent-cyan font-bold' : 'text-white'
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
                className="text-white hover:text-purple-400 transition-colors p-1"
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
