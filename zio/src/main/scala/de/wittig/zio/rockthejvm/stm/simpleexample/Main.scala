package de.wittig.zio.rockthejvm.stm.simpleexample

import zio.*

// STM - Software Transactional Memory (atomic effect)
// TRef, TArray, TSet, TMap, TQueue, TPriorityQueue, TPromise, TSemaphore
object Main extends ZIOAppDefault {

//  def run = NotTransactional.exploitBuggyBank.ignore.repeatN(10000)
  def run = Transactional.cannotExploit.ignore.repeatN(10000)
}
