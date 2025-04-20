package com.terkula.uaxctf.statistics.dto

data class Quadruple<out A, out B, out C, out D>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D
)