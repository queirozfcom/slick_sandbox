import slick.driver.MySQLDriver.api._
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object Raw {

  val db = Database.forConfig("slickexamples")
  val timeout = 20.seconds

  def main(args: Array[String]) {

    val msg =
      """
        |
        |
        |To run the examples, open sbt console at the project root, and then call the method you wish to run
        |
        |For example:
        |
        |$ sbt console
        |....
        |scala> SlickExamples.rawSqlSelect
        |Vector(.....)
        |scala> SlickExamples.rawSqlInsert
        |
        |
      """.stripMargin

    println(msg)
    System.exit(0)
  }

  def select(): Unit = {

    val q = sql"select u.name from users u".as[String]

    Await.result(
      db.run(q).map { res =>
        // res is a Vector[String]
        println(res)
      }, timeout
    )

  }

  def insert(): Unit = {

    // queries that don't return a resultset use 'sqlu'
    val q = sqlu"insert into users(name,password) values ('john','mypasswd')"

    Await.result(
      db.run(q).map{ res =>
        // prints the number of affected rows
        // which amount to 1 if the insert was successful
        println(res)
      } ,timeout)

  }

  def update():Unit = {
    val q = sqlu"update users u set u.name='mary' where u.name = 'john'"

    Await.result(
      db.run(q).map{ res =>
        // again, the number of affected rows
        println(res)
      } ,timeout)

  }


  def errorHandling():Unit = {
    val id = "1"
    val name = "peter"
    val password = "passwd"

    val q = sqlu"insert into users(id,name,password) values($id,$name,$password)"

    Await.result(db.run(q).map { res =>
      res match{
        case 1 => println("all good")
        case 0 => println("couldn't add record")
      }
    }.recover{
      case e:Exception =>println("Caught exception: "+e.getMessage)
    }, timeout)
  }

}
