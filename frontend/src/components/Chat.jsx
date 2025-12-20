import React, { useEffect, useState } from 'react';
import {connectWebSocket, disconnectWebSocket, sendMessage} from '../ws';
import { api } from '../api';
import { logout } from '../auth';

export default function Chat({ user }) {
    const [room, setRoom] = useState('general');   // <-- now dynamic
    const [messages, setMessages] = useState([]);
    const [text, setText] = useState('');

    useEffect(() => {
        setMessages([]); // reset when room changes

        api.get(`/api/chat/rooms/${room}/messages`)
            .then(r => setMessages(r.data.reverse()))
            .catch(err => console.error('History load failed', err));

        connectWebSocket(room, msg => {
            setMessages(prev => [...prev, msg]);
        });

        return () => {
            disconnectWebSocket();   // <— cleanup
        };
    }, [room]); // IMPORTANT: re-run on room change

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
                <select
                    value={room}
                    onChange={e => setRoom(e.target.value)}
                >
                    <option value="general">general</option>
                    <option value="sports">sports</option>
                    <option value="music">music</option>
                    <option value="finland">finland</option>
                    <option value="cats">cats</option>
                </select>
            </div>

            <div
                style={{
                    border: '1px solid #ccc',
                    height: 400,
                    overflowY: 'scroll',
                    padding: 10,
                    marginTop: 10
                }}
            >
                {messages.map((m, i) => (
                    <div key={i}>
                        <b>{m.sender}:</b> {m.content}
                    </div>
                ))}
            </div>

            <form onSubmit={handleSend}>
                <input
                    value={text}
                    onChange={e => setText(e.target.value)}
                    placeholder="Type..."
                    style={{ width: '80%' }}
                />
                <button type="submit">Send</button>
            </form>
        </div>
    );
}
