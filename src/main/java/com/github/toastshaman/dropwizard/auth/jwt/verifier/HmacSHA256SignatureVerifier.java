package com.github.toastshaman.dropwizard.auth.jwt.verifier;

import com.github.toastshaman.dropwizard.auth.jwt.JWTVerifier;
import com.github.toastshaman.dropwizard.auth.jwt.exceptioons.JsonWebTokenException;
import com.github.toastshaman.dropwizard.auth.jwt.model.JsonWebToken;
import com.google.common.base.Joiner;
import com.google.common.io.BaseEncoding;
import org.apache.commons.lang.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class HmacSHA256SignatureVerifier implements JWTVerifier {

    private static final String HMAC_SHA256_ALG = "HmacSHA256";

    private final byte[] secret;

    private SecretKeySpec signingKey;

    private Mac hmac;

    public HmacSHA256SignatureVerifier(byte[] secret) {
        this.secret = secret;
        initialiseKey(secret);
    }

    private void initialiseKey(byte[] key) {
        this.signingKey = new SecretKeySpec(secret, HMAC_SHA256_ALG);
        try {
            this.hmac = Mac.getInstance(HMAC_SHA256_ALG);
        } catch (NoSuchAlgorithmException e) {
            throw new JsonWebTokenException("cannot use HmacSHA256TokenParser on system without HmacSHA256 algorithm", e);
        }

        try {
            hmac.init(signingKey);
        } catch (InvalidKeyException e) {
            throw new JsonWebTokenException(e.getMessage(), e);
        }
    }

    @Override
    public String algorithm() { return "HS256"; }

    @Override
    public boolean verifySignature(JsonWebToken token) {
        checkArgument(token.getRawToken().isPresent());
        checkNotNull(token.getSignature());
        checkArgument(token.getSignature().length > 0);

        final String calculatedSignature = encode(calculateSignatureFor(token));
        final String providedSignature = encode(token.getSignature());

        return StringUtils.equals(providedSignature, calculatedSignature);
    }

    private byte[] calculateSignatureFor(JsonWebToken token) {
        final List<String> pieces = token.getRawToken().get();
        return hmac.doFinal(Joiner.on(".").join(pieces.get(0), pieces.get(1)).getBytes(Charset.forName("UTF-8")));
    }

    private String encode(byte[] signature) { return BaseEncoding.base64Url().omitPadding().encode(signature); }
}