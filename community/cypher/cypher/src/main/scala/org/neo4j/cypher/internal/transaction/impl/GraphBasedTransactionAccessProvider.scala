/*
 * Copyright (c) 2002-2016 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.cypher.internal.transaction.impl

import org.neo4j.cypher.internal.transaction.api.{TransactionWriteAccess, TransactionAccess, TransactionAccessProvider, TransactionReadAccess}
import org.neo4j.graphdb.Transaction
import org.neo4j.kernel.GraphDatabaseAPI
import org.neo4j.kernel.api.Statement
import org.neo4j.kernel.impl.core.ThreadToStatementContextBridge

class GraphBasedTransactionAccessProvider(graph: GraphDatabaseAPI) extends TransactionAccessProvider {
  private val graphAPI = graph.asInstanceOf[GraphDatabaseAPI]
  private val (kernel, txBridge) = {
    val dependencyResolver = graphAPI.getDependencyResolver
    val kernel = dependencyResolver.resolveDependency(classOf[org.neo4j.kernel.api.KernelAPI])
    val txBridge = dependencyResolver.resolveDependency(classOf[ThreadToStatementContextBridge])
    (kernel, txBridge)
  }

  private var readers = new AccessorRegistry[TransactionAccess]("TransactionReadAccess")
  private var writers = new AccessorRegistry[TransactionAccess]("TransactionWriteAccess")
  private var transactionControl = new GraphTransactionControl

  override def currentTransaction = transactionControl.get().transaction

  override def acquireReadAccess(name: String): TransactionReadAccess = {
    if (writers.nonEmpty)
      throw new IllegalStateException("Cannot acquire read access while writing")

    val graphTransaction = transactionControl.get()

    new AbstractTransactionAccess(name, graphTransaction, readers) with TransactionReadAccess  {
      override def markToCommit(): Unit = failIfClosed { graphTransaction.markTo(Commit) }
      override def markToAbort(): Unit = failIfClosed { graphTransaction.markTo(Abort) }

      override def release(): Unit = failIfClosed {
        try {
          if (isExclusive) graphTransaction.finish()
        } finally {
          close()
        }
      }
    }
  }

  override def acquireWriteAccess(name: String): TransactionWriteAccess = {
    if (readers.nonEmpty)
      throw new IllegalStateException("Cannot acquire write access while reading")

    if (writers.nonEmpty)
      throw new IllegalStateException("Cannot acquire write access while there is another writer")

    val graphTransaction = transactionControl.get()

    new AbstractTransactionAccess(name, graphTransaction, writers) with TransactionWriteAccess  {
      override def commit(): Unit = failIfClosed {
        try {
          graphTransaction.markTo(Commit)
          graphTransaction.finish()
        } finally {
          close()
        }
      }

      override def abort(): Unit = failIfClosed {
        try {
          graphTransaction.markTo(Abort)
          graphTransaction.finish()
        } finally {
          close()
        }
      }
    }
  }

  override def toString = s"TransactionAccessProvider(readers=$readers, writers=$writers)"

  private abstract class AbstractTransactionAccess(
      name: String,
      graphTransaction: GraphTransaction,
      registry: AccessorRegistry[TransactionAccess])
    extends TransactionAccess with CloseableOnce {

    self =>

    registry += name -> self

    def statement = failIfClosed { graphTransaction.statement }

    override def insideTopLevelTransaction: Boolean =
      failIfClosed { graphTransaction.isTopLevelTx }

    override def isExclusive: Boolean =
      failIfClosed { registry.size == 1 }

    def discard() = close()

    override protected def close(): Unit = try {
        super.close()
      } finally  {
        registry -= name
      }
  }

  type GraphTransaction = GraphTransactionControl#GraphTransaction

  class GraphTransactionControl {

    control =>

    private var graphTransaction: Option[GraphTransaction] = None

    def get() = graphTransaction match {
      case Some(_graphTransaction) =>
        _graphTransaction

      case None =>
        val _graphTransaction = new GraphTransaction
        graphTransaction = Some(_graphTransaction)
        _graphTransaction
    }

    def clear(): Unit = {
      graphTransaction = None
    }

    class GraphTransaction {
      private val _isTopLevelTx: Boolean = !txBridge.hasTransaction
      private var finishMode: FinishMode = KeepOpen

      private val _transaction: Transaction = graph.beginTx()
      private var _statement: Statement = null

      def transaction = _transaction

      def statement = {
        if (_statement == null)
          _statement = txBridge.get()
        _statement
      }

      def isTopLevelTx = _isTopLevelTx

      def markTo(newMode: FinishMode) {
        finishMode = finishMode.update(newMode)
      }

      def finish(): Unit = finishMode match {
        case Commit =>
          try {
            closeStatement()
            transaction.success()
            transaction.close()
          } finally {
            control.clear()
          }

        case Abort =>
          try {
            closeStatement()
            transaction.failure()
            transaction.close()
          } finally {
            control.clear()
          }

        case KeepOpen =>
          // free associated locks etc
          closeStatement()
      }

      private def closeStatement(): Unit =
      {
        if (_statement != null) {
          _statement.close()
          _statement = null
        }
      }
    }
  }
}
