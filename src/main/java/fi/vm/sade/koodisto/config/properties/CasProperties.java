package fi.vm.sade.koodisto.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "cas")
@Getter
@Setter
public class CasProperties {
    private String service;
    private Boolean sendRenew;
    private String key;
    private String base;
    private String login;
}
