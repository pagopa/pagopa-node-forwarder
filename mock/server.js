const fs = require("fs");
const https = require("https");
const options = {
  key: fs.readFileSync(`${__dirname}/certs/server-key.pem`),
  cert: fs.readFileSync(`${__dirname}/certs/server-crt.pem`),
   ca: [
     fs.readFileSync(`${__dirname}/certs/client-ca-crt.pem`),
//     fs.readFileSync(`${__dirname}/dev/api-platform-pagopa-it-chain.pem`)
     fs.readFileSync(`/Users/pasqualespica/my_data/__TEMP/pagopa-node-forwarder/scripts/api-dev-platform-pagopa-it-catena.pem`),
     fs.readFileSync(`/Users/pasqualespica/my_data/__TEMP/pagopa-node-forwarder/scripts/api-uat-platform-pagopa-it-catena.pem`)
   ],
  // Requesting the client to provide a certificate, to authenticate.
  requestCert: true,
  // As specified as "true", so no unauthenticated traffic
  // will make it to the specified route specified
  rejectUnauthorized: true
};
const port = 443;
console.log(`Listen on port ${port}...`);
https
//    .createServer(options)
  .createServer(options, function(req, res) {
    console.log(
      new Date() +
        " " +
        req.connection.remoteAddress +
        " " +
        req.method +
        " " +
        req.url +
        "- CN " +
        req.socket.getPeerCertificate().subject.CN
    );
//    console.log(req.socket.getPeerCertificate())
    res.writeHead(200);
    //res.end("OK!\n");
    res.end("OK!\n");
  })
//  .on('request', (request, response) => {
//      let body = [];
//      request.on('data', (chunk) => {
//        body.push(chunk);
//      }).on('end', () => {
//        body = Buffer.concat(body).toString();
//        console.log(`==== ${request.method} ${request.url}`);
//        console.log('> Headers');
//        console.log(request.headers);
//        console.log('> Body');
//        console.log(body);
//        response.end(body);
//      });
//    })
  .listen(port);
