package sdk.primitive

import scala.collection.mutable

trait MapSyntax:
  extension [K, V](map: Map[K, V])
    def getUnsafe(k: K): V =
      val vOpt = map.get(k)
      require(vOpt.isDefined, s"No key $k in a map.")
      vOpt.get

  extension [K, V](map: mutable.Map[K, V])
    def getUnsafe(k: K): V =
      val vOpt = map.get(k)
      require(vOpt.isDefined, s"No key $k in a map.")
      vOpt.get
