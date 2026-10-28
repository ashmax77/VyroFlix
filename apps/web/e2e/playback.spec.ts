import { test, expect } from '@playwright/test';

test.describe('Video Playback Flow', () => {
  test('redirects signed-out visitors from the watch page to login and remembers the destination', async ({ page }) => {
    await page.goto('/watch/b0000000-0000-0000-0000-000000000001');

    await expect(page).toHaveURL(/\/login\?redirect=%2Fwatch%2F/);
    await expect(page.getByRole('button', { name: /sign in|log in/i }).first()).toBeVisible();
  });
});
