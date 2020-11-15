# Texas hold 'em Hand Validator - Comparator

## Requires / tested

`SBT 1.4.2`  
`Scala 2.12.12`  
`Java 11`

## Build

IN Command line interface.

1.  Clone repository
2.  Navigate to repository & Run commands

### To Compile project as executable .jar

`sbt assembly`

_You should find executable jar file in `./target/scala-2.12/holdem-validator-assembly-0.1.0-SNAPSHOT.jar`_

### To entry in development mode

1. `sbt`
2. `~run`

## RUN JAR

Mainly program works in two ways:

1. As Validator
2. As Comparator

Full cycle run in linux with named piping  
Create pipes:  
`mkfifo fifo0 fifo1`  
Run program
`java -jar ./holdem-validator-assembly-0.1.0-SNAPSHOT.jar --size=100 --hand=6 --dbg > fifo0 < fifo1 & java -jar ./holdem-validator-assembly-0.1.0-SNAPSHOT.jar --type=2 --dbg < fifo0 > fifo1`

_First program works as validator, it creates samples and pushes to STDOUT. Then waits for calculation results in STDIN._

_Second program works as Calculator, it receives samples in STDIN, calculates, and pushes results to STDOUT_

With example [prog1 > fifo0 < fifo1 & prog2 < fifo0 > fifo1](https://unix.stackexchange.com/questions/53641/how-to-make-bidirectional-pipe-between-two-programs) we piping programs OUT/IN together.

In Windows with pipes its not so easy. There for I created line pipes as follows:  
 `prog1 | prog2 | prog3 -> generator | comparator | validator`

To get Generated Data you need to specify file where to store/read samples for generator and validator.

Example: `java -jar ./holdem-validator-assembly-0.1.0-SNAPSHOT.jar --size=100 --ofile=data.txt | java -jar ./holdem-validator-assembly-0.1.0-SNAPSHOT.jar --type=2 | java -jar ./holdem-validator-assembly-0.1.0-SNAPSHOT.jar --type=3 --ifile=data.txt`  
_To work --ofile--ifile option values should match!_

# Data I/O

Card syntax example:  
`List("2", "3", "4", "5", "6", "7", "8", "9", "T", "J", "Q", "K", "A")`  
Cartesian product with  
`List("h", "d", "c", "s")`

Cards `2h 2d 2c 2s 3h 3d...Ac As` = 52 cards.

Input data for Comparator

Sample example:

5 cards for table and 2 hand cards for each player or 4 hand cards for each player in Omaha style.  
Input example:  
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;2 players:  
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`9sTh4d4s7s 7h5s QsAs`  
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`8h9d6c4d5h Ah7d Kh5c`  
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;2 players Omaha:  
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`6c3sJs8s7c Td6s5c3d Kd4cKh2h`  
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;5 players  
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;` 9d2d6hJsAd Ks5h QsAs 8s4d Jd3d Jc7h`  
Expected output:

For input `9sTh4d4s7s 7h5s QsAs` => `7h5s QsAs`  
For input `8h9d6c4d5h Ah7d Kh5c` => `Kh5c Ah7d`  
For input `6c3sJs8s7c Kd4cKh2h Td6s5c3d` => `Kd4cKh2h Td6s5c3d`  
For input `9d2d6hJsAd Ks5h QsAs 8s4d Jd3d Jc7h` => `8s4d Ks5h Jc7h QsAs Jd3d`

In results, hands should be ordered from weakest to strongest values. If Strength is equal then equal score hands is ordered alphabetically.

## Options

When running program you can add additional options.  
Default flags if needed: --type=1 --size=10 --hand=5

```
    [--type=?] <1 - 3> 1=Generate&validate; 2=Calculate; 3=Validate;
    [--ofile=?] --ofile=<File-path>; Output file; Used with [--type=1] generated results in file + STDOUT
    [--ifile=?] --ifile=<File-path>; Static input samples. Used with [--type=1; --type=3];
    [--size=?] Sample size. Used with [--type=1];
    [--hand=?] Players <0-10>; --hand=0 -> Rnd(2-10) Used with [--type=1];
    [--omaha] Hand card count = 4; Used with [--type=1];
    [--dbg]
    [--Help]
```

When added option --ofile=? with --type=1, generator will not wait for STDIN. You can use it to explore algorithm like:  
`java -jar ./holdem-validator-assembly-0.1.0-SNAPSHOT.jar --size=10 --hand=2 --ofile=data.txt | java -jar ./holdem-validator-assembly-0.1.0-SNAPSHOT.jar --type=2 --dbg`

```
Ts6s|Js8dAd7cTs->11411100807| JhKh|Js8dAdJhKh->21111141308|
Ah6d|ThTc6cAh6d->31010060614| JcJh|ThTcKsJcJh->31111101013|
3hTd|8h8c6cKsTd->20808131006| Jh5d|8h8c6cKsJh->20808131106|
Ah5s|8cTcJs8dAh->20808141110| Td5c|8cTcJs8dTd->31010080811|
TdQh|8hAcKcTdQh->11413121008| JhJd|8hAcKcJhJd->21111141308|
Kh6d|9h6c6hKh6d->40606061309| 5d4c|9h6h8s7c5d->50908070605|
Qd6s|6cKsJhQd6s->20606131211| Kd7s|9s8dKsJhKd->21313110908|
7s4h|Kc8dKdAh7s->21313140807| 3hTs|Kc8dKdAhTs->21313141008|
2hAs|JsAdKsQcAs->21414131211| 6dTs|JsAdKsQcTs->51413121110|
AhQc|AcThKcAhQc->21414131210| KhAs|AcThKcKhAs->31414131310|
```

_It prints out hands best score and samples will be found in file_

```
2sJs8dAd7c JhKh Ts6s->Ts6s JhKh
8hThTc6cKs JcJh Ah6d->Ah6d JcJh
8h8c6cKs4s Jh5d 3hTd->3hTd Jh5d
8cTcJs8d3c Ah5s Td5c->Ah5s Td5c
8hAcKc4s5h JhJd TdQh->TdQh JhJd
9h6c6h8s7c 5d4c Kh6d->Kh6d 5d4c
9s6c8dKsJh Kd7s Qd6s->Qd6s Kd7s
6cKc8dKdAh 7s4h 3hTs->7s4h 3hTs
9sJsAdKsQc 2hAs 6dTs->2hAs 6dTs
2sAc9hThKc AhQc KhAs->AhQc KhAs
```

More you can create validation file and later import generated samples for performance tests as you develop you own hand comparator.  
Create file:  
`java -jar holdem-validator-assembly-0.1.0-SNAPSHOT.jar --ofile=data.txt --size=10000 --hand=4 --omaha`

Using Import file:  
`java -jar holdem-validator-assembly-0.1.0-SNAPSHOT.jar --ifile--ofile=data.txt | java -jar holdem-validator-assembly-0.1.0-SNAPSHOT.jar --type=2 | java -jar holdem-validator-assembly-0.1.0-SNAPSHOT.jar --type=3 --ifile=data.txt`

Validation file first line is ignored. First line stores approximate time to print calculation time. If --ifile--ofile is same, program will override file dropping corrupted data.

Syntax:  
`2s8h9sAc9d 8cThTc6c Ad3cJhKd Ah7s5s5d 2c3hAs6d->8cThTc6c 2c3hAs6d Ah7s5s5d Ad3cJhKd`  
Each line stores sample and correct result. Saperated by `->`.

## Algorithm

Algorithm validates scores with highest-suit law in poker.
If you notice, with --dbg option we can see additional output like |9h6h8s7c5d->50908070605| it shows best composition and score for hand.
Scores most significant character determines Rank of composition:

1. High Card.
2. Pair.
3. Two Pair.
4. Three of a Kind.
5. Straight.
6. Flush.
7. Full House.
8. Four of a Kind.
9. Straight Flush.

Other 10 characters creates score from composition cards, ordered as determined by Pokers Law to get valid result.  
In current example we got `[5]0908070605` Straight from 5 to 9.  
In score `[3]1414131310` we got Two Pair(two Aces and two kings)

As else, there is only calculations.

To get best hand combination from Table(5 cards) and hand(2 cards) it creates combinations `7 of 5`. 7 cards of 5 positions and for each 21(result) combination it calculates score.

For omaha there is one additional step, From Pokers law, combination for Omaha has to consist of exact 3 cards from 5 table cards and exact 2 cards from 4 hand cards.
You can get it as combinations `5 of 3` and `4 of 2` in cartesian product.  
`5 of 3 = 10`  
 and  
`4 of 2 = 6`  
For each hand it will calculate score for 60 combinations instead of 21 for native holdem.

# Summary

Full clean calculation code is in HoldemValidator.scala file.  
With method:

```scala
def calculate(rawData: String): Try[Array[HoldemHandScore]]
```

You can get Array with hands. Each hand holds: table, hand, best combination, score.

With method:

```scala
def stringify(data: Try[Array[HoldemHandScore]], isDbg: Boolean): String
```

You can get prepared result.

I suggest you to write your own Hand comparator and check results with created validator:  
Usage in Linux  
`java -jar ./holdem-validator-assembly-0.1.0-SNAPSHOT.jar --size=1000 --hand=3 --dbg > fifo0 < fifo1 & <your-program-here> < fifo0 > fifo1`  
Usage in Windows  
`java -jar ./holdem-validator-assembly-0.1.0-SNAPSHOT.jar --size=100 --ofile=data.txt | <your-program-here> | java -jar ./holdem-validator-assembly-0.1.0-SNAPSHOT.jar --type=3 --ifile=data.txt`

# Software created as part of learning Scala
