'use client';

import { useState, useEffect } from 'react';
import { db, auth } from './firebase';
import { 
  collection, 
  onSnapshot, 
  query, 
  orderBy, 
  doc, 
  updateDoc,
  getDoc
} from 'firebase/firestore';
import { 
  signInWithPopup, 
  GoogleAuthProvider, 
  onAuthStateChanged,
  signOut 
} from 'firebase/auth';

export default function AdminDashboard() {
  const [user, setUser] = useState(null);
  const [isAdmin, setIsAdmin] = useState(false);
  const [withdrawals, setWithdrawals] = useState([]);
  const [loading, setLoading] = useState(true);
  const [authLoading, setAuthLoading] = useState(true);

  useEffect(() => {
    const unsubscribeAuth = onAuthStateChanged(auth, async (user) => {
      setAuthLoading(true);
      if (user) {
        setUser(user);
        // Check if user is admin in Firestore or matches the primary admin email
        const userDoc = await getDoc(doc(db, "users", user.uid));
        const isUserAdmin = userDoc.exists() && userDoc.data().isAdmin === true;
        const isPrimaryAdmin = user.email === 'luckykaseqq@gmail.com';
        
        setIsAdmin(isUserAdmin || isPrimaryAdmin);
      } else {
        setUser(null);
        setIsAdmin(false);
      }
      setAuthLoading(false);
    });

    return () => unsubscribeAuth();
  }, []);

  useEffect(() => {
    if (!user || !isAdmin) {
      setLoading(false);
      return;
    }

    setLoading(true);
    const q = query(collection(db, "withdrawals"), orderBy("createdAt", "desc"));
    const unsubscribeData = onSnapshot(q, (snapshot) => {
      const docs = snapshot.docs.map(doc => ({
        id: doc.id,
        ...doc.data()
      }));
      setWithdrawals(docs);
      setLoading(false);
    }, (error) => {
      console.error("Firestore error:", error);
      setLoading(false);
    });

    return () => unsubscribeData();
  }, [user, isAdmin]);

  const handleLogin = async () => {
    const provider = new GoogleAuthProvider();
    try {
      await signInWithPopup(auth, provider);
    } catch (error) {
      alert("Login failed: " + error.message);
    }
  };

  const handleLogout = async () => {
    await signOut(auth);
  };

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

  if (authLoading) {
    return <div className="loading-state">Authenticating...</div>;
  }

  if (!user) {
    return (
      <div className="login-container" style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', height: '100vh', gap: '2rem' }}>
        <h1 style={{ color: '#0f172a' }}>GenGhealth Admin Access</h1>
        <p style={{ color: '#64748b' }}>Please sign in with an authorized admin account.</p>
        <button 
          onClick={handleLogin}
          style={{ 
            padding: '1rem 2rem', 
            background: '#10b981', 
            color: 'white', 
            border: 'none', 
            borderRadius: '8px', 
            fontWeight: 'bold', 
            cursor: 'pointer',
            fontSize: '1.1rem'
          }}
        >
          Sign in with Google
        </button>
      </div>
    );
  }

  if (!isAdmin) {
    return (
      <div className="login-container" style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', height: '100vh', gap: '2rem' }}>
        <h1 style={{ color: '#ef4444' }}>Access Denied</h1>
        <p style={{ color: '#64748b', textAlign: 'center' }}>Account <strong>{user.email}</strong> is not authorized.<br/>Please contact the system administrator.</p>
        <button onClick={handleLogout} style={{ color: '#3b82f6', background: 'none', border: 'none', cursor: 'pointer', textDecoration: 'underline' }}>
          Sign out and try another account
        </button>
      </div>
    );
  }

  return (
    <div className="dashboard-page">
      <div className="header">
        <h1>GenGhealth Admin</h1>
        <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
          <div className="admin-profile" style={{ fontSize: '0.9rem' }}>{user.email}</div>
          <button onClick={handleLogout} style={{ padding: '0.4rem 0.8rem', background: 'rgba(239, 68, 68, 0.1)', color: '#ef4444', border: '1px solid #ef4444', borderRadius: '4px', cursor: 'pointer', fontSize: '0.8rem' }}>
            Logout
          </button>
        </div>
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
        {loading ? (
          <div style={{ textAlign: 'center', padding: '3rem', color: '#64748b' }}>Syncing with GenGhealth Cloud...</div>
        ) : (
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
        )}
      </div>
    </div>
  );
}
