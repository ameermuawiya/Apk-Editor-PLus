package com.saas.apkeditorplus

import android.content.Context
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.io.File
import java.io.FileOutputStream
import java.math.BigInteger
import java.security.*
import java.security.cert.X509Certificate
import java.util.*

class KeyStoreManager(private val context: Context) {

    private val keyStoreDir = File(context.filesDir, "keystores")

    init {
        if (!keyStoreDir.exists()) {
            keyStoreDir.mkdirs()
        }
        // Remove existing "BC" provider and insert ours at the top to avoid conflicts with Android's built-in version
        Security.removeProvider("BC")
        Security.insertProviderAt(org.bouncycastle.jce.provider.BouncyCastleProvider(), 1)
    }

    fun createKeyStore(
        fileName: String,
        password: CharArray,
        alias: String,
        commonName: String,
        orgUnit: String,
        orgName: String,
        locality: String,
        state: String,
        country: String
    ): File {
        val file = File(keyStoreDir, if (fileName.endsWith(".jks")) fileName else "$fileName.jks")
        
        val keyStore = KeyStore.getInstance("PKCS12") 
        keyStore.load(null, null)

        val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
        keyPairGenerator.initialize(2048)
        val keyPair = keyPairGenerator.generateKeyPair()

        val cert = generateSelfSignedCertificate(
            keyPair, 
            "CN=$commonName, OU=$orgUnit, O=$orgName, L=$locality, ST=$state, C=$country"
        )

        keyStore.setKeyEntry(alias, keyPair.private, password, arrayOf(cert))

        FileOutputStream(file).use { keyStore.store(it, password) }
        
        return file
    }

    private fun generateSelfSignedCertificate(keyPair: KeyPair, dn: String): X509Certificate {
        val issuer = X500Name(dn)
        val serialNumber = BigInteger.valueOf(System.currentTimeMillis())
        val notBefore = Date()
        val notAfter = Date(notBefore.time + 3650L * 24 * 60 * 60 * 1000) // 10 anos

        val builder = JcaX509v3CertificateBuilder(
            issuer,
            serialNumber,
            notBefore,
            notAfter,
            issuer,
            keyPair.public
        )

        val contentSigner = JcaContentSignerBuilder("SHA256WithRSA").build(keyPair.private)
        // Use the BC provider specifically to ensure we use our registered version
        return JcaX509CertificateConverter()
            .setProvider(Security.getProvider("BC"))
            .getCertificate(builder.build(contentSigner))
    }

    fun listKeyStores(): List<File> {
        return keyStoreDir.listFiles()?.toList() ?: emptyList()
    }

    fun getTestKey(): File {
        val testKeyFile = File(keyStoreDir, "testkey.jks")
        if (!testKeyFile.exists()) {
            createKeyStore(
                "testkey.jks",
                "testkey".toCharArray(),
                "testkey",
                "Test Key",
                "Android",
                "ApkEditorPlus",
                "World",
                "Internet",
                "US"
            )
        }
        return testKeyFile
    }
}
