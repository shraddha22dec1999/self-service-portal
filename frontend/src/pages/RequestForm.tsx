import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { api } from "../api/endpoints";
import type { Capability, CatalogueSummary } from "../api/types";
import { ErrorBox, Modal, RiskBadge } from "../components/ui";

/** The form for raising a request against one capability. OPA checks it on submit. */
export function RequestForm({
  catalogue,
  capability,
  onClose,
}: {
  catalogue: CatalogueSummary;
  capability: Capability;
  onClose: () => void;
}) {
  const navigate = useNavigate();
  const [option, setOption] = useState(capability.options[0] ?? "");
  const [justification, setJustification] = useState("");
  const [backupOwner, setBackupOwner] = useState("");
  const [permanent, setPermanent] = useState(false);
  const [months, setMonths] = useState(1);
  const [days, setDays] = useState(0);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<unknown>(null);

  const durationOk = permanent || months > 0 || days > 0;

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      const r = await api.requests.create({
        catalogueId: catalogue.id,
        capability: capability.name,
        option,
        justification: justification.trim(),
        backupOwner: backupOwner.trim(),
        permanent,
        months: permanent ? 0 : months,
        days: permanent ? 0 : days,
      });
      navigate(`/requests/${r.id}`, { state: { created: true } });
    } catch (err) {
      setError(err);
      setBusy(false);
    }
  };

  return (
    <Modal title="Request access" onClose={onClose}>
      <form className="form" onSubmit={submit}>
        <div className="summary-box">
          <div className="card-row">
            <strong>{capability.name}</strong>
            <RiskBadge risk={capability.risk} />
          </div>
          <span className="muted">{catalogue.name}</span>
        </div>

        <label className="field">
          <span>Access level</span>
          <select className="input" value={option} onChange={(e) => setOption(e.target.value)} required>
            {capability.options.map((o) => (
              <option key={o}>{o}</option>
            ))}
          </select>
        </label>

        <label className="field">
          <span>Why do you need it?</span>
          <textarea
            className="input"
            rows={3}
            value={justification}
            onChange={(e) => setJustification(e.target.value)}
            placeholder="For example: I need to review the payments repository for the Q1 release."
            required
            maxLength={2000}
          />
        </label>

        <label className="field">
          <span>Backup owner</span>
          <input
            className="input"
            value={backupOwner}
            onChange={(e) => setBackupOwner(e.target.value)}
            placeholder="User id of someone in your team"
            required
          />
          <small className="muted">Someone who can take over this access if you are away. Must be in your team.</small>
        </label>

        <fieldset className="field">
          <span>How long?</span>
          <label className="check">
            <input type="checkbox" checked={permanent} onChange={(e) => setPermanent(e.target.checked)} />
            Permanent access
          </label>
          {!permanent && (
            <div className="row">
              <label className="field compact">
                <span>Months</span>
                <input
                  className="input"
                  type="number"
                  min={0}
                  max={24}
                  value={months}
                  onChange={(e) => setMonths(Math.max(0, Math.min(24, Number(e.target.value))))}
                />
              </label>
              <label className="field compact">
                <span>Days</span>
                <input
                  className="input"
                  type="number"
                  min={0}
                  max={30}
                  value={days}
                  onChange={(e) => setDays(Math.max(0, Math.min(30, Number(e.target.value))))}
                />
              </label>
            </div>
          )}
          {!durationOk && <small className="error-text">Pick at least one day, or permanent access.</small>}
        </fieldset>

        <p className="muted small">
          Your request goes to your L1 approver, then your L2 approver. Access is given automatically after the last
          approval.
        </p>

        <ErrorBox error={error} />

        <div className="form-actions">
          <button type="button" className="btn btn-ghost" onClick={onClose}>
            Cancel
          </button>
          <button className="btn btn-primary" disabled={busy || !durationOk || !option}>
            {busy ? "Submitting…" : "Submit request"}
          </button>
        </div>
      </form>
    </Modal>
  );
}
