import React, { useState } from 'react'
import { register } from './auth'

export default function Register({ onRegistered, onSwitchToLogin }) {
    const [username, setUsername] = useState('')
    const [password, setPassword] = useState('')
    const [error, setError] = useState('')

    async function handleRegister(e) {
        e.preventDefault()
        setError('')
        try {
            await register(username, password)
            onRegistered(username)
        } catch (err) {
            setError("Registration failed: " + (err?.response?.data || "Unknown error"))
        }
    }

    return (
        <div style={{padding:20}}>
            <h2>Register</h2>

            {error && <div style={{color:'red', marginBottom:10}}>{error}</div>}

            <form onSubmit={handleRegister}>
                <input
                    placeholder="Username"
                    value={username}
                    onChange={e => setUsername(e.target.value)}
                    style={{display:'block', marginBottom:10}}
                />
                <input
                    placeholder="Password"
                    value={password}
                    type="password"
                    onChange={e => setPassword(e.target.value)}
                    style={{display:'block', marginBottom:10}}
                />
                <button>Register</button>
            </form>

            <button onClick={onSwitchToLogin} style={{marginTop:10}}>
                Already have an account? Login →
            </button>
        </div>
    )
}
