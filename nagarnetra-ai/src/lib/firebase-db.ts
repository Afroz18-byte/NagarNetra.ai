import { initializeApp, getApps, getApp } from 'firebase/app';
import { 
  getFirestore, 
  collection, 
  addDoc, 
  getDoc, 
  getDocs, 
  doc, 
  updateDoc, 
  deleteDoc, 
  query, 
  where, 
  orderBy,
  serverTimestamp,
  FieldValue
} from 'firebase/firestore';

// --- Firebase Configuration & Initialization ---
const firebaseConfig = {
  apiKey: process.env.FIREBASE_API_KEY || "AIzaSyFakeKey_Placeholder",
  authDomain: `${process.env.FIREBASE_PROJECT_ID}.firebaseapp.com`,
  projectId: process.env.FIREBASE_PROJECT_ID || "nagarnetra-ai-fallback",
  storageBucket: `${process.env.FIREBASE_PROJECT_ID}.appspot.com`,
  messagingSenderId: "123456789012",
  appId: process.env.FIREBASE_APP_ID || "1:123456789012:web:abcdef1234567890"
};

// Singleton App initialization
const app = getApps().length === 0 ? initializeApp(firebaseConfig) : getApp();
export const db = getFirestore(app);

// --- TS Interface Definition for Civic Issue ---
export interface CivicIssue {
  id?: string;
  reporterId: string;
  title: string;
  category: 'Pothole' | 'Water' | 'Lights' | 'Garbage' | 'Sewage' | 'Pedestrian' | 'Other';
  description: string;
  latitude: number;
  longitude: number;
  locationName: string;
  severity: number; // Scale of 1-10
  status: 'Reported' | 'Assigned' | 'In Progress' | 'Urgent Escalation' | 'Resolved' | string;
  slaStatus: 'SLA OK' | 'PENDING_ESCALATION' | 'CRITICAL DISPATCH' | string;
  assignedDept: string; // e.g. BBMP, BESCOM, BWSSB, PWD
  upvotes: number;
  photoBase64?: string;
  createdAt: FieldValue;
  updatedAt?: FieldValue;
}

// --- CRUD Implementation Functions ---

/**
 * Save / Create a new civic issue report into Firestore.
 */
export async function createIssueReport(issueData: Omit<CivicIssue, 'createdAt'>): Promise<string> {
  try {
    const issuesCollection = collection(db, 'issues');
    const enrichedData = {
      ...issueData,
      createdAt: serverTimestamp()
    };
    const docRef = await addDoc(issuesCollection, enrichedData);
    console.log(`[Firestore] Issue successfully created with ID: ${docRef.id}`);
    return docRef.id;
  } catch (error) {
    console.error(`[Firestore] Error creating issue:`, error);
    throw error;
  }
}

/**
 * Retrieve a specific civic issue report by Firestore Document ID.
 */
export async function getIssueReportById(docId: string): Promise<CivicIssue | null> {
  try {
    const docRef = doc(db, 'issues', docId);
    const docSnap = await getDoc(docRef);
    if (docSnap.exists()) {
      return { id: docSnap.id, ...docSnap.data() } as CivicIssue;
    }
    return null;
  } catch (error) {
    console.error(`[Firestore] Error retrieving issue ${docId}:`, error);
    throw error;
  }
}

/**
 * Retrieve all registered active issues from the database ordered by time.
 */
export async function getAllActiveIssues(): Promise<CivicIssue[]> {
  try {
    const issuesCollection = collection(db, 'issues');
    // Query items that are not Resolved yet, ordered by timestamp
    const q = query(
      issuesCollection, 
      where('status', '!=', 'Resolved'),
      orderBy('status'),
      orderBy('createdAt', 'desc')
    );
    const querySnapshot = await getDocs(q);
    const items: CivicIssue[] = [];
    querySnapshot.forEach((doc) => {
      items.push({ id: doc.id, ...doc.data() } as CivicIssue);
    });
    return items;
  } catch (error) {
    console.error('[Firestore] Error reading active issues:', error);
    return [];
  }
}

/**
 * Query issues submitted by a specific citizen reporter.
 */
export async function getIssuesByReporter(reporterId: string): Promise<CivicIssue[]> {
  try {
    const issuesCollection = collection(db, 'issues');
    const q = query(
      issuesCollection,
      where('reporterId', '==', reporterId),
      orderBy('createdAt', 'desc')
    );
    const querySnapshot = await getDocs(q);
    const items: CivicIssue[] = [];
    querySnapshot.forEach((doc) => {
      items.push({ id: doc.id, ...doc.data() } as CivicIssue);
    });
    return items;
  } catch (error) {
    console.error(`[Firestore] Error querying reports for reporter ${reporterId}:`, error);
    return [];
  }
}

/**
 * Update the status, SLA priority, or assigned department of an issue.
 */
export async function updateIssueSLAAndStatus(
  docId: string, 
  updates: Partial<Pick<CivicIssue, 'status' | 'slaStatus' | 'assignedDept'>>
): Promise<void> {
  try {
    const docRef = doc(db, 'issues', docId);
    await updateDoc(docRef, {
      ...updates,
      updatedAt: serverTimestamp()
    });
    console.log(`[Firestore] Successfully updated status on ticket ${docId}.`);
  } catch (error) {
    console.error(`[Firestore] Error updating issue status:`, error);
    throw error;
  }
}

/**
 * Handle citizen upvoting an issue (increment upvotes on Firestore).
 */
export async function incrementIssueUpvotes(docId: string, currentUpvotes: number): Promise<void> {
  try {
    const docRef = doc(db, 'issues', docId);
    await updateDoc(docRef, {
      upvotes: currentUpvotes + 1,
      updatedAt: serverTimestamp()
    });
    console.log(`[Firestore] Ticket ${docId} upvoted.`);
  } catch (error) {
    console.error(`[Firestore] Error incrementing upvote count:`, error);
    throw error;
  }
}

/**
 * Delete a ticket from the system database.
 */
export async function deleteIssueReport(docId: string): Promise<void> {
  try {
    const docRef = doc(db, 'issues', docId);
    await deleteDoc(docRef);
    console.log(`[Firestore] Removed report document: ${docId}`);
  } catch (error) {
    console.error(`[Firestore] Error deleting ticket:`, error);
    throw error;
  }
}
