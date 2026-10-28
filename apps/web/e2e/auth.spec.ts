import { test, expect } from '@playwright/test';

test.describe('Authentication Flow', () => {
  test('renders login page with email, password fields and Google button', async ({ page }) => {
    await page.goto('/login');

    await expect(page.locator('input[type="email"]')).toBeVisible();
    await expect(page.locator('input[type="password"]')).toBeVisible();
    await expect(page.getByRole('button', { name: /sign in/i })).toBeVisible();
    await expect(page.getByRole('button', { name: /google/i })).toBeVisible();
    await expect(page.getByRole('link', { name: /sign up/i })).toBeVisible();
  });

  test('validates invalid email and short password on login form', async ({ page }) => {
    await page.goto('/login');

    const emailInput = page.locator('input[type="email"]');
    const passwordInput = page.locator('input[type="password"]');
    const submitBtn = page.getByRole('button', { name: /sign in/i });

    // Try submitting with invalid inputs
    await emailInput.fill('invalid-email');
    await passwordInput.fill('123');
    await submitBtn.click();

    // Verify validation error messages render
    await expect(page.getByText(/valid email address/i)).toBeVisible();
    await expect(page.getByText(/at least 6 characters/i)).toBeVisible();
  });

  test('renders registration page with name, email and password', async ({ page }) => {
    await page.goto('/register');

    await expect(page.locator('input[name="name"]').or(page.locator('input[placeholder*="name" i]'))).toBeVisible();
    await expect(page.locator('input[type="email"]')).toBeVisible();
    await expect(page.locator('input[type="password"]')).toBeVisible();
    await expect(page.getByRole('button', { name: /sign up|create account/i })).toBeVisible();
  });
});
