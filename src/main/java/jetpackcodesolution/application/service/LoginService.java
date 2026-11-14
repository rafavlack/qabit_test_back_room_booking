package jetpackcodesolution.application.service;

import jetpackcodesolution.application.dto.TokenResponse;
import jetpackcodesolution.application.port.input.LoginUseCase;
import jetpackcodesolution.application.port.output.AccessTokenIssuer;
import jetpackcodesolution.application.port.output.CredentialsAuthenticator;
import org.springframework.stereotype.Service;

@Service
public class LoginService implements LoginUseCase {

    private final CredentialsAuthenticator credentialsAuthenticator;
    private final AccessTokenIssuer accessTokenIssuer;

    public LoginService(
            CredentialsAuthenticator credentialsAuthenticator,
            AccessTokenIssuer accessTokenIssuer) {
        this.credentialsAuthenticator = credentialsAuthenticator;
        this.accessTokenIssuer = accessTokenIssuer;
    }

    @Override
    public TokenResponse login(String username, String password) {
        String authenticatedUsername = credentialsAuthenticator.authenticate(username, password);
        return accessTokenIssuer.issue(authenticatedUsername);
    }
}
