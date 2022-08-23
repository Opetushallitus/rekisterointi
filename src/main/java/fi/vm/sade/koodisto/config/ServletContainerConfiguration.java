package fi.vm.sade.koodisto.configuration;

import org.apache.catalina.connector.Connector;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Servlet containeriin liittyvät konfiguraatiot.
 */
@Configuration
public class ServletContainerConfiguration {

    /**
     * Konfiguraatio kun palvelua ajetaan HTTPS proxyn läpi. Käytännössä tämä
     * muuttaa {@link javax.servlet.ServletRequest#getScheme()} palauttamaan
     * `https` jolloin palvelun kautta luodut urlit muodostuvat oikein.
     *
     * Aktivointi: `koodisto-app.uses-ssl-proxy` arvoon `true`.
     *
     * @return EmbeddedServletContainerCustomizer jonka Spring automaattisesti
     * tunnistaa ja lisää servlet containerin konfigurointiin
     */
    @Bean
    @ConditionalOnProperty("rekisterointi-ui.uses-ssl-proxy")
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> sslProxyCustomizer() {
        return (TomcatServletWebServerFactory container) -> {
            if (container != null) {
                container.addConnectorCustomizers((Connector connector) -> {
                    connector.setScheme("https");
                    connector.setSecure(true);
                });
            }
        };
    }
}
