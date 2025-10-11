package edu.kangwon.university.taxicarpool.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import java.io.ByteArrayInputStream;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import java.io.IOException;
import org.springframework.core.io.Resource;

@Configuration
public class FirebaseConfig {

    // 운영: Base64 로 전달된 서비스계정 JSON
    @Value("${firebase.credentials.base64:}")
    private String base64Credentials;

    // 개발: 클래스패스 파일 사용
    @Value("${firebase.credentials.classpath:}")
    private Resource classpathCredentials;

    @Bean
    public FirebaseMessaging firebaseMessaging() throws IOException {
        GoogleCredentials googleCredentials;

        // 1) 운영 - Base64 환경변수로부터 로딩
        if (base64Credentials != null && !base64Credentials.isBlank()) {
            byte[] decoded = Base64.getDecoder().decode(base64Credentials);
            googleCredentials = GoogleCredentials.fromStream(new ByteArrayInputStream(decoded));

            // 2) 개발 - classpath 리소스에서 로딩
        } else if (classpathCredentials != null && classpathCredentials.exists()) {
            googleCredentials = GoogleCredentials.fromStream(classpathCredentials.getInputStream());

            // 3) 둘 다 없으면 즉시 실패(배포 오류를 초기에 드러내기 위함)
        } else {
            throw new IllegalStateException(
                "Firebase credentials not provided. " +
                    "Set 'firebase.credentials.base64' (prod) or 'firebase.credentials.classpath' (dev)."
            );
        }

        // Firebase 옵션 설정
        FirebaseOptions firebaseOptions = FirebaseOptions.builder()
            .setCredentials(googleCredentials)
            .build();

        // Firebase 앱 초기화 (이미 초기화된 경우 기존 앱 사용)
        FirebaseApp firebaseApp;
        if (FirebaseApp.getApps().isEmpty()) {
            firebaseApp = FirebaseApp.initializeApp(firebaseOptions);
        } else {
            firebaseApp = FirebaseApp.getInstance();
        }

        return FirebaseMessaging.getInstance(firebaseApp);
    }
}
