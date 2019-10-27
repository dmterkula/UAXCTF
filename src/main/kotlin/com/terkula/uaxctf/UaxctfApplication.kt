package com.terkula.uaxctf

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import springfox.documentation.builders.ApiInfoBuilder
import springfox.documentation.builders.PathSelectors
import springfox.documentation.builders.RequestHandlerSelectors
import springfox.documentation.service.ApiInfo
import springfox.documentation.spi.DocumentationType
import springfox.documentation.spring.web.plugins.Docket
import springfox.documentation.swagger.web.UiConfiguration
import springfox.documentation.swagger.web.UiConfigurationBuilder
import springfox.documentation.swagger2.annotations.EnableSwagger2
import java.util.concurrent.Executor

@SpringBootApplication
@ComponentScan(basePackages = ["com.terkula"])
@EnableJpaRepositories("com.terkula.uaxctf.statistics.repository", "com.terkula.uaxctf.training.repository" )
@EnableSwagger2
@EnableAsync
class UaxctfApplication {

	@Bean
	fun docket(@Value("\${swagger.host:}") swaggerHost: String, @Value("\${swagger.path:}") swaggerPath: String,
			   @Value("\${info.app.version:1.0}") version: String): Docket {
		return Docket(DocumentationType.SWAGGER_2)
				.host(swaggerHost)
				.pathMapping(swaggerPath)
				.select()
				.apis(RequestHandlerSelectors.basePackage("com.terkula.uaxctf"))
				.paths(PathSelectors.any()).build()
				.apiInfo(apiInfo(version))
	}

	private fun apiInfo(version: String): ApiInfo {

		return ApiInfoBuilder()
				.title("Ursuline XCTF Data Utility")
				.description("REST API for UA XCTF")
				.termsOfServiceUrl("")
				.license("")
				.licenseUrl("")
				.version(version)
				.build()
	}

	@Bean
	internal fun uiConfig(): UiConfiguration {
		return UiConfigurationBuilder.builder()
				.displayRequestDuration(true)
				.validatorUrl("")
				.build()
	}

}

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
	runApplication<UaxctfApplication>(*args)
}
