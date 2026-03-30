'use client';

import { useState, useEffect } from 'react';
import { db } from './firebase';
import { 
  collection, 
  onSnapshot, 
  query, 
  orderBy, 
  doc, 
  updateDoc 
} from 'firebase/firestore';

export default function AdminDashboard() {
  const [withdrawals, setWithdrawals] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const q = query(collection(db, "withdrawals"), orderBy("createdAt", "desc"));
    const unsubscribe = onSnapshot(q, (snapshot) => {
      const docs = snapshot.docs.map(doc => ({
        id: doc.id,
        ...doc.data()
      }));
      setWithdrawals(docs);
      setLoading(false);
    });

    return () => unsubscribe();
  }, []);

  const handleApprove = async (id) => {
    const code = prompt("Enter the Gift Card Code for this user:");
    if (code) {
      try {
        const docRef = doc(db, "withdrawals", id);
        await updateDoc(docRef, {
          status: 'Approved',
          giftCardCode: code,
          processedAt: Date.now()
        });
        alert(`Success: Gift card issued for ${id}`);
      } catch (error) {
        alert("Error approving withdrawal: " + error.message);
      }
    }
  };

  const handleReject = async (id) => {
    if (confirm("Are you sure you want to REJECT this request?")) {
      try {
        const docRef = doc(db, "withdrawals", id);
        await updateDoc(docRef, {
          status: 'Rejected',
          processedAt: Date.now()
        });
      } catch (error) {
        alert("Error rejecting withdrawal: " + error.message);
      }
    }
  };

  const formatDate = (timestamp) => {
    if (!timestamp) return 'N/A';
    return new Date(timestamp).toLocaleDateString() + ' ' + new Date(timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  };

  if (loading) {
    return <div className="loading-state">Syncing with GenGhealth Cloud...</div>;
  }

  return (
    <div className="dashboard-page">
      <div className="header">
        <h1>GenGhealth Admin</h1>
        <div className="admin-profile">Admin Dashboard (Real-time)</div>
      </div>

      <div className="stats-grid">
        <div className="stat-card">
          <h3>Pending Requests</h3>
          <p>{withdrawals.filter(w => w.status === 'Pending').length}</p>
        </div>
        <div className="stat-card">
          <h3>Approved Total</h3>
          <p>₹{withdrawals.filter(w => w.status === 'Approved').reduce((acc, curr) => acc + curr.amountRs, 0)}</p>
        </div>
        <div className="stat-card">
          <h3>Rejected</h3>
          <p>{withdrawals.filter(w => w.status === 'Rejected').length}</p>
        </div>
      </div>

      <div className="table-container">
        <table>
          <thead>
            <tr>
              <th>Request Date</th>
              <th>User ID</th>
              <th>Amount</th>
              <th>Points</th>
              <th>Status</th>
              <th>Action</th>
            </tr>
          </thead>
          <tbody>
            {withdrawals.length === 0 ? (
              <tr>
                <td colSpan="6" style={{ textAlign: 'center', padding: '3rem', color: '#64748b' }}>
                  No withdrawal requests found.
                </td>
              </tr>
            ) : (
              withdrawals.map(w => (
                <tr key={w.id}>
                  <td>{formatDate(w.createdAt)}</td>
                  <td><code style={{ fontSize: '0.8rem' }}>{w.userId}</code></td>
                  <td>₹{w.amountRs}</td>
                  <td>{w.pointsDeducted} pts</td>
                  <td>
                    <span className={`badge-${w.status.toLowerCase()}`}>
                      {w.status}
                    </span>
                  </td>
                  <td>
                    {w.status === 'Pending' ? (
                      <div style={{ display: 'flex', gap: '0.5rem' }}>
                        <button className="btn-approve" onClick={() => handleApprove(w.id)}>Issue Code</button>
                        <button 
                          onClick={() => handleReject(w.id)}
                          style={{ background: 'none', border: 'none', color: '#ef4444', cursor: 'pointer', fontWeight: 'bold', fontSize: '0.8rem' }}
                        >
                          Reject
                        </button>
                      </div>
                    ) : (
                      <div style={{ display: 'flex', flexDirection: 'column' }}>
                        <code style={{ fontSize: '0.8rem', color: '#1e293b', fontWeight: 'bold' }}>{w.giftCardCode || 'No Code'}</code>
                        <span style={{ fontSize: '0.7rem', color: '#64748b' }}>{formatDate(w.processedAt)}</span>
                      </div>
                    )}
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
