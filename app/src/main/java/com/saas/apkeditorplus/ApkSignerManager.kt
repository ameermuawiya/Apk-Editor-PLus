package com.saas.apkeditorplus

import com.android.apksig.ApkSigner
import java.io.File
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.X509Certificate

class ApkSignerManager {

    interface SignerListener {
        fun onStart()
        fun onProgress(message: String)
        fun onSuccess()
        fun onError(message: String)
    }

    /**
     * Assina um APK usando uma KeyStore
     */
    fun signApk(
        inputApk: File,
        outputApk: File,
        keyStoreFile: File,
        keyStorePassword: CharArray,
        keyAlias: String,
        keyPassword: CharArray,
        listener: SignerListener? = null
    ): Boolean {
        return try {
            listener?.onStart()
            
            listener?.onProgress("Carregando KeyStore...")
            val ks = KeyStore.getInstance("PKCS12")
            keyStoreFile.inputStream().use { ks.load(it, keyStorePassword) }
            
            // Se o alias for vazio, tenta pegar o primeiro disponível
            listener?.onProgress("Identificando alias...")
            val alias = if (keyAlias.isNotEmpty()) keyAlias else ks.aliases().nextElement()
            
            listener?.onProgress("Recuperando chave privada...")
            val privateKey = ks.getKey(alias, keyPassword) as PrivateKey
            val certificate = ks.getCertificate(alias) as X509Certificate
            
            listener?.onProgress("Configurando assinador...")
            val signerConfig = ApkSigner.SignerConfig.Builder(
                "CERT",
                privateKey,
                listOf(certificate)
            ).build()

            val apkSigner = ApkSigner.Builder(listOf(signerConfig))
                .setInputApk(inputApk)
                .setOutputApk(outputApk)
                .setV1SigningEnabled(true)
                .setV2SigningEnabled(true)
                .setV3SigningEnabled(true)
                .build()

            listener?.onProgress("Assinando arquivo...")
            apkSigner.sign()
            
            listener?.onSuccess()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            listener?.onError(e.message ?: "Erro desconhecido")
            false
        }
    }
}
