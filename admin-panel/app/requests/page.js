'use client';

import { useState, useEffect } from 'react';
import { db } from '../firebase';
import { collection, onSnapshot, query, orderBy, doc, updateDoc } from 'firebase/firestore';

export default function RequestsPage() {
  const [withdrawals, setWithdrawals] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const q = query(collection(db, "withdrawals"), orderBy("createdAt", "desc"));
    const unsubscribeData = onSnapshot(q, (snapshot) => {
      const docs = snapshot.docs.map(doc => ({
        id: doc.id,
        ...doc.data()
      }));
      // Filter only pending requests
      setWithdrawals(docs.filter(w => w.status === 'Pending'));
      setLoading(false);
    }, (error) => {
      console.error("Firestore error:", error);
      setLoading(false);
    });

    return () => unsubscribeData();
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
    if (confirm("Are you sure you want to reject this withdrawal request?")) {
      try {
        const docRef = doc(db, "withdrawals", id);
        await updateDoc(docRef, {
          status: 'Rejected',
          processedAt: Date.now()
        });
        alert(`Success: Withdrawal request rejected`);
      } catch (error) {
        alert("Error rejecting withdrawal: " + error.message);
      }
    }
  };

  const formatDate = (timestamp) => {
    if (!timestamp) return 'N/A';
    return new Date(timestamp).toLocaleDateString() + ' ' + new Date(timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  };

  return (
    <div className="dashboard-page">
      <div className="header">
        <h1>Pending Gift Card Requests</h1>
      </div>

      <div className="table-container">
        {loading ? (
          <div style={{ textAlign: 'center', padding: '3rem', color: '#64748b' }}>Syncing Requests...</div>
        ) : (
          <table>
            <thead>
              <tr>
                <th>Request Date</th>
                <th>User ID</th>
                <th>Amount</th>
                <th>Points Deducted</th>
                <th>Action</th>
              </tr>
            </thead>
            <tbody>
              {withdrawals.length === 0 ? (
                <tr><td colSpan="5" style={{ textAlign: 'center', padding: '3rem' }}>No pending requests.</td></tr>
              ) : (
                withdrawals.map(w => (
                  <tr key={w.id}>
                    <td>{formatDate(w.createdAt)}</td>
                    <td><code style={{ fontSize: '0.8rem' }}>{w.userId}</code></td>
                    <td style={{ fontWeight: 'bold' }}>₹{w.amountRs}</td>
                    <td>{w.pointsDeducted} pts</td>
                    <td>
                      <div style={{ display: 'flex', gap: '0.5rem' }}>
                        <button className="btn-approve" onClick={() => handleApprove(w.id)}>Issue Code</button>
                        <button onClick={() => handleReject(w.id)} style={{ background: 'none', border: 'none', color: '#ef4444', cursor: 'pointer', fontWeight: 'bold', padding: '0.5rem' }}>Reject</button>
                      </div>
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
