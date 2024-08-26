package com.terkula.uaxctf.training.model.journal

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import java.sql.Date
import java.sql.Timestamp
import javax.persistence.*

@Entity
@Table(name = "daily_journal", schema = "uaxc")
@JsonInclude(value = JsonInclude.Include.NON_NULL)
class DailyJournalEntry(
        @Column(name = "runner_id")
        var runnerId: Int,
        @Column(name = "uuid")
        var uuid: String,
        @Column(name = "date")
        var date: Date,
        @Column(name = "text")
        var text: String,
        @Column(name = "title")
        var title: String,
        //@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy hh:mm:ss")
        @Column(name = "timestamp")
        var timestamp: Timestamp,
        @Column(name = "what_if")
        var whatIfStatement: String?,
        @Column(name = "favorite")
        var favorite: Boolean
) {

    @JsonIgnore
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JoinColumn
    val id: Int = 0

}


