package holdem

import scala.util.{Try, Success, Failure}
import scala.reflect.ClassTag

object HoldemValidator {
  import CardsImplicits._
  case class HoldemHandScore(table: String, hand: String) {
    //bestScore => tuple of combination : score
    val bestScore = getCombinations
      .map(e => (e, e.getScore.toLong))
      .sortWith((o1, o2) => o1._2 > o2._2)
      .head

    def getCombinations: Array[Array[String]] =
      hand.length match {
        case 4 => (table + hand).toCards.getCombinations(5)
        case _ =>
          for {
            t <- table.toCards.getCombinations(3)
            h <- hand.toCards.getCombinations(2)
          } yield { t ++ h }
      }
  }

  def calculate(rawData: String): Try[Array[HoldemHandScore]] =
    Try(
      rawData.parseInput.toItems.tail
        .map(e => HoldemHandScore(rawData.toItems.head, e))
    )

  def calculateStringify(rawData: String, isDbg: Boolean): String =
    stringify(calculate(rawData), isDbg)

  def stringify(data: Try[Array[HoldemHandScore]], isDbg: Boolean): String =
    data match {
      case Success(hands) =>
        hands
          .groupBy(e => e.bestScore._2)
          .toSeq
          .sortBy(_._1)
          .map(e =>
            e._2
              .sortBy(_.hand)
              .map(e =>
                s"${e.hand}" + (if (isDbg)
                                  s"|${e.bestScore._1.mkString}->${e.bestScore._2}|"
                                else "")
              )
              .mkString("=")
          )
          .mkString(" ")
      case Failure(exception) => "Error: " + exception.toString()
    }
}

object CombinationUtils {
  def combinationPositions(
      items: Int,
      positions: Int
  ): Option[Array[Array[Int]]] = {
    combinationPositions(0, items - positions, positions)
  }

  private def combinationPositions(
      start: Int,
      end: Int,
      depth: Int
  ): Option[Array[Array[Int]]] =
    if (depth != 0) {
      val results = combinationPositions(start + 1, end + 1, depth - 1)
      results match {
        case Some(value) => {
          Some(for {
            x <- (start to end).toArray
            //if filters off repeated items, Could not manage recursion direct call in for loop. return None breaks logic
            y <- value
            if y(0) > x
          } yield {
            y.+:(x)
          })
        }
        case None => Some(for (x <- (start to end).toArray) yield { Array(x) })
      }
    } else {
      None
    }

  def fill[T](
      items: Array[T],
      combinations: Array[Array[Int]]
  )(implicit tag: ClassTag[T]): Array[Array[T]] =
    combinations.map(i => i.map(j => items(j)))
}

object Deck {
  val cardSign = List("h", "d", "c", "s")
  val cardType =
    List("2", "3", "4", "5", "6", "7", "8", "9", "T", "J", "Q", "K", "A")
  val deck = {
    for {
      y <- cardType
      x <- cardSign
    } yield { y + x }
  }
}

object CardsImplicits {
  implicit class IntUtils(score: Int) {
    def toScoreString: String =
      (if (score < 10) "0" else "") + score.toString
  }

  implicit class CardsUtils(cards: Array[String]) {
    def isFlushCards: Boolean =
      cards.map(_.charAt(1)).distinct.length == 1

    /** @param cardScores input ordered score array low to high
      * @return if cards is in sequence, returns reversed input Array
      */
    private def getSequenceScore(cardScores: Array[Int]): Option[Array[Int]] =
      cardScores
        .zip(cardScores.head to (cardScores.head + cardScores.length))
        .filter(x => x._1 != x._2)
        .headOption match {
        case Some(value) => None
        case None        => Some(cardScores.reverse)
      }

    //if cards is in sequence, returns seq score
    private def getSequenceScore: Option[Array[Int]] =
      getSequenceScore(cards.map(_.score).sortBy(e => e)) match {
        case Some(value) => Some(value)
        case None if cards.mkString.contains("A") =>
          getSequenceScore(
            cards
              .map(_.score match {
                case 14 => 1
                case x  => x
              })
              .sortBy(e => e)
          )
        case _ => None
      }

    /** https://www.quora.com/What-is-the-highest-suit-in-poker => Ben Arnold answer
      * 1. High Card 2. Pair 3. Two Pair 4. Three of a Kind 5.Straight 6.Flush 7.Full
      * House 8. Four of a Kind 9. Straight Flush
      */
    def getScoreWithPrefix(isFlush: Boolean): (Int, String) =
      getSequenceScore match {
        case Some(v) =>
          ((if (isFlush) 9 else 5), v.map(e => e.toScoreString).mkString)
        case None =>
          cards
            .map(_.score)
            .groupBy(e => e)
            .toSeq
            .sortWith((o1, o2) =>
              o1._2.length > o2._2.length
                || o1._2.length == o2._2.length && o1._1 > o2._1
            )
            .foldLeft(((if (isFlush) 6 else 1), ""))(
              (
                  prev: (Int, String),
                  value: (Int, Array[Int])
              ) => { //value => score: count
                val v = prev._2 + value._2.map(e => e.toScoreString).mkString
                value._2.length match {
                  case 4 => (8, v) // Four of a Kind
                  case 3 => (4, v) // three of a Kind
                  case 2 =>
                    prev._1 match {
                      case 4 => (7, v) // Three of a Kind upg. to Full House
                      case 2 => (3, v) // Pair upg. to Two Pair
                      case _ => (2, v) // Pair
                    }
                  case _ => (prev._1, v)
                }
              }
            )
      }

    def getScore: String =
      getScoreWithPrefix(isFlushCards) match {
        case (a, b) => a.toString + b
      }

    def getCombinations(positions: Int): Array[Array[String]] =
      CombinationUtils.combinationPositions(cards.size, positions) match {
        case Some(value) => {
          value.size match {
            case 0 =>
              throw new Error(
                s"Could not calculate Combinations for ${cards}! -> Empty"
              )
            case _ => CombinationUtils.fill(cards, value)
          }
        }
        case None =>
          throw new Error(
            s"Could not calculate Combinations for ${cards}! -> None"
          )
      }
  }

  implicit class StringUtils(cards: String) {
    def toCards: Array[String] =
      // cards.replaceAll("\\s+", "").grouped(2).toArray //slow!
      cards
        .replaceAll("\\s+", "")
        .toCharArray
        .zipWithIndex
        .foldLeft("")((o1: String, o2: Tuple2[Char, Int]) => {
          o1 + o2._1 + (if (o2._2 % 2 == 0) "" else " ")
        })
        .split("\\s+")
        .toArray

    def toItems: Array[String] = cards.split("\\s+")

    def score: Int =
      cards(0) match {
        case '2' => 2
        case '3' => 3
        case '4' => 4
        case '5' => 5
        case '6' => 6
        case '7' => 7
        case '8' => 8
        case '9' => 9
        case 'T' => 10
        case 'J' => 11
        case 'Q' => 12
        case 'K' => 13
        case 'A' => 14
        case _   => 1
      }

    def parseInput: String = {
      if (toItems.length < 2 || toItems.length > 11) {
        throw new Error(
          s"Data input item count -> ${toItems.length}. Should be between 2 and 11"
        )
      } else if (toItems.head.length != 10) {
        throw new Error(
          s"Table character count -> ${toItems.head.length}. Should be 10."
        )
      }
      for { hand <- cards.toItems.tail } {
        if (hand.length != 4 && hand.length != 8) {
          throw new Error(
            s"Hand ${hand} character length mismatch -> ${hand.length}. Should be 4 or 8 for Omaha."
          )
        }
      }
      if (
        toCards
          .filter(e => !Deck.deck.contains(e))
          .length != 0
      ) {
        throw new Error(
          s"Unknown cards ${toCards
            .filter(e => !Deck.deck.contains(e))
            .mkString(";")}"
        )
      } else if (
        toCards.length !=
          toCards.toSet.size
      ) {
        throw new Error(
          s"Multiple times defined cards -> ${toCards.groupBy(e => e).filter(_._2.length > 1).map(_._1).mkString(";")}"
        )
      }
      return cards
    }
  }
}
