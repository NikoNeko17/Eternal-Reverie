package com.nikoneko.eternalReverie.remnants

import org.bukkit.inventory.ItemStack
import org.bukkit.util.io.BukkitObjectInputStream
import org.bukkit.util.io.BukkitObjectOutputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Base64

/**
 * Serializa ItemStacks completos (con TODO su PDC — materiales, rareza de
 * celda, UUID de instancia, etc.) a String Base64 y viceversa. Usado para
 * persistir Vestigios equipados directamente en el PDC del jugador sin
 * necesitar un inventario "vivo" en memoria — el ItemStack serializado ES
 * el dato completo, nada se reconstruye ni se recalcula al guardarlo.
 *
 * Bukkit soporta esto nativamente vía BukkitObjectOutputStream, que ya sabe
 * serializar ItemStack (incluyendo su PersistentDataContainer) sin ayuda
 * extra de nuestra parte.
 */
object ItemStackSerializer {

    fun serialize(item: ItemStack): String {
        val outputStream = ByteArrayOutputStream()
        BukkitObjectOutputStream(outputStream).use { dataOutput ->
            dataOutput.writeObject(item)
        }
        return Base64.getEncoder().encodeToString(outputStream.toByteArray())
    }

    fun deserialize(data: String?): ItemStack? {
        return try {
            val inputStream = ByteArrayInputStream(Base64.getDecoder().decode(data))
            BukkitObjectInputStream(inputStream).use { dataInput ->
                dataInput.readObject() as? ItemStack
            }
        } catch (e: Exception) {
            null // dato corrupto o formato viejo — se ignora esa entrada, no rompe el resto
        }
    }
}