import { useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { sampleUsers, ssoEnabled, useAuth } from "../auth/AuthContext";
import { Loading } from "../components/ui";

export function LoginPage() {
  const { signInLocal, signInSso, error } = useAuth();
  const [custom, setCustom] = useState("");
  const [busy, setBusy] = useState(false);

  const pick = async (id: string) => {
    if (!id.trim()) return;
    setBusy(true);
    try {
      await signInLocal(id.trim());
    } catch {
      // shown through `error`
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="login">
      <div className="login-card">
        <div className="brand brand-lg">
          <span className="brand-mark">SP</span> Self Service Portal
        </div>
        <p className="muted">Request access to tools and resources, and approve requests from your team.</p>

        {error && <div className="alert alert-error">{error}</div>}

        {ssoEnabled ? (
          <button className="btn btn-primary btn-block" onClick={signInSso}>
            Sign in with SSO
          </button>
        ) : (
          <>
            <div className="alert alert-warn">
              <div>
                <strong>Local mode</strong>
                <p>SSO is off. Pick who you want to be. Use this only on your own machine.</p>
              </div>
            </div>
            <div className="user-list">
              {sampleUsers.map((u) => (
                <button key={u.id} className="user-pick" disabled={busy} onClick={() => pick(u.id)}>
                  <strong>{u.id}</strong>
                  <span className="muted">{u.label}</span>
                </button>
              ))}
            </div>
            <form
              className="row"
              onSubmit={(e) => {
                e.preventDefault();
                pick(custom);
              }}
            >
              <input
                className="input"
                placeholder="Or type a user id"
                value={custom}
                onChange={(e) => setCustom(e.target.value)}
              />
              <button className="btn" disabled={busy || !custom.trim()}>
                Continue
              </button>
            </form>
          </>
        )}
      </div>
    </div>
  );
}

export function SsoCallbackPage() {
  const { completeSsoSignIn } = useAuth();
  const navigate = useNavigate();
  const done = useRef(false);
  useEffect(() => {
    if (done.current) return; // StrictMode runs effects twice; the code can be used only once.
    done.current = true;
    completeSsoSignIn().then(() => navigate("/", { replace: true }));
  }, [completeSsoSignIn, navigate]);
  return <Loading label="Finishing sign-in…" />;
}
