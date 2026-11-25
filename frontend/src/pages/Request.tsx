import { useEffect, useState } from "react";
import { Link, useLocation, useParams } from "react-router-dom";
import { api } from "../api/endpoints";
import type { AccessRequest, AuditEntry, Job } from "../api/types";
import { useAuth } from "../auth/AuthContext";
import {
  ErrorBox,
  Loading,
  Modal,
  StatusBadge,
  duration,
  errorText,
  fmtDate,
  useLoad,
  useToast,
} from "../components/ui";

export function RequestPage() {
  const { id = "" } = useParams();
  const { user, isAdmin } = useAuth();
  const location = useLocation();
  const toast = useToast();
  const req = useLoad(() => api.requests.get(id), [id]);
  const [activity, setActivity] = useState<AuditEntry[] | null>(null);
  const [jobs, setJobs] = useState<Job[] | null>(null);
  const [dialog, setDialog] = useState<null | "approve" | "reject" | "delegate" | "cancel" | "revoke">(null);

  useEffect(() => {
    if ((location.state as { created?: boolean } | null)?.created) toast.show("Request submitted");
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // Activity and jobs are secondary. Load them after the request and do not block on errors.
  const status = req.data?.status;
  useEffect(() => {
    if (!req.data) return;
    api.requests.activity(id).then(setActivity).catch(() => setActivity([]));
    api.provisioning.jobs(id).then(setJobs).catch(() => setJobs([]));
  }, [id, status, req.data]);

  if (req.loading && !req.data) return <Loading />;
  if (!req.data)
    return (
      <div className="page">
        <Link to="/requests" className="back">
          ← My requests
        </Link>
        <ErrorBox error={req.error} onRetry={req.reload} />
      </div>
    );

  const r = req.data;
  const cur = r.steps.find((s) => s.current);
  const isApprover = !!cur && !!user && r.requester !== user && (cur.assignees.includes(user) || isAdmin);
  const isRequester = r.requester === user;
  const canCancel = isRequester && r.status === "PENDING_APPROVAL";
  const canRevoke = (isRequester || isAdmin) && r.status === "ACTIVE" && !r.opaPath;

  const updated = (next: AccessRequest, msg: string) => {
    req.setData(next);
    setDialog(null);
    toast.show(msg);
  };

  return (
    <div className="page">
      <Link to={isRequester ? "/requests" : "/approvals"} className="back">
        ← {isRequester ? "My requests" : "Approvals"}
      </Link>

      <div className="page-head">
        <div>
          <div className="card-kicker">{r.id}</div>
          <h1>
            {r.capability} <span className="muted">· {r.option}</span>
          </h1>
          <StatusBadge status={r.status} />
        </div>
        <div className="actions">
          {isApprover && (
            <>
              <button className="btn btn-primary" onClick={() => setDialog("approve")}>
                Approve
              </button>
              <button className="btn btn-danger" onClick={() => setDialog("reject")}>
                Reject
              </button>
              <button className="btn" onClick={() => setDialog("delegate")}>
                Delegate
              </button>
            </>
          )}
          {canCancel && (
            <button className="btn btn-danger" onClick={() => setDialog("cancel")}>
              Cancel request
            </button>
          )}
          {canRevoke && (
            <button className="btn btn-danger" onClick={() => setDialog("revoke")}>
              Remove access
            </button>
          )}
        </div>
      </div>

      <div className="two-col">
        <section className="card">
          <h2>Details</h2>
          <dl className="facts facts-stack">
            <div>
              <dt>Catalogue</dt>
              <dd>
                <Link to={`/catalogues/${r.catalogueId}`}>{r.catalogueId}</Link>
              </dd>
            </div>
            <div>
              <dt>Requester</dt>
              <dd>{r.requester}</dd>
            </div>
            <div>
              <dt>Backup owner</dt>
              <dd>{r.backupOwner}</dd>
            </div>
            <div>
              <dt>Duration</dt>
              <dd>{duration(r)}</dd>
            </div>
            <div>
              <dt>Raised</dt>
              <dd>{fmtDate(r.createdAt)}</dd>
            </div>
            <div>
              <dt>Decided</dt>
              <dd>{fmtDate(r.decidedAt)}</dd>
            </div>
            {r.opaPath && (
              <div>
                <dt>Policy change</dt>
                <dd>
                  <code>{r.opaPath}</code>
                </dd>
              </div>
            )}
          </dl>
          <h3>Justification</h3>
          <p className="prewrap">{r.justification}</p>
        </section>

        <section className="card">
          <h2>Approval chain</h2>
          <ol className="steps">
            {r.steps.map((s, i) => (
              <li key={i} className={`step ${s.current ? "step-current" : ""} step-${(s.decision ?? "open").toLowerCase()}`}>
                <div className="step-dot" />
                <div>
                  <div className="card-row">
                    <strong>{s.level}</strong>
                    {s.decision ? (
                      <StatusBadge status={s.decision} />
                    ) : s.current ? (
                      <span className="badge badge-warn">Waiting</span>
                    ) : (
                      <span className="badge badge-muted">Not started</span>
                    )}
                  </div>
                  <div className="muted small">
                    {s.decidedBy ? `By ${s.decidedBy} · ${fmtDate(s.decidedAt)}` : `Any of: ${s.assignees.join(", ")}`}
                    {s.delegatedFrom && ` · delegated by ${s.delegatedFrom}`}
                  </div>
                  {s.comment && <blockquote>{s.comment}</blockquote>}
                </div>
              </li>
            ))}
          </ol>
        </section>
      </div>

      {jobs && jobs.length > 0 && (
        <section className="card">
          <h2>Provisioning</h2>
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th>Connector</th>
                  <th>Status</th>
                  <th>Message</th>
                  <th>When</th>
                  <th>Expires</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {jobs.map((j) => (
                  <tr key={j.id}>
                    <td>{j.connector}</td>
                    <td>
                      <StatusBadge status={j.status} />
                    </td>
                    <td className="muted">{j.message}</td>
                    <td className="nowrap">{fmtDate(j.createdAt)}</td>
                    <td className="nowrap">{j.expiresAt ? fmtDate(j.expiresAt) : "Never"}</td>
                    <td>
                      {j.status === "FAILED" && (isRequester || isAdmin) && (
                        <RetryButton
                          job={j}
                          onDone={() => {
                            toast.show("Retried");
                            req.reload();
                          }}
                        />
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      )}

      <section className="card">
        <h2>Activity</h2>
        {activity === null ? (
          <Loading />
        ) : activity.length === 0 ? (
          <p className="muted">No activity recorded.</p>
        ) : (
          <ul className="timeline">
            {activity.map((a) => (
              <li key={a.seq}>
                <span className="muted nowrap small">{fmtDate(a.time)}</span>
                <span>
                  <strong>{a.actor}</strong> {a.message}
                </span>
              </li>
            ))}
          </ul>
        )}
      </section>

      {(dialog === "approve" || dialog === "reject") && (
        <DecisionDialog
          decision={dialog === "approve" ? "APPROVED" : "REJECTED"}
          onClose={() => setDialog(null)}
          onSubmit={async (comment) =>
            updated(
              await api.requests.decide(r.id, dialog === "approve" ? "APPROVED" : "REJECTED", comment),
              dialog === "approve" ? "Approved" : "Rejected",
            )
          }
        />
      )}
      {dialog === "delegate" && (
        <TextDialog
          title="Delegate this step"
          label="Delegate to (user id)"
          button="Delegate"
          required
          onClose={() => setDialog(null)}
          onSubmit={async (to) => updated(await api.requests.delegate(r.id, to), `Delegated to ${to}`)}
        />
      )}
      {dialog === "cancel" && (
        <ConfirmDialog
          title="Cancel this request?"
          body="Your approvers will no longer see it. You can raise a new request later."
          button="Cancel request"
          onClose={() => setDialog(null)}
          onConfirm={async () => updated(await api.requests.cancel(r.id), "Request cancelled")}
        />
      )}
      {dialog === "revoke" && (
        <ConfirmDialog
          title="Remove this access?"
          body={`The ${r.option} access for ${r.requester} will be removed now.`}
          button="Remove access"
          onClose={() => setDialog(null)}
          onConfirm={async () => {
            await api.provisioning.revoke(r.id);
            setDialog(null);
            toast.show("Access removed");
            req.reload();
          }}
        />
      )}
      {toast.node}
    </div>
  );
}

function RetryButton({ job, onDone }: { job: Job; onDone: () => void }) {
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState<string | null>(null);
  return (
    <>
      <button
        className="btn btn-small"
        disabled={busy}
        onClick={async () => {
          setBusy(true);
          setErr(null);
          try {
            await api.provisioning.retry(job.id);
            onDone();
          } catch (e) {
            setErr(errorText(e));
          } finally {
            setBusy(false);
          }
        }}
      >
        {busy ? "Retrying…" : "Retry"}
      </button>
      {err && <div className="error-text small">{err}</div>}
    </>
  );
}

function DecisionDialog({
  decision,
  onClose,
  onSubmit,
}: {
  decision: "APPROVED" | "REJECTED";
  onClose: () => void;
  onSubmit: (comment: string) => Promise<void>;
}) {
  const approve = decision === "APPROVED";
  return (
    <TextDialog
      title={approve ? "Approve request" : "Reject request"}
      label={approve ? "Comment (optional)" : "Reason (shown to the requester)"}
      button={approve ? "Approve" : "Reject"}
      danger={!approve}
      multiline
      required={!approve}
      onClose={onClose}
      onSubmit={onSubmit}
    />
  );
}

function TextDialog(props: {
  title: string;
  label: string;
  button: string;
  danger?: boolean;
  multiline?: boolean;
  required?: boolean;
  onClose: () => void;
  onSubmit: (value: string) => Promise<void>;
}) {
  const [value, setValue] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<unknown>(null);
  return (
    <Modal title={props.title} onClose={props.onClose}>
      <form
        className="form"
        onSubmit={async (e) => {
          e.preventDefault();
          setBusy(true);
          setError(null);
          try {
            await props.onSubmit(value.trim());
          } catch (err) {
            setError(err);
            setBusy(false);
          }
        }}
      >
        <label className="field">
          <span>{props.label}</span>
          {props.multiline ? (
            <textarea
              className="input"
              rows={3}
              value={value}
              onChange={(e) => setValue(e.target.value)}
              required={props.required}
              maxLength={1000}
              autoFocus
            />
          ) : (
            <input
              className="input"
              value={value}
              onChange={(e) => setValue(e.target.value)}
              required={props.required}
              autoFocus
            />
          )}
        </label>
        <ErrorBox error={error} />
        <div className="form-actions">
          <button type="button" className="btn btn-ghost" onClick={props.onClose}>
            Back
          </button>
          <button className={`btn ${props.danger ? "btn-danger" : "btn-primary"}`} disabled={busy}>
            {busy ? "Saving…" : props.button}
          </button>
        </div>
      </form>
    </Modal>
  );
}

function ConfirmDialog(props: {
  title: string;
  body: string;
  button: string;
  onClose: () => void;
  onConfirm: () => Promise<void>;
}) {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<unknown>(null);
  return (
    <Modal title={props.title} onClose={props.onClose}>
      <div className="form">
        <p>{props.body}</p>
        <ErrorBox error={error} />
        <div className="form-actions">
          <button className="btn btn-ghost" onClick={props.onClose}>
            Back
          </button>
          <button
            className="btn btn-danger"
            disabled={busy}
            onClick={async () => {
              setBusy(true);
              setError(null);
              try {
                await props.onConfirm();
              } catch (err) {
                setError(err);
                setBusy(false);
              }
            }}
          >
            {busy ? "Working…" : props.button}
          </button>
        </div>
      </div>
    </Modal>
  );
}
