import React, { useState, useEffect } from 'react'
import Login from './components/Login'
import Register from './Register'
import Chat from './components/Chat'
import { api } from './api'

export default function App() {
    const [user, setUser] = useState(null)
    const [mode, setMode] = useState("login") // login | register

    useEffect(() => {
        api.get('/api/auth/me').then(res => {
            setUser(res.data)
        }).catch(() => {})
    }, [])

    function handleLoggedIn(tokens) {
        // after login, refresh /me
        api.get('/api/auth/me').then(res => setUser(res.data))
    }

    function handleRegistered(username) {
        alert("Registered successfully! Now log in.")
        setMode("login")
    }

    if (!user) {
        return mode === "login"
            ? <Login
                onLoggedIn={handleLoggedIn}
                onSwitchToRegister={() => setMode("register")}
            />
            : <Register
                onRegistered={handleRegistered}
                onSwitchToLogin={() => setMode("login")}
            />
    }

    return <Chat user={user}/>
}
