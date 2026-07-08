package com.marauders.map.data

import kotlin.math.absoluteValue

/**
 * 把没有广播名的设备映射成稳定的魔法绰号（活点地图内味）。
 * 同一 MAC 永远得到同一个绰号，不会闪烁。
 */
object NameProvider {
    private val NICKNAMES = listOf(
        "Moony", "Wormtail", "Padfoot", "Prongs",
        "Hedwig", "Fawkes", "Dobby", "Kreacher",
        "Winky", "Peeves", "Crookshanks", "Norbert",
        "Buckbeak", "Aragog", "Griphook", "Trevor"
    )

    /** 对外暴露绰号表（单元测试用） */
    val names: List<String> get() = NICKNAMES

    /** 依 MAC 稳定地取一个魔法绰号 */
    fun nameFor(address: String): String {
        var h = 0
        address.forEach { h = (h * 31 + it.code).absoluteValue }
        return NICKNAMES[h % NICKNAMES.size]
    }
}
