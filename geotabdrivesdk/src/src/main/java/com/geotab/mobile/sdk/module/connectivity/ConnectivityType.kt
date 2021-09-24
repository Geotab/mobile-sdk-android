package com.geotab.mobile.sdk.module.connectivity

enum class ConnectivityType(val type: String) {
    UNKNOWN("UNKNOWN"),
    NONE("NONE"),
    ETHERNET("ETHERNET"),
    WIFI("WIFI"),
    CELL("CELL");

    override fun toString(): String {
        return type
    }
}
