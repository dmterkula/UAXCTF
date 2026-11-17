package com.terkula.uaxctf.statistics.request

class CreateMeetResultRequest(
    var season: String,
    var runnerId: Int,
    var meetName: String,
    var time: String,
    var place: Int,
    var passesLastMile: Int,
    var passesSecondMile: Int,
    var skullsEarned: Int,
    var mileOneSplit: String,
    var mileTwoSplit: String,
    var mileThreeSplit: String,
    // Manual override from the app's division picker — takes precedence over everything below.
    var divisionId: Int? = null,
    // From the scraper: this race's actual clock start time ("HH:mm"), matched against each
    // configured division's own start time. Preferred over divisionSequence, since sequence
    // counts every race at the meet (including other genders/schools) while a team's own
    // divisions are usually only added for the races it actually runs.
    var raceStartTime: String? = null,
    // From the scraper: the race's position on the meet's events page (1 = first race of the
    // day). Preferred over divisionLabel since division names vary by timing site.
    var divisionSequence: Int? = null,
    // From the scraper: the scraped race/division heading text, used as a fallback match and
    // as the name for a newly auto-created division when nothing else matches.
    var divisionLabel: String? = null
)