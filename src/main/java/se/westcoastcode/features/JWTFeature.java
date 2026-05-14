package se.westcoastcode.features;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.RSAKeyProvider;
import io.fusionauth.http.server.HTTPRequest;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;

import static se.westcoastcode.features.HTTPFeatures.forbidden;
import static se.westcoastcode.features.HTTPFeatures.unauthorized;

/**
 * A collection of useful JWT-related functions
 */
public final class JWTFeature {
    /**
     * The required audience
     */
    private static final String AUDIENCE = "urn:company:myapp";

    private static final Algorithm ALGORITHM;
    private static final PrivateKey PRIVATE_KEY;
    private static final PublicKey PUBLIC_KEY;

    static {
        var keyAlgorithm = "RSA";
        var numBits = 1024;

        // Get the public/private key pair
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance(keyAlgorithm);
            keyGen.initialize(numBits);
            KeyPair keyPair = keyGen.genKeyPair();
            PRIVATE_KEY = keyPair.getPrivate();
            PUBLIC_KEY = keyPair.getPublic();

            byte[] publicKeyBytes = PUBLIC_KEY.getEncoded();
            var KID = Base64.getEncoder().encodeToString(publicKeyBytes);

            ALGORITHM = Algorithm.RSA256(new RSAKeyProvider() {
                @Override
                public RSAPublicKey getPublicKeyById(String kid) {
                    return (RSAPublicKey) PUBLIC_KEY;
                }

                @Override
                public RSAPrivateKey getPrivateKey() {
                    // return the private key used
                    return (RSAPrivateKey) PRIVATE_KEY;
                }

                @Override
                public String getPrivateKeyId() {
                    return KID;
                }
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Authenticate bearer token
     *
     * @param req The request
     * @return A valid and decoded JWT token
     * @throws HTTPFeatures.HttpError if the authorization is missing or is invalid
     */
    public static DecodedJWT authenticate(final HTTPRequest req) throws HTTPFeatures.HttpError {
        var header = req.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw unauthorized(req);
        }

        header = header.substring("Bearer ".length());
        try {
            var jwt = JWT.decode(header);
            if (!jwt.getAudience().contains(AUDIENCE)) {
                throw unauthorized(req);
            }
            return JWT.require(ALGORITHM)
                    .build().verify(jwt);
        } catch (JWTVerificationException e) {
            throw unauthorized(req);
        }
    }

    /**
     * Authenticate bearer token and validate that it has all the required scopes
     *
     * @param req            The request
     * @param requiredScopes The required scopes
     * @return A valid and decoded JWT token
     * @throws HTTPFeatures.HttpError if the authorization is missing, is invalid or do not fulfill the required scopes
     */
    public static DecodedJWT authenticate(final HTTPRequest req, final String... requiredScopes) throws HTTPFeatures.HttpError {
        var user = authenticate(req);
        if (!verifyScopes(user, requiredScopes)) {
            throw forbidden(req);
        }
        return user;
    }

    /**
     * Verify that the user has all scopes
     *
     * @param user           The user
     * @param requiredScopes The required scopes
     * @return true if all scopes are met
     */
    public static boolean verifyScopes(final DecodedJWT user, final String... requiredScopes) {
        var userScopeString = user.getClaim("scope").asString();
        if (userScopeString == null || userScopeString.isEmpty()) {
            return false;
        }
        var userScopes = Arrays.asList(userScopeString.split(" "));
        for (var requiredScope : requiredScopes) {
            if (!userScopes.contains(requiredScope)) {
                return false;
            }
        }
        return true;
    }

    public static String createAccessToken() {
        return JWT.create()
                .withAudience(AUDIENCE)
                .withClaim("scope", "test:read test:write test:delete")
                .withIssuedAt(Instant.now())
                .withIssuer("myapp")
                .withExpiresAt(Instant.now().plusSeconds(3600))
                .sign(ALGORITHM);
    }
}
