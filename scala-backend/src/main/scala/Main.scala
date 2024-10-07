import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model._
import slick.jdbc.MySQLProfile.api._
import repository.ProfileRepository
import sangria.execution.{ErrorWithResolver, Executor, QueryReducer}
import sangria.parser.QueryParser
import sangria.marshalling.circe._
import io.circe.{Json}
import io.circe.parser._
import io.circe.syntax._
import api.GraphQLSchema
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Success, Failure}
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.server.Route
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
import ch.megard.akka.http.cors.scaladsl.settings.CorsSettings
import ch.megard.akka.http.cors.scaladsl.model.{HttpHeaderRange, HttpOriginMatcher}


object Main extends App with FailFastCirceSupport {
  implicit val system: ActorSystem = ActorSystem("parent-profile")
  implicit val executionContext: scala.concurrent.ExecutionContextExecutor = system.dispatcher

  val db = Database.forConfig("mysqlDB")
  val profileRepo = new ProfileRepository(db)

  val corsSettings = CorsSettings.defaultSettings
    .withAllowGenericHttpRequests(true)
    .withAllowedOrigins(HttpOriginMatcher.*)
    .withAllowedMethods(Seq(GET, POST, PUT, DELETE, OPTIONS))
    .withAllowedHeaders(HttpHeaderRange.*)
    .withAllowCredentials(true)
    .withMaxAge(Some(1800L))

  val route =
      cors(corsSettings) {
      path("graphql") {
        post {
          entity(as[Json]) { json =>
            val queryOpt = json.hcursor.downField("query").as[String]
            val variablesOpt = json.hcursor.downField("variables").focus.getOrElse(Json.obj())

            queryOpt match {
              case Right(query) =>
                QueryParser.parse(query) match {
                  case Success(ast) =>
                    complete(
                      Executor.execute(
                        GraphQLSchema.schema,
                        ast,
                        userContext = profileRepo,
                        variables = variablesOpt
                      ).map(result => HttpResponse(OK, entity = result.asJson.noSpaces))
                        .recover {
                          case error: ErrorWithResolver =>
                            HttpResponse(BadRequest, entity = error.resolveError.asJson.noSpaces)
                        }
                    )

                  case Failure(error) =>
                    complete(BadRequest, Json.obj("error" -> Json.fromString(s"Invalid GraphQL query: ${error.getMessage}")))
                }

              case Left(error) =>
                complete(BadRequest, Json.obj("error" -> Json.fromString(s"Invalid GraphQL query: ${error.getMessage}")))
            }
          } ~
          get {
            complete(HttpResponse(OK, entity = "GraphQL endpoint is working. Please send POST requests to this endpoint."))
          } ~
          options {
            complete(StatusCodes.OK)
          }
        }
      }
    }

  val bindingFuture = Http().bindAndHandle(route, "0.0.0.0", 9001)

  println(s"Server online at http://0.0.0.0:9001/")
  println("Press RETURN to stop...")
  // Keep the application running
  scala.sys.addShutdownHook {
    bindingFuture
      .flatMap(_.unbind())
      .onComplete(_ => system.terminate())
  }

  // Wait indefinitely
  Thread.currentThread().join()
}

