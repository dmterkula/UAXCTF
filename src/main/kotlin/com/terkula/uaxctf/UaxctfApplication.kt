package com.terkula.uaxctf

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.terkula.uaxctf.statistics.controller.firebase.FirebaseAuthService.Companion.FIREBASE_KEY
import com.terkula.uaxctf.statistics.controller.firebase.FirebaseAuthService.Companion.FIREBASE_MESSAGING_SCOPES
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.io.ByteArrayInputStream
import java.util.concurrent.Executor

@SpringBootApplication
@ComponentScan(basePackages = ["com.terkula"])
@EnableJpaRepositories("com.terkula.uaxctf.statistics.repository", "com.terkula.uaxctf.training.repository" )
@EnableAsync
@Configuration
@EnableScheduling
class UaxctfApplication

@Bean(name = ["asyncExecutor"])
fun asyncExecutor(): Executor {
	val executor = ThreadPoolTaskExecutor()
	executor.isDaemon = true
	executor.corePoolSize = 100
	executor.maxPoolSize = 100
	executor.setQueueCapacity(100)
	executor.setThreadNamePrefix("AsyncRecommendationCoreThread-")
	executor.initialize()
	return executor
}


fun main(args: Array<String>) {

	var inputStream = ByteArrayInputStream(FIREBASE_KEY.toByteArray())

	val googleCredentials: GoogleCredentials = GoogleCredentials
			.fromStream(inputStream)
			.createScoped(FIREBASE_MESSAGING_SCOPES)

	googleCredentials.refresh()

	var options: FirebaseOptions = FirebaseOptions.builder()
			.setCredentials(googleCredentials)
			.build()

	FirebaseApp.initializeApp(options)
	runApplication<UaxctfApplication>(*args)
}
