package com.pred.pred_api.config;

import com.pred.pred_api.service.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    // استخدام Logger بدلاً من System.out.println لممارسة أفضل
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;

    /**
     * يتم استدعاء هذه الدالة مرة واحدة لكل طلب HTTP
     * تتأكد من وجود رمز JWT صالح وتُعد سياق الأمان وفقاً لذلك
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        // المسار الحالي للطلب
        String path = request.getServletPath();

        // السماح لنقاط النهاية العامة بالمرور دون تحقق
        if (isPublicPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // التحقق من وجود ترويسة المصادقة وأنها تبدأ بـ "Bearer "
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // لا يوجد رمز - استمرار السلسلة (سيتم رفض الطلب لاحقاً إذا كان يتطلب مصادقة)
            filterChain.doFilter(request, response);
            return;
        }

        // استخراج الرمز (إزالة "Bearer " من البداية)
        jwt = authHeader.substring(7);
        userEmail = jwtUtil.extractEmail(jwt);

        /**
         * إذا كان البريد الإلكتروني موجوداً ولم تتم المصادقة بعد في هذا السياق
         * نقوم بالتحقق من صحة الرمز وإعداد المصادقة
         */
        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);

                if (jwtUtil.isTokenValid(jwt)) {
                    // إنشاء رمز المصادقة
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,                        // كلمة المرور غير مطلوبة هنا
                            userDetails.getAuthorities()  // صلاحيات المستخدم من قاعدة البيانات
                    );

                    // إضافة تفاصيل الطلب (IP، session، إلخ)
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // تعيين المصادقة في سياق الأمان
                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    logger.debug("تمت مصادقة المستخدم: {}", userEmail);
                }
            } catch (Exception e) {
                // فشلت المصادقة - متابعة بدون تعيين سياق الأمان
                logger.error("فشلت مصادقة JWT للمستخدم {}: {}", userEmail, e.getMessage());
            }
        }

        // متابعة السلسلة في جميع الحالات
        filterChain.doFilter(request, response);
    }

    /**
     * التحقق مما إذا كان المسار عاماً لا يتطلب مصادقة
     * @param path مسار الطلب
     * @return true إذا كان المسار عاماً
     */
    private boolean isPublicPath(String path) {
        return path.equals("/api/auth/login") ||
                path.equals("/api/auth/register") ||
                path.equals("/api/auth/forgot-password") ||
                path.equals("/api/auth/reset-password") ||
                path.equals("/api/auth/verify-reset-token") ||
                path.startsWith("/api/test/") ||
                path.equals("/error") ||
                path.startsWith("/swagger-ui") ||
                path.startsWith("/v3/api-docs");
    }
}