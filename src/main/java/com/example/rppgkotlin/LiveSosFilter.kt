package com.example.rppgkotlin

//class inspired by the LiveSosFilter from Samuel Pröll's yarppg project
class LiveSosFilter(private val sos: Array<DoubleArray>) {
    private val nSections = sos.size
    private val state = Array(nSections) {
        DoubleArray(2)
    }

    fun process(x0: Double): Double {
        var x = x0
        var y = 0.0
        for(s in 0 until nSections){
            val b0 = sos[s][0]
            val b1 = sos[s][1]
            val b2 = sos[s][2]
            val a0 = sos[s][3]
            val a1 = sos[s][4]
            val a2 = sos[s][5]

            y = b0 * x + state[s][0]
            state[s][0] = b1 * x - a1 * y + state[s][1]
            state[s][1] = b2 * x - a2 * y
            x = y
        }
        return y
    }
}