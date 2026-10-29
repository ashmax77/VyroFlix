import { test, expect, type Page } from '@playwright/test';

/** Viewport wider than its content would mean a horizontal scrollbar, which breaks the layout on phones. */
async function expectNoHorizontalOverflow(page: Page) {
  const { scroll, inner } = await page.evaluate(() => ({
    scroll: document.documentElement.scrollWidth,
    inner: window.innerWidth,
  }));
  expect(scroll, 'page is wider than the viewport').toBeLessThanOrEqual(inner);
}

test.describe('Layout and chrome', () => {
  test('the site has one navigation landmark and one header', async ({ page }) => {
    await page.goto('/');

    await expect(page.locator('nav')).toHaveCount(1);
    await expect(page.locator('header')).toHaveCount(1);
    await expect(page.getByRole('navigation', { name: 'Main' })).toBeVisible();
  });

  test('the landing page does not scroll sideways', async ({ page }) => {
    await page.goto('/');
    await expect(page.getByRole('heading', { level: 1 })).toBeVisible();

    await expectNoHorizontalOverflow(page);
  });

  test('sign-in and register render without the site navbar or footer', async ({ page }) => {
    for (const path of ['/login', '/register']) {
      await page.goto(path);
      await expect(page.getByRole('heading', { level: 1 })).toBeVisible();
      await expect(page.locator('nav')).toHaveCount(0);
      await expect(page.locator('footer')).toHaveCount(0);
      await expectNoHorizontalOverflow(page);
    }
  });

  test('an unknown address shows the not-found page', async ({ page }) => {
    const response = await page.goto('/no-such-page-for-tests');

    expect(response?.status()).toBe(404);
    await expect(page.getByRole('heading', { name: /schedule/i })).toBeVisible();
  });
});

test.describe('Phone navigation', () => {
  test.beforeEach(async ({ page, isMobile }) => {
    test.skip(!isMobile, 'Mobile navigation is only shown on phone viewports.');
    await page.goto('/');
  });

  test('the menu opens and lists every destination', async ({ page }) => {
    const toggle = page.getByRole('button', { name: 'Open menu' });
    await expect(toggle).toBeVisible();
    await toggle.click();

    const menu = page.locator('#mobile-menu');
    await expect(menu).toBeVisible();
    await expect(menu.getByRole('link', { name: 'Home' })).toBeVisible();
    await expect(menu.getByRole('link', { name: 'Movies' })).toBeVisible();
    await expect(menu.getByRole('link', { name: 'Series' })).toBeVisible();
    await expect(menu.getByRole('link', { name: 'My list' })).toBeVisible();
    await expect(menu.getByRole('link', { name: 'Sign in' })).toBeVisible();
  });

  test('the menu closes with the close control', async ({ page }) => {
    await page.getByRole('button', { name: 'Open menu' }).click();
    await page.getByRole('button', { name: 'Close menu' }).click();

    await expect(page.locator('#mobile-menu')).toHaveCount(0);
  });
});
