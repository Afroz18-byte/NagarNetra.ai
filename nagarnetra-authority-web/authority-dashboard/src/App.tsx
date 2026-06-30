import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './contexts/AuthContext';
import { ThemeProvider } from './contexts/ThemeContext';
import { DataProvider } from './contexts/DataContext';
import Sidebar from './components/Sidebar';
import LoginPage from './pages/LoginPage';
import DashboardPage from './pages/DashboardPage';
import ReportsPage from './pages/ReportsPage';
import VoiceReportPage from './pages/VoiceReportPage';
import ReportDetailPage from './pages/ReportDetailPage';
import MapPage from './pages/MapPage';
import DepartmentsPage from './pages/DepartmentsPage';
import AgentActivityPage from './pages/AgentActivityPage';
import EscalationPage from './pages/EscalationPage';
import AnalyticsPage from './pages/AnalyticsPage';
import NotificationsPage from './pages/NotificationsPage';
import SettingsPage from './pages/SettingsPage';

function ProtectedLayout({ children }: { children: React.ReactNode }) {
  const { isAuthenticated } = useAuth();
  if (!isAuthenticated) return <Navigate to="/login" replace />;
  return (
    <div className="app-layout">
      <Sidebar />
      <main className="main-content">
        {children}
      </main>
    </div>
  );
}

function AppRoutes() {
  const { isAuthenticated } = useAuth();
  return (
    <Routes>
      <Route path="/login" element={isAuthenticated ? <Navigate to="/dashboard" replace /> : <LoginPage />} />
      <Route path="/dashboard" element={<ProtectedLayout><DashboardPage /></ProtectedLayout>} />
      <Route path="/reports" element={<ProtectedLayout><ReportsPage /></ProtectedLayout>} />
      <Route path="/voice-intake" element={<ProtectedLayout><VoiceReportPage /></ProtectedLayout>} />
      <Route path="/reports/:id" element={<ProtectedLayout><ReportDetailPage /></ProtectedLayout>} />
      <Route path="/map" element={<ProtectedLayout><MapPage /></ProtectedLayout>} />
      <Route path="/departments" element={<ProtectedLayout><DepartmentsPage /></ProtectedLayout>} />
      <Route path="/agents" element={<ProtectedLayout><AgentActivityPage /></ProtectedLayout>} />
      <Route path="/escalations" element={<ProtectedLayout><EscalationPage /></ProtectedLayout>} />
      <Route path="/analytics" element={<ProtectedLayout><AnalyticsPage /></ProtectedLayout>} />
      <Route path="/notifications" element={<ProtectedLayout><NotificationsPage /></ProtectedLayout>} />
      <Route path="/settings" element={<ProtectedLayout><SettingsPage /></ProtectedLayout>} />
      <Route path="*" element={<Navigate to={isAuthenticated ? "/dashboard" : "/login"} replace />} />
    </Routes>
  );
}

export default function App() {
  return (
    <ThemeProvider>
      <AuthProvider>
        <DataProvider>
          <BrowserRouter>
            <AppRoutes />
          </BrowserRouter>
        </DataProvider>
      </AuthProvider>
    </ThemeProvider>
  );
}


