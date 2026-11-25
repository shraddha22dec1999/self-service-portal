import { useCallback, useEffect, useRef, useState, type ReactNode } from "react";
import { ApiError } from "../api/client";
import type { RequestStatus } from "../api/types";

/** Load data on mount (and when deps change). Gives back data, error, loading and a reload. */
export function useLoad<T>(fn: () => Promise<T>, deps: unknown[] = []) {
  const [data, setData] = useState<T | null>(null);
  const [error, setError] = useState<unknown>(null);
  const [loading, setLoading] = useState(true);
  const seq = useRef(0);

  const reload = useCallback(async () => {
    const mine = ++seq.current;
    setLoading(true);
    setError(null);
    try {
      const d = await fn();
      if (mine === seq.current) setData(d);
    } catch (e) {
      if (mine === seq.current) setError(e);
    } finally {
      if (mine === seq.current) setLoading(false);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps);

  useEffect(() => {
    reload();
  }, [reload]);

  return { data, error, loading, reload, setData };
}

export function errorText(e: unknown): string {
  if (e instanceof ApiError) return e.message;
  if (e instanceof Error) return e.message;
  return "Something went wrong.";
}

export function ErrorBox({ error, onRetry }: { error: unknown; onRetry?: () => void }) {
  if (!error) return null;
  const forbidden = error instanceof ApiError && error.status === 403;
  return (
    <div className={`alert ${forbidden ? "alert-warn" : "alert-error"}`} role="alert">
      <div>
        <strong>{forbidden ? "Not allowed" : "Something went wrong"}</strong>
        <p>{errorText(error)}</p>
      </div>
      {onRetry && (
        <button className="btn btn-ghost" onClick={onRetry}>
          Try again
        </button>
      )}
    </div>
  );
}

export function Loading({ label = "Loading…" }: { label?: string }) {
  return (
    <div className="loading" aria-live="polite">
      <span className="spinner" /> {label}
    </div>
  );
}

export function Empty({ title, children }: { title: string; children?: ReactNode }) {
  return (
    <div className="empty">
      <strong>{title}</strong>
      {children && <p>{children}</p>}
    </div>
  );
}

const statusLabel: Record<RequestStatus, string> = {
  PENDING_APPROVAL: "Pending approval",
  PROVISIONING: "Provisioning",
  ACTIVE: "Active",
  REJECTED: "Rejected",
  CANCELLED: "Cancelled",
  FAILED: "Failed",
};

const statusTone: Record<string, string> = {
  PENDING_APPROVAL: "warn",
  PROVISIONING: "info",
  ACTIVE: "ok",
  SUCCEEDED: "ok",
  APPROVED: "ok",
  REJECTED: "bad",
  FAILED: "bad",
  CANCELLED: "muted",
  REVOKED: "muted",
};

export function StatusBadge({ status }: { status: string }) {
  const label = statusLabel[status as RequestStatus] ?? status.charAt(0) + status.slice(1).toLowerCase();
  return <span className={`badge badge-${statusTone[status] ?? "muted"}`}>{label}</span>;
}

export function RiskBadge({ risk }: { risk: string }) {
  const tone = risk === "High" ? "bad" : risk === "Medium" ? "warn" : "ok";
  return <span className={`badge badge-${tone}`}>{risk} risk</span>;
}

export function fmtDate(iso: string | null | undefined): string {
  if (!iso) return "—";
  return new Date(iso).toLocaleString(undefined, { dateStyle: "medium", timeStyle: "short" });
}

export function duration(r: { permanent: boolean; months: number; days: number }): string {
  if (r.permanent) return "Permanent";
  const parts = [];
  if (r.months) parts.push(`${r.months} month${r.months === 1 ? "" : "s"}`);
  if (r.days) parts.push(`${r.days} day${r.days === 1 ? "" : "s"}`);
  return parts.join(" ") || "—";
}

export function Modal({
  title,
  onClose,
  children,
}: {
  title: string;
  onClose: () => void;
  children: ReactNode;
}) {
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => e.key === "Escape" && onClose();
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [onClose]);
  return (
    <div className="modal-backdrop" onMouseDown={(e) => e.target === e.currentTarget && onClose()}>
      <div className="modal" role="dialog" aria-modal="true" aria-label={title}>
        <header className="modal-head">
          <h2>{title}</h2>
          <button className="btn btn-icon" onClick={onClose} aria-label="Close">
            ×
          </button>
        </header>
        {children}
      </div>
    </div>
  );
}

/** A short-lived message in the corner after an action succeeds. */
export function useToast() {
  const [msg, setMsg] = useState<string | null>(null);
  useEffect(() => {
    if (!msg) return;
    const t = setTimeout(() => setMsg(null), 3500);
    return () => clearTimeout(t);
  }, [msg]);
  const node = msg ? (
    <div className="toast" role="status">
      {msg}
    </div>
  ) : null;
  return { show: setMsg, node };
}
