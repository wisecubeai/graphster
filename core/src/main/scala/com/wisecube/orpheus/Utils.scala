package com.wisecube.orpheus

import scala.util.Try

object Utils {
  def trycall[R](function: () => R)(default: PartialFunction[Throwable, R]): R =
    Try(function()).recover(default).get
  def trycall[R, A](function: A => R)(default: PartialFunction[Throwable, R])(arg: A): R =
    Try(function(arg)).recover(default).get
  def trycall[R, A1, A2](function: (A1, A2) => R)(default: PartialFunction[Throwable, R])(arg1: A1, arg2: A2): R =
    Try(function(arg1, arg2)).recover(default).get
  def trycall[R, A1, A2, A3](function: (A1, A2, A3) => R)(default: PartialFunction[Throwable, R])(arg1: A1, arg2: A2, arg3: A3): R =
    Try(function(arg1, arg2, arg3)).recover(default).get
  def trycall[R, A1, A2, A3, A4](function: (A1, A2, A3, A4) => R)(default: PartialFunction[Throwable, R])(arg1: A1, arg2: A2, arg3: A3, arg4: A4): R =
    Try(function(arg1, arg2, arg3, arg4)).recover(default).get
}
