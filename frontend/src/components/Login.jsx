import React, { useState } from 'react'
import { login } from '../auth'

export default function Login({ onLoggedIn, onSwitchToRegister }) {
    const [username, setUsername] = useState('')
    const [password, setPassword] = useState('')
    const [error, setError] = useState('')

    async function handleLogin(e) {
        e.preventDefault()
        try {
            let r = await login(username, password)
            onLoggedIn(r.data)
        } catch (err) {
            setError('Invalid credentials')
        }
    }

    return (
        <div style={{padding:20}}>
            <h2>Login</h2>

            {error && <div style={{color:'red'}}>{error}</div>}

            <form onSubmit={handleLogin}>
                <input
                    placeholder="Username"
                    value={username}
                    onChange={e => setUsername(e.target.value)}
                    style={{display:'block', marginBottom:10}}
                />
                <input
                    type="password"
                    placeholder="Password"
                    value={password}
                    onChange={e => setPassword(e.target.value)}
                    style={{display:'block', marginBottom:10}}
                />
                <button>Login</button>
            </form>

            <button onClick={onSwitchToRegister} style={{marginTop:10}}>
                No account yet? Register →
            </button>
        </div>
    )
}
