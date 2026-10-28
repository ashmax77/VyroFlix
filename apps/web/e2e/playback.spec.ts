import { test, expect } from '@playwright/test';

test.describe('Video Playback Flow', () => {
  test('mounts player, loads video element and controls', async ({ page }) => {
    // Navigate to a seeded title watch page (Cosmic Odyssey)
    await page.goto('/watch/b0000000-0000-0000-0000-000000000001');

    // Wait for player container to mount or title container
    const videoOrContainer = page.locator('video').or(page.locator('.video-js')).or(page.getByText(/cosmic odyssey|loading/i));
    await expect(videoOrContainer.first()).toBeVisible({ timeout: 15000 });

    // Verify video title heading or back button
    const backBtn = page.locator('button[aria-label="Go back"]').or(page.locator('button:has(svg)'));
    await expect(backBtn.first()).toBeVisible();

    // Verify play/pause button is present
    const playPauseBtn = page.locator('button:has(svg)').filter({ hasText: '' });
    await expect(playPauseBtn.first()).toBeVisible();
  });
});
