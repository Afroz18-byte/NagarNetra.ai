import React, { createContext, useContext, useEffect, useState } from 'react';
import type { Issue, AgentActivity } from '../types';
import { MOCK_ISSUES, AGENT_ACTIVITIES } from '../data/mockData';

interface DataContextType {
  issues: Issue[];
  logs: any[];
  aiLogs: any[];
  agents: AgentActivity[];
  loading: boolean;
  refreshData: () => void;
  updateIssueOnServer: (id: string, updates: Partial<Issue>) => Promise<boolean>;
  createIssueOnServer: (issue: Issue) => Promise<Issue>;
}

const DataContext = createContext<DataContextType | undefined>(undefined);

export const DataProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [issues, setIssues] = useState<Issue[]>(MOCK_ISSUES);
  const [logs, setLogs] = useState<any[]>([]);
  const [aiLogs, setAiLogs] = useState<any[]>([]);
  const [agents, setAgents] = useState<AgentActivity[]>(AGENT_ACTIVITIES);
  const [loading, setLoading] = useState(true);

  const fetchData = async () => {
    try {
      // 1. Fetch Issues
      const resIssues = await fetch('http://localhost:5000/api/issues');
      if (resIssues.ok) {
        const data = await resIssues.json();
        if (data && data.length > 0) {
          setIssues(data);
        }
      }
    } catch (e) {
      console.warn('Backend offline or error fetching issues, falling back to mock data:', e);
    }

    try {
      // 2. Fetch Logs
      const resLogs = await fetch('http://localhost:5000/api/logs');
      if (resLogs.ok) {
        const data = await resLogs.json();
        setLogs(data);
      }
    } catch (e) {
      console.warn('Error fetching logs:', e);
    }

    try {
      // 3. Fetch AI Logs
      const resAiLogs = await fetch('http://localhost:5000/api/ai-logs');
      if (resAiLogs.ok) {
        const data = await resAiLogs.json();
        setAiLogs(data);
      }
    } catch (e) {
      console.warn('Error fetching AI logs:', e);
    }

    setLoading(false);
  };

  // Poll server every 3 seconds for real-time synchronization
  useEffect(() => {
    fetchData();
    const interval = setInterval(fetchData, 3000);
    return () => clearInterval(interval);
  }, []);

  // Update agents dynamically whenever aiLogs change
  useEffect(() => {
    if (aiLogs.length === 0) {
      setAgents(AGENT_ACTIVITIES);
      return;
    }

    const updatedAgents = AGENT_ACTIVITIES.map(agent => {
      const matchingLogs = aiLogs
        .filter(l => l.agentName.toLowerCase() === agent.name.toLowerCase())
        .sort((a, b) => b.timestamp - a.timestamp);

      if (matchingLogs.length === 0) return agent;

      const newLogs = matchingLogs.map(l => l.decision);
      const uniqueLogs = Array.from(new Set([...newLogs, ...agent.recentLogs]));

      return {
        ...agent,
        status: 'Active' as const,
        lastAction: matchingLogs[0].decision,
        lastActionTime: 'Just now',
        recentLogs: uniqueLogs.slice(0, 6)
      };
    });

    setAgents(updatedAgents);
  }, [aiLogs]);

  const createIssueOnServer = async (issue: Issue): Promise<Issue> => {
    try {
      const response = await fetch('http://localhost:5000/api/issues', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(issue)
      });
      if (response.ok) {
        const data = await response.json();
        fetchData();
        return data.issue || issue;
      }
    } catch (e) {
      console.error('Failed to create issue on server:', e);
    }
    setIssues(prev => [issue, ...prev]);
    return issue;
  };

  const updateIssueOnServer = async (id: string, updates: Partial<Issue>): Promise<boolean> => {
    try {
      const response = await fetch(`http://localhost:5000/api/issues/${id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(updates)
      });
      if (response.ok) {
        // Refetch immediately
        fetchData();
        return true;
      }
    } catch (e) {
      console.error('Failed to update issue on server:', e);
    }
    // Fallback local update
    setIssues(prev => prev.map(issue => issue.id === id ? { ...issue, ...updates } : issue));
    return false;
  };

  return (
    <DataContext.Provider value={{ issues, logs, aiLogs, agents, loading, refreshData: fetchData, updateIssueOnServer, createIssueOnServer }}>
      {children}
    </DataContext.Provider>
  );
};

export const useData = () => {
  const context = useContext(DataContext);
  if (!context) {
    throw new Error('useData must be used within a DataProvider');
  }
  return context;
};

