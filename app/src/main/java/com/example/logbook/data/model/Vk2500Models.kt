package com.example.logbook.data.model

interface Vk2500RecordItem {
    val id: Long
    val profileId: Long
    val isHistorical: Boolean
    val date: String
    val sortOrder: Long
    val i: Int
    val g: Double
    val colSNE: Int
    val pprA: Int
    val col8: Double
    val b: Double
    val col10: Double
    val col11: Double
    val col12: Double
    val d: Double
    val e: Double
    val h: Double
    val pprH: Double
    val isTotal: Boolean
    val totalMonth: String
    val needsManualRecalc: Boolean
}

interface Vk2500StartItem {
    val id: Long
    val profileId: Long
    val date: String
    val i: Int
    val g: Double
    val colSNE: Int
    val pprA: Int
    val col8: Double
    val b: Double
    val col10: Double
    val col11: Double
    val col12: Double
    val d: Double
    val e: Double
    val h: Double
    val pprH: Double
    val coeffA: Double
    val coeffH: Double

    fun copyWith(
        date: String = this.date,
        i: Int = this.i,
        g: Double = this.g,
        colSNE: Int = this.colSNE,
        pprA: Int = this.pprA,
        col8: Double = this.col8,
        b: Double = this.b,
        col10: Double = this.col10,
        col11: Double = this.col11,
        col12: Double = this.col12,
        d: Double = this.d,
        e: Double = this.e,
        h: Double = this.h,
        pprH: Double = this.pprH,
        coeffA: Double = this.coeffA,
        coeffH: Double = this.coeffH
    ): Vk2500StartItem
}
