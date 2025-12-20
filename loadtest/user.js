// loadtest/user.js
const { v4: uuid } = require('uuid');

function randomUser() {
    return {
        username: "user_" + Date.now() + "_" + Math.floor(Math.random() * 99999),
        password: "test123"
    };
}

module.exports = { randomUser };
