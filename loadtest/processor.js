const fs = require("fs");

const tokens = fs.readFileSync("./tokens.jsonl", "utf8")
    .trim()
    .split("\n")
    .map(line => JSON.parse(line));

module.exports = {
    pickToken(context, events, done) {
        const t = tokens[Math.floor(Math.random() * tokens.length)];
        ontext.vars.username = t.username;
        context.vars.token = t.token;
        console.log("context.vars.token: " + context.vars.token)
        return done(null, {});          // IMPORTANT FIX
    }

    // stompConnect(context, events, done) {
    //     context.ws.send("CONNECT\naccept-version:1.2\nhost:localhost\n\n\0");
    //     return done(null, {});          // IMPORTANT FIX
    // },
    //
    // stompSubscribe(context, events, done) {
    //     context.ws.send("SUBSCRIBE\nid:sub-1\ndestination:/topic/rooms/general\n\n\0");
    //     return done(null, {});          // IMPORTANT FIX
    // },
    //
    // stompSendMessage(context, events, done) {
    //     const payload = JSON.stringify({
    //         roomId: "general",
    //         content: "Hello from artillery " + Math.random()
    //     });
    //
    //     const frame =
    //         "SEND\ndestination:/app/chat.sendMessage\n\n{\"roomId\":\"general\",\"content\":\"Hello from Artillery\"}\0";
    //
    //     context.ws.send(frame);
    //     return done(null, {});          // IMPORTANT FIX
    // }
};
