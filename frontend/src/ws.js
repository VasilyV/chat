// src/ws.js
import { Client } from '@stomp/stompjs';

let client = null;
let subscription = null;
let messageHandler = null;

function subscribeToRoom(roomId) {
  if (!client || !client.connected) return;

  if (subscription) {
    try { subscription.unsubscribe(); } catch {}
    subscription = null;
  }

  subscription = client.subscribe(`/topic/rooms/${roomId}`, (msg) => {
    try {
      messageHandler?.(JSON.parse(msg.body));
    } catch (e) {
      console.error('Failed to parse WS message', e);
    }
  });
}

export function connectWebSocket(roomId, onMessage) {
  messageHandler = onMessage;

  if (!client) {
    client = new Client({
      webSocketFactory: () => new WebSocket('ws://localhost:8080/ws-chat'),
      reconnectDelay: 3000,

      // IMPORTANT: set callbacks here (before activate)
      onConnect: () => {
        subscribeToRoom(roomId);
      },
      onStompError: (frame) => {
        console.error('STOMP error', frame);
      },
      onWebSocketError: (e) => {
        console.error('WebSocket error', e);
      },
      debug: () => {}, // silence
    });

    client.activate();
    return;
  }

  // If client already exists:
  // - if connected: resubscribe immediately
  // - if not connected yet: onConnect will subscribe using the latest roomId
  if (client.connected) {
    subscribeToRoom(roomId);
  } else {
    // Ensure that when it connects, it subscribes to the *current* room
    client.onConnect = () => subscribeToRoom(roomId);
  }
}

export function disconnectWebSocket() {
  if (subscription) {
    try { subscription.unsubscribe(); } catch {}
    subscription = null;
  }
  if (client) {
    client.deactivate();
    client = null;
  }
  messageHandler = null;
}

export function sendMessage(roomId, content, sender) {
  if (!client || !client.connected) {
    console.warn('sendMessage called but STOMP client not connected');
    return;
  }

  const payload = { roomId, content, sender };

  client.publish({
    destination: '/app/chat.sendMessage',
    body: JSON.stringify(payload),
  });
}
