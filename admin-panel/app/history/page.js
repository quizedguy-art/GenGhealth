'use client';

import { useState, useEffect } from 'react';
import { db } from '../firebase';
import { collection, onSnapshot, query, orderBy } from 'firebase/firestore';

export default function HistoryPage() {
  const [history, setHistory] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const q = query(collection(db, "withdrawals"), orderBy("createdAt", "desc"));
    const unsubscribeData = onSnapshot(q, (snapshot) => {
      const docs = snapshot.docs.map(doc => ({
        id: doc.id,
        ...doc.data()
      }));
      // Filter out pending requests to only show completed ones
      setHistory(docs.filter(w => w.status !== 'Pending'));
      setLoading(false);
    }, (error) => {
      console.error("Firestore error:", error);
      setLoading(false);
    });

    return () => unsubscribeData();
  }, []);

  const formatDate = (timestamp) => {
    if (!timestamp) return 'N/A';
    return new Date(timestamp).toLocaleDateString() + ' ' + new Date(timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  };

  return (
    <div className="dashboard-page">
      <div className="header">
        <h1>Reward History</h1>
      </div>

      <div className="table-container">
        {loading ? (
          <div style={{ textAlign: 'center', padding: '3rem', color: '#64748b' }}>Syncing History...</div>
        ) : (
          <table>
            <thead>
              <tr>
                <th>Processed Date</th>
                <th>User ID</th>
                <th>Amount</th>
                <th>Status</th>
                <th>Gift Card Code</th>
              </tr>
            </thead>
            <tbody>
              {history.length === 0 ? (
                <tr><td colSpan="5" style={{ textAlign: 'center', padding: '3rem' }}>No history found.</td></tr>
              ) : (
                history.map(w => (
                  <tr key={w.id}>
                    <td>{formatDate(w.processedAt || w.createdAt)}</td>
                    <td><code style={{ fontSize: '0.8rem' }}>{w.userId}</code></td>
                    <td style={{ fontWeight: 'bold' }}>₹{w.amountRs}</td>
                    <td>
                      <span className={`badge-${w.status.toLowerCase()}`}>{w.status}</span>
                    </td>
                    <td>
                      {w.status === 'Approved' ? (
                        <code style={{ fontWeight: 'bold', color: '#10b981' }}>{w.giftCardCode}</code>
                      ) : (
                        <span style={{ color: '#64748b', fontSize: '0.9rem' }}>N/A</span>
                      )}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
