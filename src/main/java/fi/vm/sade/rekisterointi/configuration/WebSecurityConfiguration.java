package fi.vm.sade.rekisterointi.configuration;

import fi.vm.sade.properties.OphProperties;
import fi.vm.sade.rekisterointi.NameContainer;
import org.jasig.cas.client.validation.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.preauth.*;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.filter.GenericFilterBean;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.*;

import static java.util.Collections.singletonList;
import static fi.vm.sade.rekisterointi.configuration.LocaleConfiguration.SESSION_ATTRIBUTE_NAME_LOCALE;
import static fi.vm.sade.rekisterointi.configuration.LocaleConfiguration.DEFAULT_LOCALE;
import static fi.vm.sade.rekisterointi.util.Constants.SESSION_ATTRIBUTE_NAME_ORIGINAL_REQUEST;
import static fi.vm.sade.rekisterointi.util.ServletUtils.setSessionAttribute;
import static fi.vm.sade.rekisterointi.util.ServletUtils.findSessionAttribute;

import java.io.IOException;

@Profile("!dev")
@Configuration
@EnableWebSecurity
public class WebSecurityConfiguration {
  private static final String HAKIJA_ROLE = "APP_REKISTEROINTI_HAKIJA";
  private final OphProperties ophProperties;

  public WebSecurityConfiguration(OphProperties ophProperties) {
    this.ophProperties = ophProperties;
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    AuthenticationProvider authenticationProvider = hakijaAuthenticationProvider();
    AuthenticationManager authenticationManager = authenticationManager(List.of(authenticationProvider));
    Filter authenticationFilter = hakijaAuthenticationProcessingFilter(authenticationManager);
    http.headers().disable().csrf().disable()
        .securityMatcher("/hakija/**")
        .authorizeHttpRequests((authz) -> authz
            .requestMatchers("/hakija/**").hasRole(HAKIJA_ROLE)
            .anyRequest().authenticated())
        .authenticationProvider(authenticationProvider)
        .addFilterBefore(new SaveLoginRedirectFilter(), BasicAuthenticationFilter.class)
        .addFilterBefore(authenticationFilter, BasicAuthenticationFilter.class)
        .exceptionHandling()
        .authenticationEntryPoint(hakijaAuthenticationEntryPoint());
    return http.build();
  }

  @Bean
  public AuthenticationManager authenticationManager(List<AuthenticationProvider> myAuthenticationProviders) {
    return new ProviderManager(myAuthenticationProviders);
  }

  @Bean
  @DependsOn("properties")
  public TicketValidator casOppijaticketValidator() {
    return new Cas20ServiceTicketValidator(ophProperties.url("cas-oppija.url"));
  }

  @Bean
  public Filter hakijaAuthenticationProcessingFilter(AuthenticationManager authenticationManager) {
    HakijaAuthenticationFilter filter = new HakijaAuthenticationFilter("/hakija/login", casOppijaticketValidator(),
        ophProperties);
    filter.setAuthenticationManager(authenticationManager);
    String authenticationSuccessUrl = ophProperties.url("rekisterointi.hakija.valtuudet.redirect");
    filter.setAuthenticationSuccessHandler(new SimpleUrlAuthenticationSuccessHandler(authenticationSuccessUrl));
    return filter;
  }

  @Bean
  public AuthenticationEntryPoint hakijaAuthenticationEntryPoint() {
    String loginCallbackUrl = ophProperties.url("rekisterointi.hakija.login");
    String defaultLoginUrl = ophProperties.url("cas-oppija.login", loginCallbackUrl);
    return new AuthenticationEntryPointImpl(defaultLoginUrl, ophProperties, loginCallbackUrl);
  }

  private static class AuthenticationEntryPointImpl extends LoginUrlAuthenticationEntryPoint {

    private final OphProperties properties;
    private final String loginCallbackUrl;

    public AuthenticationEntryPointImpl(String loginFormUrl, OphProperties properties, String loginCallbackUrl) {
      super(loginFormUrl);
      this.properties = properties;
      this.loginCallbackUrl = loginCallbackUrl;
    }

    @Override
    protected String determineUrlToUseForThisRequest(HttpServletRequest request, HttpServletResponse response,
        AuthenticationException exception) {
      Locale locale = findSessionAttribute(request, SESSION_ATTRIBUTE_NAME_LOCALE, Locale.class)
          .orElse(DEFAULT_LOCALE);
      String language = locale.getLanguage();
      return properties.url("cas-oppija.login", loginCallbackUrl, language);
    }
  }

  @Bean
  public AuthenticationProvider hakijaAuthenticationProvider() {
    PreAuthenticatedAuthenticationProvider authenticationProvider = new PreAuthenticatedAuthenticationProvider();
    authenticationProvider
        .setPreAuthenticatedUserDetailsService(new PreAuthenticatedGrantedAuthoritiesUserDetailsService());
    return authenticationProvider;
  }

  private static class SaveLoginRedirectFilter extends GenericFilterBean {
    @Override
    public void doFilter(
        ServletRequest request,
        ServletResponse response,
        FilterChain chain) throws IOException, ServletException {
      var httpRequest = (HttpServletRequest) request;
      var url = httpRequest.getRequestURL().toString();
      if (httpRequest.getQueryString() != null && httpRequest.getQueryString().contains("login=true")) {
        setSessionAttribute(httpRequest, SESSION_ATTRIBUTE_NAME_ORIGINAL_REQUEST, url);
      }
      chain.doFilter(request, response);
    }
  }

  private static class HakijaAuthenticationFilter extends AbstractAuthenticationProcessingFilter {

    private final TicketValidator oppijaticketValidator;
    private final OphProperties properties;

    public HakijaAuthenticationFilter(String defaultFilterProcessesUrl, TicketValidator oppijaticketValidator,
        OphProperties properties) {
      super(defaultFilterProcessesUrl);
      this.properties = properties;
      this.oppijaticketValidator = oppijaticketValidator;
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
        throws AuthenticationException {
      try {
        return getAuthenticationManager()
            .authenticate(createAuthRequest(request, validateTicket(resolveTicket(request))));
      } catch (TicketValidationException e) {
        throw new AuthenticationCredentialsNotFoundException(
            "Unable to authenticate because required param doesn't exist");
      }
    }

    private PreAuthenticatedAuthenticationToken createAuthRequest(HttpServletRequest request,
        Map<String, Object> casPrincipalAttributes) {
      String nationalIdentificationNumber = Optional
          .ofNullable((String) casPrincipalAttributes.get("nationalIdentificationNumber"))
          .orElseThrow(() -> new PreAuthenticatedCredentialsNotFoundException(
              "Unable to authenticate because required param doesn't exist"));
      String surname = Optional.ofNullable((String) casPrincipalAttributes.get("sn"))
          .orElse("");
      String firstName = Optional.ofNullable((String) casPrincipalAttributes.get("firstName"))
          .orElse("");

      PreAuthenticatedAuthenticationToken authRequest = new PreAuthenticatedAuthenticationToken(
          nationalIdentificationNumber, "N/A");
      List<? extends GrantedAuthority> authorities = singletonList(
          new SimpleGrantedAuthority(String.format("ROLE_%s", HAKIJA_ROLE)));
      authRequest.setDetails(new CasOppijaAuthenticationDetails(request, authorities, firstName, surname));
      return authRequest;
    }

    private String resolveTicket(HttpServletRequest request) {
      return Optional.ofNullable(request.getParameter("ticket"))
          .orElseThrow(() -> new PreAuthenticatedCredentialsNotFoundException(
              "Unable to authenticate because required param doesn't exist"));
    }

    private Map<String, Object> validateTicket(String ticket) throws TicketValidationException {
      return oppijaticketValidator.validate(ticket, properties.url("rekisterointi.hakija.login")).getPrincipal()
          .getAttributes();
    }

  }

  private static class CasOppijaAuthenticationDetails extends PreAuthenticatedGrantedAuthoritiesWebAuthenticationDetails
      implements NameContainer {

    private final String firstName;
    private final String surname;

    public CasOppijaAuthenticationDetails(HttpServletRequest request,
        Collection<? extends GrantedAuthority> authorities, String firstName, String surname) {
      super(request, authorities);
      this.firstName = firstName;
      this.surname = surname;
    }

    @Override
    public String getFirstName() {
      return firstName;
    }

    @Override
    public String getSurname() {
      return surname;
    }

  }

}
