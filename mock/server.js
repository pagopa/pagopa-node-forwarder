const fs = require("fs");
const https = require("https");
const options = {
  key: fs.readFileSync(`${__dirname}/certs/server-key.pem`),
  cert: fs.readFileSync(`${__dirname}/certs/server-crt.pem`),
   ca: [
     fs.readFileSync(`${__dirname}/certs/client-ca-crt.pem`)
//     fs.readFileSync(`${__dirname}/dev/api-platform-pagopa-it-chain.pem`)
   ],
  // Requesting the client to provide a certificate, to authenticate.
  requestCert: true,
  // As specified as "true", so no unauthenticated traffic
  // will make it to the specified route specified
  rejectUnauthorized: true
};
const port = 8888;
console.log(`Listen on port ${port}...`);
https
  .createServer(options, function(req, res) {
    console.log(
      new Date() +
        " " +
        req.connection.remoteAddress +
        " " +
        req.method +
        " " +
        req.url +
        " " 
//        + req.socket.getPeerCertificate().subject
    );
    res.writeHead(200);
    res.end("OK!\n");
  })
  .listen(port);
