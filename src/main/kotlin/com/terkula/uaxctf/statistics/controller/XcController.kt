package com.terkula.uaxctf.statistics.controller

import com.google.common.collect.Lists
import com.terkula.uaxctf.statistics.repository.MeetRepository
import com.terkula.uaxctf.statistics.service.MeetPerformanceService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController

@RestController("/uaxc")
class XcController(@field:Autowired
                   private val meetRepository: MeetRepository, @field:Autowired
                   internal var meetPerformanceService: MeetPerformanceService) {

    val meets: String
        @GetMapping("/get")
        get() {
            val meets = Lists.newArrayList(meetRepository.findAll())
            return meets[0].name
        }

    @RequestMapping(value = ["/load/{meetId}"], method = [RequestMethod.GET])
    fun loadPerformances(@PathVariable("meetId") meetId: Int): String {

        meetPerformanceService.loadMeetPerformance(meetId)
        return "loaded"
    }


}
