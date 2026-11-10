package com.example.model

object StringsDictionary {

  val SLOTS = listOf(
    GameSlot(10, 4, "4:00 O’clock (Morning 10:00 AM)", "4:00 ሰዓት (ጠዋት)", "4:00 ሰዓት (ንጉሆ)"),
    GameSlot(12, 6, "6:00 O’clock (Midday 12:00 PM)", "6:00 ሰዓት (ቀትር)", "6:00 ሰዓት (ቀትሪ)"),
    GameSlot(14, 8, "8:00 O’clock (Afternoon 2:00 PM)", "8:00 ሰዓት (ከሰዓት)", "8:00 ሰዓት (ድሕሪ ቀትሪ)"),
    GameSlot(16, 10, "10:00 O’clock (Afternoon 4:00 PM)", "10:00 ሰዓት (ከሰዓት)", "10:00 ሰዓት (ድሕሪ ቀትሪ)"),
    GameSlot(18, 12, "12:00 O’clock (Dusk 6:00 PM)", "12:00 ሰዓት (ምሽት)", "12:00 ሰዓት (ምሸት)"),
    GameSlot(20, 2, "2:00 O’clock (Night 8:00 PM)", "2:00 ሰዓት (ምሽት)", "2:00 ሰዓት (ምሸት)")
  )

  fun findSlot(label: String?): GameSlot? {
    if (label.isNullOrBlank()) return null
    val clean = label.trim().lowercase().replace('’', '\'')

    // 1. Exact match on normalized labels
    SLOTS.firstOrNull { slot ->
      slot.labelEn.lowercase().replace('’', '\'') == clean ||
      slot.labelAm.lowercase().replace('’', '\'') == clean ||
      slot.labelTi.lowercase().replace('’', '\'') == clean
    }?.let { return it }

    // 2. Ethiopian hour prefix / keyword matching
    if (clean.startsWith("4:00") || clean.contains("ጠዋት 4:00") || clean.contains("10:00 am")) return SLOTS[0]
    if (clean.startsWith("6:00") || clean.contains("ቀትር 6:00") || clean.contains("12:00 pm")) return SLOTS[1]
    if (clean.startsWith("8:00") || clean.contains("ከሰዓት 8:00") || clean.contains("2:00 pm")) return SLOTS[2]
    if (clean.startsWith("10:00") || clean.contains("ከሰዓት 10:00") || clean.contains("4:00 pm")) return SLOTS[3]
    if (clean.startsWith("12:00") || clean.contains("ምሽት 12:00") || clean.contains("6:00 pm")) return SLOTS[4]
    if (clean.startsWith("2:00") || clean.contains("ምሽት 2:00") || clean.contains("8:00 pm")) return SLOTS[5]

    return null
  }

  // Amharic numerals pronunciation dictionary
  val AM_ONES = listOf("", "አንድ", "ሁለት", "ሶስት", "አራት", "አምስት", "ስድስት", "ሰባት", "ስምንት", "ዘጠኝ")
  val AM_TENS = listOf("", "አስር", "ሃያ", "ሰላሳ", "አርባ", "ሃምሳ", "ስልሳ", "ሰባ")
  val AM_LETTERS = mapOf("B" to "ቢ", "I" to "አይ", "N" to "ኤን", "G" to "ጂ", "O" to "ኦ")
  val EN_LETTERS = mapOf("B" to "Bee", "I" to "Eye", "N" to "En", "G" to "Gee", "O" to "Oh")
  val EN_DIGITS = listOf("zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine")

  fun amharicNumber(n: Int): String {
    if (n <= 0) return ""
    val tens = n / 10
    val ones = n % 10
    return when {
      tens == 0 -> AM_ONES.getOrElse(ones) { "" }
      tens == 1 -> if (ones == 0) "አስር" else "አስራ ${AM_ONES[ones]}"
      else -> {
        val tensStr = AM_TENS.getOrElse(tens) { "" }
        if (ones == 0) tensStr else "$tensStr ${AM_ONES[ones]}"
      }
    }
  }

  fun getBallLetter(number: Int): String {
    return when {
      number in 1..15 -> "B"
      number in 16..30 -> "I"
      number in 31..45 -> "N"
      number in 46..60 -> "G"
      number in 61..75 -> "O"
      else -> ""
    }
  }

  // Pre-configured 17 Target Goals
  val ALL_GOALS: List<BingoGoal> by lazy {
    buildGoals()
  }

  private fun buildGoals(): List<BingoGoal> {
    val list = mutableListOf<BingoGoal>()

    // 1. Any Line (5 rows, 5 cols, 2 diagonals)
    val lines = mutableListOf<GoalPattern>()
    for (r in 0..4) {
      lines.add(GoalPattern((0..4).map { c -> Pair(c, r) })) // row
    }
    for (c in 0..4) {
      lines.add(GoalPattern((0..4).map { r -> Pair(c, r) })) // col
    }
    lines.add(GoalPattern((0..4).map { i -> Pair(i, i) })) // diagonal 1
    lines.add(GoalPattern((0..4).map { i -> Pair(i, 4 - i) })) // diagonal 2
    list.add(BingoGoal("any_line", "Any Line", "ማንኛውም መስመር", "ዝኾነ መስመር", lines))

    // 2. Left & Right Columns
    val lr = (0..4).map { r -> Pair(0, r) } + (0..4).map { r -> Pair(4, r) }
    list.add(BingoGoal("lr_cols", "Left & Right Columns", "ግራ እና ቀኝ ዓምዶች", "ጸጋምን የማንን ዓምዲታት", listOf(GoalPattern(lr))))

    // 3. Top & Bottom Rows
    val tb = (0..4).map { c -> Pair(c, 0) } + (0..4).map { c -> Pair(c, 4) }
    list.add(BingoGoal("tb_rows", "Top & Bottom Rows", "ላይ እና ታች ረድፎች", "ላዕለዋይን ታሕተዋይን መስመራት", listOf(GoalPattern(tb))))

    // 4. Four Corners
    val corners = listOf(Pair(0, 0), Pair(4, 0), Pair(0, 4), Pair(4, 4))
    list.add(BingoGoal("corners", "Four Corners", "አራቱ ማዕዘኖች", "ኣርባዕተ ማዕዘናት", listOf(GoalPattern(corners))))

    // 5. Both Diagonals
    val bothDiag = (0..4).map { i -> Pair(i, i) } + (0..4).map { i -> Pair(i, 4 - i) }
    list.add(BingoGoal("both_diagonals", "Both Diagonals", "ሁለቱም ሰያፎች", "ክልቲኡ ሰያፍ", listOf(GoalPattern(bothDiag.distinct()))))

    // 6. Small Square (any 2x2 anywhere)
    val sqList = mutableListOf<GoalPattern>()
    for (x in 0..3) {
      for (y in 0..3) {
        sqList.add(GoalPattern(listOf(Pair(x, y), Pair(x + 1, y), Pair(x, y + 1), Pair(x + 1, y + 1))))
      }
    }
    list.add(BingoGoal("small_square", "Small Square", "ትንሽ ካሬ", "ንእሽቶ ካሬ", sqList))

    // 7. Large Square (outer border)
    val border = mutableListOf<Pair<Int, Int>>()
    for (c in 0..4) {
      for (r in 0..4) {
        if (c == 0 || c == 4 || r == 0 || r == 4) border.add(Pair(c, r))
      }
    }
    list.add(BingoGoal("large_square", "Large Square", "ትልቅ ካሬ", "ዓቢ ካሬ", listOf(GoalPattern(border))))

    // 8. Rectangle (2x3 or 3x2)
    val rectList = mutableListOf<GoalPattern>()
    for (x in 0..3) for (y in 0..2) {
      val cells = mutableListOf<Pair<Int, Int>>()
      for (dx in 0..1) for (dy in 0..2) cells.add(Pair(x + dx, y + dy))
      rectList.add(GoalPattern(cells))
    }
    for (x in 0..2) for (y in 0..3) {
      val cells = mutableListOf<Pair<Int, Int>>()
      for (dx in 0..2) for (dy in 0..1) cells.add(Pair(x + dx, y + dy))
      rectList.add(GoalPattern(cells))
    }
    list.add(BingoGoal("rectangle", "Rectangle", "አራት ማዕዘን", "ኣርባዕተ ማዕዘን", rectList))

    // 9. Half House (8 winning split variants: Left, Right, Top, Bottom, Diagonal Top, Diagonal Bottom, Diagonal Right, Diagonal Left)
    val halfList = listOf(
      // 1. Left Half (Column splits: columns 0..2)
      GoalPattern((0..4).flatMap { r -> (0..2).map { c -> Pair(c, r) } }),
      // 2. Right Half (Column splits: columns 2..4)
      GoalPattern((0..4).flatMap { r -> (2..4).map { c -> Pair(c, r) } }),
      // 3. Top Half (Row splits: rows 0..2)
      GoalPattern((0..2).flatMap { r -> (0..4).map { c -> Pair(c, r) } }),
      // 4. Bottom Half (Row splits: rows 2..4)
      GoalPattern((2..4).flatMap { r -> (0..4).map { c -> Pair(c, r) } }),
      // 5. Diagonal Top Half (anti-diagonal upper triangle: c + r <= 4)
      GoalPattern((0..4).flatMap { r -> (0..4).filter { c -> c + r <= 4 }.map { c -> Pair(c, r) } }),
      // 6. Diagonal Bottom Half (anti-diagonal lower triangle: c + r >= 4)
      GoalPattern((0..4).flatMap { r -> (0..4).filter { c -> c + r >= 4 }.map { c -> Pair(c, r) } }),
      // 7. Diagonal Right Half (main-diagonal right/upper triangle: c >= r)
      GoalPattern((0..4).flatMap { r -> (0..4).filter { c -> c >= r }.map { c -> Pair(c, r) } }),
      // 8. Diagonal Left Half (main-diagonal left/lower triangle: c <= r)
      GoalPattern((0..4).flatMap { r -> (0..4).filter { c -> c <= r }.map { c -> Pair(c, r) } })
    )
    list.add(BingoGoal("half_house", "Half House", "ግማሽ ቤት", "ፍርቂ ገዛ", halfList))

    // 10. Full House (all 25 cells)
    val allCells = (0..4).flatMap { c -> (0..4).map { r -> Pair(c, r) } }
    list.add(BingoGoal("full_house", "Full House", "ሙሉ ቤት", "ምሉእ ገዛ", listOf(GoalPattern(allCells))))

    // 11. Two Lines + Small Square (two lines in any direction + a 2x2 square block anywhere without contacting/intersecting)
    val allLines = mutableListOf<List<Pair<Int, Int>>>()
    for (r in 0..4) allLines.add((0..4).map { c -> Pair(c, r) })
    for (c in 0..4) allLines.add((0..4).map { r -> Pair(c, r) })
    allLines.add((0..4).map { i -> Pair(i, i) }) // main diagonal
    allLines.add((0..4).map { i -> Pair(i, 4 - i) }) // anti-diagonal

    val allSquares = mutableListOf<List<Pair<Int, Int>>>()
    for (x in 0..3) {
      for (y in 0..3) {
        allSquares.add(listOf(Pair(x, y), Pair(x + 1, y), Pair(x, y + 1), Pair(x + 1, y + 1)))
      }
    }

    val twoLinesSqPatterns = mutableListOf<GoalPattern>()
    val seenPatterns = mutableSetOf<Set<Pair<Int, Int>>>()

    for (i in 0 until allLines.size) {
      val l1 = allLines[i].toSet()
      for (j in i + 1 until allLines.size) {
        val l2 = allLines[j].toSet()
        for (sq in allSquares) {
          val sqSet = sq.toSet()
          // Constraint: The 2x2 square block must NOT contact (intersect) either line
          if (sqSet.none { it in l1 } && sqSet.none { it in l2 }) {
            val combined = l1 + l2 + sqSet
            if (seenPatterns.add(combined)) {
              twoLinesSqPatterns.add(GoalPattern(combined.toList()))
            }
          }
        }
      }
    }
    list.add(BingoGoal("two_lines_sq", "Two Lines + Small Square", "ሁለት መስመር + ትንሽ ካሬ", "ክልተ መስመር + ንእሽቶ ካሬ", twoLinesSqPatterns))

    // 12. Plus Sign (center row r=2 and center col c=2)
    val plus = (0..4).map { c -> Pair(c, 2) } + (0..4).map { r -> Pair(2, r) }
    list.add(BingoGoal("plus_sign", "Plus Sign", "የመደመር ምልክት", "ምልክት ምውሳኽ", listOf(GoalPattern(plus.distinct()))))

    // 13. Letter T (top row r=0 + center col c=2)
    val letterT = (0..4).map { c -> Pair(c, 0) } + (0..4).map { r -> Pair(2, r) }
    list.add(BingoGoal("letter_t", "Letter T", "ፊደል T", "ፊደል T", listOf(GoalPattern(letterT.distinct()))))

    // 14. Letter L (left col c=0 + bottom row r=4)
    val letterL = (0..4).map { r -> Pair(0, r) } + (0..4).map { c -> Pair(c, 4) }
    list.add(BingoGoal("letter_l", "Letter L", "ፊደል L", "ፊደል L", listOf(GoalPattern(letterL.distinct()))))

    // 15. Diamond (|c-2| + |r-2| == 2)
    val diamond = listOf(Pair(2, 0), Pair(1, 1), Pair(3, 1), Pair(0, 2), Pair(4, 2), Pair(1, 3), Pair(3, 3), Pair(2, 4))
    list.add(BingoGoal("diamond", "Diamond", "አልማዝ", "ኣልማዝ", listOf(GoalPattern(diamond))))

    // 16. Letter H (col 0, col 4, row 2)
    val letterH = (0..4).map { r -> Pair(0, r) } + (0..4).map { r -> Pair(4, r) } + (0..4).map { c -> Pair(c, 2) }
    list.add(BingoGoal("letter_h", "Letter H", "ፊደል H", "ፊደል H", listOf(GoalPattern(letterH.distinct()))))

    // 17. Letter Z (row 0, row 4, anti-diagonal c+r=4)
    val letterZ = (0..4).map { c -> Pair(c, 0) } + (0..4).map { c -> Pair(c, 4) } + (0..4).map { c -> Pair(c, 4 - c) }
    list.add(BingoGoal("letter_z", "Letter Z", "ፊደል Z", "ፊደል Z", listOf(GoalPattern(letterZ.distinct()))))

    return list
  }
}
