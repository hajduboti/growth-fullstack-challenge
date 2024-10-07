package api

import domain._
import repository.ProfileRepository
import sangria.schema._
import sangria.macros.derive._
import scala.concurrent.ExecutionContext

object GraphQLSchema {

  implicit val PaymentMethodType: ObjectType[Unit, PaymentMethod] = deriveObjectType[Unit, PaymentMethod]()
  implicit val InvoiceType: ObjectType[Unit, Invoice] = deriveObjectType[Unit, Invoice]()
  implicit val ParentProfileType: ObjectType[Unit, ParentProfile] = deriveObjectType[Unit, ParentProfile]()

  def QueryType(implicit ec: ExecutionContext) = ObjectType(
    "Query",
    fields[ProfileRepository, Unit](
      Field("parentProfile", OptionType(ParentProfileType),
        arguments = Argument("parentId", LongType) :: Nil,
        resolve = ctx => ctx.ctx.getParentProfile(ctx.arg[Long]("parentId"))
      ),
      Field("paymentMethods", ListType(PaymentMethodType),
        arguments = Argument("parentId", LongType) :: Nil,
        resolve = ctx => ctx.ctx.listPaymentMethods(ctx.arg[Long]("parentId"))
      ),
      Field("invoices", ListType(InvoiceType),
        arguments = Argument("parentId", LongType) :: Nil,
        resolve = ctx => ctx.ctx.listInvoices(ctx.arg[Long]("parentId"))
      )
    )
  )

  def MutationType(implicit ec: ExecutionContext) = ObjectType(
    "Mutation",
    fields[ProfileRepository, Unit](
      Field("addPaymentMethod", OptionType(PaymentMethodType),
        arguments = Argument("parentId", LongType) :: Argument("method", StringType) :: Nil,
        resolve = ctx => {
          val newMethod = PaymentMethod(
            id = 0,
            parentId = ctx.arg[Long]("parentId"),
            method = ctx.arg[String]("method"),
            isActive = false,
            createdAt = ""  // Placeholder for createdAt since it's set by the database
          )

          ctx.ctx.addPaymentMethod(newMethod).map {
            case Right(method) => Some(method)
            case Left(_) => None
          }
        }
      ),
      Field("setActivePaymentMethod", OptionType(PaymentMethodType),
        arguments = Argument("parentId", LongType) :: Argument("methodId", LongType) :: Nil,
        resolve = ctx => ctx.ctx.setActivePaymentMethod(ctx.arg[Long]("parentId"), ctx.arg[Long]("methodId")).map {
          case Right(method) => Some(method)
          case Left(_) => None
        }
      ),
      Field("deletePaymentMethod", BooleanType,
        arguments = Argument("parentId", LongType) :: Argument("methodId", LongType) :: Nil,
        resolve = ctx => {
          ctx.ctx.deletePaymentMethod(ctx.arg[Long]("parentId"), ctx.arg[Long]("methodId")).map {
            case Right(_) => true
            case Left(_) => false
          }
        }
      )
    )
  )

  def schema(implicit ec: ExecutionContext): Schema[ProfileRepository, Unit] = Schema(QueryType, Some(MutationType))
}
