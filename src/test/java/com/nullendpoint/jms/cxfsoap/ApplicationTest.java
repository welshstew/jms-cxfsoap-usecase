package com.nullendpoint.jms.cxfsoap;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.jms.JmsComponent;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.commons.io.IOUtils;
import org.apache.cxf.message.MessageContentsList;
import org.example.fakeflexicube.NewType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import javax.jms.JMSException;
import java.io.IOException;
import java.util.HashMap;

/**
 * Created by swinchester on 5/7/17.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
public class ApplicationTest {

    @Autowired
    CamelContext camelContext;


    @Configuration
    @Import(Application.class) // override the ems configuration with local activemq "vm://" for unit testing
    public static class TestConfig
    {
        @Bean
        public JmsComponent ems() throws JMSException {
            JmsComponent jmsComponent = new JmsComponent();
            jmsComponent.setConnectionFactory(cachingConnectionFactory());
            jmsComponent.setTransacted(true);
            return jmsComponent;
        }

        @Bean
        @Primary
        public CachingConnectionFactory cachingConnectionFactory() throws JMSException {
            CachingConnectionFactory cachingConnectionFactory = new CachingConnectionFactory();
            cachingConnectionFactory.setTargetConnectionFactory(new ActiveMQConnectionFactory("vm://localhost?broker.persistent=false"));
            cachingConnectionFactory.setSessionCacheSize(10);
            cachingConnectionFactory.setCacheConsumers(false);
            cachingConnectionFactory.setCacheProducers(false);
            cachingConnectionFactory.setReconnectOnException(true);
            return cachingConnectionFactory;
        }
    }

    public boolean isUseAdviceWith(){
        return true;
    }

    public void publishMessages(String endpoint, int amount) throws Exception {
        ProducerTemplate pt = camelContext.createProducerTemplate();
        for(int i =0; i < amount; i++){
            pt.sendBody(endpoint, getFileWithUtil("request_message.xml"));

        }
    }

    private HashMap generateGoodCxfHeaders(){
        HashMap<String,Object> respMap = new HashMap<>();
        respMap.put("org.apache.cxf.message.Message.RESPONSE_CODE", new Integer(200));
        respMap.put("Content-Type", new String("text/xml; charset=utf-8"));
        return respMap;
    }

    @Before
    public void setupCxfMocks() throws Exception {
        camelContext.getRouteDefinition("cxf-message-format").adviceWith(camelContext, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                interceptSendToEndpoint("cxf:bean:soapClient?dataFormat=MESSAGE")
                        .skipSendToOriginalEndpoint().to("mock:cxf-message-format").process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        exchange.getOut().setBody(getFileWithUtil("soap-response.xml"));
                        exchange.getOut().setHeader("CamelCxfMessage", generateGoodCxfHeaders());
                    }
                });
            }
        });

        camelContext.getRouteDefinition("cxf-pojo-format").adviceWith(camelContext, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                interceptSendToEndpoint("cxf:bean:soapClient?dataFormat=POJO")
                        .skipSendToOriginalEndpoint().to("mock:cxf-pojo-format").process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        MessageContentsList mcl = new MessageContentsList();
                        mcl.add(new Object());
                        mcl.add(new NewType());
                        exchange.getOut().setBody(mcl);
                        exchange.getOut().setHeader("CamelCxfMessage", generateGoodCxfHeaders());

                    }
                });
            }
        });

        camelContext.getRouteDefinition("cxf-payload-format").adviceWith(camelContext, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                interceptSendToEndpoint("cxf:bean:soapClient?dataFormat=PAYLOAD")
                        .skipSendToOriginalEndpoint().to("mock:cxf-payload-format").process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        //not generating a CxfPayload, just use the plain String response
                        exchange.getOut().setBody(getFileWithUtil("soap-response.xml"));
                        exchange.getOut().setHeader("CamelCxfMessage", generateGoodCxfHeaders());

                    }
                });
            }
        });


    }

    @Test
    public void testSomething() throws Exception {
//        setupCxfMocks();

        MockEndpoint pojoMock = (MockEndpoint) camelContext.getEndpoint("mock:cxf-pojo-format");
        MockEndpoint messageMock = (MockEndpoint) camelContext.getEndpoint("mock:cxf-message-format");
        MockEndpoint payloadMock = (MockEndpoint) camelContext.getEndpoint("mock:cxf-payload-format");

        messageMock.expectedMessageCount(1);
        payloadMock.expectedMessageCount(0);
        pojoMock.expectedMessageCount(0);

        Thread.sleep(2000);
        publishMessages("ems:queue:queue.xslt", 1);
        Thread.sleep(2000);

        messageMock.assertIsSatisfied();
        payloadMock.assertIsSatisfied();
        pojoMock.assertIsSatisfied();

    }



    private String getFileWithUtil(String fileName) {
        String result = "";
        try {
            result = IOUtils.toString(getClass().getClassLoader().getResourceAsStream(fileName));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }
}
