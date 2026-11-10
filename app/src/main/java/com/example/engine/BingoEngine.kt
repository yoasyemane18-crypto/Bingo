package com.example.engine

import com.example.model.BingoCard
import com.example.model.BingoGoal
import kotlin.math.floor

object BingoEngine {

  // Deterministic PRNG matching the web app's xorshift / Mulberry32 variation
  class Mulberry32(initialSeed: Long) {
    private var s: Int = initialSeed.toInt()

    fun nextDouble(): Double {
      s += 0x6D2B79F5.toInt()
      var t = (s xor (s ushr 15)) * (1 or s)
      t = (t + ((t xor (t ushr 7)) * (61 or t))) xor t
      val unsignedT = ((t xor (t ushr 14)).toLong()) and 0xFFFFFFFFL
      return unsignedT.toDouble() / 4294967296.0
    }

    fun <T> shuffle(list: List<T>): List<T> {
      val mutable = list.toMutableList()
      for (i in mutable.size - 1 downTo 1) {
        val j = floor(nextDouble() * (i + 1)).toInt().coerceIn(0, i)
        val temp = mutable[i]
        mutable[i] = mutable[j]
        mutable[j] = temp
      }
      return mutable
    }
  }

  // Generates 75-number calling sequence for a game seed
  fun generateCallingSequence(seed: Long): List<Int> {
    val rng = Mulberry32(seed)
    return rng.shuffle((1..75).toList())
  }

  // Generates 5x5 card for cardNum (1..500) and seed
  fun generateCard(cardNum: Int, seed: Long): BingoCard {
    val cardSeed = seed * 1000L + cardNum.toLong() * 7919L + 13L
    val rng = Mulberry32(cardSeed)

    // 5 columns:
    // col 0: B (1..15)
    // col 1: I (16..30)
    // col 2: N (31..45), center [2][2] is 0 (FREE space)
    // col 3: G (46..60)
    // col 4: O (61..75)
    val grid = mutableListOf<List<Int>>()
    for (c in 0..4) {
      val min = c * 15 + 1
      val max = c * 15 + 15
      val nums = rng.shuffle((min..max).toList()).take(5).toMutableList()
      if (c == 2) {
        nums[2] = 0 // FREE space
      }
      grid.add(nums)
    }

    return BingoCard(
      cardNum = cardNum,
      seed = seed,
      grid = grid,
      markedCells = setOf(Pair(2, 2)) // Free space is always pre-marked
    )
  }

  // Check if a card satisfies the target goal.
  // Returns winning coordinates list, or null if not winning.
  fun checkWin(card: BingoCard, goal: BingoGoal): List<Pair<Int, Int>>? {
    for (pattern in goal.patterns) {
      val allMarked = pattern.cells.all { (col, row) ->
        card.isMarked(col, row)
      }
      if (allMarked) {
        return pattern.cells
      }
    }
    return null
  }

  // Check if a card satisfies the target goal given a set of called numbers
  fun checkWinWithCalledNumbers(card: BingoCard, goal: BingoGoal, calledNumbers: Set<Int>): List<Pair<Int, Int>>? {
    for (pattern in goal.patterns) {
      val allSatisfied = pattern.cells.all { (col, row) ->
        val num = card.grid[col][row]
        num == 0 || calledNumbers.contains(num)
      }
      if (allSatisfied) {
        return pattern.cells
      }
    }
    return null
  }
}
