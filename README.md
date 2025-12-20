# Chat App with Kafka (MSK) + Postgres (RDS) — Full package

## Architecture Overview

The chat system uses a combination of Kafka and RabbitMQ:

- **Kafka** (`chat-messages` topic)
   - Source of truth for all chat events
   - Durable, partitioned log
   - Suitable for replay, analytics, and backfills

- **RabbitMQ** (`chat.ws.exchange` TopicExchange)
   - Real-time fanout to all WebSocket nodes
   - Each backend instance creates its own anonymous queue bound with `room.*`
   - Every chat message is broadcast to all instances, so any node can deliver
     messages to its connected WebSocket clients

- **WebSockets (STOMP + SockJS)**
   - Clients send messages to `/app/chat.sendMessage`
   - Clients subscribe to `/topic/rooms/{roomId}` for real-time updates

### Message Flow

1. Client sends a message via WebSocket:
   - `SEND /app/chat.sendMessage`
2. `ChatController` receives the message and:
   - publishes JSON to Kafka (`chat-messages` topic)
   - publishes the same JSON to RabbitMQ (`chat.ws.exchange`, routing key `room.{roomId}`)
3. Each backend instance has a RabbitMQ listener:
   - consumes the message
   - persists it to PostgreSQL
   - broadcasts it over WebSocket to `/topic/rooms/{roomId}` using `SimpMessagingTemplate`
4. REST endpoint `/api/chat/rooms/{roomId}/messages` reads recent history from the database.

This separation allows us to scale:

- **Kafka**: horizontal scaling via partitions (we can partition by `roomId` to preserve order per room).
- **RabbitMQ**: decouples WebSocket fanout from Kafka, avoids session affinity, and allows us to scale WebSocket nodes horizontally.


### High-level Diagram

```mermaid
flowchart LR
    subgraph Client
        A[WebSocket Client]
    end

    subgraph Backend[Backend Instance(s)]
        C1[STOMP Controller<br/>/app/chat.sendMessage]
        C2[RabbitMQ Listener<br/>(ChatRabbitListener)]
        C3[REST Controller<br/>/api/chat/rooms/{roomId}/messages]
        DB[(PostgreSQL)]
    end

    subgraph MQ[Messaging]
        K[Kafka<br/>chat-messages topic]
        R[(RabbitMQ<br/>chat.ws.exchange)]
    end

    A -- SEND /app/chat.sendMessage --> C1
    C1 -- JSON --> K
    C1 -- JSON (room.{roomId}) --> R

    R -- broadcast --> C2
    C2 -- save --> DB
    C2 -- /topic/rooms/{roomId} --> A

    A -- HTTP GET /api/chat/rooms/{roomId}/messages --> C3
    C3 -- query --> DB




This archive contains a runnable (local) and AWS-deployable chat application:
- backend/: Spring Boot 3.2 (Java 21) with WebSocket STOMP endpoints, Kafka producer/consumer, JWT auth, Postgres persistence.
- frontend/: minimal placeholder for static hosting.
- docker-compose.yml : local Kafka (bitnami), Postgres, backend, frontend.
- ecs/: sample ECS task definitions (placeholders for MSK/RDS secrets).
- aws/cloudformation/: sample CloudFormation templates for MSK cluster and RDS Postgres (fill subnet/security IDs).
- .github/workflows/deploy-to-aws.yml : CI to build images, push to ECR, register ECS task defs and force redeploy.

How to run locally:
1. Export a base64 JWT secret:
   ```bash
   export APP_JWT_SECRET=$(openssl rand -base64 32)
   ```
2. Start services:
   ```bash
   docker-compose up --build
   ```
3. Backend API: http://localhost:8080
- POST /api/auth/login {"username":"alice","password":"x"}
- WebSocket endpoint: ws://localhost:8080/ws-chat (STOMP)
- Message flow: client sends to /app/chat.sendMessage -> backend publishes to Kafka topic 'chat-messages' -> backend Kafka consumer persists and broadcasts to /topic/rooms/{roomId}

Notes:
- Replace placeholder values in ecs/*.json and aws/cloudformation/*.yml before deploying to AWS.
- For AWS, prefer using Secrets Manager for DB password and JWT secret.
