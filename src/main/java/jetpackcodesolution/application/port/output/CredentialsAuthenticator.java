package jetpackcodesolution.application.port.output;

public interface CredentialsAuthenticator {

    String authenticate(String username, String password);
}
