import { Link } from "react-router-dom";
import { api } from "../api/endpoints";
import { Empty, ErrorBox, Loading, fmtDate, useLoad } from "../components/ui";

const REQ = /\bREQ-\d+\b/;

export function NotificationsPage() {
  const { data, error, loading, reload, setData } = useLoad(() => api.notifications.list());

  const markRead = async (id: number) => {
    const n = await api.notifications.markRead(id).catch(() => null);
    if (n && data) setData(data.map((x) => (x.id === id ? n : x)));
  };

  const markAll = async () => {
    if (!data) return;
    await Promise.all(data.filter((n) => !n.read).map((n) => api.notifications.markRead(n.id).catch(() => null)));
    reload();
  };

  const unread = data?.filter((n) => !n.read).length ?? 0;

  return (
    <div className="page">
      <div className="page-head">
        <div>
          <h1>Notifications</h1>
          <p className="muted">{unread ? `${unread} unread` : "You are all caught up."}</p>
        </div>
        {unread > 0 && (
          <button className="btn" onClick={markAll}>
            Mark all as read
          </button>
        )}
      </div>

      <ErrorBox error={error} onRetry={reload} />
      {loading && !data && <Loading />}
      {data && data.length === 0 && <Empty title="No notifications yet" />}

      <ul className="notif-list">
        {data?.map((n) => {
          const reqId = (n.subject + " " + n.body).match(REQ)?.[0];
          return (
            <li key={n.id} className={n.read ? "notif" : "notif unread"}>
              <div className="notif-main">
                <div className="card-row">
                  <strong>{n.subject}</strong>
                  <span className="muted small nowrap">{fmtDate(n.createdAt)}</span>
                </div>
                <p className="prewrap">{n.body}</p>
                <div className="row">
                  {reqId && (
                    <Link to={`/requests/${reqId}`} onClick={() => !n.read && markRead(n.id)}>
                      Open {reqId}
                    </Link>
                  )}
                  {!n.read && (
                    <button className="btn btn-link" onClick={() => markRead(n.id)}>
                      Mark as read
                    </button>
                  )}
                </div>
              </div>
            </li>
          );
        })}
      </ul>
    </div>
  );
}
