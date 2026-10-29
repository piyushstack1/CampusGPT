import axios from 'axios';
import React, { useEffect, useRef, useState } from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter, NavLink, Route, Routes } from 'react-router-dom';
import ReactMarkdown from 'react-markdown';
import { BookOpen, FileText, GraduationCap, LayoutDashboard, MessageSquare, Settings as SettingsIcon, Upload, Send, Trash2 } from 'lucide-react';
import { api } from './services/api';
import './styles.css';

type Doc = { id:number; filename:string; fileSize:number; status:string; errorMessage?:string; createdAt:string };
type Source = { documentId:number; filename:string; pageNumber?:number; chunkId:string; excerpt:string };
type Msg = { id?:number; role:string; content:string; createdAt?:string; sources?:Source[] };
type QuizQuestion = { question:string; options:string[]; correctOption:number; explanation:string };
type Quiz = { questions:QuizQuestion[] };
type IndexedDoc = { id:number; filename:string; status:string };

function errorMessage(error:unknown, fallback:string) {
  if (axios.isAxiosError(error) && typeof error.response?.data?.message === 'string') {
    return error.response.data.message;
  }
  return fallback;
}

function Shell({children}:{children:React.ReactNode}) {
  const nav = [['/', LayoutDashboard, 'Dashboard'], ['/documents', FileText, 'Documents'], ['/chat', MessageSquare, 'AI Assistant'], ['/quiz', GraduationCap, 'Quiz'], ['/settings', SettingsIcon, 'Settings']] as const;
  return <div className="app"><aside><div className="brand"><BookOpen/>CampusGPT</div>{nav.map(([to,Icon,label])=><NavLink key={to} to={to} end={to==='/'}><Icon size={18}/>{label}</NavLink>)}</aside><main>{children}</main></div>;
}

function Dashboard() {
  const [data,setData]=useState<{totalDocuments:number;indexedDocuments:number;totalConversations:number;recentDocuments:Doc[]}>({totalDocuments:0,indexedDocuments:0,totalConversations:0,recentDocuments:[]});
  const [error,setError]=useState('');
  useEffect(()=>{api.get('/dashboard').then(r=>setData(r.data)).catch(e=>setError(errorMessage(e,'Could not load dashboard.')));},[]);
  return <section><h1>Academic Knowledge Dashboard</h1>{error&&<div className="error">{error}</div>}<div className="metrics"><Metric label="Documents" value={data.totalDocuments}/><Metric label="Indexed" value={data.indexedDocuments}/><Metric label="Conversations" value={data.totalConversations}/></div><div className="panel"><h2>Recent documents</h2>{data.recentDocuments.length?data.recentDocuments.map(d=><DocRow key={d.id} doc={d}/>):<Empty text="Upload PDFs to begin grounded question answering."/>}</div></section>;
}
function Metric(p:{label:string;value:number}){return <div className="metric"><span>{p.label}</span><strong>{p.value}</strong></div>}
function Empty({text}:{text:string}){return <div className="empty">{text}</div>}
function DocRow({doc,onDelete}:{doc:Doc;onDelete?:(id:number)=>void}){return <div className="row"><FileText size={18}/><div><b>{doc.filename}</b><small>{Math.round(doc.fileSize/1024)} KB</small></div><span className={'status '+doc.status.toLowerCase()}>{doc.status}</span>{onDelete&&<button className="icon" onClick={()=>onDelete(doc.id)}><Trash2 size={16}/></button>}</div>}

function Documents() {
  const [docs,setDocs]=useState<Doc[]>([]), [busy,setBusy]=useState(false), [err,setErr]=useState('');
  const load=()=>api.get('/documents').then(r=>setDocs(r.data)).catch(e=>setErr(errorMessage(e,'Could not load documents.')));
  useEffect(()=>{load();},[]);
  const upload=async(e:React.ChangeEvent<HTMLInputElement>)=>{const f=e.target.files?.[0]; if(!f)return; setBusy(true); setErr(''); const fd=new FormData(); fd.append('file',f); try{await api.post('/documents/upload',fd); await load();}catch(x){setErr(errorMessage(x,'Upload failed.'));}finally{setBusy(false); e.target.value='';}};
  const del=async(id:number)=>{setErr(''); try{await api.delete('/documents/'+id); await load();}catch(e){setErr(errorMessage(e,'Could not delete document.'));}};
  return <section><h1>Documents</h1><label className="upload"><Upload/> <span>{busy?'Processing PDF...':'Upload PDF'}</span><input type="file" accept="application/pdf" onChange={upload} disabled={busy}/></label>{err&&<div className="error">{err}</div>}<div className="panel">{docs.length?docs.map(d=><DocRow key={d.id} doc={d} onDelete={del}/>):<Empty text="No documents uploaded yet."/>}</div></section>;
}

function Chat() {
  const [question,setQuestion]=useState(''), [messages,setMessages]=useState<Msg[]>([]), [busy,setBusy]=useState(false), [session,setSession]=useState<number|undefined>(); const end=useRef<HTMLDivElement>(null);
  useEffect(()=>{end.current?.scrollIntoView({behavior:'smooth'});},[messages]);
  const ask=async()=>{if(!question.trim()||busy)return; const q=question; setQuestion(''); setMessages(m=>[...m,{role:'USER',content:q}]); setBusy(true); try{const r=await api.post('/chat/ask',{question:q,sessionId:session,documentIds:[]}); setSession(r.data.sessionId); setMessages(m=>[...m,{role:'ASSISTANT',content:r.data.answer,sources:r.data.sources}]);}catch(e){setMessages(m=>[...m,{role:'ASSISTANT',content:errorMessage(e,'Request failed.')}]);}finally{setBusy(false);}};
  return <section className="chat"><h1>AI Assistant</h1><div className="messages">{messages.length?messages.map((m,i)=><div className={'msg '+m.role.toLowerCase()} key={i}><ReactMarkdown>{m.content}</ReactMarkdown>{m.sources?.length&&<small className="source">Source: {m.sources.map(s=>`${s.filename}${s.pageNumber?` (page ${s.pageNumber})`:''}`).join(', ')}</small>}</div>):<Empty text="Ask a question after uploading and indexing a PDF."/>}{busy&&<div className="msg assistant">Searching documents and generating an answer...</div>}<div ref={end}/></div><div className="composer"><input value={question} onChange={e=>setQuestion(e.target.value)} onKeyDown={e=>e.key==='Enter'&&ask()} placeholder="Explain normalization in DBMS"/><button onClick={ask} disabled={busy}><Send size={18}/>Send</button></div></section>
}

function Quiz(){
  const [docs,setDocs]=useState<IndexedDoc[]>([]);
  const [doc,setDoc]=useState('');
  const [count,setCount]=useState(5);
  const [out,setOut]=useState<Quiz>();
  const [answers,setAnswers]=useState<Record<number, number>>({});
  const [revealed,setRevealed]=useState<Record<number, boolean>>({});
  const [error,setError]=useState('');
  useEffect(()=>{
    api.get<Doc[]>('/documents')
      .then(r=>{
        const indexed = r.data.filter(d=>d.status==='INDEXED').map(d=>({id:d.id,filename:d.filename,status:d.status}));
        setDocs(indexed);
        if(!doc && indexed.length){setDoc(String(indexed[0].id));}
      })
      .catch(e=>setError(errorMessage(e,'Could not load indexed documents.')));
  },[]);
  const generate=async()=>{
    const id=Number(doc);
    if(!Number.isInteger(id)||id<1){setError('Select a valid indexed document.');return;}
    if(!Number.isInteger(count)||count<1||count>20){setError('Question count must be between 1 and 20.');return;}
    setError('');
    setAnswers({});
    setRevealed({});
    try{
      setOut((await api.post<Quiz>('/quiz/generate',{documentId:id,questionCount:count})).data);
    }catch(e){
      setError(errorMessage(e,'Could not generate quiz.'));
    }
  };
  return <section><h1>Quiz Generator</h1><div className="panel form"><select value={doc} onChange={e=>setDoc(e.target.value)}>{docs.length?docs.map(d=><option key={d.id} value={d.id}>{d.id} — {d.filename}</option>):<option value="">No indexed documents available</option>}</select><input type="number" min={1} max={20} value={count} onChange={e=>setCount(Number(e.target.value))} placeholder="Question count"/><button onClick={generate} disabled={!docs.length}>Generate</button></div>{error&&<div className="error">{error}</div>}{out&&<div className="panel"><h2>Generated quiz</h2>{out.questions.map((q,i)=><div className="quiz" key={i}><h3>{i+1}. {q.question}</h3><div className="options">{q.options.map((option,index)=>{const selected=answers[i]===index; const correct=q.correctOption===index; const show=revealed[i]; const state=show?(correct?'correct':selected?'wrong':''):(selected?'selected':''); return <button key={index} type="button" className={'option '+state} onClick={()=>setAnswers(a=>({...a,[i]:index}))}>{String.fromCharCode(65+index)}. {option}</button>;})}</div><div className="quiz-actions"><button type="button" onClick={()=>setRevealed(r=>({...r,[i]:true}))}>Show answer</button></div>{revealed[i]&&<p><b>Answer:</b> {String.fromCharCode(65+q.correctOption)}. {q.explanation}</p>}</div>)}</div>}</section>
}
function Settings(){
  const apiUrl=import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8081/api';
  return <section><h1>Settings</h1><div className="settings-grid"><div className="panel"><h2>Application</h2><div className="setting-row"><span>Application</span><strong>CampusGPT</strong></div><div className="setting-row"><span>Mode</span><strong>Local AI</strong></div><div className="setting-row"><span>Frontend</span><strong>React + Vite</strong></div></div><div className="panel"><h2>Connected services</h2><div className="setting-row"><span>Backend API</span><code>{apiUrl}</code></div><div className="setting-row"><span>AI provider</span><strong>Ollama</strong></div><div className="setting-row"><span>Chat model</span><strong>llama3.2:3b</strong></div><div className="setting-row"><span>Embedding model</span><strong>nomic-embed-text</strong></div></div><div className="panel"><h2>How configuration works</h2><p className="muted">Backend and Ollama settings are managed through the environment configuration. API keys are not stored in the frontend.</p><p className="muted">Upload an indexed PDF before using grounded chat or quiz generation.</p></div></div></section>
}

createRoot(document.getElementById('root')!).render(<BrowserRouter><Shell><Routes><Route path="/" element={<Dashboard/>}/><Route path="/documents" element={<Documents/>}/><Route path="/chat" element={<Chat/>}/><Route path="/quiz" element={<Quiz/>}/><Route path="/settings" element={<Settings/>}/></Routes></Shell></BrowserRouter>);
