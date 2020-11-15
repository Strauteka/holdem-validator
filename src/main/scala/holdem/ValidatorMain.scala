package holdem

import scala.util.Random
import scala.util.Success
import scala.util.{Try, Success, Failure}
import HoldemValidator._
import OptionsImplicits._
object ValidatorMain {

  def main(args: Array[String]): Unit = {
    //prepare input options
    implicit val options = ArgsParser.parse(
      args.flatMap(e => e.split("="))
    )
    //get main options

    val mainOpt = (
      options.getValue('type, ArgsParser.defaultType),
      options.getValue('dbg, ArgsParser.defaultDbg),
      options.getValue('oFile, ""),
      options.getValue('iFile, "")
    )

    mainOpt._1 match {
      case 1 => {
        val startGenTime = System.currentTimeMillis;
        //manual set threads won't help, if thread count are low!
        // val parData = generateData.par
        // parData.tasksupport = new scala.collection.parallel.ForkJoinTaskSupport(
        //   new scala.concurrent.forkjoin.ForkJoinPool(3)
        // )
        //if not input file, generate data
        val data = (mainOpt._4 match {
          case "" => Some(generateData)
          case x  => readFile(x)
        }) match {
          case Some(samples) =>
            samples.par
              .map(e =>
                if (e.contains("->"))
                  (e.split("->").head, e.split("->").tail.mkString)
                else (e, calculateStringify(e, false))
              ) //filter off error data -> first timestamp row!
              .filter(!_._2.contains("Error:"))
              .toArray
          case None =>
            throw new Error(
              s"Could not find file: ${mainOpt._4}"
            )
        }

        val calcGenTime = System.currentTimeMillis - startGenTime;

        //in windows piping in line, results through file
        if (!"".equals(mainOpt._3)) {
          deleteFile(mainOpt._3)
          writeToFile(mainOpt._3, System.currentTimeMillis.toString, data)
        } else {
          //prepare input read for validation data
          //if used wisely, we can avoid using new thread for input
          new Thread {
            override def run {
              //approximately time
              val startTime = System.currentTimeMillis;
              readInput match {
                case Some(value) => {
                  val calcTime = System.currentTimeMillis - startTime;
                  printValidation(data, value, mainOpt._2)
                  printTime("Calculation", calcTime)
                }
                case None => System.err.println("Missing calculation results!")
              }
            }
          }.start
        }
        //
        data.foreach(e => println(e._1))
        //Sends EOF
        System.out.flush()
        System.out.close()
        printTime("Generate/Load", calcGenTime)
      }
      case 2 =>
        readInput match {
          case Some(data) =>
            data.par
              .map(e => (calculateStringify(e, mainOpt._2)))
              .toArray //if not toArray, foreach async
              .foreach(println)
            //Sends EOF
            System.out.flush()
            System.out.close()
          case None =>
            throw new Error(
              s"Data input samples missing!"
            )
        }
      case 3 =>
        if ("".equals(mainOpt._4))
          throw new Error(
            s"For Type=3 need to specify validation file with --ifile option!"
          )
        else
          //first will read input to be sure file is ready to read!
          readInput match {
            case Some(calcData) => {
              val recTime = System.currentTimeMillis
              readFile(mainOpt._4) match {
                case Some(data) => {
                  val calcTime =
                    recTime - Try(data.head.toLong).getOrElse(recTime)
                  val resultData =
                    data.tail.map(e =>
                      (e.split("->").head, e.split("->").tail.mkString)
                    )
                  printValidation(resultData, calcData, mainOpt._2)
                  printTime("Calculation", calcTime)
                }
                case None =>
                  throw new Error(
                    s"Could not find file: ${mainOpt._3}"
                  )
              }
            }
            case None =>
              throw new Error(
                s"Empty Calculation input!"
              )
          }
      case x =>
        throw new Error(
          s"Unknown type:${x} Should match < 1 - 3 > option!"
        )
    }
  }

  def printTime(prefix: String, time: Long): Unit =
    System.err.println(
      s"${prefix} time: ${time / 1000} seconds ${time % 1000}  millis"
    )

  def readInput: Option[Array[String]] =
    Option(
      Iterator
        .continually(scala.io.StdIn.readLine())
        .takeWhile(e => e != null && !"exit".equals(e))
        .toArray
    ).filter(_.nonEmpty)

  def deleteFile(filePath: String): Boolean =
    new java.io.File(filePath).delete()

  def writeToFile(
      filePath: String,
      header: String,
      data: Array[(String, String)]
  ): Unit = {
    val pw = new java.io.PrintWriter(new java.io.File(filePath))
    pw.write(header)
    pw.write(sys.props("line.separator"))
    for ((x, y) <- data) {
      pw.write(x)
      pw.write("->")
      pw.write(y)
      pw.write(sys.props("line.separator"))
    }
    pw.close()
  }
  //Don't need if running with current generator,
  // because it will push data to calculator after validation file is created
  def wait(filePath: String): Boolean = {
    for (x <- 0 to 20) {
      if (!java.nio.file.Files.exists(java.nio.file.Paths.get(filePath))) {
        Thread.sleep(100)
      }
    }
    java.nio.file.Files.exists(java.nio.file.Paths.get(filePath))
  }

  def readFile(filePath: String): Option[Array[String]] = {
    if (wait(filePath)) {
      Option(scala.io.Source.fromFile(filePath).getLines.toArray)
        .filter(_.nonEmpty)
    } else
      None
  }

  def generateData(implicit
      options: Map[Symbol, Any]
  ): Array[String] =
    Generate.generateData(
      options.getValue('size, ArgsParser.defaultSize),
      options.getValue('hand, ArgsParser.defaultHand),
      options.getValue('omaha, ArgsParser.defaultOmaha)
    )

  def rmComment(value: String): String =
    value.trim
      .split("\\|")
      .zipWithIndex
      .filter(e => e._2 % 2 == 0)
      .map(e => e._1)
      .mkString

  def printValidation(
      resultData: Array[(String, String)],
      calcData: Array[String],
      dbg: Boolean
  ): Unit = {
    val results = resultData
      .zip(calcData)
      .map(e => (e._1._1, e._1._2, e._2, e._1._2.equals(rmComment(e._2))))

    results
      .filter(e => !e._4 || dbg)
      .foreach(e =>
        System.err
          .println(s"${e._1} => ${e._2}-> ${e._3} :> ${e._4}")
      )

    System.err.println(
      results
        .groupBy(_._4)
        .mapValues(_.size)
        .foldLeft("Matched: ${x}; Failed: ${y}; ")(
          (result: String, data: (Boolean, Int)) =>
            (data match {
              case (true, x) =>
                result.replace("${x}", x.toString())
              case (false, x) =>
                result.replace("${y}", x.toString())
            })
        )
        .replace("${x}", "0")
        .replace("${y}", "0")
    )
  }
}

object Generate {
  def generateData(
      items: Int,
      handCnt: Int,
      isOmaha: Boolean
  ): Array[String] = {
    (0 until items by 1).toArray.map(e =>
      getRandomCards(
        getRequiredCardCount(handCnt, isOmaha)
      ).zipWithIndex.toArray
        .sortBy(_._2)
        .foldLeft("")((result: String, cardHolder: (String, Int)) => {
          createResult(result, cardHolder, isOmaha)
        })
    )
  }

  def createResult(
      result: String,
      cardHolder: (String, Int),
      isOmaha: Boolean
  ): String = {
    cardHolder match {
      case (card, idx)
          if idx >= 5 &&
            (isOmaha && (idx - 5) % 4 == 0
              || !isOmaha && (idx - 5) % 2 == 0) =>
        result + " " + card
      case (card, _) => result + card
    }
  }

  def getRequiredCardCount(handCnt: Int, isOmaha: Boolean): Int = {
    5 + ((
      handCnt match {
        case 0                    => 2 + Random.nextInt(9)
        case x if x < 10 && x > 0 => x
        case _                    => 10
      }
    ) * 2 * (if (isOmaha) 2 else 1))
  }

  def getRandomCards(cnt: Int): Set[String] = {
    randomPositions(Set(), Deck.deck.size, cnt).map(e => Deck.deck(e))
  }

  private def randomPositions(
      sets: Set[Int],
      size: Int,
      target: Int
  ): Set[Int] = {
    sets.size match {
      case x
          if x >= target
            || x >= size =>
        sets
      case _ => randomPositions(sets.+(Random.nextInt(size)), size, target)
    }
  }

  // def manOf[T: Manifest](t: T): Manifest[T] = manifest[T]
}

//https://stackoverflow.com/questions/2315912/best-way-to-parse-command-line-parameters
object ArgsParser {
  val defaultType = 1
  val defaultSize = 10
  val defaultHand = 5
  val defaultOmaha = false
  val defaultDbg = false
  val default =
    s"""--type=${defaultType} --size=${defaultSize} --hand=${defaultHand} ${if (
      defaultOmaha
    ) "--omaha"
    else ""} ${if (defaultDbg) "--dbg" else ""}
    """

  val usage = s"""
    Usage: HoldemValidator 
    [--type=?] <1 - 3> 1=Generate&validate; 2=Calculate; 3=Validate;
    [--ofile=?] --ofile=<File-path>; Output file;
                With --type=1 Stores generated results in file + STDOUT;
                With --type=3 Reads generated calculation results from file + STDIN;
    [--ifile=?] --ifile=<File-path>;  Static input samples. Used with [--type=1];
    [--size=?] Sample size. Used with [--type=1];
    [--hand=?] Players <0-10>; --hand=0 -> Rnd(2-10) Used with [--type=1];
    [--omaha] Hand card count = 4; Used with [--type=1];
    [--dbg] 
    [--Help]

    Default flags: ${default.trim()}
  """
  def parse(args: Array[String]): Map[Symbol, Any] = {
    nextOption(
      Map(),
      Option(args)
        .filter(_.nonEmpty)
        .getOrElse(default.split("\\s+").flatMap(e => e.split("=")))
        .toList
    )
  }

  def nextOption(
      map: Map[Symbol, Any],
      list: List[String]
  ): Map[Symbol, Any] = {
    def isMultipleParams(s: String) = s.stripPrefix("--").contains("--")
    def sp(s: String) = {
      val l = s.stripPrefix("--").split("--")
      ("--" + l.head, l.tail.map("--" + _).mkString)
    }

    list match {
      case Nil => map
      case string :: opt2 :: tail if isMultipleParams(string) => {
        nextOption(
          map,
          List(sp(string)._1, opt2, sp(string)._2, opt2) ++ tail
        )
      }
      case "--type" :: value :: tail =>
        nextOption(map ++ Map('type -> value.toInt), tail)
      case "--size" :: value :: tail =>
        nextOption(map ++ Map('size -> value.toInt), tail)
      case "--hand" :: value :: tail =>
        nextOption(map ++ Map('hand -> value.toInt), tail)
      case "--ofile" :: value :: tail =>
        nextOption(map ++ Map('oFile -> value), tail)
      case "--ifile" :: value :: tail =>
        nextOption(map ++ Map('iFile -> value), tail)
      case "--omaha" :: tail =>
        nextOption(map ++ Map('omaha -> true), tail)
      case "--dbg" :: tail =>
        nextOption(map ++ Map('dbg -> true), tail)
      case "--Help" :: tail =>
        println(usage)
        System.exit(0)
        map
      case option :: tail =>
        println("Unknown option " + option)
        println(usage)
        System.exit(0)
        map
    }
  }
}

object OptionsImplicits {
  implicit class OptionUtils(options: Map[Symbol, Any]) {
    def getValue[A](key: Symbol, default: A): A =
      Try(options.getOrElse(key, default).asInstanceOf[A])
        .getOrElse(default)
  }
}
