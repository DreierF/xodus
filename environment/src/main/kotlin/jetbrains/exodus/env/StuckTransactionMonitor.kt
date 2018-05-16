/**
 * Copyright 2010 - 2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jetbrains.exodus.env

import jetbrains.exodus.core.execution.Job
import jetbrains.exodus.core.execution.ThreadJobProcessorPool
import mu.KLogging
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.util.*

internal class StuckTransactionMonitor(private val env: EnvironmentImpl) : Job() {

    init {
        processor = ThreadJobProcessorPool.getOrCreateJobProcessor("Exodus shared stuck transaction monitor")
        queueThis()
    }

    var stuckTxnCount: Int = 0
        private set

    override fun execute() {
        if (env.isOpen) {
            var stuckTxnCount = 0
            try {
                env.transactionTimeout().forEachExpiredTransaction {
                    val trace = it.trace
                    if (trace != null) {
                        val creatingThread = it.creatingThread
                        val out = ByteArrayOutputStream()
                        val ps = PrintStream(out)
                        val errorHeader = "Transaction timed out: created at ${Date(it.startTime)}, thread = $creatingThread(${creatingThread.id})"
                        ps.writer().write(errorHeader)
                        trace.printStackTrace(ps)
                        logger.error(errorHeader, trace)
                        ++stuckTxnCount
                    }
                }
                env.transactionExpirationTimeout().forEachExpiredTransaction {
                    env.finishTransaction(it)
                }
            } finally {
                this.stuckTxnCount = stuckTxnCount
                queueThis()
            }
        }
    }

    private fun queueThis() {
        processor.queueIn(this, env.environmentConfig.envMonitorTxnsCheckFreq.toLong())
    }

    private fun Int.forEachExpiredTransaction(callback: (TransactionBase) -> Unit) {
        if (this != 0) {
            val timeBound = System.currentTimeMillis() - this
            env.forEachActiveTransaction {
                val txn = it as TransactionBase
                if (txn.startTime < timeBound) {
                    callback(it)
                }
            }
        }
    }

    companion object : KLogging()
}
