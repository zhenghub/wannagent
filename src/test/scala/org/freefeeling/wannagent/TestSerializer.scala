package org.freefeeling.wannagent

import akka.util.ByteString
import org.freefeeling.wannagent.ReverseProxy.{Msg, ClientId}
import org.scalatest.WordSpec
import scala.pickling.shareNothing._
import scala.pickling.static._
import scala.pickling.binary._
import scala.pickling.Defaults._

/**
  * Created by zhenghu on 16-3-16.
  */
class TestSerializer extends WordSpec{

  "A serializer" when {
    "serialize a object" should {
      "can deserialize it" in {
        val ci = ClientId("dingding")
        val bytes = ci.pickle
        val bs = ByteString(bytes.value)
        val ci2 = BinaryPickle(bs.toArray).unpickle[Msg]
        assert(ci2 == ci)
      }
    }
  }

}