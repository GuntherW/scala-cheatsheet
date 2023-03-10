package de.wittig.zio.rockthejvm.stm.simpleexample

import de.wittig.zio.rockthejvm.util.debugThread
import zio.*
import zio.stm.*

// STM - Software Transactional Memory (atomic effect)
object Main extends ZIOAppDefault {

//  def run = NotTransactional.exploitBuggyBank.ignore.repeatN(10000)
  def run = Transactional.cannotExploit.ignore.repeatN(10000)
}
