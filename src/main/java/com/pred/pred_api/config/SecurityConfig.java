package com.pred.pred_api.config;

import com.pred.pred_api.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
/**
 * تُفعّل هذه التعليمة التحكم في الصلاحيات على مستوى الدوال (Method Security)
 * مما يتيح استخدام @PreAuthorize و @PostAuthorize في طبقة التحكم أو الخدمة
 * هذا هو التغيير الجوهري لإضافة طبقة حماية دقيقة
 */
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // تعطيل الحماية من CSRF لأن التطبيق يستخدم JWT (بدون حالة)
                .csrf(csrf -> csrf.disable())
                // إعداد إدارة الجلسات لتكون بدون حالة (Stateless) مناسبة لـ JWT
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // تعريف قواعد الوصول للمسارات
                .authorizeHttpRequests(auth -> auth
                        // المسموح للجميع: المصادقة والاختبارات
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/test/**").permitAll()
                        // صفحات الخطأ مسموحة بدون مصادقة
                        .requestMatchers("/error").permitAll()
                        // نقطة إضافية: السماح بالوصول إلى توثيق Swagger (إن وُجد)
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        // أي طلب آخر يتطلب المصادقة
                        .anyRequest().authenticated()
                )
                // مزود المصادقة الخاص بنا
                .authenticationProvider(authenticationProvider())
                /**
                 * إضافة فلتر JWT قبل فلتر المصادقة الافتراضي
                 * هذا يضمن التحقق من رمز JWT قبل أي معالجة أمنية أخرى
                 */
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * مزود المصادقة الذي يستخدم خدمة المستخدمين المخصصة
     * ويربطها بمشفر كلمات المرور BCrypt
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * مدير المصادقة - مطلوب لعمليات تسجيل الدخول اليدوية
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * مشفر كلمات المرور باستخدام خوارزمية BCrypt
     * قوة التشفير الافتراضية 10 جولات
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}