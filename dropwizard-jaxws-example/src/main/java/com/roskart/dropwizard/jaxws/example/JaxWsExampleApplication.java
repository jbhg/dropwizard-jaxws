package com.roskart.dropwizard.jaxws.example;

import com.roskart.dropwizard.jaxws.BasicAuthentication;
import com.roskart.dropwizard.jaxws.ClientBuilder;
import com.roskart.dropwizard.jaxws.EndpointBuilder;
import com.roskart.dropwizard.jaxws.JAXWSBundle;
import com.roskart.dropwizard.jaxws.example.auth.BasicAuthenticator;
import com.roskart.dropwizard.jaxws.example.core.Person;
import com.roskart.dropwizard.jaxws.example.db.PersonDAO;
import com.roskart.dropwizard.jaxws.example.resources.*;
import com.roskart.dropwizard.jaxws.example.ws.*;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.hibernate.HibernateBundle;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import ws.example.jaxws.dropwizard.roskart.com.wsdlfirstservice.WsdlFirstService;

public class JaxWsExampleApplication extends Application<JaxWsExampleApplicationConfiguration> {

    // HibernateBundle is used by HibernateExampleService
    private final HibernateBundle<JaxWsExampleApplicationConfiguration> hibernate = new HibernateBundle<JaxWsExampleApplicationConfiguration>(Person.class) {
        @Override
        public DataSourceFactory getDataSourceFactory(JaxWsExampleApplicationConfiguration configuration) {
            return configuration.getDatabaseConfiguration();
        }
    };

    // JAX-WS Bundle
    private JAXWSBundle jaxWsBundle = new JAXWSBundle();

    public static void main(String[] args) throws Exception {
        new JaxWsExampleApplication().run(args);
    }

    @Override
    public void initialize(Bootstrap<JaxWsExampleApplicationConfiguration> bootstrap) {
        bootstrap.addBundle(hibernate);
        bootstrap.addBundle(jaxWsBundle);
    }

    @Override
    public void run(JaxWsExampleApplicationConfiguration jaxWsExampleApplicationConfiguration, Environment environment) throws Exception {

        // Hello world service
        jaxWsBundle.publishEndpoint(
                new EndpointBuilder("/simple", new SimpleService()));

        // Java first service protected with basic authentication
        jaxWsBundle.publishEndpoint(
                new EndpointBuilder("/javafirst", new JavaFirstServiceImpl())
                    .authentication(new BasicAuthentication(new BasicAuthenticator(), "TOP_SECRET")));

        // WSDL first service using server side JAX-WS handler and CXF logging interceptors
        jaxWsBundle.publishEndpoint(
                new EndpointBuilder("/wsdlfirst", new WsdlFirstServiceImpl())
                    .cxfInInterceptors(new LoggingInInterceptor())
                    .cxfOutInterceptors(new LoggingOutInterceptor()));

        // Service using Hibernate
        jaxWsBundle.publishEndpoint(
                new EndpointBuilder("/hibernate",
                    new HibernateExampleService(new PersonDAO(hibernate.getSessionFactory())))
                .sessionFactory(hibernate.getSessionFactory()));

        // RESTful resource that invokes WsdlFirstService on localhost and uses client side JAX-WS handler.
        environment.jersey().register(new AccessWsdlFirstServiceResource(
                jaxWsBundle.getClient(
                        new ClientBuilder<WsdlFirstService>(
                                WsdlFirstService.class,
                                "http://localhost:8080/soap/wsdlfirst")
                                .handlers(new WsdlFirstClientHandler()))));

        // RESTful resource that invokes JavaFirstService on localhost and uses basic authentication and
        // client side CXF interceptors.
        environment.jersey().register(new AccessProtectedServiceResource(
                jaxWsBundle.getClient(
                        new ClientBuilder<JavaFirstService>(
                                JavaFirstService.class,
                                "http://localhost:8080/soap/javafirst")
                                .cxfInInterceptors(new LoggingInInterceptor())
                                .cxfOutInterceptors(new LoggingOutInterceptor()))));
    }
}
