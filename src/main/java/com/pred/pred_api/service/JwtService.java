package com.pred.pred_api.service;

import com.pred.pred_api.config.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

/**
 * خدمة موحّدة لإدارة رموز JWT
 * تجمع بين الدوال القديمة (لتوافق مع الكود الموجود) والدوال الجديدة (المعززة بالأمان)
 *
 * تم الدمج لتجنب تكرار الكود بين JwtUtil و JwtService
 */
@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtUtil jwtUtil;

    // ==================== الدوال الأصلية (موجودة مسبقاً) ====================

    /**
     * توليد رمز JWT جديد باستخدام البريد الإلكتروني
     * @param email البريد الإلكتروني للمستخدم
     * @return رمز JWT موقع
     */
    public String generateToken(String email) {
        return jwtUtil.generateToken(email);
    }

    /**
     * استخراج البريد الإلكتروني من الرمز
     * @param token رمز JWT
     * @return البريد الإلكتروني للمستخدم
     */
    public String extractEmail(String token) {
        return jwtUtil.extractEmail(token);
    }

    /**
     * التحقق من صحة الرمز فقط (دون التحقق من تطابق المستخدم)
     * @param token رمز JWT
     * @return true إذا كان الرمز صالحاً ولم ينتهِ
     */
    public boolean validateToken(String token) {
        return jwtUtil.isTokenValid(token);
    }

    // ==================== الدوال الجديدة (معززة بالأمان) ====================

    /**
     * توليد رمز JWT جديد لمستخدم (نسخة محسّنة تقبل UserDetails)
     * @param userDetails تفاصيل المستخدم من Spring Security
     * @return رمز JWT موقع
     */
    public String generateToken(UserDetails userDetails) {
        return jwtUtil.generateToken(userDetails.getUsername());
    }

    /**
     * استخراج اسم المستخدم من الرمز
     * @param token رمز JWT
     * @return اسم المستخدم (البريد الإلكتروني)
     */
    public String extractUsername(String token) {
        return jwtUtil.extractEmail(token);
    }

    /**
     * التحقق من صلاحية الرمز مع التحقق من تطابق المستخدم
     * هذه الدالة أكثر أماناً لأنها تتحقق من أن الرمز يخص المستخدم المحدد
     *
     * @param token رمز JWT
     * @param userDetails تفاصيل المستخدم للمقارنة
     * @return true إذا كان الرمز صالحاً ويطابق المستخدم ولم ينتهِ
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            String username = jwtUtil.extractEmail(token);
            // التحقق الثلاثي: الرمز موجود + يطابق المستخدم + لم ينتهِ
            return (username != null
                    && username.equals(userDetails.getUsername())
                    && jwtUtil.isTokenValid(token));
        } catch (Exception e) {
            return false;
        }
    }
}