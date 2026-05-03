'use client';

import { useState, useEffect } from 'react';
import { db } from './firebase';
import { collection, onSnapshot, query } from 'firebase/firestore';

export default function AdminDashboard() {
  const [withdrawals, setWithdrawals] = useState([]);
  const [usageRecords, setUsageRecords] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const unsubscribeData = onSnapshot(query(collection(db, "withdrawals")), (snapshot) => {
      setWithdrawals(snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() })));
    });

    const unsubscribeUsage = onSnapshot(query(collection(db, "daily_usage")), (snapshot) => {
      setUsageRecords(snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() })));
      setLoading(false);
    });

    return () => {
      unsubscribeData();
      unsubscribeUsage();
    };
  }, []);

  if (loading) {
    return <div style={{ padding: '3rem', color: '#64748b' }}>Loading overview...</div>;
  }

  const pendingRequests = withdrawals.filter(w => w.status === 'Pending').length;
  const approvedTotal = withdrawals.filter(w => w.status === 'Approved').reduce((acc, curr) => acc + curr.amountRs, 0);
  const pendingUsage = usageRecords.filter(r => !r.isCollected).length;
  const todayStr = new Date().toISOString().split('T')[0];
  const creditedToday = usageRecords.filter(r => r.isCollected && r.date === todayStr).length;

  return (
    <div className="dashboard-page">
      <div className="header">
        <h1>Overview</h1>
      </div>

      <div className="stats-grid">
        <div className="stat-card">
          <h3>Pending Withdrawals</h3>
          <p>{pendingRequests}</p>
        </div>
        <div className="stat-card">
          <h3>Pending Usage Reviews</h3>
          <p>{pendingUsage}</p>
        </div>
        <div className="stat-card">
          <h3>Total Approved ₹</h3>
          <p>₹{approvedTotal}</p>
        </div>
        <div className="stat-card">
          <h3>Usage Credited Today</h3>
          <p>{creditedToday}</p>
        </div>
      </div>
      
      <div style={{ marginTop: '2rem', padding: '2rem', background: 'white', borderRadius: '16px', border: '1px solid #e2e8f0' }}>
        <h2 style={{ marginTop: 0 }}>Welcome to the Admin Panel</h2>
        <p style={{ color: '#64748b', lineHeight: 1.6 }}>
          Use the sidebar navigation to manage user usage, review gift card requests, and view the history of rewarded items.
        </p>
      </div>
    </div>
  );
}
