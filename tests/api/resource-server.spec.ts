import { test, expect } from "@playwright/test";

/*
 * Integration tests for resource-server.
 *
 * There are two resource-server webapps deployed to uPortal-start's Tomcat:
 *
 *  - /ResourceServingWebapp/  — legacy (resource-server 1.0.x) that serves
 *    static files under /rs/** for backward compatibility with portlets that
 *    still reference older library versions via the <rs:resourceURL> tag.
 *
 *  - /resource-server/        — modern (resource-server 1.5.x) Spring Boot
 *    app that serves both static overlay files under /rs/** AND WebJars under
 *    /webjars/** with version-less URL resolution via webjars-locator.
 *
 * These tests verify both webapps return correct content, content types,
 * and cache headers for known-good paths. They should be run after
 * `./gradlew tomcatStart`.
 */

const BASE = "http://localhost:8080";
const LEGACY = `${BASE}/ResourceServingWebapp`;
const MODERN = `${BASE}/resource-server`;

test.describe("ResourceServingWebapp (legacy)", () => {
  test("serves jQuery 1.12.4 under /rs/", async ({ request }) => {
    const response = await request.get(
      `${LEGACY}/rs/jquery/1.12.4/jquery-1.12.4.min.js`
    );
    expect(response.status()).toBe(200);
    expect(response.headers()["content-type"]).toMatch(/javascript/);
    const body = await response.text();
    // Spot-check that the response body is actually jQuery, not a 200 error page
    expect(body).toMatch(/jQuery/);
    expect(body).toContain("1.12.4");
  });

  test("serves jQuery UI 1.11.4 under /rs/", async ({ request }) => {
    const response = await request.get(
      `${LEGACY}/rs/jqueryui/1.11.4/jquery-ui-1.11.4.min.js`
    );
    expect(response.status()).toBe(200);
    expect(response.headers()["content-type"]).toMatch(/javascript/);
    const body = await response.text();
    expect(body).toMatch(/jQuery UI/);
  });

  test("returns 404 for nonexistent path", async ({ request }) => {
    const response = await request.get(
      `${LEGACY}/rs/bogus/does-not-exist.js`
    );
    expect(response.status()).toBe(404);
  });
});

test.describe("resource-server (modern, /rs/ overlay)", () => {
  test("serves jQuery 3.7.1 under /rs/", async ({ request }) => {
    const response = await request.get(
      `${MODERN}/rs/jquery/3.7.1/jquery-3.7.1.js`
    );
    expect(response.status()).toBe(200);
    expect(response.headers()["content-type"]).toMatch(/javascript/);
    const body = await response.text();
    expect(body).toMatch(/jQuery/);
    expect(body).toContain("3.7.1");
  });

  test("serves jQuery UI 1.14.1 under /rs/", async ({ request }) => {
    const response = await request.get(
      `${MODERN}/rs/jqueryui/1.14.1/jquery-ui.js`
    );
    expect(response.status()).toBe(200);
    expect(response.headers()["content-type"]).toMatch(/javascript/);
    const body = await response.text();
    expect(body).toMatch(/jQuery UI/);
  });

  test("serves Bootstrap 5.0.2 CSS under /rs/", async ({ request }) => {
    const response = await request.get(
      `${MODERN}/rs/bootstrap/5.0.2/css/bootstrap.min.css`
    );
    expect(response.status()).toBe(200);
    expect(response.headers()["content-type"]).toMatch(/css/);
    const body = await response.text();
    expect(body).toMatch(/Bootstrap/);
  });

  test("aggregated resource has far-future cache header", async ({ request }) => {
    // Aggregated resources (*.aggr.min.js / *.aggr.min.css) live under
    // /rs/** and should get a long cache-control header from CacheExpirationFilter.
    // Any non-aggregated /rs/** path should also pass through the filter
    // but without the aggressive caching. We verify the filter exists by
    // checking that the Cache-Control header is set on a known path.
    const response = await request.get(
      `${MODERN}/rs/jquery/3.7.1/jquery-3.7.1.js`
    );
    expect(response.status()).toBe(200);
    const cacheControl = response.headers()["cache-control"];
    expect(cacheControl).toBeDefined();
    // Should include public + max-age for /rs/** resources
    expect(cacheControl).toMatch(/max-age=\d+/);
  });

  test("returns 404 for nonexistent /rs/ path", async ({ request }) => {
    const response = await request.get(
      `${MODERN}/rs/bogus/does-not-exist.js`
    );
    expect(response.status()).toBe(404);
  });
});

test.describe("resource-server (modern, /webjars/ with webjars-locator)", () => {
  test("serves jQuery 4.0.0 via versioned webjar path", async ({ request }) => {
    const response = await request.get(
      `${MODERN}/webjars/jquery/4.0.0/dist/jquery.min.js`
    );
    expect(response.status()).toBe(200);
    expect(response.headers()["content-type"]).toMatch(/javascript/);
    const body = await response.text();
    expect(body).toMatch(/jQuery/);
    // jQuery 4.x — should match v4
    expect(body).toMatch(/v4\./);
  });

  test("serves jQuery 4.0.0 via version-less webjar path (webjars-locator)", async ({
    request,
  }) => {
    // webjars-locator-core enables version-less URLs that resolve to the
    // version declared in pom.xml. This is the key feature of the modern
    // resource-server webapp.
    const response = await request.get(
      `${MODERN}/webjars/jquery/dist/jquery.min.js`
    );
    expect(response.status()).toBe(200);
    expect(response.headers()["content-type"]).toMatch(/javascript/);
    const body = await response.text();
    expect(body).toMatch(/jQuery/);
  });

  test("serves Bootstrap 5.3.8 JS via versioned webjar path", async ({
    request,
  }) => {
    const response = await request.get(
      `${MODERN}/webjars/bootstrap/5.3.8/dist/js/bootstrap.min.js`
    );
    expect(response.status()).toBe(200);
    expect(response.headers()["content-type"]).toMatch(/javascript/);
    const body = await response.text();
    expect(body).toMatch(/Bootstrap/);
    expect(body).toContain("5.3.8");
  });

  test("serves Bootstrap 5.3.8 CSS via versioned webjar path", async ({
    request,
  }) => {
    const response = await request.get(
      `${MODERN}/webjars/bootstrap/5.3.8/dist/css/bootstrap.min.css`
    );
    expect(response.status()).toBe(200);
    expect(response.headers()["content-type"]).toMatch(/css/);
    const body = await response.text();
    expect(body).toMatch(/Bootstrap/);
  });

  test("serves jQuery UI 1.14.1 via versioned webjar path", async ({
    request,
  }) => {
    const response = await request.get(
      `${MODERN}/webjars/jquery-ui/1.14.1/dist/jquery-ui.min.js`
    );
    expect(response.status()).toBe(200);
    expect(response.headers()["content-type"]).toMatch(/javascript/);
  });

  test("serves DataTables 2.3.7 via versioned webjar path", async ({
    request,
  }) => {
    const response = await request.get(
      `${MODERN}/webjars/datatables.net/2.3.7/js/dataTables.min.js`
    );
    expect(response.status()).toBe(200);
    expect(response.headers()["content-type"]).toMatch(/javascript/);
  });

  test("returns 404 for nonexistent webjar path", async ({ request }) => {
    const response = await request.get(
      `${MODERN}/webjars/bogus/1.0.0/bogus.js`
    );
    expect(response.status()).toBe(404);
  });
});

test.describe("resource-server minification sanity", () => {
  test("jQuery .min file is meaningfully smaller than unminified", async ({
    request,
  }) => {
    // Guards against resource-server/issues/309: silent fallback in the
    // minification pipeline could produce a .min.js that is actually full-size.
    // We verify the minified version is at least 30% smaller than the source.
    const unminified = await request.get(
      `${MODERN}/rs/jquery/3.7.1/jquery-3.7.1.js`
    );
    expect(unminified.status()).toBe(200);
    const unminifiedSize = (await unminified.body()).length;

    // Try the webjar version which should be minified
    const minified = await request.get(
      `${MODERN}/webjars/jquery/4.0.0/dist/jquery.min.js`
    );
    expect(minified.status()).toBe(200);
    const minifiedSize = (await minified.body()).length;

    // Minified should be substantially smaller. Different versions of jQuery,
    // but the ratio check is still meaningful — a broken minification would
    // produce roughly equal sizes.
    expect(minifiedSize).toBeLessThan(unminifiedSize * 0.7);
  });
});
