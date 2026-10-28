import { test, expect } from '@playwright/test';

test.describe('Browse & Discovery Flow', () => {
  test('loads home/browse feed with hero banner and content rails', async ({ page }) => {
    await page.goto('/browse');

    // Verify main page title and brand
    await expect(page).toHaveTitle(/VyroFlix/);

    // Verify Navbar renders
    const navbar = page.locator('nav');
    await expect(navbar).toBeVisible();
    await expect(page.getByText('VyroFlix')).toBeVisible();

    // Verify Hero title or content rails render
    const mainContent = page.locator('main');
    await expect(mainContent).toBeVisible();

    // Verify at least one content card or rail is present
    const cards = page.locator('img');
    await expect(cards.first()).toBeVisible({ timeout: 10000 });
  });

  test('navigates from catalog card to title detail page', async ({ page }) => {
    await page.goto('/browse');

    // Find first clickable title card
    const firstTitleLink = page.locator('a[href^="/title/"]').first();
    await expect(firstTitleLink).toBeVisible({ timeout: 10000 });

    const href = await firstTitleLink.getAttribute('href');
    await firstTitleLink.click();

    // URL should match title detail pattern
    await expect(page).toHaveURL(new RegExp(href || '/title/'));

    // Title detail elements should be present
    await expect(page.locator('h1')).toBeVisible();
    await expect(page.getByRole('link', { name: /play/i })).toBeVisible();
  });

  test('search input displays live suggestions', async ({ page }) => {
    await page.goto('/browse');

    // Click search icon / bar if present
    const searchInput = page.getByPlaceholder(/search movies/i).or(page.locator('input[type="search"], input[type="text"]')).first();
    if (await searchInput.isVisible()) {
      await searchInput.fill('Cosmic');
      // Should show suggestion or results
      await page.waitForTimeout(500);
      const suggestions = page.locator('[role="listbox"], [id*="suggestion"], div:has-text("Cosmic")');
      await expect(suggestions.first()).toBeVisible({ timeout: 5000 });
    }
  });
});
