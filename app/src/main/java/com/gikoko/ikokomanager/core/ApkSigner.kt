package com.gikoko.ikokomanager.core

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.android.apksig.ApkSigner
import java.io.File
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.X509Certificate
import javax.security.auth.x500.X500Principal

/**
 * Signs APKs with the v1 + v2 + v3 schemes using the user's own key.
 *
 * The signing key is generated once inside the Android Keystore and never
 * leaves it. This is the same open-source apksig library that the Android
 * build tools themselves use (Apache-2.0).
 */
class ApkSigner {

    private val keyAlias = "ikoko_manager_signing_key"

    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
        load(null)
    }

    @Synchronized
    fun signingKey(): Pair<PrivateKey, X509Certificate> {
        if (!keyStore.containsAlias(keyAlias)) {
            val generator = KeyPairGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_RSA, ANDROID_KEYSTORE
            )
            generator.initialize(
                KeyGenParameterSpec.Builder(
                    keyAlias,
                    KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
                )
                    .setKeySize(2048)
                    .setCertificateSubject(X500Principal("CN=I KO KO Manager, O=Gi Koko"))
                    .setCertificateSerialNumber(BigInteger.valueOf(System.currentTimeMillis()))
                    .setDigests(
                        KeyProperties.DIGEST_SHA256,
                        KeyProperties.DIGEST_SHA512
                    )
                    .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
                    .build()
            )
            generator.generateKeyPair()
        }
        val key = keyStore.getKey(keyAlias, null) as PrivateKey
        val cert = keyStore.getCertificate(keyAlias) as X509Certificate
        return key to cert
    }

    /** Sign [input] into [output]. v4 signing is off (it needs an extra .idsig file). */
    fun sign(input: File, output: File) {
        val (key, cert) = signingKey()
        val signerConfig = ApkSigner.SignerConfig.Builder(
            "ikoko-manager", key, listOf(cert)
        ).build()
        ApkSigner.Builder(listOf(signerConfig))
            .setInputApk(input)
            .setOutputApk(output)
            .setMinSdkVersion(24)
            .setV1SigningEnabled(true)
            .setV2SigningEnabled(true)
            .setV3SigningEnabled(true)
            .setV4SigningEnabled(false)
            .build()
            .sign()
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    }
}
