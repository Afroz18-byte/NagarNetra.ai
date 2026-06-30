import http from 'http';
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const PORT = 5000;
const DATA_DIR = path.join(__dirname, 'data_store');

// Ensure data directory exists
if (!fs.existsSync(DATA_DIR)) {
  fs.mkdirSync(DATA_DIR);
}

const ISSUES_PATH = path.join(DATA_DIR, 'issues.json');
const LOGS_PATH = path.join(DATA_DIR, 'logs.json');
const AI_LOGS_PATH = path.join(DATA_DIR, 'ai_logs.json');
const USERS_PATH = path.join(DATA_DIR, 'users.json');

// Default initial data to populate if files don't exist
const DEFAULT_USERS = [
  { id: 'USR-1000', email: 'citizen@nagarnetra.in', name: 'Rahul Sharma', authMethod: 'email', createdAt: 1719500000000 },
  { id: 'USR-2000', email: 'citizen@nagarnetra.in', name: 'Rahul S.', authMethod: 'email', createdAt: 1719505000000 }, // Duplicate
  { id: 'USR-3000', email: 'oauth@nagarnetra.in', name: 'Google User', authMethod: 'google', createdAt: 1719510000000 }
];

const DEFAULT_ISSUES = [
  { id: 'IDS-187', title: 'Critical Deep Pothole', description: 'Severe deep pothole situated on the fast lane. Causes extreme traffic congestion and poses a lethal hazard to motorcyclists.', category: 'Pothole', status: 'Reported', severity: 9, riskLevel: 'Critical', assignedDept: 'PWD', locationName: 'Indiranagar 12th Main, Bengaluru', latitude: 12.9725, longitude: 77.6412, upvotes: 47, timePosted: new Date(Date.now() - 2 * 86400000).toISOString(), ward: 'Ward 80', slaStatus: 'SLA OK', priorityScore: 94, daysOpen: 2, assignedOfficer: 'Rajesh Kumar', aiConfidence: 97, reporterId: 'USR-2000' }, // Linked to duplicate to test merging
  { id: 'IDS-182', title: 'Major Water Leakage', description: 'Substantial active potable water leak in the middle of the road causing local flooding and poor water pressure in nearby homes.', category: 'Water', status: 'Verified', severity: 7, riskLevel: 'High', assignedDept: 'BWSSB', locationName: 'Koramangala 5th Block, Bengaluru', latitude: 12.9348, longitude: 77.6189, upvotes: 23, timePosted: new Date(Date.now() - 4 * 86400000).toISOString(), ward: 'Ward 68', slaStatus: 'SLA OK', priorityScore: 78, daysOpen: 4, assignedOfficer: 'Priya Sharma', aiConfidence: 92, reporterId: 'USR-1000' },
  { id: 'IDS-179', title: 'Broken Streetlight Belt', description: 'Row of 3 Streetlights completely dead in Sector 2. The darkness poses critical women\'s safety concerns and local road hazards.', category: 'Lights', status: 'In Progress', severity: 6, riskLevel: 'Medium', assignedDept: 'BESCOM', locationName: 'HSR Layout Sector 2, Bengaluru', latitude: 12.9103, longitude: 77.6450, upvotes: 18, timePosted: new Date(Date.now() - 6 * 86400000).toISOString(), ward: 'Ward 174', slaStatus: 'SLA OK', priorityScore: 61, daysOpen: 6, assignedOfficer: 'Priya Sharma', aiConfidence: 88, reporterId: 'USR-3000' },
  { id: 'IDS-185', title: 'Illegal Garbage Dump Pile', description: 'Massive pile of rotting civic waste left uncollected by municipal trucks. Emits foul odors and poses health risks for school children.', category: 'Garbage', status: 'Reported', severity: 8, riskLevel: 'High', assignedDept: 'BBMP', locationName: 'BTM Layout near market, Bengaluru', latitude: 12.9141, longitude: 77.6059, upvotes: 34, timePosted: new Date(Date.now() - 1 * 86400000).toISOString(), ward: 'Ward 150', slaStatus: 'SLA OK', priorityScore: 74, daysOpen: 1, aiConfidence: 85, reporterId: 'USR-1000' },
  { id: 'IDS-181', title: 'Lethal Open Sewer Manhole', description: 'Completely open sewer slab directly on the pedestrian pathway. Extremely hazardous for commuters, especially during monsoon rains.', category: 'Sewage', status: 'Verified', severity: 10, riskLevel: 'Critical', assignedDept: 'BWSSB', locationName: 'MG Road near Metro, Bengaluru', latitude: 12.9738, longitude: 77.6119, upvotes: 89, timePosted: new Date(Date.now() - 14 * 86400000).toISOString(), ward: 'Ward 76', slaStatus: 'SLA BREACHED', priorityScore: 91, daysOpen: 14, escalationReason: 'SLA exceeded — health hazard', aiConfidence: 96, reporterId: 'USR-2000' }, // Linked to duplicate to test merging
  { id: 'IDS-162', title: 'Damaged Footpath Tile Repair', description: 'Loose and severely broken paving blocks in front of public library making sidewalk unusable for elderly citizens.', category: 'Pedestrian', status: 'Resolved', severity: 5, riskLevel: 'Low', assignedDept: 'PWD', locationName: 'Jayanagar 4th Block, Bengaluru', latitude: 12.9307, longitude: 77.5830, upvotes: 56, timePosted: new Date(Date.now() - 10 * 86400000).toISOString(), ward: 'Ward 147', slaStatus: 'Resolved', priorityScore: 44, daysOpen: 10, resolutionNotes: 'Footpath paving stones replaced completely and clean safety yellow margins painted. Inspected by Ward officer on June 21, 2026.', aiConfidence: 85, reporterId: 'USR-1000' }
];

const DEFAULT_LOGS = [
  { timestamp: Date.now() - 300000, isBotAction: true, message: 'Fake report of garbage dump flagged from User ID 2341 - trust score adjusted', priority: 'High' },
  { timestamp: Date.now() - 720000, isBotAction: true, message: '3 adjacent water leak reports merged in Koramangala Ward 5 — Escalated to Ward Engineer', priority: 'Medium' },
  { timestamp: Date.now() - 2100000, isBotAction: true, message: 'CRITICAL ALERT: Issue IDS-181 (Open Sewer Manhole on MG Road) escalated — 14 days pending, SLA breached!', priority: 'Critical' },
  { timestamp: Date.now() - 3600000, isBotAction: false, message: 'Citizen Rahul upvoted \'Critical Deep Pothole\' (IDS-187)', priority: 'Low' }
];

const DEFAULT_AI_LOGS = [
  { timestamp: Date.now() - 600000, agentName: 'VisionBot', issueId: 'IDS-187', decision: 'Pavement surface failure verified.', detail: 'Confidence: 95%. Depth estimated: 1.2ft.' },
  { timestamp: Date.now() - 540000, agentName: 'CategoryBot', issueId: 'IDS-187', decision: 'Routed to PWD Roads Division.', detail: 'Classification aligned with BBMP-PWD shared jurisdiction.' },
  { timestamp: Date.now() - 480000, agentName: 'PriorityBot', issueId: 'IDS-187', decision: 'Severity score assessed as 9/10.', detail: 'Critical status due to presence on fast lane of busy arterial road.' }
];

// Helper functions to read/write JSON files
function readDataFile(filePath, defaultValue) {
  try {
    if (!fs.existsSync(filePath)) {
      fs.writeFileSync(filePath, JSON.stringify(defaultValue, null, 2));
      return defaultValue;
    }
    const content = fs.readFileSync(filePath, 'utf8');
    return JSON.parse(content);
  } catch (err) {
    console.error(`Error reading ${filePath}:`, err);
    return defaultValue;
  }
}

function writeDataFile(filePath, data) {
  try {
    fs.writeFileSync(filePath, JSON.stringify(data, null, 2));
  } catch (err) {
    console.error(`Error writing ${filePath}:`, err);
  }
}

// Initialize files if they don't exist
let issues = readDataFile(ISSUES_PATH, DEFAULT_ISSUES);
let logs = readDataFile(LOGS_PATH, DEFAULT_LOGS);
let aiLogs = readDataFile(AI_LOGS_PATH, DEFAULT_AI_LOGS);
let users = readDataFile(USERS_PATH, DEFAULT_USERS);

// Cleanup script to identify and merge duplicate accounts safely while preserving historical report data
function runDatabaseCleanupAndMigration() {
  console.log("🧹 Running duplicate user account cleanup script...");
  const usersByEmail = {};
  users.forEach(user => {
    if (!user.email) return;
    const emailKey = user.email.toLowerCase().trim();
    if (!usersByEmail[emailKey]) {
      usersByEmail[emailKey] = [];
    }
    usersByEmail[emailKey].push(user);
  });

  let databaseChanged = false;
  const cleanedUsers = [];

  Object.keys(usersByEmail).forEach(email => {
    const userGroup = usersByEmail[email];
    if (userGroup.length > 1) {
      // Sort by creation timestamp (oldest first). Fallback to ID sorting if no createdAt
      userGroup.sort((a, b) => (a.createdAt || 0) - (b.createdAt || 0));
      const oldestUser = userGroup[0];
      cleanedUsers.push(oldestUser);

      console.log(`⚠️ Found ${userGroup.length} duplicate accounts for email [${email}]. Keeping oldest ID [${oldestUser.id}].`);

      // Merge data (e.g. issues submitted by secondary accounts must be mapped to the primary account)
      const duplicateIds = userGroup.slice(1).map(u => u.id);
      let mergedIssuesCount = 0;
      issues = issues.map(issue => {
        if (issue.reporterId && duplicateIds.includes(issue.reporterId)) {
          mergedIssuesCount++;
          return { ...issue, reporterId: oldestUser.id };
        }
        return issue;
      });

      if (mergedIssuesCount > 0) {
        console.log(`✅ Merged ${mergedIssuesCount} issues from duplicate accounts to primary ID [${oldestUser.id}].`);
        writeDataFile(ISSUES_PATH, issues);
      }
      databaseChanged = true;
    } else {
      cleanedUsers.push(userGroup[0]);
    }
  });

  if (databaseChanged) {
    users = cleanedUsers;
    writeDataFile(USERS_PATH, users);
    console.log("🧹 Cleanup complete. Duplicate accounts successfully merged.");
  } else {
    console.log("✨ No duplicate user accounts found in database.");
  }
}

// Run cleanup immediately
runDatabaseCleanupAndMigration();


const CATEGORY_DEPT = {
  Pothole: 'PWD',
  Water: 'BWSSB',
  Lights: 'BESCOM',
  Garbage: 'BBMP',
  Sewage: 'BWSSB',
  Pedestrian: 'PWD',
  Other: 'BBMP'
};

function inferCategory(text = '') {
  const t = text.toLowerCase();
  if (/\b(pothole|road|cave|divider|asphalt)\b/.test(t)) return 'Pothole';
  if (/\b(water|pipeline|leak|flood|supply|contamination)\b/.test(t)) return 'Water';
  if (/\b(light|streetlight|electric|power|blackout|transformer)\b/.test(t)) return 'Lights';
  if (/\b(garbage|trash|waste|dump|bin)\b/.test(t)) return 'Garbage';
  if (/\b(sewage|sewer|drain|manhole|storm drain)\b/.test(t)) return 'Sewage';
  if (/\b(footpath|sidewalk|pedestrian|crossing|tile)\b/.test(t)) return 'Pedestrian';
  return 'Other';
}

function extractObjectId(text = '') {
  const match = text.match(/\b(?:pc|pole|light|manhole|mh|bin|pipe|meter|pump|transformer|tf|road)\s*[-#:]?\s*([a-z0-9]{1,8})\b/i);
  return match ? match[0].replace(/\s+/g, '').toUpperCase() : undefined;
}

function distanceMeters(a, b) {
  const earth = 6371000;
  const dLat = ((b.latitude - a.latitude) * Math.PI) / 180;
  const dLng = ((b.longitude - a.longitude) * Math.PI) / 180;
  const lat1 = (a.latitude * Math.PI) / 180;
  const lat2 = (b.latitude * Math.PI) / 180;
  const h = Math.sin(dLat / 2) ** 2 + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLng / 2) ** 2;
  return 2 * earth * Math.asin(Math.sqrt(h));
}

function calculatePriority(issue) {
  const duplicateBoost = Math.min(24, Math.max(0, (issue.duplicateReports || 1) - 1) * 6);
  const upvoteBoost = Math.min(22, (issue.upvotes || 0) * 2);
  return Math.min(100, Math.round((issue.severity || 5) * 7 + upvoteBoost + duplicateBoost));
}

function riskForScore(score) {
  if (score >= 90) return 'Critical';
  if (score >= 72) return 'High';
  if (score >= 45) return 'Medium';
  return 'Low';
}

function normalizeIssue(rawIssue) {
  const text = `${rawIssue.title || ''} ${rawIssue.description || ''} ${rawIssue.voiceTranscript || ''}`;
  const category = rawIssue.category || inferCategory(text);
  const priorityScore = calculatePriority({ ...rawIssue, duplicateReports: rawIssue.duplicateReports || 1 });
  return {
    ...rawIssue,
    id: rawIssue.id || `NN-${Date.now()}`,
    category,
    status: rawIssue.status || 'Open',
    severity: rawIssue.severity || 5,
    upvotes: rawIssue.upvotes || 1,
    duplicateReports: rawIssue.duplicateReports || 1,
    objectId: rawIssue.objectId || extractObjectId(text),
    assignedDept: rawIssue.assignedDept || CATEGORY_DEPT[category] || 'BBMP',
    locationName: rawIssue.locationName || 'Bengaluru',
    latitude: Number(rawIssue.latitude) || 12.9716,
    longitude: Number(rawIssue.longitude) || 77.5946,
    ward: rawIssue.ward || 'Unassigned Ward',
    slaStatus: rawIssue.slaStatus || 'SLA OK',
    daysOpen: rawIssue.daysOpen || 0,
    timePosted: rawIssue.timePosted || new Date().toISOString(),
    priorityScore,
    riskLevel: rawIssue.riskLevel || riskForScore(priorityScore),
    aiConfidence: rawIssue.aiConfidence || 86
  };
}

function findDuplicateIssue(newIssue) {
  return issues.find(existing => {
    if (existing.status === 'Resolved') return false;
    const sameObject = existing.objectId && newIssue.objectId && existing.objectId.toUpperCase() === newIssue.objectId.toUpperCase();
    const sameCategory = existing.category === newIssue.category;
    const sameLocation = (existing.locationName || '').toLowerCase().replace(/[^a-z0-9]/g, '') === (newIssue.locationName || '').toLowerCase().replace(/[^a-z0-9]/g, '');
    const closeEnough = distanceMeters(existing, newIssue) <= (sameObject ? 250 : 120);
    return (sameObject && (sameLocation || closeEnough)) || (sameCategory && closeEnough);
  });
}

function mergeDuplicateIssue(existing, incoming) {
  const duplicateReports = (existing.duplicateReports || 1) + (incoming.duplicateReports || 1);
  const upvotes = (existing.upvotes || 0) + Math.max(1, incoming.upvotes || 1);
  const clusteredIssueIds = Array.from(new Set([...(existing.clusteredIssueIds || []), incoming.id]));
  const severity = Math.max(existing.severity || 5, incoming.severity || 5);
  const priorityScore = calculatePriority({ ...existing, severity, upvotes, duplicateReports });
  return {
    ...existing,
    duplicateReports,
    upvotes,
    severity,
    priorityScore,
    riskLevel: riskForScore(priorityScore),
    clusteredIssueIds,
    status: priorityScore >= 90 ? 'Escalated' : existing.status,
    escalationReason: priorityScore >= 90 ? 'Duplicate reports increased priority' : existing.escalationReason,
    lastDuplicateAt: new Date().toISOString()
  };
}

// Set up server
const server = http.createServer((req, res) => {
  // CORS Headers
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization');

  if (req.method === 'OPTIONS') {
    res.writeHead(204);
    res.end();
    return;
  }

  const url = new URL(req.url, `http://${req.headers.host}`);
  const pathname = url.pathname;

  // GET /api/issues
  if (pathname === '/api/issues' && req.method === 'GET') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify(issues));
    return;
  }

  // POST /api/issues (Create or overwrite issue)
  if (pathname === '/api/issues' && req.method === 'POST') {
    let body = '';
    req.on('data', chunk => { body += chunk; });
    req.on('end', () => {
      try {
        const newIssue = normalizeIssue(JSON.parse(body));
        const existingDuplicate = findDuplicateIssue(newIssue);

        if (existingDuplicate && existingDuplicate.id !== newIssue.id) {
          const idx = issues.findIndex(i => i.id === existingDuplicate.id);
          issues[idx] = mergeDuplicateIssue(existingDuplicate, newIssue);
          logs.unshift({
            timestamp: Date.now(),
            isBotAction: true,
            message: `DuplicateBot merged ${newIssue.id} into ${existingDuplicate.id} at ${existingDuplicate.locationName}; ${issues[idx].duplicateReports} reports now clustered.`,
            priority: issues[idx].priorityScore >= 90 ? 'Critical' : 'High'
          });
          aiLogs.unshift({
            timestamp: Date.now(),
            agentName: 'DuplicateBot',
            issueId: existingDuplicate.id,
            decision: `Merged duplicate report ${newIssue.id}`,
            detail: `Matched on ${newIssue.objectId ? `object ${newIssue.objectId}` : 'nearby location'} and category ${newIssue.category}.`
          });
          writeDataFile(LOGS_PATH, logs.slice(0, 100));
          writeDataFile(AI_LOGS_PATH, aiLogs.slice(0, 100));
        } else {
          const idx = issues.findIndex(i => i.id === newIssue.id);
          if (idx !== -1) {
            issues[idx] = { ...issues[idx], ...newIssue };
          } else {
            issues.unshift(newIssue);
          }
        }

        writeDataFile(ISSUES_PATH, issues);
        const savedIssue = existingDuplicate && existingDuplicate.id !== newIssue.id
          ? issues.find(i => i.id === existingDuplicate.id)
          : newIssue;
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ success: true, issue: savedIssue, merged: Boolean(existingDuplicate && existingDuplicate.id !== newIssue.id) }));
      } catch (err) {
        res.writeHead(500, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: 'Failed to process JSON body' }));
      }
    });
    return;
  }

  // PUT /api/issues/:id (Update specific properties)
  const issueMatch = pathname.match(/^\/api\/issues\/([^/]+)$/);
  if (issueMatch && req.method === 'PUT') {
    const issueId = issueMatch[1];
    let body = '';
    req.on('data', chunk => { body += chunk; });
    req.on('end', () => {
      try {
        const updates = JSON.parse(body);
        const idx = issues.findIndex(i => i.id === issueId);
        if (idx === -1) {
          res.writeHead(404, { 'Content-Type': 'application/json' });
          res.end(JSON.stringify({ error: 'Issue not found' }));
          return;
        }

        issues[idx] = { ...issues[idx], ...updates };
        writeDataFile(ISSUES_PATH, issues);

        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ success: true, issue: issues[idx] }));
      } catch (err) {
        res.writeHead(500, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: 'Failed to process JSON body' }));
      }
    });
    return;
  }

  // GET /api/logs
  if (pathname === '/api/logs' && req.method === 'GET') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify(logs));
    return;
  }

  // POST /api/logs
  if (pathname === '/api/logs' && req.method === 'POST') {
    let body = '';
    req.on('data', chunk => { body += chunk; });
    req.on('end', () => {
      try {
        const newLog = JSON.parse(body);
        newLog.timestamp = newLog.timestamp || Date.now();
        logs.unshift(newLog);
        // Keep logs under 100 entries to prevent file bloating
        if (logs.length > 100) {
          logs = logs.slice(0, 100);
        }
        writeDataFile(LOGS_PATH, logs);
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ success: true, log: newLog }));
      } catch (err) {
        res.writeHead(500, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: 'Failed to parse log' }));
      }
    });
    return;
  }

  // GET /api/ai-logs
  if (pathname === '/api/ai-logs' && req.method === 'GET') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify(aiLogs));
    return;
  }

  // POST /api/ai-logs
  if (pathname === '/api/ai-logs' && req.method === 'POST') {
    let body = '';
    req.on('data', chunk => { body += chunk; });
    req.on('end', () => {
      try {
        const newAiLog = JSON.parse(body);
        newAiLog.timestamp = newAiLog.timestamp || Date.now();
        aiLogs.unshift(newAiLog);
        if (aiLogs.length > 100) {
          aiLogs = aiLogs.slice(0, 100);
        }
        writeDataFile(AI_LOGS_PATH, aiLogs);
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ success: true, aiLog: newAiLog }));
      } catch (err) {
        res.writeHead(500, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: 'Failed to parse AI log' }));
      }
    });
    return;
  }

  // DELETE /api/issues (Reset back to default demo database)
  if (pathname === '/api/issues' && req.method === 'DELETE') {
    issues = [...DEFAULT_ISSUES];
    logs = [...DEFAULT_LOGS];
    aiLogs = [...DEFAULT_AI_LOGS];
    writeDataFile(ISSUES_PATH, issues);
    writeDataFile(LOGS_PATH, logs);
    writeDataFile(AI_LOGS_PATH, aiLogs);
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ success: true, message: 'Database reset to default demo issues.' }));
    return;
  }

  // GET /api/users
  if (pathname === '/api/users' && req.method === 'GET') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify(users));
    return;
  }

  // POST /api/auth/register
  if (pathname === '/api/auth/register' && req.method === 'POST') {
    let body = '';
    req.on('data', chunk => { body += chunk; });
    req.on('end', () => {
      try {
        const payload = JSON.parse(body);
        const email = (payload.email || '').toLowerCase().trim();
        const name = (payload.name || '').trim();
        const authMethod = payload.authMethod || 'email';
        
        if (!email) {
          res.writeHead(400, { 'Content-Type': 'application/json' });
          res.end(JSON.stringify({ error: 'Email cannot be empty.' }));
          return;
        }

        // Check if user exists
        const existingUser = users.find(u => u.email.toLowerCase().trim() === email);
        if (existingUser) {
          if (existingUser.authMethod === 'google' && authMethod === 'email') {
            res.writeHead(409, { 'Content-Type': 'application/json' });
            res.end(JSON.stringify({ 
              error: 'This email is registered using Google OAuth. Please log in using your Google account.',
              code: 'SOCIAL_AUTH_CONFLICT'
            }));
            return;
          } else {
            res.writeHead(409, { 'Content-Type': 'application/json' });
            res.end(JSON.stringify({ 
              error: 'An account with this email already exists. Please log in instead.',
              code: 'EMAIL_ALREADY_EXISTS'
            }));
            return;
          }
        }

        // Create new user
        const newUser = {
          id: 'USR-' + Date.now(),
          email: email,
          name: name || email.split('@')[0],
          authMethod: authMethod,
          createdAt: Date.now()
        };

        users.push(newUser);
        writeDataFile(USERS_PATH, users);

        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ success: true, user: newUser }));
      } catch (err) {
        res.writeHead(500, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: 'Failed to process registration' }));
      }
    });
    return;
  }

  // POST /api/auth/login
  if (pathname === '/api/auth/login' && req.method === 'POST') {
    let body = '';
    req.on('data', chunk => { body += chunk; });
    req.on('end', () => {
      try {
        const payload = JSON.parse(body);
        const email = (payload.email || '').toLowerCase().trim();
        const user = users.find(u => u.email.toLowerCase().trim() === email);
        
        if (!user) {
          res.writeHead(404, { 'Content-Type': 'application/json' });
          res.end(JSON.stringify({ error: 'No account found with this email. Please sign up.' }));
          return;
        }

        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ success: true, user }));
      } catch (err) {
        res.writeHead(500, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: 'Failed to process login' }));
      }
    });
    return;
  }

  // 404 Route
  res.writeHead(404, { 'Content-Type': 'application/json' });
  res.end(JSON.stringify({ error: 'Not Found' }));
});

server.listen(PORT, '0.0.0.0', () => {
  console.log(`🚀 NagarNetra backend server running at http://localhost:${PORT}`);
});
