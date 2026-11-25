import { useState } from "react";
import { api } from "../../api/endpoints";
import type { Tool } from "../../api/types";
import { ErrorBox, Modal } from "../../components/ui";

export function AddCapabilityForm({
  catalogueId,
  onClose,
  onSaved,
}: {
  catalogueId: string;
  onClose: () => void;
  onSaved: () => void;
}) {
  const [name, setName] = useState("");
  const [risk, setRisk] = useState("Low");
  const [description, setDescription] = useState("");
  const [options, setOptions] = useState("Read access\nWrite access");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<unknown>(null);

  const list = options
    .split("\n")
    .map((s) => s.trim())
    .filter(Boolean);

  return (
    <Modal title="Add capability" onClose={onClose}>
      <form
        className="form"
        onSubmit={async (e) => {
          e.preventDefault();
          setBusy(true);
          setError(null);
          try {
            await api.catalogues.addCapability(catalogueId, { name: name.trim(), risk, description: description.trim(), options: list });
            onSaved();
          } catch (err) {
            setError(err);
            setBusy(false);
          }
        }}
      >
        <label className="field">
          <span>Name</span>
          <input className="input" value={name} onChange={(e) => setName(e.target.value)} required placeholder="Grant repository access" />
        </label>
        <label className="field">
          <span>Risk</span>
          <select className="input" value={risk} onChange={(e) => setRisk(e.target.value)}>
            <option>Low</option>
            <option>Medium</option>
            <option>High</option>
          </select>
        </label>
        <label className="field">
          <span>Description</span>
          <textarea className="input" rows={3} value={description} onChange={(e) => setDescription(e.target.value)} required maxLength={1000} />
        </label>
        <label className="field">
          <span>Options (one per line)</span>
          <textarea className="input" rows={4} value={options} onChange={(e) => setOptions(e.target.value)} />
          {list.length === 0 && <small className="error-text">Add at least one option.</small>}
        </label>
        <ErrorBox error={error} />
        <div className="form-actions">
          <button type="button" className="btn btn-ghost" onClick={onClose}>
            Cancel
          </button>
          <button className="btn btn-primary" disabled={busy || list.length === 0}>
            {busy ? "Saving…" : "Add capability"}
          </button>
        </div>
      </form>
    </Modal>
  );
}

const blank: Tool = { name: "", tool: "", category: "", url: "", description: "" };

export function ToolsEditor({
  catalogueId,
  initial,
  onClose,
  onSaved,
}: {
  catalogueId: string;
  initial: Tool[];
  onClose: () => void;
  onSaved: () => void;
}) {
  const [rows, setRows] = useState<Tool[]>(initial.length ? initial.map((t) => ({ ...t, description: t.description ?? "" })) : [{ ...blank }]);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<unknown>(null);

  const set = (i: number, k: keyof Tool, v: string) => setRows(rows.map((r, j) => (j === i ? { ...r, [k]: v } : r)));

  return (
    <Modal title="Tools and resources" onClose={onClose}>
      <form
        className="form"
        onSubmit={async (e) => {
          e.preventDefault();
          setBusy(true);
          setError(null);
          try {
            await api.catalogues.replaceTools(catalogueId, rows.map((r) => ({ ...r, description: r.description || null })));
            onSaved();
          } catch (err) {
            setError(err);
            setBusy(false);
          }
        }}
      >
        <p className="muted small">Saving replaces the whole list.</p>
        {rows.map((r, i) => (
          <fieldset key={i} className="tool-row">
            <input className="input" placeholder="Name (Source code)" value={r.name} onChange={(e) => set(i, "name", e.target.value)} required />
            <input className="input" placeholder="Tool (GitHub)" value={r.tool} onChange={(e) => set(i, "tool", e.target.value)} required />
            <input className="input" placeholder="Category (Code)" value={r.category} onChange={(e) => set(i, "category", e.target.value)} required />
            <input className="input" type="url" placeholder="https://…" value={r.url} onChange={(e) => set(i, "url", e.target.value)} required />
            <input className="input wide" placeholder="Description (optional)" value={r.description ?? ""} onChange={(e) => set(i, "description", e.target.value)} />
            <button type="button" className="btn btn-ghost btn-small" onClick={() => setRows(rows.filter((_, j) => j !== i))}>
              Remove
            </button>
          </fieldset>
        ))}
        <button type="button" className="btn btn-small" onClick={() => setRows([...rows, { ...blank }])}>
          + Add row
        </button>
        <ErrorBox error={error} />
        <div className="form-actions">
          <button type="button" className="btn btn-ghost" onClick={onClose}>
            Cancel
          </button>
          <button className="btn btn-primary" disabled={busy}>
            {busy ? "Saving…" : "Save"}
          </button>
        </div>
      </form>
    </Modal>
  );
}
