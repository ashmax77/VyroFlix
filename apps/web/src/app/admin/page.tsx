'use client';

import React, { useState, useEffect, useCallback, useRef } from 'react';
import Navbar from '../../components/Navbar';
import Footer from '../../components/Footer';
import {
  UploadCloud,
  FileVideo,
  CheckCircle2,
  AlertTriangle,
  RotateCcw,
  Play,
  Layers,
  Clock,
  ArrowRight,
  HardDrive,
  RefreshCw,
} from 'lucide-react';
import {
  createUploadIntent,
  uploadFileDirectToStorage,
  completeUploadIntent,
  getEncodingJobs,
  retryEncodingJob,
  fetchAdminTitles,
  ApiError,
} from '../../lib/api';
import { useAuth } from '../../lib/auth-context';
import { ErrorState, Spinner } from '../../components/common';
import AnalyticsPanel from '../../components/admin/AnalyticsPanel';
import TitleManager from '../../components/admin/TitleManager';
import { EncodingJob, EncodingStatus, Title } from '../../types';
import Link from 'next/link';

export default function AdminStudioPage() {
  const { isAdmin, isAuthed, loading: authLoading } = useAuth();

  // Title picker (real catalog, including drafts)
  const [titles, setTitles] = useState<Title[]>([]);
  const [titlesError, setTitlesError] = useState<string | null>(null);

  // Upload Studio State
  const [selectedContentId, setSelectedContentId] = useState('');
  const [targetType, setTargetType] = useState<'MOVIE' | 'EPISODE'>('MOVIE');
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [isUploading, setIsUploading] = useState(false);
  const [uploadProgress, setUploadProgress] = useState(0);
  const [uploadStep, setUploadStep] = useState<'IDLE' | 'INTENT' | 'UPLOADING' | 'COMPLETING' | 'DONE'>('IDLE');
  const [uploadMessage, setUploadMessage] = useState('');
  const [uploadError, setUploadError] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  // Encoding Jobs Monitor State
  const [jobs, setJobs] = useState<EncodingJob[]>([]);
  const [statusFilter, setStatusFilter] = useState<EncodingStatus | 'ALL'>('ALL');
  const [isLoadingJobs, setIsLoadingJobs] = useState(true);
  const [autoRefresh, setAutoRefresh] = useState(true);
  const [retryingJobId, setRetryingJobId] = useState<string | null>(null);

  const [jobsError, setJobsError] = useState<string | null>(null);

  const fetchJobs = useCallback(async () => {
    try {
      const data = await getEncodingJobs(statusFilter === 'ALL' ? undefined : statusFilter);
      setJobs(data);
      setJobsError(null);
    } catch (e) {
      // The encoding worker runs outside the free-tier backend, so a 404/502 here usually means "worker offline".
      setJobsError(
        e instanceof ApiError && (e.isNotFound || e.isNetwork)
          ? 'The encoding service is not reachable. Start the encoding worker to process uploads.'
          : e instanceof ApiError
            ? e.message
            : 'Could not load encoding jobs.',
      );
    } finally {
      setIsLoadingJobs(false);
    }
  }, [statusFilter]);

  const loadTitles = useCallback(async () => {
    setTitlesError(null);
    try {
      const data = await fetchAdminTitles();
      setTitles(data);
      setSelectedContentId((current) => current || data[0]?.id || '');
    } catch (e) {
      setTitlesError(e instanceof ApiError ? e.message : 'Could not load titles.');
    }
  }, []);

  useEffect(() => {
    if (isAdmin) loadTitles();
  }, [isAdmin, loadTitles]);

  useEffect(() => {
    if (isAdmin) fetchJobs();
  }, [isAdmin, fetchJobs]);

  useEffect(() => {
    if (!autoRefresh || !isAdmin) return;
    const interval = setInterval(() => {
      fetchJobs();
    }, 8000);
    return () => clearInterval(interval);
  }, [autoRefresh, isAdmin, fetchJobs]);

  const handleFileDrop = (e: React.DragEvent) => {
    e.preventDefault();
    if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
      setSelectedFile(e.dataTransfer.files[0]);
    }
  };

  const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files.length > 0) {
      setSelectedFile(e.target.files[0]);
    }
  };

  const handleStartUpload = async () => {
    if (!selectedFile || !selectedContentId) return;

    setIsUploading(true);
    setUploadError(null);
    setUploadProgress(0);

    try {
      // Step 1: Create upload intent via Spring Boot API Gateway
      setUploadStep('INTENT');
      setUploadMessage('Requesting short-lived pre-signed S3 / MinIO upload URL...');

      const intent = await createUploadIntent({
        contentId: selectedContentId,
        targetType,
        filename: selectedFile.name,
        contentType: selectedFile.type || 'video/mp4',
        fileSizeBytes: selectedFile.size,
      });

      // Step 2: Direct browser PUT to pre-signed S3 URL (Zero bytes pass through Spring Boot)
      setUploadStep('UPLOADING');
      setUploadMessage('Direct binary transfer to Cloudflare R2 / MinIO storage...');

      await uploadFileDirectToStorage(intent.uploadUrl, selectedFile, (pct) => {
        setUploadProgress(pct);
      });

      // Step 3: Complete upload intent and trigger Kafka video.uploaded.v1 event
      setUploadStep('COMPLETING');
      setUploadMessage('Verifying storage object and dispatching video.uploaded.v1 event...');

      await completeUploadIntent(intent.id);

      setUploadStep('DONE');
      setUploadMessage('Upload verified! Asynchronous FFmpeg encoding pipeline has been queued.');
      setSelectedFile(null);
      fetchJobs();
    } catch (err: any) {
      console.error('Upload flow error:', err);
      setUploadError(err.message || 'An unexpected error occurred during direct media upload');
      setUploadStep('IDLE');
    } finally {
      setIsUploading(false);
    }
  };

  const handleRetryJob = async (jobId: string) => {
    setRetryingJobId(jobId);
    try {
      await retryEncodingJob(jobId);
      await fetchJobs();
    } catch (err: any) {
      console.error('Failed to retry job:', err);
    } finally {
      setRetryingJobId(null);
    }
  };

  if (authLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <Spinner />
      </div>
    );
  }

  if (!isAuthed || !isAdmin) {
    return (
      <div className="min-h-screen flex flex-col items-center justify-center gap-4 text-center px-6">
        <h1 className="text-2xl font-bold">Admin access required</h1>
        <p className="text-text-secondary text-sm">
          {isAuthed ? 'Your account does not have the ADMIN role.' : 'Sign in with an administrator account.'}
        </p>
        <Link
          href={isAuthed ? '/browse' : '/login?redirect=/admin'}
          className="px-6 py-2.5 rounded-full bg-white text-black font-semibold"
        >
          {isAuthed ? 'Back to browse' : 'Sign in'}
        </Link>
      </div>
    );
  }

  const selectedTitle = titles.find((t) => t.id === selectedContentId);

  // Statistics
  const totalJobs = jobs.length;
  const processingCount = jobs.filter((j) => j.status === 'PROCESSING' || j.status === 'QUEUED').length;
  const readyCount = jobs.filter((j) => j.status === 'READY').length;
  const failedCount = jobs.filter((j) => j.status === 'FAILED').length;

  return (
    <div className="min-h-screen bg-background text-text-primary flex flex-col">
      <Navbar />

      <main className="flex-1 pt-28 pb-16 px-4 sm:px-6 lg:px-8 max-w-7xl mx-auto w-full">
        {/* Page Header */}
        <div className="flex flex-col md:flex-row md:items-center justify-between pb-8 border-b border-white/10 gap-4">
          <div>
            <div className="flex items-center space-x-3 mb-2">
              <div className="p-2 rounded-lg gradient-accent shadow-lg shadow-purple-500/20">
                <HardDrive className="w-5 h-5 text-white" />
              </div>
              <h1 className="text-3xl font-heading font-black tracking-tight text-white">
                VyroFlix <span className="gradient-text">Studio</span>
              </h1>
            </div>
            <p className="text-sm text-text-secondary">
              Zero-cost cloud-native media ingestion pipeline. Direct S3/R2 presigned PUTs with automated multi-rendition HLS transcoding.
            </p>
          </div>

          <div className="flex items-center space-x-3">
            <button
              onClick={() => fetchJobs()}
              className="flex items-center space-x-2 px-3.5 py-2 rounded-lg bg-white/5 hover:bg-white/10 text-white text-xs font-medium border border-white/10 transition-colors"
            >
              <RefreshCw className={`w-3.5 h-3.5 ${isLoadingJobs ? 'animate-spin' : ''}`} />
              <span>Refresh</span>
            </button>
            <button
              onClick={() => setAutoRefresh(!autoRefresh)}
              className={`flex items-center space-x-1.5 px-3 py-2 rounded-lg text-xs font-medium border transition-colors ${
                autoRefresh
                  ? 'bg-purple-500/20 text-purple-300 border-purple-500/30'
                  : 'bg-white/5 text-text-muted border-white/10'
              }`}
            >
              <div className={`w-2 h-2 rounded-full ${autoRefresh ? 'bg-purple-400 animate-pulse' : 'bg-gray-500'}`} />
              <span>Auto-refresh {autoRefresh ? 'ON' : 'OFF'}</span>
            </button>
          </div>
        </div>

        {/* Studio Grid */}
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 mt-8">
          {/* Left Column: Direct Upload Studio */}
          <div className="lg:col-span-5 space-y-6">
            <div className="p-6 rounded-2xl bg-card border border-white/10 shadow-xl relative overflow-hidden">
              <div className="flex items-center justify-between mb-5">
                <h2 className="text-lg font-heading font-bold text-white flex items-center space-x-2">
                  <UploadCloud className="w-5 h-5 text-purple-400" />
                  <span>Direct Ingestion Studio</span>
                </h2>
                <span className="text-[11px] font-semibold px-2.5 py-1 rounded-full bg-purple-500/10 text-purple-300 border border-purple-500/20">
                  Pre-signed PUT
                </span>
              </div>

              {/* Architecture Notice */}
              <div className="p-3.5 rounded-xl bg-purple-950/20 border border-purple-500/20 mb-6 text-xs text-text-secondary leading-relaxed">
                <span className="text-purple-300 font-semibold block mb-0.5">Strict AGENTS.md Media Constraint:</span>
                Raw video binaries stream directly from your browser to MinIO/Cloudflare R2. Backend Spring Boot servers only coordinate signed upload tokens and dispatch Kafka events.
              </div>

              {/* Content Association */}
              <div className="space-y-4 mb-6">
                <div>
                  <label className="block text-xs font-semibold text-text-secondary mb-1.5 uppercase tracking-wider">
                    Target Content
                  </label>
                  <select
                    value={selectedContentId}
                    onChange={(e) => setSelectedContentId(e.target.value)}
                    disabled={isUploading}
                    className="w-full bg-[#12121A] border border-white/10 rounded-xl px-3.5 py-2.5 text-sm text-white focus:outline-none focus:border-purple-500 transition-colors"
                  >
                    {titles.length === 0 && <option value="">{titlesError ? 'Could not load titles' : 'No titles yet'}</option>}
                    {titles.map((title) => (
                      <option key={title.id} value={title.id}>
                        {title.title} ({title.type})
                      </option>
                    ))}
                  </select>
                  {titlesError && <ErrorState compact message={titlesError} onRetry={loadTitles} />}
                </div>

                <div>
                  <label className="block text-xs font-semibold text-text-secondary mb-1.5 uppercase tracking-wider">
                    Target Type
                  </label>
                  <div className="grid grid-cols-2 gap-3">
                    <button
                      type="button"
                      onClick={() => setTargetType('MOVIE')}
                      className={`py-2 px-3 rounded-xl text-xs font-semibold border transition-all ${
                        targetType === 'MOVIE'
                          ? 'bg-purple-600/20 border-purple-500 text-white'
                          : 'bg-[#12121A] border-white/10 text-text-secondary hover:text-white'
                      }`}
                    >
                      Movie (Feature Film)
                    </button>
                    <button
                      type="button"
                      onClick={() => setTargetType('EPISODE')}
                      className={`py-2 px-3 rounded-xl text-xs font-semibold border transition-all ${
                        targetType === 'EPISODE'
                          ? 'bg-purple-600/20 border-purple-500 text-white'
                          : 'bg-[#12121A] border-white/10 text-text-secondary hover:text-white'
                      }`}
                    >
                      Series Episode
                    </button>
                  </div>
                </div>
              </div>

              {/* Drag and Drop Zone */}
              <div
                onDragOver={(e) => e.preventDefault()}
                onDrop={handleFileDrop}
                onClick={() => fileInputRef.current?.click()}
                className={`border-2 border-dashed rounded-2xl p-6 text-center cursor-pointer transition-all ${
                  selectedFile
                    ? 'border-purple-500/50 bg-purple-500/5'
                    : 'border-white/15 hover:border-purple-400/40 bg-[#12121A]/50 hover:bg-[#12121A]'
                }`}
              >
                <input
                  ref={fileInputRef}
                  type="file"
                  accept="video/mp4,video/quicktime,video/x-matroska,video/webm"
                  onChange={handleFileSelect}
                  className="hidden"
                />

                {selectedFile ? (
                  <div className="space-y-2">
                    <div className="w-12 h-12 rounded-xl bg-purple-500/20 text-purple-400 mx-auto flex items-center justify-center">
                      <FileVideo className="w-6 h-6" />
                    </div>
                    <p className="text-sm font-semibold text-white truncate max-w-xs mx-auto">
                      {selectedFile.name}
                    </p>
                    <p className="text-xs text-text-muted">
                      {(selectedFile.size / (1024 * 1024)).toFixed(2)} MB • {selectedFile.type || 'video/mp4'}
                    </p>
                    <span className="text-[11px] text-purple-400 font-medium inline-block hover:underline">
                      Click to choose another file
                    </span>
                  </div>
                ) : (
                  <div className="space-y-2 py-4">
                    <div className="w-12 h-12 rounded-xl bg-white/5 text-text-muted mx-auto flex items-center justify-center">
                      <UploadCloud className="w-6 h-6" />
                    </div>
                    <p className="text-sm font-medium text-white">
                      Drop video file here or <span className="text-purple-400 underline">browse</span>
                    </p>
                    <p className="text-xs text-text-muted">
                      Supports MP4, MOV, MKV, WebM up to 5GB
                    </p>
                  </div>
                )}
              </div>

              {/* Progress & Status Message */}
              {isUploading && (
                <div className="mt-6 space-y-3">
                  <div className="flex justify-between text-xs font-semibold">
                    <span className="text-purple-300 flex items-center space-x-1.5">
                      <span className="inline-block w-2 h-2 rounded-full bg-purple-400 animate-pulse" />
                      <span>{uploadMessage}</span>
                    </span>
                    <span className="text-white">{uploadProgress}%</span>
                  </div>
                  <div className="w-full bg-white/10 rounded-full h-2 overflow-hidden">
                    <div
                      className="gradient-accent h-2 transition-all duration-300 rounded-full"
                      style={{ width: `${uploadProgress}%` }}
                    />
                  </div>
                </div>
              )}

              {uploadStep === 'DONE' && (
                <div className="mt-6 p-4 rounded-xl bg-emerald-950/30 border border-emerald-500/30 flex items-start space-x-3 text-xs text-emerald-200">
                  <CheckCircle2 className="w-5 h-5 text-emerald-400 shrink-0 mt-0.5" />
                  <div>
                    <span className="font-bold block text-emerald-300">Upload Complete & Verified</span>
                    {uploadMessage}
                  </div>
                </div>
              )}

              {uploadError && (
                <div className="mt-6 p-4 rounded-xl bg-rose-950/30 border border-rose-500/30 flex items-start space-x-3 text-xs text-rose-200">
                  <AlertTriangle className="w-5 h-5 text-rose-400 shrink-0 mt-0.5" />
                  <div>
                    <span className="font-bold block text-rose-300">Upload Failed</span>
                    {uploadError}
                  </div>
                </div>
              )}

              {/* Action Button */}
              <button
                type="button"
                onClick={handleStartUpload}
                disabled={!selectedFile || isUploading}
                className={`w-full mt-6 py-3 px-4 rounded-xl text-sm font-semibold flex items-center justify-center space-x-2 transition-all ${
                  !selectedFile || isUploading
                    ? 'bg-white/5 text-text-muted cursor-not-allowed border border-white/5'
                    : 'gradient-accent text-white hover:opacity-95 shadow-lg shadow-purple-600/30 hover:scale-[1.01]'
                }`}
              >
                <span>{isUploading ? 'Transferring Directly to R2/MinIO...' : 'Initiate Direct Upload'}</span>
                {!isUploading && <ArrowRight className="w-4 h-4 ml-1" />}
              </button>
            </div>
          </div>

          {/* Right Column: Encoding Pipeline Jobs Monitor */}
          <div className="lg:col-span-7 space-y-6">
            {/* Quick Stat Rails */}
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
              <div className="p-4 rounded-xl bg-card border border-white/10">
                <span className="text-[11px] uppercase tracking-wider text-text-muted block font-semibold mb-1">
                  Total Jobs
                </span>
                <span className="text-2xl font-black text-white">{totalJobs}</span>
              </div>
              <div className="p-4 rounded-xl bg-card border border-white/10">
                <span className="text-[11px] uppercase tracking-wider text-text-muted block font-semibold mb-1">
                  Active Transcodes
                </span>
                <span className="text-2xl font-black text-cyan-400 flex items-center space-x-1.5">
                  <span>{processingCount}</span>
                  {processingCount > 0 && <span className="w-2 h-2 rounded-full bg-cyan-400 animate-pulse" />}
                </span>
              </div>
              <div className="p-4 rounded-xl bg-card border border-white/10">
                <span className="text-[11px] uppercase tracking-wider text-text-muted block font-semibold mb-1">
                  Ready (HLS)
                </span>
                <span className="text-2xl font-black text-emerald-400">{readyCount}</span>
              </div>
              <div className="p-4 rounded-xl bg-card border border-white/10">
                <span className="text-[11px] uppercase tracking-wider text-text-muted block font-semibold mb-1">
                  Failed
                </span>
                <span className="text-2xl font-black text-rose-400">{failedCount}</span>
              </div>
            </div>

            {/* Filter Tabs */}
            <div className="flex items-center space-x-2 border-b border-white/10 pb-3 overflow-x-auto">
              {(['ALL', 'PROCESSING', 'READY', 'FAILED'] as const).map((st) => (
                <button
                  key={st}
                  onClick={() => setStatusFilter(st)}
                  className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-colors ${
                    statusFilter === st
                      ? 'bg-purple-600 text-white'
                      : 'bg-white/5 text-text-secondary hover:text-white'
                  }`}
                >
                  {st}
                </button>
              ))}
            </div>

            {/* Jobs List */}
            <div className="space-y-4">
              {jobsError && <ErrorState compact message={jobsError} onRetry={fetchJobs} />}
              {!jobsError && jobs.length === 0 ? (
                <div className="p-12 text-center rounded-2xl bg-card border border-white/10 text-text-muted">
                  <Layers className="w-10 h-10 mx-auto mb-3 opacity-30 text-purple-400" />
                  <p className="text-sm font-medium text-white mb-1">No encoding jobs found</p>
                  <p className="text-xs">Upload a media file above to trigger the FFmpeg pipeline</p>
                </div>
              ) : (
                jobs.map((job) => {
                  const titleMeta = titles.find((t) => t.id === job.contentId);
                  return (
                    <div
                      key={job.id}
                      className="p-5 rounded-2xl bg-card border border-white/10 hover:border-white/20 transition-all space-y-4"
                    >
                      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                        <div>
                          <div className="flex items-center space-x-2">
                            <h3 className="text-base font-bold text-white">
                              {titleMeta ? titleMeta.title : `Content: ${job.contentId.slice(0, 8)}...`}
                            </h3>
                            <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-white/5 text-text-muted border border-white/5">
                              {job.id.slice(0, 8)}
                            </span>
                          </div>
                          <p className="text-xs text-text-muted font-mono mt-0.5 truncate max-w-md">
                            {job.rawS3Key}
                          </p>
                        </div>

                        {/* Status Badge */}
                        <div className="flex items-center space-x-2">
                          {job.status === 'READY' && (
                            <span className="flex items-center space-x-1 px-2.5 py-1 rounded-full bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 text-xs font-semibold">
                              <CheckCircle2 className="w-3.5 h-3.5" />
                              <span>READY</span>
                            </span>
                          )}
                          {job.status === 'PROCESSING' && (
                            <span className="flex items-center space-x-1 px-2.5 py-1 rounded-full bg-cyan-500/10 text-cyan-400 border border-cyan-500/20 text-xs font-semibold animate-pulse">
                              <RefreshCw className="w-3.5 h-3.5 animate-spin" />
                              <span>PROCESSING</span>
                            </span>
                          )}
                          {job.status === 'QUEUED' && (
                            <span className="flex items-center space-x-1 px-2.5 py-1 rounded-full bg-amber-500/10 text-amber-400 border border-amber-500/20 text-xs font-semibold">
                              <Clock className="w-3.5 h-3.5" />
                              <span>QUEUED</span>
                            </span>
                          )}
                          {job.status === 'FAILED' && (
                            <span className="flex items-center space-x-1 px-2.5 py-1 rounded-full bg-rose-500/10 text-rose-400 border border-rose-500/20 text-xs font-semibold">
                              <AlertTriangle className="w-3.5 h-3.5" />
                              <span>FAILED</span>
                            </span>
                          )}
                        </div>
                      </div>

                      {/* Renditions & Master Playlist */}
                      {job.masterPlaylistKey && (
                        <div className="p-3 rounded-xl bg-[#12121A] border border-white/5 space-y-2">
                          <div className="flex items-center justify-between text-xs">
                            <span className="text-text-muted font-mono">
                              Master Manifest: <span className="text-purple-300">{job.masterPlaylistKey}</span>
                            </span>
                            {job.durationSeconds && (
                              <span className="text-text-muted text-[11px]">
                                Duration: {Math.floor(job.durationSeconds / 60)}m {job.durationSeconds % 60}s
                              </span>
                            )}
                          </div>

                          {job.renditions && job.renditions.length > 0 && (
                            <div className="flex flex-wrap gap-2 pt-1">
                              {job.renditions.map((r) => (
                                <span
                                  key={r.id || r.resolution}
                                  className="text-[11px] px-2.5 py-1 rounded-lg bg-white/5 border border-white/10 text-white font-medium flex items-center space-x-1.5"
                                >
                                  <span className="w-1.5 h-1.5 rounded-full bg-purple-400" />
                                  <span>{r.resolution}</span>
                                  <span className="text-text-muted text-[10px]">
                                    ({(r.bitrate / 1000000).toFixed(1)} Mbps)
                                  </span>
                                </span>
                              ))}
                            </div>
                          )}
                        </div>
                      )}

                      {/* Error details if failed */}
                      {job.status === 'FAILED' && job.errorMessage && (
                        <div className="p-3 rounded-xl bg-rose-950/20 border border-rose-500/20 text-xs text-rose-300">
                          <span className="font-semibold block mb-0.5">Transcode Error:</span>
                          {job.errorMessage}
                        </div>
                      )}

                      {/* Actions Footer */}
                      <div className="flex items-center justify-between pt-2 border-t border-white/5 text-xs">
                        <span className="text-text-muted text-[11px]">
                          Created: {new Date(job.createdAt).toLocaleTimeString()} • Retries: {job.retryCount}
                        </span>

                        <div className="flex items-center space-x-2">
                          {job.status === 'FAILED' && (
                            <button
                              onClick={() => handleRetryJob(job.id)}
                              disabled={retryingJobId === job.id}
                              className="flex items-center space-x-1.5 px-3 py-1.5 rounded-lg bg-white/5 hover:bg-white/10 text-white font-medium transition-colors border border-white/10"
                            >
                              <RotateCcw className={`w-3.5 h-3.5 ${retryingJobId === job.id ? 'animate-spin' : ''}`} />
                              <span>Retry</span>
                            </button>
                          )}

                          {job.status === 'READY' && (
                            <Link
                              href={`/watch/${job.contentId}`}
                              className="flex items-center space-x-1.5 px-3 py-1.5 rounded-lg bg-purple-600 hover:bg-purple-500 text-white font-semibold transition-colors shadow-md shadow-purple-600/30"
                            >
                              <Play className="w-3.5 h-3.5 fill-white" />
                              <span>Preview Stream</span>
                            </Link>
                          )}
                        </div>
                      </div>
                    </div>
                  );
                })
              )}
            </div>
          </div>
        </div>

        {/* Catalog management: create, publish, unpublish and delete titles */}
        <section className="mt-12 space-y-4">
          <h2 className="text-lg font-bold text-white">Catalog</h2>
          <TitleManager titles={titles} onChanged={loadTitles} />
        </section>

        {/* Platform analytics, each figure from the service that owns it */}
        <section className="mt-12 space-y-4">
          <h2 className="text-lg font-bold text-white">Analytics</h2>
          <AnalyticsPanel />
        </section>
      </main>

      <Footer />
    </div>
  );
}
