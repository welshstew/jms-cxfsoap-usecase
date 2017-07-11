package com.nullendpoint.jms.cxfsoap;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.ValidationException;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JaxbDataFormat;
import org.springframework.stereotype.Component;

/**
 * Created by swinchester on 4/7/17.
 */
@Component
public class JmsRouteBuilder extends RouteBuilder {

    public long maxDelay = 60000;

    @Override
    public void configure() throws Exception {

        onException(ValidationException.class)
                .handled(true)
                .to("{{app.endpoint.dlc}}"); //log:endpoint

        //also jms -> soap -> jms

        JaxbDataFormat jaxb = new JaxbDataFormat(true);
        jaxb.setContextPath("org.example.fakeflexicube");

        //poison xml - check (bad chars etc)
        //validation not done yet (extract schema from FakeFlexicube.wsdl)

        from("timer:hello?period=10000&repeatCount=1").autoStartup(false)
                .process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        String normalBody = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                "<FCUBS_REQ_ENV xmlns=\"http://nullendpoint.example.com/service/SomeServiceName/CreateSomething/GT\">\n" +
                                "    <FCUBS_HEADER>\n" +
                                "        <SourceSystem>somewhere</SourceSystem>\n" +
                                "        <SourceName>Dave</SourceName>\n" +
                                "        <SourceCooker>ObsidianToasting</SourceCooker>\n" +
                                "        <TargetSystem>CamelBased</TargetSystem>\n" +
                                "        <TargetEnvironment>RedHat</TargetEnvironment>\n" +
                                "        <TargetHappiness>true</TargetHappiness>\n" +
                                "    </FCUBS_HEADER>\n" +
                                "    <FCUBS_BODY>\n" +
                                "        <Some-Big-Element>\n" +
                                "            <ReferenceNo>123123123</ReferenceNo>\n" +
                                "            <CorrelationNumber>32432432423432</CorrelationNumber>\n" +
                                "            <XmlType>Rigid</XmlType>\n" +
                                "            <CoolnessFactor>Zero</CoolnessFactor>\n" +
                                "            <PracticalityFactor>10</PracticalityFactor>\n" +
                                "            <StarTrek>\n" +
                                "                <Ship>Enterprise</Ship>\n" +
                                "                <Captain>Kirk</Captain>\n" +
                                "                <Value>21313999999</Value>\n" +
                                "                <AwesomeSauce>true</AwesomeSauce>\n" +
                                "            </StarTrek>\n" +
                                "        </Some-Big-Element>\n" +
                                "    </FCUBS_BODY>\n" +
                                "</FCUBS_REQ_ENV>";

                        String fakeElementBody = "<fak:FakeElement xmlns:fak=\"http://www.example.org/FakeFlexicube/\">\n" +
                                "<FCUBS_REQ_ENV xmlns=\"http://nullendpoint.example.com/service/SomeServiceName/CreateSomething/GT\">\n" +
                                "    <FCUBS_HEADER>\n" +
                                "        <SourceSystem>somewhere</SourceSystem>\n" +
                                "        <SourceName>Dave</SourceName>\n" +
                                "        <SourceCooker>ObsidianToasting</SourceCooker>\n" +
                                "        <TargetSystem>CamelBased</TargetSystem>\n" +
                                "        <TargetEnvironment>RedHat</TargetEnvironment>\n" +
                                "        <TargetHappiness>true</TargetHappiness>\n" +
                                "    </FCUBS_HEADER>\n" +
                                "    <FCUBS_BODY>\n" +
                                "        <Some-Big-Element>\n" +
                                "            <ReferenceNo>123123123</ReferenceNo>\n" +
                                "            <CorrelationNumber>32432432423432</CorrelationNumber>\n" +
                                "            <XmlType>Rigid</XmlType>\n" +
                                "            <CoolnessFactor>Zero</CoolnessFactor>\n" +
                                "            <PracticalityFactor>10</PracticalityFactor>\n" +
                                "            <StarTrek>\n" +
                                "                <Ship>Enterprise</Ship>\n" +
                                "                <Captain>Kirk</Captain>\n" +
                                "                <Value>21313999999</Value>\n" +
                                "                <AwesomeSauce>true</AwesomeSauce>\n" +
                                "            </StarTrek>\n" +
                                "        </Some-Big-Element>\n" +
                                "    </FCUBS_BODY>\n" +
                                "</FCUBS_REQ_ENV>\n" +
                                "</fak:FakeElement>";

                        ProducerTemplate pt = exchange.getContext().createProducerTemplate();
                        for(int i=0; i<1; i++){
                            log.info("sending message " + (i+1));
                            pt.sendBody("ems:queue:queue.pojo.dataformat", fakeElementBody);
                            pt.sendBody("ems:queue:queue.payload.dataformat", fakeElementBody);
                            pt.sendBody("ems:queue:queue.xslt", normalBody);
                            pt.sendBody("ems:queue:queue.no.xslt", normalBody);
                            pt.sendBody("ems:queue:queue.freemarker", normalBody);
                            log.info("sent message " + (i+1));
                        }
                    }
                });

        from("ems:queue:queue.xslt?asyncConsumer=true&cacheLevelName=CACHE_CONSUMER&concurrentConsumers={{app.service.threads}}&acknowledgementModeName=CLIENT_ACKNOWLEDGE").routeId("route-xslt-soapmessage")
                .log("Got ${body}")
                .to("direct:delayer")
                .choice()
                    .when(xpath("local-name(/*) = 'FCUBS_REQ_ENV'")).to("xslt:xslt/xml-to-soap.xsl")
                        .setHeader("operationName", constant("CreateTransaction")).to("direct:callSOAP-message")
                    .endChoice()
                    .otherwise().throwException(new ValidationException(null, "unable to validate schema")).endChoice();

        from("ems:queue:queue.no.xslt?asyncConsumer=true&cacheLevelName=CACHE_CONSUMER&concurrentConsumers={{app.service.threads}}&acknowledgementModeName=CLIENT_ACKNOWLEDGE").routeId("route-java-soapmessage")
                .log("Got ${body}")
                .to("direct:delayer")
                .choice()
                .when(xpath("local-name(/*) = 'FCUBS_REQ_ENV'")).process(new XMLtoSOAPBodyProcessor())
                    .setHeader("operationName", constant("CreateTransaction")).to("direct:callSOAP-message")
                .endChoice()
                .otherwise().throwException(new ValidationException(null, "unable to validate schema")).endChoice();

        from("ems:queue:queue.freemarker?asyncConsumer=true&cacheLevelName=CACHE_CONSUMER&concurrentConsumers={{app.service.threads}}&acknowledgementModeName=CLIENT_ACKNOWLEDGE").routeId("route-freemarker-soapmessage")
                .log("Got ${body}")
                .to("direct:delayer")
                .choice()
                .when(xpath("local-name(/*) = 'FCUBS_REQ_ENV'")).process(new RemoveMarkupDeclarationProcessor()).to("freemarker:soap.ftl")
                .setHeader("operationName", constant("CreateTransaction")).to("direct:callSOAP-message")
                .endChoice()
                .otherwise().throwException(new ValidationException(null, "unable to validate schema")).endChoice();

        from("ems:queue:queue.pojo.dataformat?asyncConsumer=true&cacheLevelName=CACHE_CONSUMER&concurrentConsumers={{app.service.threads}}&acknowledgementModeName=CLIENT_ACKNOWLEDGE").routeId("route-pojo")
                .log("Got ${body}")
                .to("direct:delayer")
                .choice()
                .when(xpath("local-name(/fak:FakeElement/*) = 'FCUBS_REQ_ENV'").namespace("fak", "http://www.example.org/FakeFlexicube/"))
                .setHeader("operationName", constant("CreateTransaction")).to("direct:callSOAP-pojo")
                .endChoice()
                .otherwise().throwException(new ValidationException(null, "unable to validate schema")).endChoice();

        from("ems:queue:queue.payload.dataformat?asyncConsumer=true&cacheLevelName=CACHE_CONSUMER&concurrentConsumers={{app.service.threads}}&acknowledgementModeName=CLIENT_ACKNOWLEDGE").routeId("route-payload")
                .log("Got ${body}")
                .to("direct:delayer")
                .choice()
                .when(xpath("local-name(/fak:FakeElement/*) = 'FCUBS_REQ_ENV'").namespace("fak", "http://www.example.org/FakeFlexicube/"))
                .setHeader("operationName", constant("CreateTransaction")).to("direct:callSOAP-payload")
                .endChoice()
                .otherwise().throwException(new ValidationException(null, "unable to validate schema")).endChoice();

        from("direct:callSOAP-message").routeId("cxf-message-format")
                .log("BEFORE: ${body}")
                .to("cxf:bean:soapClient?dataFormat=MESSAGE")
                .log("AFTER: ${body}")
                .choice()
                    .when(simple("${header.CamelCxfMessage[org.apache.cxf.message.Message.RESPONSE_CODE]} != 200 || ${header.CamelCxfMessage[Content-Type]} not contains 'text/xml'"))
                        .throwException(new Exception("problems")).endChoice();

        from("direct:callSOAP-pojo").routeId("cxf-pojo-format")
                .log("BEFORE: ${body}")
                .unmarshal(jaxb)
                .to("cxf:bean:soapClient?dataFormat=POJO")
                .setBody(simple("${body[1]}")) //need to set the body here to the cxfPayloadList item 1
                .marshal(jaxb)
                .convertBodyTo(String.class)
                .log("AFTER: ${body}")
                .choice()
                .when(simple("${header.CamelCxfMessage[org.apache.cxf.message.Message.RESPONSE_CODE]} != 200 || ${header.CamelCxfMessage[Content-Type]} not contains 'text/xml'"))
                .throwException(new Exception("problems")).endChoice();

        from("direct:callSOAP-payload").routeId("cxf-payload-format")
                .log("BEFORE: ${body}")
                .to("cxf:bean:soapClient?dataFormat=PAYLOAD")
                .convertBodyTo(String.class)
                .log("AFTER: ${body}")
//                .unmarshal(jaxb) //marshall back to object (not sure if required)
                .choice()
                .when(simple("${header.CamelCxfMessage[org.apache.cxf.message.Message.RESPONSE_CODE]} != 200 || ${header.CamelCxfMessage[Content-Type]} not contains 'text/xml'"))
                .throwException(new Exception("problems")).endChoice();


        from("direct:delayer")
                .choice()
                    .when(simple("${headers[JMSXDeliveryCount]} != null"))
                        .process(new Processor() {
                            @Override
                            public void process(Exchange exchange) throws Exception {
                                Integer deliveryCount = (Integer) exchange.getIn().getHeader("JMSXDeliveryCount");
                                long wait = 2000*deliveryCount;
                                if(wait > maxDelay){
                                    exchange.getIn().setHeader("delay", maxDelay);
                                }else{
                                    exchange.getIn().setHeader("delay", wait);
                                }
                            }
                        }).delay(header("delay"));
    }
}
