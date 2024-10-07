package repository

import domain._
import slick.jdbc.MySQLProfile.api._
import scala.concurrent.{Future, ExecutionContext}

class ProfileRepository(db: Database)(implicit ec: ExecutionContext) {

  private class ParentsTable(tag: Tag) extends Table[ParentProfile](tag, "parents") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name")
    def child = column[String]("child")
    def * = (id, name, child) <> (ParentProfile.tupled, ParentProfile.unapply)
  }

  private class PaymentMethodsTable(tag: Tag) extends Table[PaymentMethod](tag, "payment_methods") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def parentId = column[Long]("parent_id")
    def method = column[String]("method")
    def isActive = column[Boolean]("is_active")
    def createdAt = column[String]("created_at") // Add this line for createdAt
    def * = (id, parentId, method, isActive, createdAt) <> (PaymentMethod.tupled, PaymentMethod.unapply)
  }

  private class InvoicesTable(tag: Tag) extends Table[Invoice](tag, "invoices") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def parentId = column[Long]("parent_id")
    def amount = column[Double]("amount")
    def date = column[String]("date")
    def * = (id, parentId, amount, date) <> (Invoice.tupled, Invoice.unapply)
  }

  private val parents = TableQuery[ParentsTable]
  private val paymentMethods = TableQuery[PaymentMethodsTable]
  private val invoices = TableQuery[InvoicesTable]

  def listPaymentMethods(parentId: Long): Future[Seq[PaymentMethod]] =
    db.run(paymentMethods.filter(_.parentId === parentId).result)

  def listInvoices(parentId: Long): Future[Seq[Invoice]] =
    db.run(invoices.filter(_.parentId === parentId).result)

  def getParentProfile(parentId: Long): Future[Option[ParentProfile]] =
    db.run(parents.filter(_.id === parentId).result.headOption)

  def addPaymentMethod(paymentMethod: PaymentMethod): Future[Either[String, PaymentMethod]] =
    db.run((paymentMethods returning paymentMethods.map(_.id)
      into ((method, id) => method.copy(id = id))) += paymentMethod)
      .map(Right(_))
      .recover { case ex => Left(s"Failed to add payment method: ${ex.getMessage}") }

  def setActivePaymentMethod(parentId: Long, methodId: Long): Future[Either[String, PaymentMethod]] = {
    val updateActions = for {
      _ <- paymentMethods.filter(_.parentId === parentId).map(_.isActive).update(false)
      updatedRows <- paymentMethods.filter(_.id === methodId).map(_.isActive).update(true)
      updatedMethod <- paymentMethods.filter(_.id === methodId).result.headOption if updatedRows > 0
    } yield updatedMethod

    db.run(updateActions.transactionally)
      .map(_.toRight(s"No payment method found with id $methodId"))
      .recover { case ex => Left(s"Failed to set active payment method: ${ex.getMessage}") }
  }

  def deletePaymentMethod(parentId: Long, methodId: Long): Future[Either[String, Int]] = {
    db.run(paymentMethods.filter(pm => pm.parentId === parentId && pm.id === methodId).delete)
      .map {
        case rowsDeleted if rowsDeleted > 0 => Right(rowsDeleted)
        case _ => Left(s"No payment method found with id $methodId for parent $parentId")
      }
      .recover { case ex => Left(s"Failed to delete payment method: ${ex.getMessage}") }
  }

}
