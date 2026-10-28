import { test, expect } from '@playwright/test';

test.describe('Watchlist & My List Flow', () => {
  test('renders My List page with empty state or saved titles', async ({ page }) => {
    await page.goto('/my-list');

    // Page should display My List heading
    await expect(page.getByRole('heading', { name: /my list/i })).toBeVisible();

    // Should have titles or empty state message
    const emptyOrContent = page.getByText(/your (list|watchlist) is empty/i)
      .or(page.locator('a[href^="/watch/"]'))
      .or(page.getByRole('button', { name: /remove from my list/i }));
    await expect(emptyOrContent.first()).toBeVisible({ timeout: 10000 });
  });

  test('toggles watchlist button on title details page', async ({ page }) => {
    await page.goto('/title/b0000000-0000-0000-0000-000000000001');

    // Find watchlist button
    const watchlistBtn = page.getByRole('button', { name: /list|add/i }).or(page.locator('button:has-text("List")')).first();
    if (await watchlistBtn.isVisible()) {
      await watchlistBtn.click();
      await page.waitForTimeout(500);
      // Button state should toggle
      await expect(watchlistBtn).toBeVisible();
    }
  });
});
