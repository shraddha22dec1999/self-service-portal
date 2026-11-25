import { Link } from "react-router-dom";
import type { AccessRequest } from "../api/types";
import { StatusBadge, duration, fmtDate } from "../components/ui";

/** Shared list used by My requests and Approvals. */
export function RequestsTable({ rows, showRequester }: { rows: AccessRequest[]; showRequester?: boolean }) {
  return (
    <div className="table-wrap">
      <table className="table">
        <thead>
          <tr>
            <th>Request</th>
            <th>Capability</th>
            {showRequester && <th>Requester</th>}
            <th>Duration</th>
            <th>Status</th>
            <th>Waiting on</th>
            <th>Raised</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((r) => {
            const cur = r.steps.find((s) => s.current);
            return (
              <tr key={r.id}>
                <td className="nowrap">
                  <Link to={`/requests/${r.id}`}>{r.id}</Link>
                </td>
                <td>
                  <div>{r.capability}</div>
                  <div className="muted small">
                    {r.option} · {r.catalogueId}
                  </div>
                </td>
                {showRequester && <td>{r.requester}</td>}
                <td>{duration(r)}</td>
                <td>
                  <StatusBadge status={r.status} />
                </td>
                <td className="muted">{cur ? `${cur.level}: ${cur.assignees.join(", ")}` : "—"}</td>
                <td className="muted nowrap">{fmtDate(r.createdAt)}</td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}
