import { expect, test } from '@playwright/test';

const API = process.env['E2E_API_URL'] ?? 'http://localhost:8080';

test.describe('RMPD smoke', () => {
  test.skip(!process.env['E2E_WITH_BACKEND'], 'Set E2E_WITH_BACKEND=1 when backend is running');

  test('login → create declaration → download XML', async ({ page }) => {
    await page.goto('/login');
    await page.getByLabel(/email/i).fill('admin@demo.local');
    await page.getByLabel(/password|пароль/i).fill('admin123');
    await page.getByRole('button', { name: /увійти|login|zaloguj/i }).click();

    await expect(page).toHaveURL(/\/($|\?)/);

    await page.goto('/declarations');
    await page.getByRole('button', { name: /нова|new|nowa/i }).click();
    await expect(page).toHaveURL(/\/declarations\/\d+/);

    const declarationId = page.url().match(/\/declarations\/(\d+)/)?.[1];
    expect(declarationId).toBeTruthy();

    const token = await page.evaluate(() => localStorage.getItem('rmpd_access_token'));
    expect(token).toBeTruthy();

    const xmlResponse = await page.request.get(`${API}/api/v1/declarations/${declarationId}/xml`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    expect(xmlResponse.ok()).toBeTruthy();
    const xml = await xmlResponse.text();
    expect(xml).toContain('RMPD100');
  });
});
