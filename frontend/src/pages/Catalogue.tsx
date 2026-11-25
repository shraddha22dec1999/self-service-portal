import { useState } from "react";
import { Link, useParams } from "react-router-dom";
import { api } from "../api/endpoints";
import type { Capability, Tool } from "../api/types";
import { useAuth } from "../auth/AuthContext";
import { Empty, ErrorBox, Loading, RiskBadge, useLoad, useToast } from "../components/ui";
import { RequestForm } from "./RequestForm";
import { AddCapabilityForm, ToolsEditor } from "./admin/CatalogueEditors";

type Tab = "capabilities" | "details";

export function CataloguePage() {
  const { id = "" } = useParams();
  const { user, isAdmin } = useAuth();
  const { data, error, loading, reload } = useLoad(() => api.catalogues.serviceDetails(id), [id]);
  const [tab, setTab] = useState<Tab>("capabilities");
  const [requesting, setRequesting] = useState<Capability | null>(null);
  const [adding, setAdding] = useState(false);
  const [editingTools, setEditingTools] = useState(false);
  const toast = useToast();

  if (loading && !data) return <Loading />;
  if (error || !data)
    return (
      <div className="page">
        <Link to="/catalogues" className="back">
          ← Catalogues
        </Link>
        <ErrorBox error={error} onRetry={reload} />
      </div>
    );

  const c = data.catalogue;
  // Whether the person may edit is decided by OPA on save. Show the buttons to the likely editors.
  const canEdit = isAdmin || user === c.creator || user === c.l1 || user === c.l2;

  return (
    <div className="page">
      <Link to="/catalogues" className="back">
        ← Catalogues
      </Link>
      <div className="page-head">
        <div>
          <div className="card-kicker">
            {c.department} · {c.businessUnit}
          </div>
          <h1>{c.name}</h1>
          <p className="muted">{c.description}</p>
        </div>
      </div>

      <dl className="facts">
        <div>
          <dt>Owner</dt>
          <dd>{c.creator}</dd>
        </div>
        <div>
          <dt>L1 approver</dt>
          <dd>{c.l1}</dd>
        </div>
        <div>
          <dt>L2 approver</dt>
          <dd>{c.l2}</dd>
        </div>
        <div>
          <dt>Visible to</dt>
          <dd>{c.visibleTo.length ? c.visibleTo.join(", ") : "Owner and approvers only"}</dd>
        </div>
      </dl>

      <div className="tabs" role="tablist">
        <button role="tab" aria-selected={tab === "capabilities"} onClick={() => setTab("capabilities")}>
          Capabilities ({data.servicesProvided.length})
        </button>
        <button role="tab" aria-selected={tab === "details"} onClick={() => setTab("details")}>
          Tools and resources ({data.toolsAndResources.length})
        </button>
      </div>

      {tab === "capabilities" && (
        <>
          {canEdit && (
            <div className="toolbar">
              <button className="btn" onClick={() => setAdding(true)}>
                + Add capability
              </button>
            </div>
          )}
          {data.servicesProvided.length === 0 && <Empty title="No capabilities yet" />}
          <div className="grid">
            {data.servicesProvided.map((cap) => (
              <div key={cap.id} className="card">
                <div className="card-row">
                  <h3>{cap.name}</h3>
                  <RiskBadge risk={cap.risk} />
                </div>
                <p className="muted">{cap.description}</p>
                <div className="chips">
                  {cap.options.map((o) => (
                    <span key={o} className="chip">
                      {o}
                    </span>
                  ))}
                </div>
                <div className="card-foot">
                  <button className="btn btn-primary" onClick={() => setRequesting(cap)}>
                    Request access
                  </button>
                </div>
              </div>
            ))}
          </div>
        </>
      )}

      {tab === "details" && (
        <>
          {canEdit && (
            <div className="toolbar">
              <button className="btn" onClick={() => setEditingTools(true)}>
                Edit tools and resources
              </button>
            </div>
          )}
          <ToolsTable tools={data.toolsAndResources} />
        </>
      )}

      {requesting && (
        <RequestForm
          catalogue={c}
          capability={requesting}
          onClose={() => setRequesting(null)}
        />
      )}
      {adding && (
        <AddCapabilityForm
          catalogueId={c.id}
          onClose={() => setAdding(false)}
          onSaved={() => {
            setAdding(false);
            toast.show("Capability added");
            reload();
          }}
        />
      )}
      {editingTools && (
        <ToolsEditor
          catalogueId={c.id}
          initial={data.toolsAndResources}
          onClose={() => setEditingTools(false)}
          onSaved={() => {
            setEditingTools(false);
            toast.show("Tools and resources saved");
            reload();
          }}
        />
      )}
      {toast.node}
    </div>
  );
}

function ToolsTable({ tools }: { tools: Tool[] }) {
  if (tools.length === 0) return <Empty title="No tools or resources listed" />;
  return (
    <div className="table-wrap">
      <table className="table">
        <thead>
          <tr>
            <th>Name</th>
            <th>Tool</th>
            <th>Category</th>
            <th>Description</th>
          </tr>
        </thead>
        <tbody>
          {tools.map((t, i) => (
            <tr key={i}>
              <td>
                {/^https?:\/\//i.test(t.url) ? (
                  <a href={t.url} target="_blank" rel="noreferrer noopener">
                    {t.name} ↗
                  </a>
                ) : (
                  t.name
                )}
              </td>
              <td>{t.tool}</td>
              <td>{t.category}</td>
              <td className="muted">{t.description}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
