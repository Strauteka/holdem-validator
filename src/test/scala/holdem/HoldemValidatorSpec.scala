package holdem

import org.scalatest._
import holdem.CardsImplicits._
class HoldemValidatorSpec extends FlatSpec with Matchers {

  "This scores results" should "match" in {
    assertResult("11211070604")("Qc6d4hJs7s".toCards.getScore)
    assertResult("11308070604")("Kc6d4h8s7s".toCards.getScore)
    assertResult("11413121109")("AcKcQcJc9d".toCards.getScore)
    assertResult("20606090807")("6s6d7s8d9s".toCards.getScore)
    assertResult("30404020203")("2s2d4c4s3c".toCards.getScore)
    assertResult("30404020205")("2s2d4c4s5c".toCards.getScore)
    assertResult("50504030201")("2s3d4h5sAh".toCards.getScore)
    assertResult("50807060504")("5d6d4h8s7s".toCards.getScore)
    assertResult("50908070605")("5s6d7d8c9d".toCards.getScore)
    assertResult("61312111007")("QcKc7cJcTc".toCards.getScore)
    assertResult("71111111313")("KcJhJdKsJs".toCards.getScore)
    assertResult("71111111313")("JhJdJsKcKs".toCards.getScore)
    assertResult("80909090911")("9h9d9c9sJs".toCards.getScore)
    assertResult("80909090914")("9h9d9c9sAh".toCards.getScore)
    assertResult("90605040302")("2d3d4d5d6d".toCards.getScore)
    assertResult("91413121110")("QcKcAcJcTc".toCards.getScore)
    assertResult("91413121110")("AcKcQcJcTc".toCards.getScore)
  }

  "This combination sizes " should "match" in {
    assertResult(6)("11223344".toCards.getCombinations(2).size)
    assertResult(21)("11223344556677".toCards.getCombinations(5).size)
    assertResult(10)("1122334455".toCards.getCombinations(3).size)
  }

  "This cards split " should "match" in {
    assertResult(Array("11", "22", "33", "44").deep)("11223344".toCards.deep)
  }

  "This score order " should "match" in {
    assertResult("Ac4d=Ad4s 5d6d As9s KhKd")(
      holdem.HoldemValidator.calculateStringify(
        "4cKs4h8s7s Ad4s Ac4d As9s KhKd 5d6d",
        false
      )
    )
    assertResult("KdKs 9hJh")(
      holdem.HoldemValidator.calculateStringify(
        "2h3h4h5d8d KdKs 9hJh",
        false
      )
    )
  }

}
