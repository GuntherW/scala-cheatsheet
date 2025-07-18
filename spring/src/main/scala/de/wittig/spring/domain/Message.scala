package de.wittig.spring.domain

import java.time.LocalDate

case class Message(
    content: String,
    id: Int,
    now: LocalDate
)
