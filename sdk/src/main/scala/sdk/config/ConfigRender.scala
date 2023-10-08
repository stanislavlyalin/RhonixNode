package sdk.config

import scala.reflect.runtime.universe.*

object ConfigRender {
  def apply(cc: Any): Iterable[(String, Any, String)] = {
    val rm             = runtimeMirror(cc.getClass.getClassLoader)
    val instanceMirror = rm.reflect(cc)
    val classSymbol    = instanceMirror.symbol.asClass

    val annotations = classSymbol.primaryConstructor.typeSignature.paramLists.head.map { s =>
      val annotationString = s.annotations.head.tree.children.tail.head match {
        case Literal(Constant(value: String)) => value
        case _                                => "Could not extract annotation string"
      }
      s.name.decodedName.toString -> annotationString
    }.toMap

    val fields = classSymbol.typeSignature.members.collect { case m: MethodSymbol if m.isCaseAccessor => m }

    fields.toList.sortBy(_.name.toString).map { fieldSymbol =>
      val fieldName        = fieldSymbol.name.toString
      val fieldValue       = instanceMirror.reflectField(fieldSymbol.asTerm).get
      val fieldDescription = annotations.getOrElse(fieldName, "No description provided")

      (fieldName, fieldValue, fieldDescription)
    }
  }

  def referenceConf(root: String, classes: Any*): String = classes
    .map { clz =>
      val rm             = runtimeMirror(clz.getClass.getClassLoader)
      val instanceMirror = rm.reflect(clz)
      val classSymbol    = instanceMirror.symbol.asClass
      val configName     = classSymbol.annotations.head.tree.children.tail.head match {
        case Literal(Constant(value: String)) => value
        case _                                => "Could not extract annotation string"
      }

      ConfigRender(clz)
        .map { case (name, value, anno) =>
          s"""|# $anno
              |$root.$configName.$name: $value
              |""".stripMargin
        }
        .mkString("")
    }
    .mkString(s"\n")
}
