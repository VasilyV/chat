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

      onConnect: () => {
        subscribeToRoom(roomId);
      },
      onStompError: (frame) => {
        console.error('STOMP error', frame);
      },
      onWebSocketError: (e) => {
        console.error('WebSocket error', e);
      },
      debug: () => {},
    });

    client.activate();
    return;
  }

  if (client.connected) {
    subscribeToRoom(roomId);
  } else {
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
