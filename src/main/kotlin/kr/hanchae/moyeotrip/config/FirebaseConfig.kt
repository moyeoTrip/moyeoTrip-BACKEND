package kr.hanchae.moyeotrip.config

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import kr.hanchae.moyeotrip.config.properties.FirebaseProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.ByteArrayInputStream
import java.util.Base64

@Configuration
class FirebaseConfig {
    @Bean
    fun firebaseApp(properties: FirebaseProperties): FirebaseApp {
        FirebaseApp.getApps().firstOrNull()?.let { return it }

        val serviceAccountJson = decodeServiceAccount(properties.serviceAccountJson)
        val options =
            FirebaseOptions
                .builder()
                .setCredentials(GoogleCredentials.fromStream(ByteArrayInputStream(serviceAccountJson)))
                .build()
        return FirebaseApp.initializeApp(options)
    }

    @Bean
    fun firebaseAuth(firebaseApp: FirebaseApp): FirebaseAuth = FirebaseAuth.getInstance(firebaseApp)

    @Bean
    fun firebaseMessaging(firebaseApp: FirebaseApp): FirebaseMessaging = FirebaseMessaging.getInstance(firebaseApp)

    private fun decodeServiceAccount(value: String): ByteArray {
        val trimmed = value.trim()
        return if (trimmed.startsWith("{")) {
            trimmed.toByteArray(Charsets.UTF_8)
        } else {
            Base64.getDecoder().decode(trimmed)
        }
    }
}
