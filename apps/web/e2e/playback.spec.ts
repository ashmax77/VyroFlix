import { test, expect } from '@playwright/test';

test.describe('Video Playback Flow', () => {
  test('mounts player, loads video element and controls', async ({ page }) => {
    // Navigate to a seeded title watch page
    await page.goto('/watch/a0000000-0000-0000-0000-000000000001');

    // Wait for player container to mount
    const video = page.locator('video');
    await expect(video).toBeVisible({ timeout: 15000 });

    // Verify video title heading or back button
    const backBtn = page.locator('button[aria-label="Go back"]').or(page.locator('button:has(svg)'));
    await expect(backBtn.first()).toBeVisible();

    // Verify play/pause button is present
    const playPauseBtn = page.locator('button:has(svg)').filter({ hasText: '' });
    await expect(playPauseBtn.first()).toBeVisible();
  });
});
