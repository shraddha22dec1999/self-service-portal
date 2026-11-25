import { useState } from "react";
import { api } from "../api/endpoints";
import { Empty, ErrorBox, Loading, useLoad } from "../components/ui";
import { RequestsTable } from "./RequestsTable";

export function ApprovalsPage() {
  const [tab, setTab] = useState<"approvals" | "decided">("approvals");
  const { data, error, loading, reload } = useLoad(() => api.requests.list(tab), [tab]);

  return (
    <div className="page">
      <div className="page-head">
        <div>
          <h1>Approvals</h1>
          <p className="muted">Requests waiting for you, and the ones you have already decided.</p>
        </div>
      </div>

      <div className="tabs" role="tablist">
        <button role="tab" aria-selected={tab === "approvals"} onClick={() => setTab("approvals")}>
          Waiting for me
        </button>
        <button role="tab" aria-selected={tab === "decided"} onClick={() => setTab("decided")}>
          Decided by me
        </button>
      </div>

      <ErrorBox error={error} onRetry={reload} />
      {loading && <Loading />}
      {!loading && data && data.length === 0 && (
        <Empty title={tab === "approvals" ? "Nothing waiting for you" : "You have not decided any requests yet"} />
      )}
      {!loading && data && data.length > 0 && <RequestsTable rows={data} showRequester />}
    </div>
  );
}
