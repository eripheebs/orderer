import postgresProfile.MyPostgresProfile.api._

// todo: extract to own file
// todo: add config
final case class AppConfig ()

class Dependencies (
    db: Database,
    // services: Services
    // logging:
)

// can pass config in here if we want to change the DB setup etc
// no config is set up to be passed ATM
class OrdererApp(config: AppConfig) {
    // here we could initialise logging etc and pass it down to the different layers 

    // todo make this more configurable
    val db = Database.forConfig("postgres")
    // pass the DB to the create schema initialiser

    // initialise service layer. pass Dependencies
    // set up handlers. pass Dependencies

    // close DB
}
