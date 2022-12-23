package com.terkula.uaxctf

import com.terkula.uaxctf.util.TimeUtilities
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner
import java.time.LocalDate

@RunWith(SpringRunner::class)
@SpringBootTest
class UaxctfApplicationTests {

	@Test
	fun contextLoads() {

		val dates = TimeUtilities.getDatesBetween(LocalDate.now(), LocalDate.now().plusDays(5))

		println(dates)

	}

}
