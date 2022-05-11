package com.project.together.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    private final AuthenticationFailureHandler customFailureHandler;

    @Bean
    public BCryptPasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Override
    public void configure(WebSecurity web) { // 4
        web.ignoring().antMatchers("/css/**", "/js/**", "/img/**");
    }

    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http    .csrf()
                .disable()//csrf토큰 비활성화
                .authorizeRequests() // user 페이지 설정
                    .antMatchers("/admin/**").hasRole("ADMIN")
                    .antMatchers("/users/new","/login/**","/","items2/**")//회원가입, 로그인 ,홈화면, 상품목록, 상품 검색
                    .permitAll()
                    .anyRequest()
                .hasRole("USER")
                    //.authenticated()
                    //.permitAll()
                .and() // 로그인 페이지 사용
                .formLogin()
                    .loginPage("/login/new") // 로그인 페이지 경로 설정
                    //.defaultSuccessUrl("/")
                    .loginProcessingUrl("/login/proc") // 로그인이 실제 이루어지는 곳
                    .failureHandler(customFailureHandler);

                    //.defaultSuccessUrl("/"); // 로그인 성공 후 기본적으로 리다이렉트되는 경로
         }

}
