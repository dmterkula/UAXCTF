package com.terkula.uaxctf.training.model.tags

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import javax.persistence.*

@Entity
@Table(name = "journal_tag_relationships", schema = "uaxc")
@JsonInclude(value = JsonInclude.Include.NON_NULL)
class JournalTagRelationship(
     @Column(name = "journal_uuid")
     var journalUuid: String,
     @Column(name = "tag_uuid")
     var tagUuid: String,
     @Column(name = "runner_id")
     var runnerId: Int?
) {
    @JsonIgnore
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JoinColumn
    val id: Int = 0
}