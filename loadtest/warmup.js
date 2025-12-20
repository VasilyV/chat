const fs = require('fs');
const path = require('path');
const USERS_FILE = path.join(__dirname, 'users.jsonl');

function randomString() {
    return Math.random().toString(36).substring(2, 10);
}

module.exports = {
    generateUser: function (context, events, done) {
        const username = "u_" + randomString();
        const password = "p_" + randomString();

        context.vars.username = username;
        context.vars.password = password;

        return done();
    },

    saveUser: function (context, events, done) {
        const entry = JSON.stringify({
            username: context.vars.username,
            password: context.vars.password
        }) + "\n";

        fs.appendFileSync(USERS_FILE, entry);

        return done();
    }
};
