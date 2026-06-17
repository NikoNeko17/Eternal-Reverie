package com.nikoneko.eternalReverie

import net.citizensnpcs.api.CitizensAPI
import net.citizensnpcs.api.event.CitizensEnableEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

class CitizensHookListener : Listener {

    @EventHandler
    fun onCitizensEnable(event: CitizensEnableEvent) {
        // En este punto exacto, la API de Citizens está lista para ser usada de forma segura
        val registry = CitizensAPI.getNPCRegistry()
        println("¡Conexión RPG con Citizens establecida con éxito!")
    }
}
