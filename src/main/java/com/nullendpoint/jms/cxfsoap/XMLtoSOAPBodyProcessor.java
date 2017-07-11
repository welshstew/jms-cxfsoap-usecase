package com.nullendpoint.jms.cxfsoap;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

/**
 * Created by swinchester on 7/7/17.
 */
public class XMLtoSOAPBodyProcessor implements Processor {
    @Override
    public void process(Exchange exchange) throws Exception {

        StringBuilder sb = new StringBuilder();

        String trimmedXml = exchange.getIn().getBody().toString().replaceAll("\\<\\?xml(.+?)\\?\\>", "").trim();

        sb.append("<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:fak=\"http://www.example.org/FakeFlexicube/\">\n" +
                "    <soapenv:Body>\n" +
                "        <fak:FakeElement>\n");

        sb.append(trimmedXml);

        sb.append("                </fak:FakeElement>\n" +
                "            </soapenv:Body>\n" +
                "        </soapenv:Envelope>");


        exchange.getIn().setBody(sb.toString());
    }
}
