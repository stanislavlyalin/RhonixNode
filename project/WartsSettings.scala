import wartremover.Wart as WartType
import wartremover.WartRemover.autoImport.{Wart, Warts}

object WartsSettings {
  val DEFAULTS: Seq[WartType] = Warts.allBut(disabledWarts*)

  val DEFAULTS_SCALA_2: Seq[WartType] = Warts.allBut(disabledLegacy*)

  private lazy val disabledWarts = Seq(
    // those we want
    Wart.DefaultArguments,
    Wart.ImplicitParameter,
    Wart.ImplicitConversion,
    Wart.LeakingSealed,
    Wart.Recursion,
    Wart.TripleQuestionMark,
    // those don't want
    Wart.Overloading,
    Wart.Nothing,
    Wart.Equals,
    Wart.PublicInference,
    Wart.ArrayEquals,
    Wart.While,
    Wart.Any,
    Wart.Product,
    Wart.Serializable,
    Wart.OptionPartial,
    Wart.Option2Iterable,
    Wart.ToString,
    Wart.MutableDataStructures,
    Wart.FinalVal,
    Wart.Null,
    Wart.AsInstanceOf,
    Wart.ExplicitImplicitTypes,
    Wart.StringPlusAny,
    Wart.AnyVal,
  )

  // For compatibility with old Scala 2 project (added after migration to Scala 2.13)
  private lazy val disabledLegacy: Seq[WartType] = disabledWarts ++ Seq(
    Wart.IterableOps,
    Wart.JavaSerializable,
    Wart.ListUnapply,
    Wart.GlobalExecutionContext,
    Wart.NoNeedImport,
    Wart.PlatformDefault,
    Wart.JavaNetURLConstructors,
    Wart.SizeIs,
    Wart.SizeToLength,
    Wart.ListAppend,
    Wart.AutoUnboxing,
    Wart.RedundantConversions,
  )
}
