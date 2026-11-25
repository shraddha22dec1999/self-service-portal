// One place that talks to the gateway. It adds who the caller is and turns the backend's
// { code, message } error shape into an ApiError.

export class ApiError extends Error {
  constructor(
    public status: number,
    public code: string,
    message: string,
  ) {
    super(message);
  }
}

type Identity = { token?: string; user?: string };

let identity: Identity = {};
let onUnauthorized: (() => void) | null = null;

/** Called by the auth layer whenever the signed-in person changes. */
export function setIdentity(next: Identity) {
  identity = next;
}

export function setUnauthorizedHandler(fn: () => void) {
  onUnauthorized = fn;
}

const BASE = (import.meta.env.VITE_API_BASE ?? "").replace(/\/$/, "") + "/api/v1";

function headers(extra?: HeadersInit): Headers {
  const h = new Headers(extra);
  if (identity.token) h.set("Authorization", `Bearer ${identity.token}`);
  // Local mode only. With SSO on, the gateway ignores this and takes the user from the token.
  else if (identity.user) h.set("X-User", identity.user);
  return h;
}

async function send(path: string, init: RequestInit = {}): Promise<Response> {
  let res: Response;
  try {
    res = await fetch(BASE + path, { ...init, headers: headers(init.headers) });
  } catch {
    throw new ApiError(0, "NETWORK", "Cannot reach the portal. Check your connection and try again.");
  }
  if (res.ok) return res;

  if (res.status === 401) onUnauthorized?.();
  let code = "HTTP_" + res.status;
  let message = res.statusText || "Request failed";
  try {
    const body = await res.json();
    if (body?.code) code = body.code;
    if (body?.message) message = body.message;
  } catch {
    // Not JSON. Keep the defaults.
  }
  if (res.status === 503 && code.startsWith("HTTP_")) {
    message = "The access policy service is unavailable. Try again shortly.";
  }
  throw new ApiError(res.status, code, message);
}

export async function get<T>(path: string): Promise<T> {
  return (await send(path)).json();
}

export async function post<T>(path: string, body?: unknown): Promise<T> {
  const res = await send(path, {
    method: "POST",
    headers: body === undefined ? undefined : { "Content-Type": "application/json" },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  const text = await res.text();
  return (text ? JSON.parse(text) : undefined) as T;
}

export async function put<T>(path: string, body: unknown): Promise<T> {
  const res = await send(path, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
  return res.json();
}

/** Fetch a file with the caller's credentials and hand it to the browser as a download. */
export async function download(path: string, filename: string) {
  const blob = await (await send(path)).blob();
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = filename;
  a.click();
  URL.revokeObjectURL(url);
}
