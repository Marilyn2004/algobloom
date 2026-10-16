package com.ziyi.leetcodereviewsystem.coach;

import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.*;

@RestController
@RequestMapping("/api/v2/auth")
public class CoachAuthController {
    final CoachStore store; final PasswordEncoder encoder;
    public CoachAuthController(CoachStore store,PasswordEncoder encoder){this.store=store;this.encoder=encoder;}
    public record Credentials(@NotBlank @jakarta.validation.constraints.Pattern(regexp="[a-zA-Z0-9_-]{3,80}") String username,@NotBlank @Size(min=12,max=128) String password){}
    @GetMapping("/csrf") public Map<String,Object> csrf(CsrfToken token) {return Map.of("token",token.getToken(),"headerName",token.getHeaderName());}
    @PostMapping("/register") public Map<String,Object> register(@Valid @RequestBody Credentials c) {
        try {store.update("insert into coach_users(username,password_hash) values (?,?)",c.username(),encoder.encode(c.password()));}
        catch(DuplicateKeyException e){throw new ResponseStatusException(HttpStatus.CONFLICT,"USERNAME_UNAVAILABLE");}
        return Map.of("username",c.username(),"registered",true);
    }
    @PostMapping("/login") public Map<String,Object> login(@Valid @RequestBody Credentials c,HttpServletRequest req,HttpServletResponse res) {
        var users=store.rows("select password_hash from coach_users where username=?",c.username());
        if(users.isEmpty()||!encoder.matches(c.password(),users.getFirst().get("passwordHash").toString())) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"INVALID_CREDENTIALS");
        req.getSession(true);req.changeSessionId();
        var context=SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(c.username(),null,List.of(new SimpleGrantedAuthority("ROLE_LEARNER"))));
        SecurityContextHolder.setContext(context);
        new HttpSessionSecurityContextRepository().saveContext(context,req,res);
        return Map.of("username",c.username());
    }
}
