package domain

case class ParentProfile(id: Long, name: String, child: String)
case class ChildProfile(id: Long, parentId: Long, name: String)
case class PaymentMethod(id: Long, parentId: Long, method: String, isActive: Boolean, createdAt: String)
case class Invoice(id: Long, parentId: Long, amount: Double, date: String)

