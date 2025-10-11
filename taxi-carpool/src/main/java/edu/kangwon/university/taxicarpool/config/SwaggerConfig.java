package edu.kangwon.university.taxicarpool.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.OpenAPI;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@SecurityScheme(
    name = "bearerAuth",                            // 스웨거 UI 상에서 참조할 이름
    type = SecuritySchemeType.HTTP,                 // HTTP 인증 스킴
    scheme = "bearer",                              // Bearer 토큰 방식
    bearerFormat = "JWT",                           // 형식은 JWT
    in = SecuritySchemeIn.HEADER                    // 헤더에 담아서 보냄
)
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("택시 카풀 API")
                .description("캡스톤 백엔드 API 명세서")
                .version("v1.0.0"))
            .servers(List.of(
                new Server().url("https://knu-carpool.store")
            ));
    }
}
