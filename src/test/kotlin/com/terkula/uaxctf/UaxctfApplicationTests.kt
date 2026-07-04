package com.terkula.uaxctf

import com.terkula.uaxctf.util.TimeUtilities
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.LocalDate

@ExtendWith(SpringExtension::class)
@SpringBootTest
class UaxctfApplicationTests {

	@Test
	fun contextLoads() {

		val dates = TimeUtilities.getDatesBetween(LocalDate.now(), LocalDate.now().plusDays(5))

		println(dates)

	}

}
