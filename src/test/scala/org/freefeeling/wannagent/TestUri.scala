package org.freefeeling.wannagent

import org.freefeeling.wannagent.http.Uri
import org.scalatest.FlatSpec

/**
  * Created by zh on 17-3-9.
  */
class TestUri extends FlatSpec{

  "Uri" should "keep the same as the original with toString" in {
    val test = "http://freefeeling.org/"
    assertResult(test)(Uri(test).toString)
  }

  it should "parse the relative path" in {
    val test = "http://freefeeling.org/"
    assertResult("/")(Uri(test).toRelative.toString)
  }

}
