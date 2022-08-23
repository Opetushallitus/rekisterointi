package fi.vm.sade.koodisto.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

@Profile("dev")
@Configuration
public class DevUserDetailsServiceConfiguration {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService() {

        return new DevUserDetailsService(passwordEncoder());
    }

    static class DevUserDetailsService implements UserDetailsService {
        PasswordEncoder passwordEncoder;

        private DevUserDetailsService(PasswordEncoder passwordEncoder) {
            this.passwordEncoder = passwordEncoder;
        }

        @Override
        public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
            return User.builder()
                    .password(this.passwordEncoder.encode("devaaja"))
                    .username("devaaja")
                    .build();
        }
    }
}
