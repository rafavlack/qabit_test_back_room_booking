package jetpackcodesolution.infrastructure.security;

import jetpackcodesolution.application.dto.TokenResponse;
import jetpackcodesolution.config.AppProperties;
import jetpackcodesolution.application.port.output.AccessTokenIssuer;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Service
public class JwtTokenService implements AccessTokenIssuer {

    private final JwtEncoder encoder;
    private final AppProperties.Security security;
    private final Clock clock;

    public JwtTokenService(JwtEncoder encoder, AppProperties properties, Clock clock) {
        this.encoder = encoder;
        this.security = properties.security();
        this.clock = clock;
    }

    @Override
    public TokenResponse issue(String username) {
        Instant now = clock.instant();
        Duration ttl = security.tokenTtl();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("qabit_test_back_room_booking")
                .subject(username)
                .issuedAt(now)
                .expiresAt(now.plus(ttl))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

        return new TokenResponse(token, "Bearer", ttl.toSeconds());
    }
}
