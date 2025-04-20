package com.terkula.uaxctf.training.service

import com.terkula.uaxctf.training.repository.CoachDavidIsmsRepository
import org.springframework.stereotype.Service

@Service
class CoachDavidIsmsService (
        val coachDavidIsmsRepository: CoachDavidIsmsRepository
    ) {

    fun getCoachDavidIsms(forNu: Boolean, forUa: Boolean): List<String> {
        return if (forNu && !forUa) {
            coachDavidIsmsRepository.findByForNu(true).map { it.text }
        } else if (forUa && !forNu) {
            coachDavidIsmsRepository.findByForUa(true).map { it.text }
        } else if (forUa && forNu) {
            coachDavidIsmsRepository.findAll().toList().map { it.text }
        } else {
            emptyList()
        }
    }

}