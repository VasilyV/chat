// src/ws.js
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

let client = null;
let subscription = null;

export function connectWebSocket(roomId, onMessage) {
    if (!client) {
        client = new Client({
            webSocketFactory: () => new WebSocket('ws://localhost:8080/ws-chat'),
            reconnectDelay: 3000,
        });
        client.activate();
    }

    client.onConnect = () => {

        if (subscription) {
            subscription.unsubscribe();
        }

        subscription = client.subscribe(`/topic/rooms/${roomId}`, msg => {
            onMessage(JSON.parse(msg.body));
        });
    };
}

export function disconnectWebSocket() {
    if (subscription) {
        subscription.unsubscribe();
    }
    if (client) {
        client.deactivate();
        client = null;
    }
}


export function sendMessage(roomId, content, sender) {
    if (!client || !client.connected) {
        console.warn('sendMessage called but STOMP client not connected');
        return;
    }

    const payload = { roomId, content, sender };

    client.publish({
        destination: '/app/chat.sendMessage', // MUST match @MessageMapping
        body: JSON.stringify(payload),
    });
}
