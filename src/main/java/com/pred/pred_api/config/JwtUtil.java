package com.pred.pred_api.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration.ms}")
    private Long expiration;

    /**
     * تحويل السلسلة النصية للمفتاح إلى كائن SecretKey آمن
     * يُستحسن أن يكون طول المفتاح 256 بت (32 حرفاً) على الأقل لـ HS256
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * إنشاء رمز JWT جديد
     * @param email البريد الإلكتروني للمستخدم (يُخزن في خانة subject)
     * @return رمز JWT مُوقع وجاهز للإرسال
     */
    public String generateToken(String email) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .setSubject(email)                          // هوية المستخدم
                .setIssuedAt(now)                            // تاريخ الإصدار
                .setExpiration(expiryDate)                   // تاريخ الانتهاء
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)  // التوقيع
                .compact();
    }

    /**
     * استخراج البريد الإلكتروني من الرمز
     * @param token رمز JWT
     * @return البريد الإلكتروني أو null إذا كان الرمز غير صالح
     */
    public String extractEmail(String token) {
        try {
            return getClaims(token).getSubject();
        } catch (Exception e) {
            // فشل استخراج claims يعني أن الرمز تالف أو مُزور أو منتهي الصلاحية
            return null;
        }
    }

    /**
     * التحقق من صلاحية الرمز
     * يتحقق من أن الرمز لم ينتهِ ومن سلامة توقيعه
     * @param token رمز JWT
     * @return true إذا كان الرمز صالحاً
     */
    public boolean isTokenValid(String token) {
        try {
            Claims claims = getClaims(token);
            // التحقق من أن تاريخ الانتهاء لم يمر بعد
            return !claims.getExpiration().before(new Date());
        } catch (Exception e) {
            // أي استثناء (توقيع خاطئ، رمز تالف، إلخ) يعني أن الرمز غير صالح
            return false;
        }
    }

    /**
     * استخراج جميع المطالبات (Claims) من الرمز
     * هذه الدالة الخاصة تتحقق من صحة التوقيع تلقائياً
     * @param token رمز JWT
     * @return كائن Claims يحتوي على جميع بيانات الرمز
     */
    private Claims getClaims(String token) {
        return Jwts.parserBuilder()                         // استخدام parserBuilder الحديث
                .setSigningKey(getSigningKey())              // مفتاح التحقق من التوقيع
                .build()
                .parseClaimsJws(token)                       // تحليل الرمز والتحقق منه
                .getBody();                                  // استخراج جسم المطالبات
    }
}