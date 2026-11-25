import { download, get, post, put } from "./client";
import type {
  AccessRequest,
  AuditEntry,
  Capability,
  CatalogueDetail,
  CatalogueSummary,
  CreateRequestInput,
  Job,
  Notification,
  ServiceDetails,
  Tool,
  VerifyResult,
} from "./types";

const enc = encodeURIComponent;

export const api = {
  me: () => get<{ id: string }>("/me"),

  catalogues: {
    list: () => get<CatalogueSummary[]>("/catalogues"),
    get: (id: string) => get<CatalogueDetail>(`/catalogues/${enc(id)}`),
    serviceDetails: (id: string) => get<ServiceDetails>(`/catalogues/${enc(id)}/service-details`),
    create: (body: {
      name: string;
      department: string;
      businessUnit: string;
      l1: string;
      l2: string;
      description: string;
    }) => post<CatalogueSummary>("/catalogues", body),
    addCapability: (id: string, body: { name: string; risk: string; description: string; options: string[] }) =>
      post<Capability>(`/catalogues/${enc(id)}/capabilities`, body),
    replaceTools: (id: string, tools: Tool[]) => put<Tool[]>(`/catalogues/${enc(id)}/tools`, tools),
  },

  requests: {
    create: (body: CreateRequestInput) => post<AccessRequest>("/requests", body),
    list: (scope: "mine" | "approvals" | "decided") => get<AccessRequest[]>(`/requests?scope=${scope}`),
    get: (id: string) => get<AccessRequest>(`/requests/${enc(id)}`),
    activity: (id: string) => get<AuditEntry[]>(`/requests/${enc(id)}/activity`),
    decide: (id: string, decision: "APPROVED" | "REJECTED", comment?: string) =>
      post<AccessRequest>(`/requests/${enc(id)}/decision`, { decision, comment: comment || null }),
    delegate: (id: string, to: string) => post<AccessRequest>(`/requests/${enc(id)}/delegate`, { to }),
    cancel: (id: string) => post<AccessRequest>(`/requests/${enc(id)}/cancel`),
  },

  provisioning: {
    jobs: (requestId: string) => get<Job[]>(`/provisioning/jobs?requestId=${enc(requestId)}`),
    retry: (jobId: number) => post<Job>(`/provisioning/jobs/${jobId}/retry`),
    revoke: (requestId: string) => post<Job>("/provisioning/revocations", { requestId }),
  },

  audit: {
    search: (q: { actor?: string; requestId?: string; from?: string; to?: string }) => {
      const p = new URLSearchParams();
      Object.entries(q).forEach(([k, v]) => v && p.set(k, v));
      const qs = p.toString();
      return get<AuditEntry[]>(`/audit${qs ? "?" + qs : ""}`);
    },
    verify: () => get<VerifyResult>("/audit/verify"),
    exportCsv: () => download("/audit/export", "audit.csv"),
    summary: () => get<Record<string, number>>("/analytics/summary"),
  },

  notifications: {
    list: () => get<Notification[]>("/notifications"),
    markRead: (id: number) => post<Notification>(`/notifications/${id}/read`),
  },
};
