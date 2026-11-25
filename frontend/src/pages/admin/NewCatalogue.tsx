import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { api } from "../../api/endpoints";
import { ErrorBox } from "../../components/ui";

const fields = [
  { key: "name", label: "Name", placeholder: "Data Platform" },
  { key: "department", label: "Department", placeholder: "Engineering" },
  { key: "businessUnit", label: "Business unit", placeholder: "Technology" },
  { key: "l1", label: "L1 approver (user id)", placeholder: "sol_engg_lead" },
  { key: "l2", label: "L2 approver (user id)", placeholder: "manpreet" },
] as const;

type Form = Record<(typeof fields)[number]["key"] | "description", string>;

export function NewCataloguePage() {
  const navigate = useNavigate();
  const [form, setForm] = useState<Form>({ name: "", department: "", businessUnit: "", l1: "", l2: "", description: "" });
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<unknown>(null);

  return (
    <div className="page narrow">
      <h1>New catalogue</h1>
      <p className="muted">
        A new catalogue is visible only to you, its approvers and super admins. To share it with teams, raise a policy
        change request afterwards.
      </p>
      <form
        className="form card"
        onSubmit={async (e) => {
          e.preventDefault();
          setBusy(true);
          setError(null);
          try {
            const trimmed = Object.fromEntries(Object.entries(form).map(([k, v]) => [k, v.trim()])) as Form;
            const c = await api.catalogues.create(trimmed);
            navigate(`/catalogues/${c.id}`);
          } catch (err) {
            setError(err);
            setBusy(false);
          }
        }}
      >
        {fields.map((f) => (
          <label key={f.key} className="field">
            <span>{f.label}</span>
            <input
              className="input"
              value={form[f.key]}
              placeholder={f.placeholder}
              onChange={(e) => setForm({ ...form, [f.key]: e.target.value })}
              required
            />
          </label>
        ))}
        <label className="field">
          <span>Description</span>
          <textarea
            className="input"
            rows={3}
            value={form.description}
            onChange={(e) => setForm({ ...form, description: e.target.value })}
            required
            maxLength={1000}
          />
        </label>
        <ErrorBox error={error} />
        <div className="form-actions">
          <button className="btn btn-primary" disabled={busy}>
            {busy ? "Creating…" : "Create catalogue"}
          </button>
        </div>
      </form>
    </div>
  );
}
