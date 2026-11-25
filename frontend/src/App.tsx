import { Navigate, Route, Routes } from "react-router-dom";
import { useAuth } from "./auth/AuthContext";
import { Layout } from "./components/Layout";
import { Loading } from "./components/ui";
import { LoginPage, SsoCallbackPage } from "./pages/Login";
import { CataloguesPage } from "./pages/Catalogues";
import { CataloguePage } from "./pages/Catalogue";
import { MyRequestsPage } from "./pages/MyRequests";
import { RequestPage } from "./pages/Request";
import { ApprovalsPage } from "./pages/Approvals";
import { NotificationsPage } from "./pages/Notifications";
import { NewCataloguePage } from "./pages/admin/NewCatalogue";
import { AuditPage } from "./pages/admin/Audit";
import { AnalyticsPage } from "./pages/admin/Analytics";

export function App() {
  const { user, isAdmin, loading } = useAuth();

  return (
    <Routes>
      <Route path="/auth/callback" element={<SsoCallbackPage />} />
      {loading ? (
        <Route path="*" element={<Loading label="Signing you in…" />} />
      ) : !user ? (
        <Route path="*" element={<LoginPage />} />
      ) : (
        <Route element={<Layout />}>
          <Route index element={<Navigate to="/catalogues" replace />} />
          <Route path="catalogues" element={<CataloguesPage />} />
          <Route path="catalogues/:id" element={<CataloguePage />} />
          <Route path="requests" element={<MyRequestsPage />} />
          <Route path="requests/:id" element={<RequestPage />} />
          <Route path="approvals" element={<ApprovalsPage />} />
          <Route path="notifications" element={<NotificationsPage />} />
          {isAdmin && (
            <>
              <Route path="admin/catalogues/new" element={<NewCataloguePage />} />
              <Route path="admin/audit" element={<AuditPage />} />
              <Route path="admin/analytics" element={<AnalyticsPage />} />
            </>
          )}
          <Route path="*" element={<NotFound />} />
        </Route>
      )}
    </Routes>
  );
}

function NotFound() {
  return (
    <div className="page">
      <h1>Page not found</h1>
      <p className="muted">The page does not exist, or you do not have access to it.</p>
    </div>
  );
}
