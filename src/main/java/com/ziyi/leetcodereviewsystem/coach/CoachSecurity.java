package com.ziyi.leetcodereviewsystem.coach;

import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.web.csrf.*;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

@Configuration
public class CoachSecurity {
    @Bean PasswordEncoder passwordEncoder() {return new BCryptPasswordEncoder(12);}
    @Bean UserDetailsService users(CoachStore store) {
        return username->{
            var rows=store.rows("select username,password_hash from coach_users where username=?",username);
            if(rows.isEmpty()) throw new UsernameNotFoundException("Invalid credentials");
            var row=rows.getFirst();
            return User.withUsername(row.get("username").toString()).password(row.get("passwordHash").toString()).roles("LEARNER").build();
        };
    }
    @Bean @Order(1) SecurityFilterChain coachChain(HttpSecurity http) throws Exception {
        var csrf=new HttpSessionCsrfTokenRepository(); csrf.setHeaderName("X-CSRF-TOKEN");
        http.securityMatcher("/api/v2/**")
          .authorizeHttpRequests(a->a.requestMatchers("/api/v2/auth/**").permitAll().anyRequest().authenticated())
          .csrf(c->c.csrfTokenRepository(csrf).csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
          .securityContext(c->c.securityContextRepository(new HttpSessionSecurityContextRepository()))
          .exceptionHandling(e->e.authenticationEntryPoint((req,res,ex)->{res.setStatus(401);res.setContentType("application/json");res.getWriter().write("{\"code\":\"UNAUTHENTICATED\"}");}))
          .logout(l->l.logoutUrl("/api/v2/auth/logout").logoutSuccessHandler((req,res,a)->res.setStatus(204)));
        return http.build();
    }
    @Bean @Order(2) SecurityFilterChain legacyChain(HttpSecurity http,@Value("${coach.legacy-api-enabled:false}") boolean enabled) throws Exception {
        http.authorizeHttpRequests(a->{ if(enabled) a.anyRequest().permitAll(); else a.requestMatchers("/problems/**","/stats").denyAll().anyRequest().permitAll(); })
            .csrf(c->c.disable());
        return http.build();
    }
}
