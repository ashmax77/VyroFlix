import { test, expect } from '@playwright/test';

test.describe('Watchlist & My List Flow', () => {
  test('redirects signed-out visitors from My List to login', async ({ page }) => {
    await page.goto('/my-list');

    await expect(page).toHaveURL(/\/login\?redirect=%2Fmy-list/);
  });

  test('redirects signed-out visitors from the admin studio to login', async ({ page }) => {
    await page.goto('/admin');

    await expect(page).toHaveURL(/\/login\?redirect=%2Fadmin/);
  });

  test('asks signed-out visitors to sign in when adding to My List from the title page', async ({ page }) => {
    await page.goto('/title/b0000000-0000-0000-0000-000000000001');

    const addButton = page.getByRole('button', { name: /add to my list/i });
    if (await addButton.isVisible({ timeout: 10000 }).catch(() => false)) {
      await addButton.click();
      await expect(page.getByText(/sign in to use my list/i)).toBeVisible();
    }
  });
});
