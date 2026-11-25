import { api } from "../../api/endpoints";
import { Empty, ErrorBox, Loading, useLoad } from "../../components/ui";

/** Headline numbers first, then every action count as a bar list. */
const headline: { key: string; label: string }[] = [
  { key: "request.created", label: "Requests raised" },
  { key: "request.approved", label: "Approvals" },
  { key: "request.rejected", label: "Rejections" },
  { key: "access.granted", label: "Access granted" },
];

export function AnalyticsPage() {
  const { data, error, loading, reload } = useLoad(() => api.audit.summary());
  const rows = Object.entries(data ?? {}).sort((a, b) => b[1] - a[1]);
  const max = rows[0]?.[1] ?? 1;

  return (
    <div className="page">
      <h1>Analytics</h1>
      <p className="muted">Counts of every recorded action, from the audit log.</p>

      <ErrorBox error={error} onRetry={reload} />
      {loading && <Loading />}
      {data && rows.length === 0 && <Empty title="Nothing recorded yet" />}

      {data && rows.length > 0 && (
        <>
          <div className="stats">
            {headline
              .filter((h) => h.key in data)
              .map((h) => (
                <div key={h.key} className="stat">
                  <span className="muted small">{h.label}</span>
                  <strong>{data[h.key].toLocaleString()}</strong>
                </div>
              ))}
          </div>
          <section className="card">
            <h2>All actions</h2>
            <ul className="bars">
              {rows.map(([k, v]) => (
                <li key={k}>
                  <code>{k}</code>
                  <div className="bar-track">
                    <div className="bar" style={{ width: `${(v / max) * 100}%` }} />
                  </div>
                  <span className="num">{v.toLocaleString()}</span>
                </li>
              ))}
            </ul>
          </section>
        </>
      )}
    </div>
  );
}
