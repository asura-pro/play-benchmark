package controllers

import java.util
import java.util.Calendar
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.util.ByteString
import com.fasterxml.jackson.databind.ObjectMapper
import controllers.HomeController._
import javax.inject._
import play.api.http.{ContentTypes, HttpEntity}
import play.api.mvc._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Promise}
import scala.util.Success

@Singleton
class HomeController @Inject()(
                                implicit val system: ActorSystem,
                                implicit val exec: ExecutionContext,
                                implicit val mat: Materializer,
                                cc: ControllerComponents
                              ) extends AbstractController(cc) {

  val qps = new ConcurrentHashMap[String, AtomicInteger]()
  val ok = Ok("Hello")

  def index() = Action { implicit request: Request[AnyContent] =>
    incOne()
    ok
  }

  def delay1s() = Action.async { implicit req =>
    val promise = Promise[Result]
    system.scheduler.scheduleOnce(1 seconds) {
      promise.complete(Success(ok))
      incOne()
    }
    promise.future
  }

  def delay10s() = Action.async { implicit req =>
    val promise = Promise[Result]
    system.scheduler.scheduleOnce(10 seconds) {
      promise.complete(Success(ok))
      incOne()
    }
    promise.future
  }

  def delay30s() = Action.async { implicit req =>
    val promise = Promise[Result]
    system.scheduler.scheduleOnce(30 seconds) {
      promise.complete(Success(ok))
      incOne()
    }
    promise.future
  }

  def clearQps() = Action { implicit req =>
    qps.clear()
    jsonOk(qps)
  }

  def getQps() = Action { implicit req =>
    val result = new util.TreeMap[String, AtomicInteger]()
    result.putAll(qps)
    jsonOk(result)
  }

  @inline
  def incOne(): Unit = {
    val key = getCurrentMin()
    val count = qps.get(key)
    if (null == count) {
      qps.put(key, new AtomicInteger(1))
    } else {
      count.incrementAndGet()
    }
  }
}

object HomeController {

  val mapper: ObjectMapper = new ObjectMapper()

  def jsonOk(data: Any): Result = {
    Result(
      header = ResponseHeader(200),
      HttpEntity.Strict(ByteString(mapper.writeValueAsString(data)), Some(ContentTypes.JSON))
    )
  }

  def getCurrentMin(): String = {
    val calendar = Calendar.getInstance()
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    val minute = calendar.get(Calendar.MINUTE)
    val second = calendar.get(Calendar.SECOND)
    val sb = StringBuilder.newBuilder
    if (hour < 10) sb.append("0").append(hour) else sb.append(hour)
    sb.append(":")
    if (minute < 10) sb.append("0").append(minute) else sb.append(minute)
    sb.append(":")
    if (second < 10) sb.append("0").append(second) else sb.append(second)
    sb.toString()
  }

  def getCurrentMinLong(): Long = {
    val now = System.currentTimeMillis()
    println(now)
    now - now % 1000
  }
}