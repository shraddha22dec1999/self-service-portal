// Shapes returned by the gateway. Kept in step with the Java records in services/*.

export type Risk = "Low" | "Medium" | "High";

export interface CatalogueSummary {
  id: string;
  name: string;
  department: string;
  businessUnit: string;
  creator: string;
  l1: string;
  l2: string;
  description: string;
  visibleTo: string[];
  capabilityCount: number;
}

export interface Capability {
  id: number;
  name: string;
  risk: Risk | string;
  description: string;
  options: string[];
}

export interface Tool {
  name: string;
  tool: string;
  category: string;
  url: string;
  description: string | null;
}

export interface CatalogueDetail {
  catalogue: CatalogueSummary;
  capabilities: Capability[];
}

export interface ServiceDetails {
  catalogue: CatalogueSummary;
  servicesProvided: Capability[];
  toolsAndResources: Tool[];
}

export type RequestStatus =
  | "PENDING_APPROVAL"
  | "PROVISIONING"
  | "ACTIVE"
  | "REJECTED"
  | "CANCELLED"
  | "FAILED";

export interface Step {
  level: string;
  assignees: string[];
  delegatedFrom: string | null;
  decision: "APPROVED" | "REJECTED" | null;
  decidedBy: string | null;
  decidedAt: string | null;
  comment: string | null;
  current: boolean;
}

export interface AccessRequest {
  id: string;
  catalogueId: string;
  capability: string;
  option: string;
  requester: string;
  backupOwner: string;
  justification: string;
  permanent: boolean;
  months: number;
  days: number;
  status: RequestStatus;
  createdAt: string;
  decidedAt: string | null;
  opaPath: string | null;
  steps: Step[];
}

export interface CreateRequestInput {
  catalogueId: string;
  capability: string;
  option: string;
  justification: string;
  backupOwner: string;
  permanent: boolean;
  months: number;
  days: number;
  opaPath?: string;
  opaValue?: Record<string, unknown>;
}

export interface Job {
  id: number;
  requestId: string;
  connector: string;
  capability: string;
  option: string;
  status: "SUCCEEDED" | "FAILED" | "REVOKED";
  message: string;
  createdAt: string;
  expiresAt: string | null;
}

export interface AuditEntry {
  seq: number;
  time: string;
  actor: string;
  action: string;
  requestId: string | null;
  message: string;
  prevHash: string;
  hash: string;
}

export interface Notification {
  id: number;
  subject: string;
  body: string;
  createdAt: string;
  read: boolean;
}

export type VerifyResult = { intact: true } | { intact: false; firstBrokenSeq: number };
