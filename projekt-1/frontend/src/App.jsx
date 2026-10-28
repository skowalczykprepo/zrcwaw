import { useEffect, useRef, useState } from 'react';
import { api } from './api.js';

const POLL_INTERVAL_MS = 3000;

export default function App() {
  const [username, setUsername] = useState(() => localStorage.getItem('username') || `User${Math.floor(Math.random() * 1000)}`);
  const [messages, setMessages] = useState([]);
  const [content, setContent] = useState('');
  const [file, setFile] = useState(null);
  const [error, setError] = useState('');
  const [sending, setSending] = useState(false);
  const listRef = useRef(null);
  const fileInputRef = useRef(null);
  const lastTimestampRef = useRef(new Date(0).toISOString());

  useEffect(() => {
    localStorage.setItem('username', username);
  }, [username]);

  // Dopisuje nowe wiadomosci (bez duplikatow) i zapamietuje znacznik czasu ostatniej.
  function appendMessages(fresh) {
    if (!fresh.length) return;
    setMessages((prev) => {
      const known = new Set(prev.map((m) => m.id));
      const merged = [...prev, ...fresh.filter((m) => !known.has(m.id))];
      lastTimestampRef.current = merged[merged.length - 1].createdAt;
      return merged;
    });
  }

  // Pierwsze zaladowanie: wszystkie wiadomosci. Potem co 3 s dopytujemy tylko o nowsze.
  useEffect(() => {
    let cancelled = false;
    api.getAllMessages()
      .then((data) => !cancelled && appendMessages(data))
      .catch((e) => setError(e.message));

    const timer = setInterval(() => {
      api.getNewMessages(lastTimestampRef.current)
        .then((fresh) => !cancelled && appendMessages(fresh))
        .catch((e) => setError(e.message));
    }, POLL_INTERVAL_MS);

    return () => {
      cancelled = true;
      clearInterval(timer);
    };
  }, []);

  useEffect(() => {
    if (listRef.current) listRef.current.scrollTop = listRef.current.scrollHeight;
  }, [messages]);

  async function handleSend(event) {
    event.preventDefault();
    if (!content.trim() && !file) return;
    setSending(true);
    setError('');
    try {
      let fileId = null;
      if (file) {
        const uploaded = await api.uploadFile(file);
        fileId = uploaded.id;
      }
      const created = await api.sendMessage(username.trim(), content.trim() || file.name, fileId);
      appendMessages([created]);
      setContent('');
      setFile(null);
      if (fileInputRef.current) fileInputRef.current.value = '';
    } catch (e) {
      setError(e.message);
    } finally {
      setSending(false);
    }
  }

  async function handleLike(id) {
    try {
      const updated = await api.likeMessage(id);
      setMessages((prev) => prev.map((m) => (m.id === id ? updated : m)));
    } catch (e) {
      setError(e.message);
    }
  }

  return (
    <main className="chat">
      <header>
        <h1>Chat</h1>
        <label>
          Nick
          <input value={username} onChange={(e) => setUsername(e.target.value)} maxLength={64} />
        </label>
      </header>

      <section className="messages" ref={listRef}>
        {messages.length === 0 && <p className="empty">Brak wiadomości — napisz pierwszą.</p>}
        {messages.map((m) => (
          <article key={m.id} className={m.username === username ? 'message mine' : 'message'}>
            <div className="meta">
              <strong>{m.username}</strong>
              <time dateTime={m.createdAt}>{new Date(m.createdAt).toLocaleTimeString()}</time>
            </div>
            <p>{m.content}</p>
            {m.fileId && <Attachment id={m.fileId} />}
            <button type="button" className="like" onClick={() => handleLike(m.id)}>
              ♥ {m.likes}
            </button>
          </article>
        ))}
      </section>

      {error && <p className="error">{error}</p>}

      <form className="composer" onSubmit={handleSend}>
        <input
          value={content}
          onChange={(e) => setContent(e.target.value)}
          placeholder="Napisz wiadomość…"
          maxLength={2000}
        />
        <input ref={fileInputRef} type="file" accept="image/*,video/*,audio/*" onChange={(e) => setFile(e.target.files[0] || null)} />
        <button type="submit" disabled={sending}>
          {sending ? 'Wysyłanie…' : 'Wyślij'}
        </button>
      </form>
    </main>
  );
}

function Attachment({ id }) {
  const url = api.fileUrl(id);
  return (
    <a className="attachment" href={url} target="_blank" rel="noreferrer">
      <img src={url} alt="załącznik" onError={(e) => (e.currentTarget.style.display = 'none')} />
      <span>Otwórz załącznik</span>
    </a>
  );
}
