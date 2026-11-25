import { useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { api } from "../api/endpoints";
import { Empty, ErrorBox, Loading, useLoad } from "../components/ui";

export function CataloguesPage() {
  const { data, error, loading, reload } = useLoad(() => api.catalogues.list());
  const [q, setQ] = useState("");

  const shown = useMemo(() => {
    const s = q.trim().toLowerCase();
    if (!data) return [];
    if (!s) return data;
    return data.filter((c) =>
      [c.name, c.department, c.businessUnit, c.description].some((v) => v?.toLowerCase().includes(s)),
    );
  }, [data, q]);

  return (
    <div className="page">
      <div className="page-head">
        <div>
          <h1>Catalogues</h1>
          <p className="muted">Pick a catalogue to see what you can request.</p>
        </div>
        <input
          className="input search"
          type="search"
          placeholder="Search catalogues"
          value={q}
          onChange={(e) => setQ(e.target.value)}
        />
      </div>

      <ErrorBox error={error} onRetry={reload} />
      {loading && !data && <Loading />}
      {data && shown.length === 0 && (
        <Empty title={q ? "No catalogues match your search" : "No catalogues to show"}>
          {q ? "Try another word." : "You can only see the catalogues shared with your team."}
        </Empty>
      )}

      <div className="grid">
        {shown.map((c) => (
          <Link key={c.id} to={`/catalogues/${c.id}`} className="card card-link">
            <div className="card-kicker">
              {c.department} · {c.businessUnit}
            </div>
            <h3>{c.name}</h3>
            <p className="muted clamp">{c.description}</p>
            <div className="card-foot">
              <span>
                {c.capabilityCount} capabilit{c.capabilityCount === 1 ? "y" : "ies"}
              </span>
              <span className="muted">
                L1 {c.l1} · L2 {c.l2}
              </span>
            </div>
          </Link>
        ))}
      </div>
    </div>
  );
}
