'use client';

import { useEffect, useState } from 'react';

/** True once `active` has stayed true for `delayMs` - used to explain slow loads (e.g. a waking backend). */
export function useSlowHint(active: boolean, delayMs = 6000): boolean {
  const [slow, setSlow] = useState(false);
  useEffect(() => {
    if (!active) {
      setSlow(false);
      return;
    }
    const timer = setTimeout(() => setSlow(true), delayMs);
    return () => clearTimeout(timer);
  }, [active, delayMs]);
  return slow;
}

export const SLOW_LOAD_MESSAGE =
  'Still working… the server may be waking up after a period of inactivity. This can take up to a minute.';
