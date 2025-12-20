import { api } from './api'

export async function register(username, password) {
    return api.post('/api/auth/register', {username, password})
}

export async function login(username, password) {
    return api.post('/api/auth/login', {username, password})
}

export async function logout() {
    return api.post('/api/auth/logout')
}
