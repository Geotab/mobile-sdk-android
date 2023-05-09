package com.geotab.mobile.sdk.module.iox.ioxBle

enum class IoxBleState(val ioxBleStateId: Int) {
    IDLE(0),
    ADVERTISING(1),
    SYNCING(2),
    HANDSHAKING(3),
    CONNECTED(4),
}
