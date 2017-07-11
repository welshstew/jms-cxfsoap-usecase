package com.nullendpoint.jms.cxfsoap;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

/**
 * Created by swinchester on 10/7/17.
 */
public class RemoveMarkupDeclarationProcessor implements Processor {
    @Override
    public void process(Exchange exchange) throws Exception {
        exchange.getIn().setBody(exchange.getIn().getBody().toString().replaceAll("\\<\\?xml(.+?)\\?\\>", "").trim());
    }
}
