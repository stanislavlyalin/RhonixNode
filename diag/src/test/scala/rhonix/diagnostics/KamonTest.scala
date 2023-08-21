package rhonix.diagnostics

import cats.effect.{IO, IOApp}
import rhonix.diagnostics.syntax.KamonSyntax.kamonSyntax

import scala.concurrent.duration.DurationInt

object KamonTest extends IOApp.Simple {
  val diagResource           = for {
    kmn <- KamonDiagnostics.kamonResource[IO]()
  } yield (kmn, ())
  override def run: IO[Unit] = diagResource.use { case (idb, kmn) =>
    def f = IO.sleep(1.second)
    f.kamonTimer("Dummy function").replicateA_(100)
  }
  // TODO embedded inflixDB for a test?
}
