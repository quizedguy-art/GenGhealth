import './globals.css';

export const metadata = {
  title: 'GenGhealth Admin',
  description: 'Admin panel for rewarding healthy phone habits',
};

export default function RootLayout({ children }) {
  return (
    <html lang="en">
      <body>
        <div className="admin-container">
          <aside className="sidebar">
            <h2>GenGhealth Admin</h2>
            <nav>
              <a href="#" className="active">Dashboard</a>
              <a href="#">Withdrawals</a>
              <a href="#">Users</a>
              <a href="#">Settings</a>
            </nav>
          </aside>
          <main className="content">
            {children}
          </main>
        </div>
      </body>
    </html>
  );
}
