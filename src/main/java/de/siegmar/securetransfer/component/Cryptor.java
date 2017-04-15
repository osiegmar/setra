/*
 * Copyright 2017 Oliver Siegmar
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.siegmar.securetransfer.component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.crypto.random.CryptoRandom;
import org.apache.commons.crypto.random.CryptoRandomFactory;
import org.apache.commons.crypto.stream.CryptoInputStream;
import org.apache.commons.crypto.stream.CryptoOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.hash.Hashing;
import com.google.common.io.ByteStreams;

import de.siegmar.securetransfer.domain.KeyIv;

@SuppressWarnings("checkstyle:classdataabstractioncoupling")
public class Cryptor {

    private static final Logger LOG = LoggerFactory.getLogger(Cryptor.class);

    private static final int MIN_KEY_LENGTH = 256;
    private static final int SALT_SIZE = 8;
    private static final int IV_SIZE = 16;
    private static final int KEY_SIZE = 32;
    private static final String TRANSFORM = "AES/CBC/PKCS5Padding";

    private final CryptoRandom random;
    private final byte[] salt;

    public Cryptor(final Path baseDir) {
        validateCipherKeyLength();
        random = initRandom();
        this.salt = initSalt(baseDir);
    }

    public Cryptor(final byte[] salt) {
        validateCipherKeyLength();
        random = initRandom();
        this.salt = salt.clone();
    }

    /**
     * Ensure that we have strong cryptography extension (JCE) installed.
     */
    private void validateCipherKeyLength() {
        try {
            final int keyLength = Cipher.getMaxAllowedKeyLength("AES");
            if (keyLength < MIN_KEY_LENGTH) {
                throw new IllegalStateException("MaxAllowedKeyLength for AES is " + keyLength);
            }
        } catch (final NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private CryptoRandom initRandom() {
        try {
            final CryptoRandom cryptoRandom = CryptoRandomFactory.getCryptoRandom();
            LOG.info("Initialized {} for secure random generation", cryptoRandom.getClass());
            return cryptoRandom;
        } catch (final GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private byte[] initSalt(final Path baseDir) {
        final Path saltFile = baseDir.resolve("salt");
        try {
            if (Files.exists(saltFile)) {
                return Files.readAllBytes(saltFile);
            }

            final byte[] newSalt = newRandom(SALT_SIZE);
            Files.write(saltFile, newSalt, StandardOpenOption.CREATE_NEW);

            LOG.info("Initialized instance salt at {}", saltFile);
            return newSalt;
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public byte[] encrypt(final byte[] src, final KeyIv keyIv) {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (final OutputStream out = getCryptOut(bos, keyIv)) {
            ByteStreams.copy(new ByteArrayInputStream(src), out);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }

        return bos.toByteArray();
    }

    public byte[] encryptString(final String src, final KeyIv keyIv) {
        return encrypt(src.getBytes(StandardCharsets.UTF_8), keyIv);
    }

    public byte[] decrypt(final byte[] src, final KeyIv keyIv) {
        try (final InputStream cryptIn = getCryptIn(new ByteArrayInputStream(src), keyIv)) {
            return ByteStreams.toByteArray(cryptIn);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public String decryptString(final byte[] src, final KeyIv keyIv) {
        return new String(decrypt(src, keyIv), StandardCharsets.UTF_8);
    }

    public OutputStream getCryptOut(final OutputStream out, final KeyIv keyIv)
        throws IOException {
        return new CryptoOutputStream(TRANSFORM, new Properties(), out,
            new SecretKeySpec(keyIv.getKey(), "AES"), new IvParameterSpec(keyIv.getIv()));
    }

    public InputStream getCryptIn(final InputStream in, final KeyIv keyIv)
        throws IOException {
        return new CryptoInputStream(TRANSFORM, new Properties(), in,
            new SecretKeySpec(keyIv.getKey(), "AES"), new IvParameterSpec(keyIv.getIv()));
    }

    public byte[] keyFromSaltedPassword(final String password) {
        return Hashing.sha256().newHasher()
            .putBytes(salt)
            .putString(password, StandardCharsets.UTF_8)
            .hash().asBytes();
    }

    public byte[] newIv() {
        return newRandom(IV_SIZE);
    }

    public byte[] newKey() {
        return newRandom(KEY_SIZE);
    }

    private byte[] newRandom(final int keySize) {
        final byte[] key = new byte[keySize];
        random.nextBytes(key);
        return key;
    }

}
