package com.opencvapp
//class inspired by the LiveSosFilter from Samuel Pröll's yarppg project
class LiveSosFilter(
    private val sos: Array<DoubleArray> = BANDPASS_SOS
) {
    private val nSections = sos.size
    private val state = Array(nSections) {
        DoubleArray(2)
    }

    fun process(x0: Double): Double {
        var x = x0
        var y: Double

        for (s in 0 until nSections) {
            val b0 = sos[s][0]
            val b1 = sos[s][1]
            val b2 = sos[s][2]
            val a0 = sos[s][3]
            val a1 = sos[s][4]
            val a2 = sos[s][5]

            // Direct Form II Transposed
            y = b0 * x + state[s][0]
            state[s][0] = b1 * x - a1 * y + state[s][1]
            state[s][1] = b2 * x - a2 * y

            // Normalize if needed
            x = if (a0 != 1.0) y / a0 else y
        }

        return x
    }

    fun reset() {
        for (section in state) {
            section[0] = 0.0
            section[1] = 0.0
        }
    }

    companion object {

        /**
         * 4th-order Butterworth band-pass filter
         * Designed for ~0.7–3.0 Hz (≈ 42–180 BPM)
         * Sampling rate assumed ~30 FPS
         *
         * Generated via SciPy:
         * signal.butter(4, [0.7, 3.0], btype='bandpass', fs=30, output='sos')
         */
        val BANDPASS_SOS = arrayOf(
            doubleArrayOf(0.004824, 0.009649, 0.004824, 1.0, -1.799096, 0.817512),
            doubleArrayOf(1.0, 2.0, 1.0, 1.0, -1.561018, 0.641352),
            doubleArrayOf(1.0, -2.0, 1.0, 1.0, -1.929356, 0.932433),
            doubleArrayOf(1.0, -2.0, 1.0, 1.0, -1.962449, 0.964921)
        )
    }
}