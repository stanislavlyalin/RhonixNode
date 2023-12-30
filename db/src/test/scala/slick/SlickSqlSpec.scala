package slick

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{Outcome, Succeeded}
import slick.jdbc.JdbcProfile

class SlickSqlSpec extends AnyFlatSpec with Matchers {
  // TODO: only SQL schema for `Validator` should be printed, but for some reason all tables are printed.
  it should "print SQL schema for testedTable" in {
    import slick.qValidators
    val testedTable = qValidators

    def test(profile: JdbcProfile): IO[Outcome] = {
      def showSql: String = {
        import profile.api.*
        testedTable.schema.createStatements.mkString(";\n")
      }
      IO(println(showSql)).as(Succeeded)
    }
    EmbeddedH2SlickDb[IO]
      .use(implicit db => test(db.profile))
      .unsafeRunSync()
  }
}
