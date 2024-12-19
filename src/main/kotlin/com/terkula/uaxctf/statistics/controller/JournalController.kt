package com.terkula.uaxctf.statistics.controller

import com.terkula.uaxctf.statistics.response.journal.DailyJournalEntryResponse
import com.terkula.uaxctf.training.model.journal.JournalComment
import com.terkula.uaxctf.training.model.journal.RunnerJournalEntry
import com.terkula.uaxctf.training.request.journal.CreateDailyJournalEntryRequest
import com.terkula.uaxctf.training.request.journal.CreateJournalCommentRequest
import com.terkula.uaxctf.training.request.journal.SearchJournalRequest
import com.terkula.uaxctf.training.service.JournalService
import io.swagger.annotations.ApiOperation
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.sql.Date
import javax.validation.Valid

@RestController
@Validated
class JournalController(val journalService: JournalService) {

    @ApiOperation("get journal entry")
    @RequestMapping(value = ["journal/dailyJournalEntry/get"], method = [RequestMethod.GET])
    fun getDailyJournalEntry(
            @RequestParam(name = "date") date: Date,
            @RequestParam(name = "runnerId") runnerId: Int,
    ): DailyJournalEntryResponse? {

        return journalService.getRunnersDailyJournalEntry(date, runnerId)
    }

    @ApiOperation("get journal entry by uuid")
    @RequestMapping(value = ["journal/dailyJournalEntryByUuid/get"], method = [RequestMethod.GET])
    fun getDailyJournalEntry(
            @RequestParam(name = "uuid") uuid: String
    ): DailyJournalEntryResponse? {

        return journalService.getRunnersDailyJournalEntry(uuid)
    }

    @ApiOperation("get journal entries between dates")
    @RequestMapping(value = ["journal/dailyJournalEntries/get"], method = [RequestMethod.GET])
    fun getDailyJournalEntriesBetweenDatesAndTitle(
            @RequestParam(name = "startDate") startDate: Date,
            @RequestParam(name = "endDate") endDate: Date,
            @RequestParam(name = "runnerId") runnerId: Int,
            @RequestParam(name = "title") title: String?,
    ): List<DailyJournalEntryResponse> {

        return journalService.getRunnersDailyJournalEntryBetweenDatesWithTitle(startDate, endDate, runnerId, title)
    }

    @ApiOperation("get journal entries between dates")
    @RequestMapping(value = ["journal/dailyJournalEntries/search"], method = [RequestMethod.POST])
    fun searchJournals(
            @RequestBody @Valid searchJournalsRequest: SearchJournalRequest
    ): List<DailyJournalEntryResponse> {

        return journalService.searchJournals(searchJournalsRequest)
    }

    @ApiOperation("create or update journal entry")
    @RequestMapping(value = ["journal/dailyJournalEntry/create"], method = [RequestMethod.POST])
    fun createDailyJournalEntry(
        @RequestBody @Valid createDailyJournalEntryRequest: CreateDailyJournalEntryRequest
    ): DailyJournalEntryResponse {

        return journalService.createDailyJournalEntry(createDailyJournalEntryRequest)
    }

    @ApiOperation("create or update journal entry")
    @RequestMapping(value = ["journal/journalComment/create"], method = [RequestMethod.POST])
    fun createDailyJournalComment(
            @RequestBody @Valid createJournalCommentRequest: CreateJournalCommentRequest
    ): JournalComment {

        return journalService.createJournalComment(createJournalCommentRequest)
    }

    @ApiOperation("get journal comments")
    @RequestMapping(value = ["journal/journalComment/get"], method = [RequestMethod.GET])
    fun getJournalComments(
            @RequestParam(name = "journalUuid") journalUuid: String,
    ): List<JournalComment> {
        return journalService.getJournalComments(journalUuid)
    }

    @ApiOperation("Get Runner's Journal By UUID")
    @RequestMapping(value = ["journal/runnersJournalEntry"], method = [RequestMethod.GET])
    fun getRunnersJournalEntry(
            @RequestParam(name = "journalUuid") journalUuid: String,
    ): RunnerJournalEntry {
        return journalService.getRunnersJournal(journalUuid)
    }


}