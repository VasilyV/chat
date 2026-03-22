import React, { useEffect, useRef, useState } from 'react';
import { connectWebSocket, disconnectWebSocket, sendMessage } from '../ws';
import { api } from '../api';
import { logout } from '../auth';

const PAGE_SIZE = 50;

export default function Chat({ user }) {
  const [room, setRoom] = useState('general');
  const [messages, setMessages] = useState([]);
  const [text, setText] = useState('');


  const [cursor, setCursor] = useState(null);
  const [hasMore, setHasMore] = useState(true);
  const [loadingHistory, setLoadingHistory] = useState(false);

  const listRef = useRef(null);

  async function loadHistory({ reset = false } = {}) {
    if (loadingHistory) return;
    if (!reset && !hasMore) return;

    setLoadingHistory(true);
    try {
      const params = {
        limit: PAGE_SIZE,
      };

      if (!reset && cursor?.createdAt && cursor?.id != null) {
        params.cursorCreatedAt = cursor.createdAt;
        params.cursorId = cursor.id;
      }

      const r = await api.get(`/api/chat/rooms/${encodeURIComponent(room)}/messages`, { params });

      const data = r.data || {};
      const contentNewestFirst = Array.isArray(data.content) ? data.content : [];

      const chunkAscending = [...contentNewestFirst].reverse();

      setMessages((prev) => {
        if (reset) return chunkAscending;
        return [...chunkAscending, ...prev];
      });

      setHasMore(!!data.hasMore);
      setCursor(data.nextCursor || null);
    } finally {
      setLoadingHistory(false);
    }
  }

  useEffect(() => {
    setMessages([]);
    setCursor(null);
    setHasMore(true);

    loadHistory({ reset: true }).then(() => {
      const el = listRef.current;
      if (el) el.scrollTop = el.scrollHeight;
    });

    connectWebSocket(room, (msg) => {
      setMessages((prev) => [...prev, msg]);

      const el = listRef.current;
      if (!el) return;
      const nearBottom = el.scrollHeight - el.scrollTop - el.clientHeight < 120;
      if (nearBottom) {
        requestAnimationFrame(() => {
          el.scrollTop = el.scrollHeight;
        });
      }
    });

    return () => {
      disconnectWebSocket();
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [room]);

  function onScroll() {
    const el = listRef.current;
    if (!el || loadingHistory || !hasMore) return;

    if (el.scrollTop < 80) {
      const prevHeight = el.scrollHeight;

      loadHistory().then(() => {
        requestAnimationFrame(() => {
          const newHeight = el.scrollHeight;
          el.scrollTop = newHeight - prevHeight + el.scrollTop;
        });
      });
    }
  }

  function handleSend(e) {
    e.preventDefault();
    if (!text.trim()) return;
    sendMessage(room, text, user.username);
    setText('');
  }

  async function handleLogout() {
    await logout();
    window.location.reload();
  }

  return (
    <div style={{ padding: 20 }}>
      <h3>Logged in as {user.username}</h3>
      <button onClick={handleLogout}>Logout</button>

      <div style={{ marginTop: 20 }}>
        <label>Select Room: </label>
        <select value={room} onChange={(e) => setRoom(e.target.value)}>
          <option value="general">general</option>
          <option value="sports">sports</option>
          <option value="music">music</option>
          <option value="finland">finland</option>
          <option value="cats">cats</option>
        </select>
      </div>

      <div
        ref={listRef}
        onScroll={onScroll}
        style={{
          border: '1px solid #ccc',
          height: 400,
          overflowY: 'scroll',
          padding: 10,
          marginTop: 10,
        }}
      >
        {loadingHistory && messages.length === 0 && <div>Loading…</div>}

        {hasMore && messages.length > 0 && (
          <div style={{ textAlign: 'center', opacity: 0.7, padding: 6 }}>
            Scroll up to load older messages…
          </div>
        )}

        {messages.map((m, i) => (
          <div key={i}>
            <b>{m.sender}:</b> {m.content}
          </div>
        ))}
      </div>

      <form onSubmit={handleSend} style={{ marginTop: 10 }}>
        <input
          value={text}
          onChange={(e) => setText(e.target.value)}
          placeholder="Type..."
          style={{ width: '80%' }}
        />
        <button type="submit">Send</button>
      </form>
    </div>
  );
}
