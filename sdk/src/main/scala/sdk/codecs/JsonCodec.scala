package sdk.codecs

import io.circe.{parser, Json}

object JsonCodec {
  def toJsonString(value: Any): String = value match {
    case s: String       => Json.fromString(s).noSpaces
    case i: Int          => Json.fromInt(i).noSpaces
    case f: Float        => Json.fromFloat(f).get.noSpaces
    case d: Double       => Json.fromDouble(d).get.noSpaces
    case b: Boolean      => Json.fromBoolean(b).noSpaces
    case list: List[Any] =>
      val serializedList = list.map(toJsonString)
      val jsonList       = serializedList.map(parser.parse(_).getOrElse(Json.Null))
      Json.fromValues(jsonList).noSpaces
    case map: Map[_, _]  =>
      val serializedMap = map.asInstanceOf[Map[String, Any]].view.mapValues(toJsonString)
      val jsonMap       = serializedMap.map { case (k, v) => (k, parser.parse(v).getOrElse(Json.Null)) }
      Json.fromFields(jsonMap).noSpaces
    case _               => "Unsupported type"
  }

  def fromJsonString(jsonStr: String): Any = parser.parse(jsonStr) match {
    case Right(json) =>
      json.fold(
        jsonNull = null,
        jsonBoolean = identity,
        jsonNumber = _.toDouble,
        jsonString = identity,
        jsonArray = values => values.toList.map(v => fromJsonString(v.noSpaces)),
        jsonObject = obj => obj.toMap.view.mapValues(v => fromJsonString(v.noSpaces)).toMap,
      )
    case Left(error) => s"Parsing error: $error"
  }
}
