package br.com.dextra.bifrost

import java.io.*

internal fun serializeObject(obj: Any?): ByteArray? {
  return try {
    val bytesOut = ByteArrayOutputStream()
    val oos = ObjectOutputStream(bytesOut)
    oos.writeObject(obj)
    oos.flush()
    val bytes: ByteArray = bytesOut.toByteArray()
    bytesOut.close()
    oos.close()
    bytes
  } catch (e: Exception) {
    e.printStackTrace()
    null
  }
}
