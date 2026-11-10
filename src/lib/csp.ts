/**
 * Builds the Content-Security-Policy for one request.
 *
 * Scripts only run if they carry this request's nonce ('strict-dynamic' lets those
 * trusted scripts load the chunks they need). Styles allow 'unsafe-inline' because
 * React style props and next/image write style attributes, which nonces can't cover.
 * CSS injection is a much smaller risk than script injection, so that's the trade.
 */
export function buildCsp(nonce: string, isDev: boolean): string {
  const directives: Record<string, string[]> = {
    "default-src": ["'self'"],
    "script-src": ["'self'", `'nonce-${nonce}'`, "'strict-dynamic'", ...(isDev ? ["'unsafe-eval'"] : [])],
    "style-src": ["'self'", "'unsafe-inline'"],
    "img-src": ["'self'", "data:", "blob:"],
    "font-src": ["'self'"],
    "connect-src": ["'self'", ...(isDev ? ["ws:"] : [])],
    "object-src": ["'none'"],
    "base-uri": ["'self'"],
    "form-action": ["'self'"],
    "frame-ancestors": ["'none'"],
  };

  const policy = Object.entries(directives)
    .map(([name, values]) => `${name} ${values.join(" ")}`)
    .join("; ");

  return isDev ? policy : `${policy}; upgrade-insecure-requests`;
}

export function createNonce(): string {
  return Buffer.from(crypto.randomUUID()).toString("base64");
}
