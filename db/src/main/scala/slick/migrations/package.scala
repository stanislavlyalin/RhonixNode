package slick

import slick.migration.api.{Dialect, ReversibleMigrationSeq}

/// Package for recording database migrations.
package object migrations {

  // This should be appended only.
  def all(dialect: Dialect[?]): ReversibleMigrationSeq = Baseline(dialect)
}
