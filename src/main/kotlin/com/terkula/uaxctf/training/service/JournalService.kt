package com.terkula.uaxctf.training.service

import com.terkula.uaxctf.statistics.repository.DailyJournalRepository
import com.terkula.uaxctf.statistics.repository.JournalCommentRepository
import com.terkula.uaxctf.statistics.repository.RunnerRepository
import com.terkula.uaxctf.statistics.repository.tags.JournalTagRelationshipRepository
import com.terkula.uaxctf.statistics.repository.tags.TagRepository
import com.terkula.uaxctf.statistics.response.journal.DailyJournalEntryResponse
import com.terkula.uaxctf.training.model.journal.DailyJournalEntry
import com.terkula.uaxctf.training.model.journal.JournalComment
import com.terkula.uaxctf.training.model.journal.RunnerJournalEntry
import com.terkula.uaxctf.training.model.tags.JournalTagRelationship
import com.terkula.uaxctf.training.model.tags.Tag
import com.terkula.uaxctf.training.request.journal.CreateDailyJournalEntryRequest
import com.terkula.uaxctf.training.request.journal.CreateJournalCommentRequest
import com.terkula.uaxctf.training.request.journal.SearchJournalRequest
import org.springframework.stereotype.Service
import java.sql.Date
import javax.persistence.EntityManager
import javax.persistence.TypedQuery

@Service
class JournalService (
        val dailyJournalRepository: DailyJournalRepository,
        val journalCommentRepository: JournalCommentRepository,
        val tagRepository: TagRepository,
        val journalTagRelationshipRepository: JournalTagRelationshipRepository,
        val entityManager: EntityManager,
        val runnerRepository: RunnerRepository
    ) {

    fun createDailyJournalEntry(createDailyJournalEntryRequest: CreateDailyJournalEntryRequest): DailyJournalEntryResponse {

        val journalEntry: DailyJournalEntry? = dailyJournalRepository.findByUuid(createDailyJournalEntryRequest.uuid).firstOrNull()


        if (journalEntry == null) {

            val dailyJournalEntry = DailyJournalEntry(
                    createDailyJournalEntryRequest.runnerId,
                    createDailyJournalEntryRequest.uuid,
                    createDailyJournalEntryRequest.date,
                    createDailyJournalEntryRequest.text,
                    createDailyJournalEntryRequest.title,
                    createDailyJournalEntryRequest.timestamp,
                    createDailyJournalEntryRequest.whatIfStatement,
                    createDailyJournalEntryRequest.favorite

            )
            dailyJournalRepository.save(dailyJournalEntry)
            dailyJournalEntry.date = createDailyJournalEntryRequest.date
            var tags = emptyList<Tag>()
            if (createDailyJournalEntryRequest.tagUuids.isNotEmpty()) {

                tags = createDailyJournalEntryRequest.tagUuids.map {
                    journalTagRelationshipRepository.save(
                            JournalTagRelationship(createDailyJournalEntryRequest.uuid, it, createDailyJournalEntryRequest.runnerId)
                    )
                }.map {
                    tagRepository.findByUuid(it.tagUuid)
                }.flatten()
            }

            return DailyJournalEntryResponse(dailyJournalEntry, emptyList(), tags)

        } else {
            journalEntry.date = createDailyJournalEntryRequest.date
            journalEntry.text = createDailyJournalEntryRequest.text
            journalEntry.title = createDailyJournalEntryRequest.title
            journalEntry.whatIfStatement = createDailyJournalEntryRequest.whatIfStatement
            journalEntry.favorite = createDailyJournalEntryRequest.favorite

            dailyJournalRepository.save(journalEntry)

            val comments = journalCommentRepository.findByJournalUuid(journalEntry!!.uuid).sortedBy { it.timestamp }

            val existingTags = journalTagRelationshipRepository.findByJournalUuid(createDailyJournalEntryRequest.uuid).map { it.tagUuid }

            createDailyJournalEntryRequest.tagUuids.forEach {
                if (existingTags.contains(it)) {
                    // do nothing
                } else {
                    // create new
                    journalTagRelationshipRepository.save(
                            JournalTagRelationship(
                                    createDailyJournalEntryRequest.uuid, it, createDailyJournalEntryRequest.runnerId
                            )
                    )
                }
            }

            existingTags.forEach {
                if (!createDailyJournalEntryRequest.tagUuids.contains(it)) {
                    // if there is an existing tag not in the updated tag set, remove the tagged relatinship
                    journalTagRelationshipRepository.deleteByJournalUuidAndTagUuid(createDailyJournalEntryRequest.uuid, it)
                }
            }

            val tags: List<Tag> = journalTagRelationshipRepository.findByJournalUuid(createDailyJournalEntryRequest.uuid).map {
                tagRepository.findByUuid(it.tagUuid)
            }.flatten()

            return DailyJournalEntryResponse(journalEntry, comments, tags)

        }

    }

    fun getRunnersDailyJournalEntry(date: Date, runnerId: Int): DailyJournalEntryResponse? {
        val entry = dailyJournalRepository.findByDateAndRunnerId(date, runnerId).firstOrNull()
        return if (entry == null) {
            null
        } else {
            val tags = journalTagRelationshipRepository.findByJournalUuid(entry.uuid).map {
                tagRepository.findByUuid(it.tagUuid)
            }.flatten()

            val comments = journalCommentRepository.findByJournalUuid(entry!!.uuid).sortedBy { it.timestamp }
            DailyJournalEntryResponse(entry, comments, tags)
        }
    }

    fun getRunnersDailyJournalEntry(uuid: String): DailyJournalEntryResponse? {
        val entry = dailyJournalRepository.findByUuid(uuid).firstOrNull()

        return if (entry == null) {
            null
        } else {
            val tags = journalTagRelationshipRepository.findByJournalUuid(entry.uuid).map {
                tagRepository.findByUuid(it.tagUuid)
            }.flatten()

            val comments = journalCommentRepository.findByJournalUuid(entry!!.uuid).sortedBy { it.timestamp }
            DailyJournalEntryResponse(entry, comments, tags)
        }
    }

    fun getRunnersDailyJournalEntryBetweenDates(startDate: Date, endDate: Date, runnerId: Int): List<DailyJournalEntryResponse> {
        val entries = dailyJournalRepository.findByDateBetweenAndRunnerId(startDate, endDate, runnerId)

        return if (entries.isEmpty()) {
            emptyList()
        } else {
            entries.map { entry ->
                val tags = journalTagRelationshipRepository.findByJournalUuid(entry.uuid).map {
                    tagRepository.findByUuid(it.tagUuid)
                }.flatten()
                val comments = journalCommentRepository.findByJournalUuid(entry!!.uuid).sortedBy { it.timestamp }
                DailyJournalEntryResponse(entry, comments, tags)
            }

        }
    }

    fun searchJournals(searchJournalRequest: SearchJournalRequest): List<DailyJournalEntryResponse> {

        return if(searchJournalRequest.andTags) {
            searchJournalsWithAndTagClauses(searchJournalRequest)
        } else {
            searchJournalsWithOrTagClauses(searchJournalRequest)
        }.sortedBy {
            it.journalEntry.date
        }


    }

    fun searchJournalsWithAndTagClauses(searchJournalRequest: SearchJournalRequest): List<DailyJournalEntryResponse> {
        //select daily_journal.id, uuid, tag_uuid, text, uuid, timestamp, title, runner_id, date from daily_journal
        //LEFT JOIN journal_tag_relationships ON journal_uuid=uuid WHERE ((date between '2024-08-01' AND '2024-08-11') AND runner_id=89 AND tag_uuid ="777384f7-8581-4ca5-b7a3-0e346cad818e" AND title like '%Friday%')
        var finalQuery = ""
        val selectStatement = "SELECT d from DailyJournalEntry d"
        val joinStatement = " LEFT JOIN JournalTagRelationship j ON j.journalUuid=d.uuid"
        var whereStatement = " WHERE("
        val whereBetweenDates = "(d.date between '" + searchJournalRequest.startDate + "' AND '" + searchJournalRequest.endDate + "' )"
        val whereRunnerId = " AND d.runnerId=" + searchJournalRequest.runnerId
        val whereTitle = " AND title like '%" + searchJournalRequest.title +"%'"
        var whereTags = ""

        if (!searchJournalRequest.tagUuids.isEmpty()) {
            whereTags += " AND tag_uuid= '" + searchJournalRequest.tagUuids.first() + "'"
        }


        whereStatement += whereBetweenDates
        whereStatement += whereRunnerId

        if (searchJournalRequest.title != null && searchJournalRequest.title.isNotEmpty()) {
            whereStatement += whereTitle
        }

        if (searchJournalRequest.tagUuids.isNotEmpty()) {
            whereStatement += whereTags
        }

        whereStatement += ")"



        finalQuery += selectStatement
        finalQuery += joinStatement
        finalQuery += whereStatement

        if (searchJournalRequest.tagUuids.size > 1) {
            for (i in 1 until searchJournalRequest.tagUuids.size) {
                // build AND in clauses
                var andClause = if (searchJournalRequest.title != null) {
                    if (searchJournalRequest.searchFavorites) {
                        " AND d.uuid IN (SELECT e.uuid from DailyJournalEntry e LEFT JOIN JournalTagRelationship j ON" + "" +
                                " j.journalUuid=uuid WHERE((d.date between '" + searchJournalRequest.startDate + "' AND '" + searchJournalRequest.endDate + "' )" +
                                " AND d.runnerId=" + searchJournalRequest.runnerId + " AND d.title like '%" + searchJournalRequest.title +"%'" +
                                " AND d.favorite=" + searchJournalRequest.searchFavorites +
                                " AND tag_uuid= '" + searchJournalRequest.tagUuids[i] + "'))"
                    } else {
                        " AND d.uuid IN (SELECT e.uuid from DailyJournalEntry e LEFT JOIN JournalTagRelationship j ON" + "" +
                                " j.journalUuid=uuid WHERE((d.date between '" + searchJournalRequest.startDate + "' AND '" + searchJournalRequest.endDate + "' )" +
                                " AND d.runnerId=" + searchJournalRequest.runnerId + " AND d.title like '%" + searchJournalRequest.title +"%'" +
                                " AND tag_uuid= '" + searchJournalRequest.tagUuids[i] + "'))"
                    }

                    } else {
                        if (searchJournalRequest.searchFavorites) {
                            " AND d.uuid IN (SELECT e.uuid from DailyJournalEntry e LEFT JOIN JournalTagRelationship j ON" + "" +
                                    " j.journalUuid=uuid WHERE((d.date between '" + searchJournalRequest.startDate + "' AND '" + searchJournalRequest.endDate + "' )" +
                                    " AND d.runnerId=" + searchJournalRequest.runnerId +
                                    " AND d.favorite=" + searchJournalRequest.searchFavorites +
                                    " AND tag_uuid= '" + searchJournalRequest.tagUuids[i] + "'))"
                        } else {
                            " AND d.uuid IN (SELECT e.uuid from DailyJournalEntry e LEFT JOIN JournalTagRelationship j ON" + "" +
                                    " j.journalUuid=uuid WHERE((d.date between '" + searchJournalRequest.startDate + "' AND '" + searchJournalRequest.endDate + "' )" +
                                    " AND d.runnerId=" + searchJournalRequest.runnerId +
                                    " AND tag_uuid= '" + searchJournalRequest.tagUuids[i] + "'))"
                        }

                }
                //AND uuid IN (SELECT uuid FROM daily_journal LEFT JOIN journal_tag_relationships ON journal_uuid=uuid WHERE((date between '2024-07-31' AND '2024-08-11' ) AND runner_id=89 AND tag_uuid= '894fc22b-2a2f-4068-9273-dfad857cca6f'))

                finalQuery += andClause
            }
        }


        var q: TypedQuery<DailyJournalEntry> = entityManager.createQuery(finalQuery, DailyJournalEntry::class.java)
        val journals = q.resultList

        return journals.map { entry ->
            val tags = journalTagRelationshipRepository.findByJournalUuid(entry.uuid).map {
                tagRepository.findByUuid(it.tagUuid)
            }.flatten()

            val comments = journalCommentRepository.findByJournalUuid(entry!!.uuid).sortedBy { it.timestamp }
            DailyJournalEntryResponse(entry, comments, tags)
        }
    }

    fun searchJournalsWithOrTagClauses(searchJournalRequest: SearchJournalRequest): List<DailyJournalEntryResponse> {
        //select daily_journal.id, uuid, tag_uuid, text, uuid, timestamp, title, runner_id, date from daily_journal
        //LEFT JOIN journal_tag_relationships ON journal_uuid=uuid WHERE ((date between '2024-08-01' AND '2024-08-11') AND runner_id=89 AND tag_uuid ="777384f7-8581-4ca5-b7a3-0e346cad818e" AND title like '%Friday%')
        var finalQuery = ""
        val selectStatement = "SELECT DISTINCT d from DailyJournalEntry d"
        val joinStatement = " LEFT JOIN JournalTagRelationship j ON j.journalUuid=d.uuid"
        var whereStatement = " WHERE("
        val whereBetweenDates = "(d.date between '" + searchJournalRequest.startDate + "' AND '" + searchJournalRequest.endDate + "' )"
        val whereRunnerId = " AND d.runnerId=" + searchJournalRequest.runnerId
        val whereFavorited = "And d.favorite=" + searchJournalRequest.searchFavorites

        var whereTags = ""

        var counter = 0


        searchJournalRequest.tagUuids.forEach {
            if (counter == 0) {
                whereTags += " AND tag_uuid= '" + it + "'"
            } else {
                whereTags += " OR tag_uuid= '" + it + "'"
            }
            counter++

        }

        val whereTitle = " AND title like '%" + searchJournalRequest.title +"%'"

        whereStatement += whereBetweenDates
        whereStatement += whereRunnerId

        if (searchJournalRequest.tagUuids.isNotEmpty()) {
            whereStatement += whereTags
        }

        if (searchJournalRequest.title != null && searchJournalRequest.title.isNotEmpty()) {
            whereStatement += whereTitle
        }

        if (searchJournalRequest.searchFavorites) {
            whereStatement += whereFavorited
        }

        whereStatement += ")"

        finalQuery += selectStatement
        finalQuery += joinStatement
        finalQuery += whereStatement


        var q: TypedQuery<DailyJournalEntry> = entityManager.createQuery(finalQuery, DailyJournalEntry::class.java)
        val journals = q.resultList

        return journals.map { entry ->
            val tags = journalTagRelationshipRepository.findByJournalUuid(entry.uuid).map {
                tagRepository.findByUuid(it.tagUuid)
            }.flatten()

            val comments = journalCommentRepository.findByJournalUuid(entry!!.uuid).sortedBy { it.timestamp }
            DailyJournalEntryResponse(entry, comments, tags)
        }
    }

    fun getRunnersDailyJournalEntryBetweenDatesWithTitle(startDate: Date, endDate: Date, runnerId: Int, title: String?): List<DailyJournalEntryResponse> {

        if (title == null || title.isEmpty()) {
            return getRunnersDailyJournalEntryBetweenDates(startDate, endDate, runnerId)
        } else {
            val entries = dailyJournalRepository.findByTitleContainingAndDateBetweenAndRunnerId(title, startDate, endDate, runnerId)

            return if (entries.isEmpty()) {
                emptyList()
            } else {
                entries.map { entry ->
                    val tags = journalTagRelationshipRepository.findByJournalUuid(entry.uuid).map {
                        tagRepository.findByUuid(it.tagUuid)
                    }.flatten()

                    val comments = journalCommentRepository.findByJournalUuid(entry!!.uuid).sortedBy { it.timestamp }
                    DailyJournalEntryResponse(entry, comments, tags)
                }

            }
        }
    }

    fun getJournalComments(journalUuid: String): List<JournalComment> {
        return  journalCommentRepository.findByJournalUuid(journalUuid).sortedBy { it.timestamp }
    }

    fun createJournalComment(createJournalCommentRequest: CreateJournalCommentRequest): JournalComment {
        return journalCommentRepository.save(
                JournalComment(
                        createJournalCommentRequest.uuid,
                        createJournalCommentRequest.journalUuid,
                        createJournalCommentRequest.madeBy,
                        createJournalCommentRequest.message,
                        createJournalCommentRequest.timestamp
                )
        )
    }

    fun getRunnersJournal(uuid: String): RunnerJournalEntry {
        val journal = getRunnersDailyJournalEntry(uuid)!!
        val runner = runnerRepository.findById(journal.journalEntry.runnerId)

        return RunnerJournalEntry(runner.get(), journal)

    }
}