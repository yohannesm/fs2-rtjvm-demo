package disneystreaming

import cats.effect.std.Queue
import disneystreaming.Main.Data.{chrisEvans, chrisHemsworth, jeremyRenner, markRuffalo, robertDowneyJr, scarlettJohansson}
import disneystreaming.Main.Model.Actor
import fs2.Pipe
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import scala.util.Random
//import cats.effect.std.Queue
import cats.effect.{IO, IOApp, Sync}
//import cats.syntax.all._
import disneystreaming.Main.Data.{andrewGarfield, benFisher, ezraMiller, galGodot, henryCavil, jasonMomoa, rayHardy, tobeyMaguire, tomHolland}
import fs2.{Chunk, Pure, Stream}
import scala.concurrent.duration._
//import fs2.{Chunk, INothing, Pipe, Pull, Pure, Stream}

object Main extends IOApp.Simple {

  implicit def unsafeLogger[F[_]: Sync]: SelfAwareStructuredLogger[F] = Slf4jLogger.getLogger

  object Model {
    case class Actor(id: Int, firstName: String, lastName: String)
  }

  object Data {
    // Justice League
    val henryCavil: Actor = Actor(0, "Henry", "Cavill")
    val galGodot: Actor   = Actor(1, "Gal", "Godot")
    val ezraMiller: Actor = Actor(2, "Ezra", "Miller")
    val benFisher: Actor  = Actor(3, "Ben", "Fisher")
    val rayHardy: Actor   = Actor(4, "Ray", "Hardy")
    val jasonMomoa: Actor = Actor(5, "Jason", "Momoa")

    // Avengers
    val scarlettJohansson: Actor = Actor(6, "Scarlett", "Johansson")
    val robertDowneyJr: Actor    = Actor(7, "Robert", "Downey Jr.")
    val chrisEvans: Actor        = Actor(8, "Chris", "Evans")
    val markRuffalo: Actor       = Actor(9, "Mark", "Ruffalo")
    val chrisHemsworth: Actor    = Actor(10, "Chris", "Hemsworth")
    val jeremyRenner: Actor      = Actor(11, "Jeremy", "Renner")
    val tomHolland: Actor        = Actor(13, "Tom", "Holland")
    val tobeyMaguire: Actor      = Actor(14, "Tobey", "Maguire")
    val andrewGarfield: Actor    = Actor(15, "Andrew", "Garfield")
  }

  //streams
  // IO = any kind of computation that might perform side effects

  implicit class IODebugOps[A](io: IO[A]) {
    def debug: IO[A] = io.map { a =>
      println(s"[${Thread.currentThread().getName}]")
      println(a)
      a
    }
  }

  //streams
  // pure streams = store actual data

  val jlActors: Stream[Pure, Actor] = Stream(
    henryCavil,
    galGodot,
    ezraMiller,
    benFisher,
    rayHardy,
    jasonMomoa
  )

  val tomHollandStream: Stream[Pure, Actor] = Stream.emit(tomHolland)
  val spiderMen: Stream[Pure, Actor]        = Stream.emits(List(tomHolland, andrewGarfield, tobeyMaguire))

  //convert a stream to a std data structure
  val jlActorList = jlActors.toList // applicable for Stream[Pure, _]

  //infinite streams
  val infiniteJLActors                  = jlActors.repeat
  val repeatedJLActorsList: List[Actor] = infiniteJLActors.take(10).toList

  //effectful streams
  val savingTomHolland: Stream[IO, Actor] = Stream.eval {
    IO {
      println("Saving actor Tom Holland into the DB")
      Thread.sleep(1000)
      tomHolland
    }
  }

  val compiledStream: IO[Unit] = savingTomHolland.compile.drain

//  //chunks
//  val avengersActors :Stream[Pure, Actor] = Stream.chunk(Chunk.array(Array(
//    scarlettJohansson,
//    robertDowneyJr,
//    chrisEvans,
//    markRuffalo,
//    chrisHemsworth,
//    jeremyRenner,
//  )))

  val avengersActors: Stream[Pure, Actor] = Stream.chunk(
    Chunk.array(
      Array(
        scarlettJohansson,
        robertDowneyJr,
        chrisEvans,
        markRuffalo,
        chrisHemsworth,
        jeremyRenner
      )
    )
  )

  val allSuperHeroes = jlActors ++ avengersActors

  //flatMap
  val printedJLActors: Stream[IO, Unit] = jlActors.flatMap { actor =>
    Stream.eval(IO.println(actor))
  }

  //flatMap + eval = evalMAp
  val printedJLActors_v2: Stream[IO, Unit] = jlActors.evalMap(IO.println)
  //flatMap + eval while keeping the original type = evalTap
  val printedJLActors_v3: Stream[IO, Actor] = jlActors.evalTap(IO.println)

  //pipe = Stream[F, I] => Stream[F, O]
  val actorToStringPipe: Pipe[IO, Actor, String] = inStream => inStream.map(actor => s"${actor.firstName} ${actor.lastName}")

  def toConsole[A]: Pipe[IO, A, Unit] = inStream => inStream.evalMap(IO.println)

  val stringNamesPrinted: Stream[IO, Unit] = jlActors.through(actorToStringPipe).through(toConsole)

  //error handling
  def savetoDatabase(actor: Actor): IO[Int] = IO {
    println(s"Saving ${actor.firstName} ${actor.lastName}")
    if (Random.nextBoolean()) {
      throw new RuntimeException("Persistence layer failed.")
    }
    println("Saved.")
    actor.id
  }

  val savedJLActors: Stream[IO, Int] = jlActors.evalMap(savetoDatabase)
  val errorHandledActors: Stream[IO, Int] = savedJLActors.handleErrorWith(error =>
    Stream.eval(IO {
      println(s"Error occurred: $error")
      -1
    })
  )

  val attemptedSavedJLActors: Stream[IO, Either[Throwable, Int]] = savedJLActors.attempt
  val attemptedProcessed = attemptedSavedJLActors.evalMap {
    case Left(error)  => IO(s"Error: $error").debug
    case Right(value) => IO(s"Successfully processed actor id: $value").debug
  }

  //resource
  case class DatabaseConnection(url: String)

  def acquireConnection(url: String): IO[DatabaseConnection] = IO {
    println("Getting DB connection ...")
    DatabaseConnection(url)
  }

  def release(conn: DatabaseConnection): IO[Unit] = IO.println(s"Releasing connection to ${conn.url}")

  //bracket pattern
  val managedJLActors: Stream[IO, Int] = Stream.bracket(acquireConnection("jdbc://mydatabase.com"))(release).flatMap { conn =>
    //process a stream using this resource
    savedJLActors.evalTap(actorId => IO(s"Saving actor $actorId to ${conn.url}").debug)
  }

  //merge
  val concurrentJLActors: Stream[IO, Actor] = jlActors.evalMap { actor =>
    IO {
      Thread.sleep(400)
      actor
    }.debug
  }

  val concurrentAvengersActors: Stream[IO, Actor] = avengersActors.evalMap { actor =>
    IO {
      Thread.sleep(200)
      actor
    }.debug
  }

  val mergedActors: Stream[IO, Actor] = concurrentJLActors.merge(concurrentAvengersActors)

  //concurrently
  //example: producer-consumer
  val queue: IO[Queue[IO, Actor]] = Queue.bounded(10)
  val concurrentSystem = Stream.eval(queue).flatMap { q =>
    //producer stream
    val producer: Stream[IO, Unit] = jlActors
      .evalTap(actor => IO(actor).debug)
      .evalMap(actor => q.offer(actor))
      .metered(1.second) // throttle at 1 effect per second
    //consumer stream
    val consumer: Stream[IO, Unit] = Stream
      .fromQueueUnterminated(q)
      .evalMap(actor => IO(s"Consumed actor $actor").debug.void)

    producer.concurrently(consumer)
  }

  override def run: IO[Unit] =
    //    compiledStream
//    stringNamesPrinted.compile.drain
//    errorHandledActors.compile.drain
//    attemptedProcessed.compile.drain
//    managedJLActors.compile.drain
//    mergedActors.compile.drain
    concurrentSystem.compile.drain

}
