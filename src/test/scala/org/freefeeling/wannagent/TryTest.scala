package org.freefeeling.wannagent

import org.scalatest.WordSpec

/**
  * Created by zh on 15-12-16.
  */
class TryTest extends WordSpec {

  "A Set" when {
    "empty" should {
      "have size 0" in {
        assert(Set.empty.size == 0)
      }

      "produce NoSuchElementException when head is invoked" in {
        intercept[NoSuchElementException] {
          Set.empty.head
        }
      }
    }
  }
}
