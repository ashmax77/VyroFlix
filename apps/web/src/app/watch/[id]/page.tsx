'use client';

import React, { useEffect, useState } from 'react';
import { useParams } from 'next/navigation';
import VideoPlayer from '../../../components/VideoPlayer';
import { fetchTitle } from '../../../lib/api';
import { Title } from '../../../types';
import { Loader2 } from 'lucide-react';

export default function WatchPage() {
  const params = useParams();
  const titleId = params.id as string;
  const [title, setTitle] = useState<Title | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (titleId) {
      fetchTitle(titleId).then((data) => {
        setTitle(data);
        setLoading(false);
      });
    }
  }, [titleId]);

  if (loading) {
    return (
      <div className="w-full h-screen bg-black flex items-center justify-center">
        <Loader2 className="w-10 h-10 text-primary animate-spin" />
      </div>
    );
  }

  if (!title) {
    return (
      <div className="w-full h-screen bg-black flex items-center justify-center text-text-secondary">
        Title not found.
      </div>
    );
  }

  return <VideoPlayer title={title} />;
}
