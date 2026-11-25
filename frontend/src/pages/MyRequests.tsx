import { useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { api } from "../api/endpoints";
import type { RequestStatus } from "../api/types";
import { Empty, ErrorBox, Loading, useLoad } from "../components/ui";
import { RequestsTable } from "./RequestsTable";

const filters: { key: "all" | RequestStatus; label: string }[] = [
  { key: "all", label: "All" },
  { key: "PENDING_APPROVAL", label: "Pending" },
  { key: "ACTIVE", label: "Active" },
  { key: "REJECTED", label: "Rejected" },
  { key: "FAILED", label: "Failed" },
  { key: "CANCELLED", label: "Cancelled" },
];

export function MyRequestsPage() {
  const { data, error, loading, reload } = useLoad(() => api.requests.list("mine"));
  const [filter, setFilter] = useState<(typeof filters)[number]["key"]>("all");

  const rows = useMemo(
    () => (data ?? []).filter((r) => filter === "all" || r.status === filter),
    [data, filter],
  );

  return (
    <div className="page">
      <div className="page-head">
        <div>
          <h1>My requests</h1>
          <p className="muted">Everything you have asked for, newest first.</p>
        </div>
        <Link to="/catalogues" className="btn btn-primary">
          New request
        </Link>
      </div>

      <div className="pills">
        {filters.map((f) => (
          <button key={f.key} className={filter === f.key ? "pill active" : "pill"} onClick={() => setFilter(f.key)}>
            {f.label}
            {data && f.key !== "all" && ` (${data.filter((r) => r.status === f.key).length})`}
          </button>
        ))}
      </div>

      <ErrorBox error={error} onRetry={reload} />
      {loading && !data && <Loading />}
      {data && rows.length === 0 && (
        <Empty title="No requests here">
          <Link to="/catalogues">Browse the catalogues</Link> to request access.
        </Empty>
      )}
      {rows.length > 0 && <RequestsTable rows={rows} />}
    </div>
  );
}
