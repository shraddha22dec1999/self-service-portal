import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from "react";
import { UserManager, WebStorageStateStore, type User } from "oidc-client-ts";
import { setIdentity, setUnauthorizedHandler } from "../api/client";
import { api } from "../api/endpoints";

/**
 * Two modes:
 * - SSO: VITE_OIDC_AUTHORITY is set. The person signs in with the identity provider and every call
 *   carries the access token. The gateway must run with AUTH_ENABLED=true.
 * - Local: no SSO. The person picks a sample user and the UI sends X-User. Development only.
 */

const authority = import.meta.env.VITE_OIDC_AUTHORITY as string | undefined;
export const ssoEnabled = !!authority;

const adminUsers = ((import.meta.env.VITE_ADMIN_USERS as string | undefined) ?? "super_admin_1,super_admin_2")
  .split(",")
  .map((s) => s.trim())
  .filter(Boolean);

const LOCAL_KEY = "portal.localUser";

const manager = ssoEnabled
  ? new UserManager({
      authority: authority!,
      client_id: import.meta.env.VITE_OIDC_CLIENT_ID as string,
      redirect_uri: `${window.location.origin}/auth/callback`,
      post_logout_redirect_uri: window.location.origin,
      scope: (import.meta.env.VITE_OIDC_SCOPE as string | undefined) ?? "openid profile email",
      response_type: "code",
      automaticSilentRenew: true,
      userStore: new WebStorageStateStore({ store: window.sessionStorage }),
    })
  : null;

interface AuthState {
  /** The id the backend sees, from GET /me. */
  user: string | null;
  isAdmin: boolean;
  loading: boolean;
  error: string | null;
  signInLocal: (user: string) => Promise<void>;
  signInSso: () => Promise<void>;
  completeSsoSignIn: () => Promise<void>;
  signOut: () => Promise<void>;
}

const Ctx = createContext<AuthState | null>(null);

function readLocal(): string | null {
  try {
    return window.localStorage.getItem(LOCAL_KEY);
  } catch {
    return null;
  }
}

function writeLocal(v: string | null) {
  try {
    if (v) window.localStorage.setItem(LOCAL_KEY, v);
    else window.localStorage.removeItem(LOCAL_KEY);
  } catch {
    // Storage blocked. The choice lasts until reload.
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  /** Ask the gateway who we are, so the UI shows exactly what the services will see. */
  const resolveMe = useCallback(async () => {
    const me = await api.me();
    if (!me.id) throw new Error("The gateway did not recognise you.");
    setUser(me.id);
  }, []);

  const applyOidcUser = useCallback(
    async (u: User | null) => {
      if (!u || u.expired) {
        setIdentity({});
        setUser(null);
        return;
      }
      setIdentity({ token: u.access_token });
      await resolveMe();
    },
    [resolveMe],
  );

  const signOut = useCallback(async () => {
    setIdentity({});
    setUser(null);
    if (manager) {
      await manager.removeUser();
      await manager.signoutRedirect().catch(() => undefined);
    } else {
      writeLocal(null);
    }
  }, []);

  // Start-up: restore the previous session.
  useEffect(() => {
    setUnauthorizedHandler(() => {
      setIdentity({});
      setUser(null);
    });
    (async () => {
      try {
        if (manager) {
          if (window.location.pathname === "/auth/callback") return; // handled by the callback page
          await applyOidcUser(await manager.getUser());
          manager.events.addUserLoaded((u) => setIdentity({ token: u.access_token }));
          manager.events.addAccessTokenExpired(() => setUser(null));
        } else {
          const saved = readLocal();
          if (saved) {
            setIdentity({ user: saved });
            await resolveMe();
          }
        }
      } catch (e) {
        setError((e as Error).message);
      } finally {
        setLoading(false);
      }
    })();
  }, [applyOidcUser, resolveMe]);

  const value = useMemo<AuthState>(
    () => ({
      user,
      isAdmin: !!user && adminUsers.includes(user),
      loading,
      error,
      signInLocal: async (u) => {
        setError(null);
        setIdentity({ user: u });
        try {
          await resolveMe();
          writeLocal(u);
        } catch (e) {
          setIdentity({});
          setError((e as Error).message);
          throw e;
        }
      },
      signInSso: async () => {
        await manager!.signinRedirect();
      },
      completeSsoSignIn: async () => {
        try {
          await applyOidcUser(await manager!.signinRedirectCallback());
        } catch (e) {
          setError((e as Error).message);
        } finally {
          setLoading(false);
        }
      },
      signOut,
    }),
    [user, loading, error, resolveMe, applyOidcUser, signOut],
  );

  return <Ctx.Provider value={value}>{children}</Ctx.Provider>;
}

export function useAuth(): AuthState {
  const v = useContext(Ctx);
  if (!v) throw new Error("useAuth outside AuthProvider");
  return v;
}

/** Sample people from opa/data.json, for local mode. */
export const sampleUsers: { id: string; label: string }[] = [
  { id: "app_engg_member_1", label: "App Engg member" },
  { id: "app_engg_member_2", label: "App Engg member (backup)" },
  { id: "sol_engg_lead", label: "Sol Engg lead · L1 for App Engg" },
  { id: "manpreet", label: "Manpreet · L2 for App Engg, L1 for Sol Engg" },
  { id: "amit", label: "Amit · L2 for Sol Engg" },
  { id: "super_admin_1", label: "Super admin" },
];
