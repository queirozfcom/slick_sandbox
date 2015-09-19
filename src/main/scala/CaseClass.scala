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

  def uniqueName = index("unique_name", (name), unique = true)

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

  def selectByName(name: String): Unit = {

    val q = users.filter(user => user.name === name)

    Await.result(
      db.run(q.result).map { res =>
        // res is a Vector[User]
        println(res)
      }, timeout)
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

  def deleteByName(name: String): Unit = {

    val q = users.filter(_.name === name).delete

    Await.result(
      db.run(q).map { numAffectedRows =>
        println(numAffectedRows)
      }, timeout)
  }


  def transaction(name1: String, name2: String, sampleUsername: String): Unit = {

    // here we will delete an user and create another two users
    // but we will abort the transaction and recover the deleted used
    // in case we fail to add the two next users

    val qDelete = users.filter(_.name === "john").delete

    val addSampleUser = (users returning users.map(_.id)) += User(0, name = sampleUsername, password = "passwd", email = None)

    val q1 = (users returning users.map(_.id)) += User(0, name = name1, password = "passwd", email = None)

    val q2 = (users returning users.map(_.id)) += User(0, name = name2, password = "passwd", email = None)

    val actions = for {

      affectedRows <- qDelete

      // watch how inserting sampleUser to the database will be 'rolled back' if the preconditions fail
      _ <- addSampleUser

      // see how an exception is thrown if affectedRows is different from 1
      // exceptions thrown here cause the transaction to be rolled back
      newUserId1 <- if (affectedRows == 1) q1 else DBIO.failed(new SQLException(s"""precondition failed... aborting! Try running selectByName("$sampleUsername") to see how it's been rolled back"""))
      newUserId2 <- if (affectedRows == 1) q2 else DBIO.failed(new SQLException(s"""precondition failed... aborting! Try running selectByName("$sampleUsername") to see how it's been rolled back"""))

    }yield(newUserId1, newUserId2)

    Await.result(
      db.run(actions.transactionally).map { res =>
        println(s"Newly-added users had the following ids ${res._1} and ${res._2}")
      }.recover {
        case e: SQLException => println("Caught exception: " + e.getMessage)
      }, timeout)

  }


  def insertUser(name: String, password: String): Unit = {

    // use zero as the ID because the database will generate a new ID
    val newUser = (users returning users.map(_.id)) += User(0, name = name, password = password, email = None)

    // note that the newly-added id is returned instead of
    // the number of affected rows
    Await.result(db.run(newUser).map { newId =>
      newId match {
        case x: Long => println(s"last entry added had id $x")
      }
    }.recover {
      case e: SQLException => println("Caught exception: " + e.getMessage)
    }, timeout)

  }


}
