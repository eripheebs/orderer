package postgresProfile

import com.github.tminglei.slickpg._

// Adding the ability to have date support, docs: https://github.com/tminglei/slick-pg

trait MyPostgresProfile extends ExPostgresProfile
                          with PgDate2Support{

  override protected def computeCapabilities: Set[slick.basic.Capability] = 
    super.computeCapabilities + slick.jdbc.JdbcCapabilities.insertOrUpdate

  override val api = MyAPI

  object MyAPI extends API with DateTimeImplicits {
  }

}

object MyPostgresProfile extends MyPostgresProfile
