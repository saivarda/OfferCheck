import { useState } from "react";
import axios from "axios";
import "./App.css";

const API = "http://127.0.0.1:8080/api";

const LEVELS = ["Junior", "Mid", "Senior", "Staff", "Principal"];
const ROLES = ["Software Engineer", "Frontend Engineer", "Backend Engineer", "Full Stack Engineer", "DevOps Engineer", "Data Engineer", "ML Engineer", "Product Manager", "Designer"];

export default function App() {
  const [step, setStep] = useState("form");
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState(null);
  const [form, setForm] = useState({
    role: "Software Engineer",
    company: "",
    location: "",
    yearsOfExperience: "",
    baseSalary: "",
    bonus: "",
    equity: "",
    benefits: "",
    level: "Senior"
  });

  const update = (k, v) => setForm(f => ({ ...f, [k]: v }));

  const analyze = async () => {
    if (!form.company || !form.location || !form.baseSalary) return;
    setLoading(true);
    try {
      const res = await axios.post(`${API}/analyze`, {
        ...form,
        yearsOfExperience: parseInt(form.yearsOfExperience) || 0,
        baseSalary: parseFloat(form.baseSalary) || 0,
        bonus: parseFloat(form.bonus) || 0,
        equity: parseFloat(form.equity) || 0,
      });
      setResult(res.data);
      setStep("result");
    } catch (e) {
      alert("Something went wrong. Please try again.");
    }
    setLoading(false);
  };

  const fmt = (n) => new Intl.NumberFormat("en-US", { style: "currency", currency: "USD", maximumFractionDigits: 0 }).format(n);

  const pct = result ? Math.min(Math.max(result.percentile, 5), 95) : 0;

  const verdictColor = result?.verdict === "LOWBALLED" ? "#ff4444" : result?.verdict === "ABOVE_MARKET" ? "#1db954" : "#f5a623";

  return (
    <div className="app">
      <nav className="nav">
        <div className="nav-brand">
          <span className="nav-icon">💰</span>
          <span className="nav-name">OfferCheck</span>
        </div>
        {step === "result" && (
          <button className="nav-back" onClick={() => setStep("form")}>← New Analysis</button>
        )}
      </nav>

      {step === "form" && (
        <div className="page fade-in">
          <div className="hero">
            <h1 className="hero-title">Did you get<br /><span className="hero-accent">lowballed?</span></h1>
            <p className="hero-sub">Paste your offer. Get the truth in seconds.</p>
          </div>

          <div className="form-card">
            <div className="form-row">
              <div className="field">
                <label className="label">Role</label>
                <select className="input" value={form.role} onChange={e => update("role", e.target.value)}>
                  {ROLES.map(r => <option key={r}>{r}</option>)}
                </select>
              </div>
              <div className="field">
                <label className="label">Level</label>
                <select className="input" value={form.level} onChange={e => update("level", e.target.value)}>
                  {LEVELS.map(l => <option key={l}>{l}</option>)}
                </select>
              </div>
            </div>

            <div className="form-row">
              <div className="field">
                <label className="label">Company</label>
                <input className="input" placeholder="e.g. Google" value={form.company} onChange={e => update("company", e.target.value)} />
              </div>
              <div className="field">
                <label className="label">Location</label>
                <input className="input" placeholder="e.g. San Francisco" value={form.location} onChange={e => update("location", e.target.value)} />
              </div>
            </div>

            <div className="field">
              <label className="label">Years of Experience</label>
              <input className="input" type="number" placeholder="e.g. 5" value={form.yearsOfExperience} onChange={e => update("yearsOfExperience", e.target.value)} />
            </div>

            <div className="divider"><span>Compensation</span></div>

            <div className="form-row">
              <div className="field">
                <label className="label">Base Salary ($)</label>
                <input className="input" type="number" placeholder="e.g. 150000" value={form.baseSalary} onChange={e => update("baseSalary", e.target.value)} />
              </div>
              <div className="field">
                <label className="label">Annual Bonus ($)</label>
                <input className="input" type="number" placeholder="e.g. 20000" value={form.bonus} onChange={e => update("bonus", e.target.value)} />
              </div>
            </div>

            <div className="form-row">
              <div className="field">
                <label className="label">Total Equity ($)</label>
                <input className="input" type="number" placeholder="e.g. 200000" value={form.equity} onChange={e => update("equity", e.target.value)} />
              </div>
              <div className="field">
                <label className="label">Benefits</label>
                <input className="input" placeholder="e.g. Health, 401k" value={form.benefits} onChange={e => update("benefits", e.target.value)} />
              </div>
            </div>

            <button className="btn-analyze" onClick={analyze} disabled={loading || !form.company || !form.location || !form.baseSalary}>
              {loading ? <span className="spinner" /> : "Analyze My Offer →"}
            </button>
          </div>
        </div>
      )}

      {step === "result" && result && (
        <div className="page fade-in">
          <div className="verdict-card" style={{ borderColor: verdictColor }}>
            <div className="verdict-emoji">{result.verdictEmoji}</div>
            <div className="verdict-label" style={{ color: verdictColor }}>{result.verdict.replace("_", " ")}</div>
            <div className="verdict-msg">{result.verdictMessage}</div>
            <div className="total-comp">{fmt(result.totalComp)} <span className="tc-label">total comp</span></div>
          </div>

          <div className="market-card">
            <h3 className="card-title">Market Comparison</h3>
            <div className="market-bar-wrap">
              <div className="market-bar">
                <div className="market-fill" style={{ width: `${pct}%`, background: verdictColor }} />
                <div className="market-thumb" style={{ left: `${pct}%`, borderColor: verdictColor }} />
              </div>
              <div className="market-labels">
                <span>P25 {fmt(result.marketP25)}</span>
                <span>Median {fmt(result.marketMedian)}</span>
                <span>P75 {fmt(result.marketP75)}</span>
              </div>
            </div>
            <div className="percentile-badge" style={{ background: verdictColor + "22", color: verdictColor }}>
              You're in the <strong>{result.percentile}th percentile</strong>
            </div>
          </div>

          <div className="breakdown-card">
            <h3 className="card-title">Comp Breakdown</h3>
            <p className="breakdown-text">{result.breakdown}</p>
          </div>

          <div className="script-card">
            <h3 className="card-title">✉️ Your Negotiation Script</h3>
            <p className="script-sub">AI-generated based on your offer and market data</p>
            <div className="script-body">{result.negotiationScript}</div>
            <button className="btn-copy" onClick={() => navigator.clipboard.writeText(result.negotiationScript)}>
              Copy Script
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
