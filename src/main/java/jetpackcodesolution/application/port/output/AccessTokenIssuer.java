package jetpackcodesolution.application.port.output;

import jetpackcodesolution.application.dto.TokenResponse;

public interface AccessTokenIssuer {

    TokenResponse issue(String subject);
}
