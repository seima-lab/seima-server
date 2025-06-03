package vn.fpt.seima.seimaserver.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class GoogleIdTokenVerifierService {
    private final GoogleIdTokenVerifier verifier;
    public GoogleIdTokenVerifierService(@Value("${spring.security.oauth2.client.registration.google.client-id}") String googleClientId) {
        this.verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory()) // hoặc JacksonFactory()
                .setAudience(Collections.singletonList(googleClientId))
                .build();
    }

    public GoogleIdToken.Payload verify(String idTokenString) {
        try {
            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken != null) {
                return idToken.getPayload();
            }
        } catch (Exception e) {
            // Log error: e.g., Invalid token format, network issue, etc.
            System.err.println("Error verifying Google ID Token: " + e.getMessage());
        }
        return null;
    }
}
