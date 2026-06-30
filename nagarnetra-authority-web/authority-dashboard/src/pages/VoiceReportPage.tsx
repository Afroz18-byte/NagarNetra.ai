import { useMemo, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Header from '../components/Header';
import { useData } from '../contexts/DataContext';
import type { Issue } from '../types';
import { calculatePriority, departmentForCategory, extractObjectId, inferCategory, riskForScore } from '../lib/issueClustering';
import { Camera, CheckCircle2, Loader2, MapPin, Mic, Send, Sparkles, Square, Wand2 } from 'lucide-react';

declare global {
  interface Window {
    SpeechRecognition?: any;
    webkitSpeechRecognition?: any;
  }
}

const DEFAULT_COORDS = { latitude: 12.9716, longitude: 77.5946 };

function cleanSentence(text: string) {
  const trimmed = text.trim().replace(/\s+/g, ' ');
  return trimmed ? trimmed.charAt(0).toUpperCase() + trimmed.slice(1) : '';
}

function localGenerate(transcript: string, locationName: string, objectId?: string) {
  const category = inferCategory(transcript);
  const issueNoun = category === 'Lights' ? 'streetlight issue' : category.toLowerCase() + ' issue';
  const title = cleanSentence((objectId ? objectId + ' ' : '') + issueNoun + ' at ' + (locationName || 'reported location'));
  const description = cleanSentence(transcript + '. Location confirmed as ' + (locationName || 'Bengaluru') + (objectId ? ', object ID ' + objectId : '') + '. Submitted through the voice agent with photo evidence for AI-assisted routing.');
  return { title, description, category };
}

async function generateWithGemini(transcript: string, locationName: string, objectId?: string) {
  const key = import.meta.env.VITE_GEMINI_API_KEY;
  if (!key) return localGenerate(transcript, locationName, objectId);

  try {
    const response = await fetch('https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=' + key, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        contents: [{
          parts: [{ text: 'Return only JSON with title, description, category. Categories: Pothole, Water, Lights, Garbage, Sewage, Pedestrian, Other. Citizen transcript: ' + transcript + '. Location: ' + locationName + '. Object ID: ' + (objectId || 'none') + '.' }]
        }]
      })
    });
    const data = await response.json();
    const text = data?.candidates?.[0]?.content?.parts?.[0]?.text || '';
    const jsonText = text.match(/\{[\s\S]*\}/)?.[0];
    if (!jsonText) return localGenerate(transcript, locationName, objectId);
    const parsed = JSON.parse(jsonText);
    const fallback = localGenerate(transcript, locationName, objectId);
    return {
      title: parsed.title || fallback.title,
      description: parsed.description || fallback.description,
      category: inferCategory(parsed.category || transcript),
    };
  } catch {
    return localGenerate(transcript, locationName, objectId);
  }
}

export default function VoiceReportPage() {
  const navigate = useNavigate();
  const { createIssueOnServer } = useData();
  const recognitionRef = useRef<any>(null);
  const cameraRef = useRef<HTMLInputElement>(null);
  const [listening, setListening] = useState(false);
  const [busy, setBusy] = useState(false);
  const [submittedId, setSubmittedId] = useState<string | null>(null);
  const [transcript, setTranscript] = useState('There is water leakage near pipe PC09 outside Koramangala 5th Block. The road is flooding.');
  const [locationName, setLocationName] = useState('Koramangala 5th Block, Bengaluru');
  const [objectId, setObjectId] = useState('PC09');
  const [photoBase64, setPhotoBase64] = useState('');
  const [generated, setGenerated] = useState(() => localGenerate(transcript, locationName, objectId));

  const supportSpeech = Boolean(window.SpeechRecognition || window.webkitSpeechRecognition);
  const category = useMemo(() => inferCategory(transcript + ' ' + generated.category), [transcript, generated.category]);
  const assignedDept = departmentForCategory(category);
  const priorityScore = calculatePriority({ severity: category === 'Water' || category === 'Sewage' ? 8 : 6, upvotes: 1, duplicateReports: 1 });

  const startListening = () => {
    const Recognition = window.SpeechRecognition || window.webkitSpeechRecognition;
    if (!Recognition) return;
    const recognition = new Recognition();
    recognition.lang = 'en-IN';
    recognition.interimResults = true;
    recognition.continuous = true;
    recognition.onresult = (event: any) => {
      const text = Array.from(event.results).map((r: any) => r[0].transcript).join(' ');
      setTranscript(text);
      const detected = extractObjectId(text);
      if (detected) setObjectId(detected);
    };
    recognition.onend = () => setListening(false);
    recognitionRef.current = recognition;
    setListening(true);
    recognition.start();
  };

  const stopListening = () => {
    recognitionRef.current?.stop();
    setListening(false);
  };

  const handleGenerate = async () => {
    setBusy(true);
    const detectedObject = objectId || extractObjectId(transcript);
    if (detectedObject) setObjectId(detectedObject);
    const result = await generateWithGemini(transcript, locationName, detectedObject);
    setGenerated(result);
    setBusy(false);
  };

  const handlePhoto = (file?: File) => {
    if (!file) return;
    const reader = new FileReader();
    reader.onload = () => setPhotoBase64(String(reader.result));
    reader.readAsDataURL(file);
  };

  const submitIssue = async () => {
    setBusy(true);
    const severity = category === 'Water' || category === 'Sewage' ? 8 : category === 'Pothole' ? 7 : 6;
    const score = calculatePriority({ severity, upvotes: 1, duplicateReports: 1 });
    const issue: Issue = {
      id: 'NN-V-' + Date.now(),
      title: generated.title,
      description: generated.description,
      category,
      status: 'Open',
      severity,
      riskLevel: riskForScore(score),
      assignedDept,
      locationName,
      latitude: DEFAULT_COORDS.latitude + (Math.random() - 0.5) / 1000,
      longitude: DEFAULT_COORDS.longitude + (Math.random() - 0.5) / 1000,
      upvotes: 1,
      timePosted: new Date().toISOString(),
      ward: 'Voice Intake',
      slaStatus: 'SLA OK',
      priorityScore: score,
      daysOpen: 0,
      aiConfidence: import.meta.env.VITE_GEMINI_API_KEY ? 92 : 84,
      photoBase64,
      objectId: objectId || extractObjectId(transcript),
      duplicateReports: 1,
      voiceTranscript: transcript,
      source: 'Voice Agent',
      reporterName: 'Voice Agent Citizen',
    };
    const saved = await createIssueOnServer(issue);
    setSubmittedId(saved.id);
    setBusy(false);
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      <Header title="Voice Intake" subtitle="Hands-free citizen report capture with camera evidence and AI routing" />
      <div className="page-body fade-in">
        <div className="voice-grid">
          <section className="voice-panel">
            <div className="voice-panel-header">
              <div>
                <div className="card-title"><Mic size={16} /> Voice Agent</div>
                <p className="voice-muted">Speak the issue, confirm the location, capture a photo, then submit.</p>
              </div>
              <span className={'badge ' + (listening ? 'badge-escalated' : 'badge-progress')}>{listening ? 'Listening' : 'Ready'}</span>
            </div>

            <textarea className="input voice-textarea" value={transcript} onChange={e => setTranscript(e.target.value)} />
            <div className="voice-actions">
              <button className="btn btn-primary" onClick={listening ? stopListening : startListening} disabled={!supportSpeech}>
                {listening ? <Square size={15} /> : <Mic size={15} />} {listening ? 'Stop' : 'Speak'}
              </button>
              <button className="btn btn-secondary" onClick={handleGenerate} disabled={busy || !transcript.trim()}>
                {busy ? <Loader2 size={15} className="spin" /> : <Wand2 size={15} />} Generate Fields
              </button>
              <button className="btn btn-secondary" onClick={() => cameraRef.current?.click()}>
                <Camera size={15} /> Open Camera
              </button>
            </div>
            {!supportSpeech && <p className="voice-muted">Speech recognition is not available in this browser; typed intake still works.</p>}

            <div className="grid-2 voice-form-grid">
              <label className="input-group">
                <span className="input-label">Exact Location</span>
                <input className="input" value={locationName} onChange={e => setLocationName(e.target.value)} />
              </label>
              <label className="input-group">
                <span className="input-label">Object ID</span>
                <input className="input" value={objectId} onChange={e => setObjectId(e.target.value.toUpperCase())} placeholder="PC09, MH12, pole 42" />
              </label>
            </div>

            <input ref={cameraRef} type="file" accept="image/*" capture="environment" style={{ display: 'none' }} onChange={e => handlePhoto(e.target.files?.[0])} />
            {photoBase64 && <img className="voice-photo" src={photoBase64} alt="Captured issue evidence" />}
          </section>

          <section className="voice-panel">
            <div className="voice-panel-header">
              <div>
                <div className="card-title"><Sparkles size={16} /> Generated Report</div>
                <p className="voice-muted">Gemini is used when configured; otherwise a local civic classifier fills the fields.</p>
              </div>
              <span className="badge badge-high">{assignedDept}</span>
            </div>

            <label className="input-group">
              <span className="input-label">Title</span>
              <input className="input" value={generated.title} onChange={e => setGenerated(g => ({ ...g, title: e.target.value }))} />
            </label>
            <label className="input-group mt-3">
              <span className="input-label">Description</span>
              <textarea className="input voice-description" value={generated.description} onChange={e => setGenerated(g => ({ ...g, description: e.target.value }))} />
            </label>

            <div className="voice-summary">
              <div><span>Category</span><strong>{category}</strong></div>
              <div><span>Priority</span><strong>{priorityScore}/100</strong></div>
              <div><span>Object</span><strong>{objectId || 'Not detected'}</strong></div>
              <div><span>Route</span><strong>{assignedDept}</strong></div>
            </div>

            <button className="btn btn-primary btn-lg w-full" onClick={submitIssue} disabled={busy || !generated.title || !generated.description || !locationName}>
              {busy ? <Loader2 size={16} className="spin" /> : <Send size={16} />} Submit Issue
            </button>

            {submittedId && (
              <button className="voice-success" onClick={() => navigate('/reports/' + submittedId)}>
                <CheckCircle2 size={18} /> Submitted as {submittedId}. Open report
              </button>
            )}
          </section>
        </div>

        <section className="voice-panel mt-4">
          <div className="card-title"><MapPin size={16} /> Automation Opportunities</div>
          <div className="automation-list">
            <div>Auto-escalate clustered health and safety risks to department heads when duplicate reports cross 3.</div>
            <div>Send field-officer assignment suggestions based on ward, department, SLA, and current workload.</div>
            <div>Generate citizen follow-up notifications when DuplicateBot merges their report into an existing case.</div>
          </div>
        </section>
      </div>
    </div>
  );
}
