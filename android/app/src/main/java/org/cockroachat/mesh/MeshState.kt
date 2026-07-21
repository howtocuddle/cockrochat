package org.cockroachat.mesh

import kotlinx.coroutines.flow.MutableStateFlow

data class Stats(
    val epoch: UInt,
    val neighborsThisEpoch: Int,
    val totalHeard: Int,
    val localSketch: List<ULong>,
    val advertising: Boolean,
    val scanning: Boolean,
    val codedPhyActive: Boolean,
    val note: String = ""
)

object MeshState {
    val running = MutableStateFlow(false)
    val stats = MutableStateFlow(
        Stats(
            epoch = 0u,
            neighborsThisEpoch = 0,
            totalHeard = 0,
            localSketch = emptyList(),
            advertising = false,
            scanning = false,
            codedPhyActive = false
        )
    )
    val measurement = Measurement()

    @Volatile
    var config: MeshConfig = MeshConfig()
}
