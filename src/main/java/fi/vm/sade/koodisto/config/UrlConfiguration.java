package fi.vm.sade.koodisto.config;

import fi.vm.sade.properties.OphProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;

@PropertySource("classpath:application.properties")
@Configuration
public class UrlConfiguration extends OphProperties {
    @Autowired
    public UrlConfiguration(Environment environment) {
        this.addOverride("host.virkailija", environment.getRequiredProperty("host.virkailija"));
        this.addOverride("url-virkailija", environment.getRequiredProperty("url-virkailija"));
        this.addDefault("host.alb", environment.getRequiredProperty("host.alb"));
    }
}
