import { useEffect, useState } from "react";
import { NavLink, Outlet, useLocation } from "react-router-dom";
import { api } from "../api/endpoints";
import { useAuth } from "../auth/AuthContext";

export function Layout() {
  const { user, isAdmin, signOut } = useAuth();
  const [unread, setUnread] = useState(0);
  const [pending, setPending] = useState(0);
  const [menuOpen, setMenuOpen] = useState(false);
  const location = useLocation();

  // Counts for the badges. Refresh on navigation and every minute.
  useEffect(() => {
    let alive = true;
    const load = () => {
      api.notifications.list().then((n) => alive && setUnread(n.filter((x) => !x.read).length)).catch(() => {});
      api.requests.list("approvals").then((r) => alive && setPending(r.length)).catch(() => {});
    };
    load();
    const t = setInterval(load, 60_000);
    return () => {
      alive = false;
      clearInterval(t);
    };
  }, [location.pathname]);

  useEffect(() => setMenuOpen(false), [location.pathname]);

  const link = ({ isActive }: { isActive: boolean }) => (isActive ? "nav-link active" : "nav-link");

  return (
    <div className="shell">
      <header className="topbar">
        <button className="btn btn-icon menu-toggle" onClick={() => setMenuOpen((v) => !v)} aria-label="Menu">
          ☰
        </button>
        <NavLink to="/" className="brand">
          <span className="brand-mark">SP</span> Self Service Portal
        </NavLink>
        <div className="topbar-right">
          <NavLink to="/notifications" className="bell" aria-label={`Notifications, ${unread} unread`}>
            🔔{unread > 0 && <span className="count">{unread}</span>}
          </NavLink>
          <span className="who" title="Signed in as">
            {user}
          </span>
          <button className="btn btn-ghost" onClick={signOut}>
            Sign out
          </button>
        </div>
      </header>

      <nav className={`sidebar ${menuOpen ? "open" : ""}`}>
        <div className="nav-group">Access</div>
        <NavLink to="/catalogues" className={link}>
          Catalogues
        </NavLink>
        <NavLink to="/requests" className={link}>
          My requests
        </NavLink>
        <NavLink to="/approvals" className={link}>
          Approvals {pending > 0 && <span className="count">{pending}</span>}
        </NavLink>
        <NavLink to="/notifications" className={link}>
          Notifications {unread > 0 && <span className="count">{unread}</span>}
        </NavLink>
        {isAdmin && (
          <>
            <div className="nav-group">Admin</div>
            <NavLink to="/admin/catalogues/new" className={link}>
              New catalogue
            </NavLink>
            <NavLink to="/admin/audit" className={link}>
              Audit log
            </NavLink>
            <NavLink to="/admin/analytics" className={link}>
              Analytics
            </NavLink>
          </>
        )}
      </nav>

      <main className="content">
        <Outlet />
      </main>
    </div>
  );
}
