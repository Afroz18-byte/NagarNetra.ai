import { createContext, useContext, useState, type ReactNode } from 'react';
import type { AuthUser, UserRole } from '../types';

interface AuthContextType {
  user: AuthUser | null;
  login: (email: string, password: string, role: UserRole) => Promise<boolean>;
  logout: () => void;
  isAuthenticated: boolean;
}

const AuthContext = createContext<AuthContextType | null>(null);

const MOCK_USERS: Record<string, AuthUser> = {
  'admin@nagarnetra.gov.in': { id: 'u1', name: 'Shri Arjun Sharma', email: 'admin@nagarnetra.gov.in', role: 'Super Admin', avatar: 'AS' },
  'bbmp@nagarnetra.gov.in': { id: 'u2', name: 'Smt. Priya Nair', email: 'bbmp@nagarnetra.gov.in', role: 'Department Admin', department: 'BBMP', avatar: 'PN' },
  'officer@nagarnetra.gov.in': { id: 'u3', name: 'Shri Rajesh Kumar', email: 'officer@nagarnetra.gov.in', role: 'Field Officer', department: 'PWD', avatar: 'RK' },
};

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(() => {
    const saved = localStorage.getItem('nn_user');
    return saved ? JSON.parse(saved) : null;
  });

  const login = async (email: string, _password: string, _role: UserRole): Promise<boolean> => {
    await new Promise(r => setTimeout(r, 800));
    const found = MOCK_USERS[email.toLowerCase()];
    if (found) {
      setUser(found);
      localStorage.setItem('nn_user', JSON.stringify(found));
      return true;
    }
    // Allow any login for demo
    const demo: AuthUser = { id: 'demo', name: 'Demo Authority', email, role: _role, avatar: email.slice(0,2).toUpperCase() };
    setUser(demo);
    localStorage.setItem('nn_user', JSON.stringify(demo));
    return true;
  };

  const logout = () => {
    setUser(null);
    localStorage.removeItem('nn_user');
  };

  return (
    <AuthContext.Provider value={{ user, login, logout, isAuthenticated: !!user }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be inside AuthProvider');
  return ctx;
}
