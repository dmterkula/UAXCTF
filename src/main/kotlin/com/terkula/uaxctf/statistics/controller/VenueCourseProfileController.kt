package com.terkula.uaxctf.statistics.controller

import com.terkula.uaxctf.statistics.dto.VenueCourseProfileResponse
import com.terkula.uaxctf.statistics.service.CourseProfileService
import org.springframework.web.bind.annotation.*

data class CreateCourseProfileRequest(val label: String?, val gpxContent: String, val setAsCurrent: Boolean = true)

@RestController
class VenueCourseProfileController(val courseProfileService: CourseProfileService) {

    @RequestMapping(value = ["/venue/{venueId}/course-profile"], method = [RequestMethod.POST])
    fun createCourseProfile(
        @PathVariable venueId: Int,
        @RequestBody request: CreateCourseProfileRequest
    ): VenueCourseProfileResponse =
        VenueCourseProfileResponse.from(
            courseProfileService.createProfile(venueId, request.label, request.gpxContent, request.setAsCurrent)
        )

    @RequestMapping(value = ["/venue/{venueId}/course-profiles"], method = [RequestMethod.GET])
    fun getCourseProfiles(@PathVariable venueId: Int): List<VenueCourseProfileResponse> =
        courseProfileService.getProfilesForVenue(venueId).map { VenueCourseProfileResponse.from(it) }

    @RequestMapping(value = ["/venue/course-profile/{id}/set-current"], method = [RequestMethod.PUT])
    fun setCurrentCourseProfile(@PathVariable id: Int): VenueCourseProfileResponse? =
        courseProfileService.setCurrent(id)?.let { VenueCourseProfileResponse.from(it) }

    @RequestMapping(value = ["/venue/course-profile/{id}"], method = [RequestMethod.DELETE])
    fun deleteCourseProfile(@PathVariable id: Int) = courseProfileService.deleteProfile(id)
}
