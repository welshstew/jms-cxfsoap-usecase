/**
 *  Copyright 2005-2016 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package com.nullendpoint.jms.cxfsoap;

import com.tibco.tibjms.TibjmsConnectionFactory;
import org.apache.camel.component.jms.JmsComponent;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.Primary;
import org.springframework.jms.connection.CachingConnectionFactory;

import javax.jms.JMSException;
import javax.xml.xpath.XPathFactory;

/**
 * The Spring-boot main class.
 */
@SpringBootApplication
@ImportResource({"classpath:spring/camel-context.xml", "classpath:spring/cxf.xml"})
public class Application {

    public static void main(String[] args) {
        //use saxon for XPath lookups
        System.setProperty(XPathFactory.DEFAULT_PROPERTY_NAME + ":" + "http://saxon.sf.net/jaxp/xpath/om", "net.sf.saxon.xpath.XPathFactoryImpl");
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public TibjmsConnectionFactory tibjmsConnectionFactory() throws JMSException {
        TibjmsConnectionFactory tibjmsConnectionFactory = new TibjmsConnectionFactory();
        tibjmsConnectionFactory.setServerUrl("tcp://192.168.99.100:61222");
        tibjmsConnectionFactory.setUserName("admin");
        tibjmsConnectionFactory.setUserPassword("");
        return tibjmsConnectionFactory;
    }

    @Bean
    @Primary
    public CachingConnectionFactory cachingConnectionFactory() throws JMSException {
        CachingConnectionFactory cachingConnectionFactory = new CachingConnectionFactory();
        cachingConnectionFactory.setTargetConnectionFactory(tibjmsConnectionFactory());
        cachingConnectionFactory.setSessionCacheSize(10);
        cachingConnectionFactory.setCacheConsumers(false);
        cachingConnectionFactory.setCacheProducers(false);
        cachingConnectionFactory.setReconnectOnException(true);
        return cachingConnectionFactory;
    }

    @Bean
    public JmsComponent ems() throws JMSException {
        JmsComponent jmsComponent = new JmsComponent();
        jmsComponent.setConnectionFactory(cachingConnectionFactory());
        jmsComponent.setTransacted(true);
        return jmsComponent;
    }
}
