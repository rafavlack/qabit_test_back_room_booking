package jetpackcodesolution.application.port.input;

import jetpackcodesolution.application.dto.TokenResponse;

public interface LoginUseCase {

    TokenResponse login(String username, String password);
}
