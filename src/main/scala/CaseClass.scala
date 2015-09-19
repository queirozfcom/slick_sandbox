import java.sql.SQLException

import com.mysql.jdbc.exceptions.MySQLDataException
import slick.driver.MySQLDriver.api._
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

case class User(id: Long, name: String, password: String, email: Option[String])

class UsersTable(tag: Tag) extends Table[User](tag, "users") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

  def name = column[String]("name")

  def password = column[String]("password")

  def email = column[Option[String]]("email")

  def * = (id, name, password, email) <>(User.tupled, User.unapply)
}

object CaseClass {

  val db = Database.forConfig("slickexamples")
  val timeout = 20.seconds
  val users = TableQuery[UsersTable]

  def selectName(): Unit = {
    val q = users.map(_.name)

    Await.result(
      db.run(q.result).map { res =>
        // res is a Vector[String]
        println(res)
      }, timeout)

  }

  def selectWithCondition(name:String): Unit = {

    val q = users.filter(user => user.name === name)

    Await.result(
      db.run(q.result).map { res =>
        // res is a Vector[User]
        println(res)
      },timeout)
  }

  def selectWithCondition2(): Unit = {

    // in slick, '===' means equal to
    // likewise, '=!=' means not equal to

    // also, you can use short form lambdas

    val q = users.filter(_.name =!= "mary").map(_.email)

    Await.result(
      db.run(q.result).map { res =>
        // res is a Vector[String]
        println(res)
      }, timeout)

  }

  def selectAll(): Unit = {
    // this is equivalent to select * from users
    val q = users

    Await.result(
      db.run(q.result).map { res =>
        // res is a Vector[User]
        println(res)
      }, timeout)

  }

  def updateName(): Unit = {

    // update users u set u.name='peter' where u.id=1
    val q = users.filter(_.id === 1L).map(_.name).update("peter")

    Await.result(
      db.run(q).map { numAffectedRows =>
        println(numAffectedRows)
      }, timeout)

  }

  def deleteByName(name:String): Unit = {

    val q = users.filter(_.name === name).delete

    Await.result(
      db.run(q).map { numAffectedRows =>
        println(numAffectedRows)
      }, timeout)
  }


  def transaction():Unit = {

    // here we will delete an user and create another two users
    // but we will abort the transaction and recover the deleted used
    // in case we fail to add the tow next users

  }


  def insertUser(name:String,password:String): Unit = {

    // use zero as the ID because the database will generate a new ID
    val newUser = (users returning users.map(_.id)) += User(0,name=name,password=password,email=None)

    // note that the newly-added id is returned instead of
    // the number of affected rows
    Await.result(db.run(newUser).map { newId =>
      newId match {
        case x:Long => println(s"last entry added had id $x")
      }
    }.recover {
      case e: SQLException => println("Caught exception: " + e.getMessage)
    }, timeout)

  }



]


}
