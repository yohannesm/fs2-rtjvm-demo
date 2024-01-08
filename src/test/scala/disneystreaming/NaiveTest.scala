package disneystreaming

import cats.effect.{IO, Resource}
import weaver._

object NaiveTest extends IOSuite {

  override type Res = Unit

  override def sharedResource: Resource[IO, Unit] = Resource.unit

  pureTest("True is true") {
    expect.eql(true, true)
  }

}
