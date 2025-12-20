const fs = require('fs');
const axios = require('axios');
const users = fs.readFileSync('./users.jsonl','utf8').trim().split('\n').map(JSON.parse);

(async () => {
    let out = fs.createWriteStream('./tokens.jsonl');

    for (let u of users) {
        let res = await axios.post("http://localhost:8080/api/auth/login", {
            username: u.username,
            password: u.password
        });
        
        const token = res.headers["set-cookie"][0].replace("accessToken=", "").match(/^[A-Za-z0-9._-]+\.[A-Za-z0-9._-]+\.[A-Za-z0-9._-]+/)[0];
        console.log("TOKEN>>> " + token)

        out.write(JSON.stringify({
            username: u.username,
            token: token
        }) + "\n");
    }

    out.end();
})();
