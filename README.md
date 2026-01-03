# Chat App (Spring Boot + React)

A small real-time chat application  
Backend is **Spring Boot** (REST + WebSocket/STOMP), frontend is **React (Vite)** served by **Nginx**. Local development uses **Docker Compose** with **Postgres**, **Redis**, and **Kafka**.

---

## Features

- User registration + login
- Cookie-based auth (`HttpOnly`) with access token refresh
- Real-time chat via WebSocket + STOMP
- Persistent message history (Postgres)
- Message fanout via Redis pub/sub
- Kafka integration for message publishing (dev via container) to support scalability
- Local environment via Docker Compose

---

## Tech stack

**Backend**
- Java 21, Spring Boot
- Spring Security
- Postgres (persistence)
- Redis (pub/sub fanout)
- Kafka (message pipeline)
- WebSocket + STOMP

**Frontend**
- React + Vite
- Axios (`withCredentials`)
- Nginx (serving static build)

**Infra / Deployability**
- Docker + Docker Compose
- CloudFormation template(s) for AWS ECS Fargate + ALB (optional RDS/ElastiCache)

---

## Quick start

From the repository root:

```bash
docker compose up --build
```

Open:
- Frontend: http://localhost:3000
- Backend (via nginx): http://localhost:8080

To reset everything (including DB volumes):

```bash
docker compose down -v
docker compose up --build
```

---

## Architecture overview

```
Browser
  |
  |  http://localhost:3000
  v
Frontend (React build served by Nginx)
  |
  |  API/WebSocket calls (same origin / reverse proxy)
  v
Nginx (reverse proxy)
  |
  +--> Backend REST (Spring Boot)
  |
  +--> Backend WebSocket/STOMP (Spring Boot)
         |
         +--> Redis pub/sub fanout
         +--> Kafka publish (optional pipeline)
         +--> Postgres persistence
```

---

## Auth model (cookies + refresh)

This project uses **HttpOnly cookies**:

- `accessToken` cookie: short-lived, used on API requests
- `refreshToken` cookie: longer-lived, used only to get new access tokens

### How refresh works

Frontend Axios is configured with `withCredentials: true`, so browser cookies are sent automatically.

On `401` responses (for non-auth calls), the interceptor calls:

- `POST /api/auth/refresh`

Backend validates the refresh token and sets a **new `accessToken` cookie**, then the original request is retried.

### Logout / revoke

Logout clears cookies and revokes refresh token(s) server-side:

- `POST /api/auth/logout`

---

## Running tests

Backend tests:

```bash
cd backend
mvn test
```

---

## AWS 

CloudFormation templates:

- ECS Fargate
- Application Load Balancer (ALB)
- Optional: RDS Postgres + ElastiCache Redis

Template example:
- `cloudformation/ecs-fargate-safe.yml`

The template is **safe by default**:
- `EnableDeploy` defaults to `false`, it won’t create resources unless explicitly enabled.

---

### Backend starts before Kafka/DB is ready
Check logs:

```bash
docker compose logs -f backend
docker compose logs -f kafka
docker compose logs -f db
docker compose logs -f redis
```

---

