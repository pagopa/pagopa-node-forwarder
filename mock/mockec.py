import json
import sys

import tornado.ioloop
import tornado.web
from tornado.log import enable_pretty_logging

enable_pretty_logging()


class ResponseHandler(tornado.web.RequestHandler):

    def set_default_headers(self):
        self.set_header("Content-Type", 'text/xml')

    def get(self):
        # print("get request received")
        self.write()

    def post(self):
        # print("post request received")
        response = """<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:paf="http://pagopa-api.pagopa.gov.it/pa/paForNode.xsd">
                <soapenv:Header/>
                <soapenv:Body>
                    <paf:paVerifyPaymentNoticeRes>
                        <outcome>OK</outcome>
                        <paymentList>
                            <paymentOptionDescription>
                                <amount>10.00</amount>
                                <options>EQ</options>
                                <dueDate>2021-12-31</dueDate>
                                <detailDescription>test</detailDescription>
                                <allCCP>1</allCCP>
                            </paymentOptionDescription>
                        </paymentList>
                        <paymentDescription>test</paymentDescription>
                        <fiscalCodePA>77777777777</fiscalCodePA>
                        <companyName>PagoPA</companyName>
                        <officeName>office</officeName>
                    </paf:paVerifyPaymentNoticeRes>
                </soapenv:Body>
            </soapenv:Envelope>
        """
        self.set_status(200)
        self.write(response)


def make_app():
    return tornado.web.Application([
        (r"/", ResponseHandler),
        (r"/servizi/PagamentiTelematiciRPT", ResponseHandler),
    ])


if __name__ == "__main__":
    default_port = '8089' if len(sys.argv) == 1 else sys.argv[1]
    port = int(default_port)
    app = make_app()
    app.listen(port)
    print(f"mockec running on port {default_port} ...")
    tornado.ioloop.IOLoop.current().start()
