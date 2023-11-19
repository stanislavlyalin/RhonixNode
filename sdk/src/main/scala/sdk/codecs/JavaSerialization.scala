package sdk.codecs

// Serialization using Java's built-in serialization. Not cross-client/platform, so can be used just for testing.
object JavaSerialization {
  def serialize(x: Serializable): Array[Byte] = {
    val baos  = new java.io.ByteArrayOutputStream()
    val oos   = new java.io.ObjectOutputStream(baos)
    oos.writeObject(x)
    oos.flush()
    val bytes = baos.toByteArray
    baos.close()
    oos.close()
    bytes
  }
}
