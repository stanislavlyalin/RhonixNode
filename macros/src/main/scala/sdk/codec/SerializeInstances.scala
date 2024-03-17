package sdk.codec

import cats.Eval
import sdk.codecs.SerializePrimitiveTypes

object SerializeInstances extends SerializeDerivation[Eval] with SerializePrimitiveTypes[Eval]
