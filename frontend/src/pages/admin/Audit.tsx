import { useState } from "react";
import { Link } from "react-router-dom";
import { api } from "../../api/endpoints";
import type { VerifyResult } from "../../api/types";
import { Empty, ErrorBox, Loading, errorText, fmtDate, useLoad } from "../../components/ui";

type Query = { actor: string; requestId: string; from: string; to: string };
const none: Query = { actor: "", requestId: "", from: "", to: "" };

/** datetime-local gives "2026-11-25T10:00"; the API wants an ISO instant. */
const iso = (v: string) => (v ? new Date(v).toISOString() : "");

export function AuditPage() {
  const [draft, setDraft] = useState<Query>(none);
  const [query, setQuery] = useState<Query>(none);
  const { data, error, loading, reload } = useLoad(
    () => api.audit.search({ actor: query.actor, requestId: query.requestId, from: iso(query.from), to: iso(query.to) }),
    [query],
  );
  const [verify, setVerify] = useState<VerifyResult | null>(null);
  const [verifying, setVerifying] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);

  const runVerify = async () => {
    setVerifying(true);
    setActionError(null);
    try {
      setVerify(await api.audit.verify());
    } catch (e) {
      setActionError(errorText(e));
    } finally {
      setVerifying(false);
    }
  };

  const exportCsv = async () => {
    setActionError(null);
    try {
      await api.audit.exportCsv();
    } catch (e) {
      setActionError(errorText(e));
    }
  };

  return (
    <div className="page">
      <div className="page-head">
        <div>
          <h1>Audit log</h1>
          <p className="muted">Every action, in a hash-chained log that cannot be changed without detection.</p>
        </div>
        <div className="actions">
          <button className="btn" onClick={runVerify} disabled={verifying}>
            {verifying ? "Checking…" : "Verify integrity"}
          </button>
          <button className="btn" onClick={exportCsv}>
            Export CSV
          </button>
        </div>
      </div>

      {verify && (
        <div className={`alert ${verify.intact ? "alert-ok" : "alert-error"}`}>
          {verify.intact ? (
            <strong>The chain is intact. No entry has been changed or removed.</strong>
          ) : (
            <strong>The chain is broken at entry #{verify.firstBrokenSeq}. Investigate before trusting later entries.</strong>
          )}
        </div>
      )}
      {actionError && <div className="alert alert-error">{actionError}</div>}

      <form
        className="filters"
        onSubmit={(e) => {
          e.preventDefault();
          setQuery(draft);
        }}
      >
        <input className="input" placeholder="Actor" value={draft.actor} onChange={(e) => setDraft({ ...draft, actor: e.target.value })} />
        <input className="input" placeholder="Request id (REQ-1001)" value={draft.requestId} onChange={(e) => setDraft({ ...draft, requestId: e.target.value })} />
        <label className="field compact">
          <span>From</span>
          <input className="input" type="datetime-local" value={draft.from} onChange={(e) => setDraft({ ...draft, from: e.target.value })} />
        </label>
        <label className="field compact">
          <span>To</span>
          <input className="input" type="datetime-local" value={draft.to} onChange={(e) => setDraft({ ...draft, to: e.target.value })} />
        </label>
        <button className="btn btn-primary">Search</button>
        <button
          type="button"
          className="btn btn-ghost"
          onClick={() => {
            setDraft(none);
            setQuery(none);
          }}
        >
          Clear
        </button>
      </form>

      <ErrorBox error={error} onRetry={reload} />
      {loading && <Loading />}
      {!loading && data && data.length === 0 && <Empty title="No entries match" />}
      {!loading && data && data.length > 0 && (
        <div className="table-wrap">
          <table className="table">
            <thead>
              <tr>
                <th>#</th>
                <th>Time</th>
                <th>Actor</th>
                <th>Action</th>
                <th>Request</th>
                <th>Message</th>
                <th>Hash</th>
              </tr>
            </thead>
            <tbody>
              {data.map((e) => (
                <tr key={e.seq}>
                  <td>{e.seq}</td>
                  <td className="nowrap">{fmtDate(e.time)}</td>
                  <td>{e.actor}</td>
                  <td>
                    <code>{e.action}</code>
                  </td>
                  <td>{e.requestId ? <Link to={`/requests/${e.requestId}`}>{e.requestId}</Link> : "—"}</td>
                  <td>{e.message}</td>
                  <td>
                    <code className="muted" title={e.hash}>
                      {e.hash?.slice(0, 10)}…
                    </code>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
