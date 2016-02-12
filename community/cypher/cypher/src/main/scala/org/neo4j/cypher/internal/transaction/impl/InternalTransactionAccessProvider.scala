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

import org.neo4j.cypher.internal.transaction.api._
import org.neo4j.graphdb
import org.neo4j.graphdb.Transaction
import org.neo4j.kernel.GraphDatabaseAPI
import org.neo4j.kernel.api.Statement
import org.neo4j.kernel.impl.core.ThreadToStatementContextBridge

class InternalTransactionAccessProvider(ops: TransactionOperations) extends TransactionAccessProvider {
  private type Transaction = ops.Transaction

  private var state: InternalTransactionState = Closed
  private var cachedTransaction: Option[Transaction] = None

  override def acquireReadAccess(): TransactionReadAccess =
    new AbstractTransactionAccess(OpenForReading) with TransactionReadAccess {
      // TODO: Temporary Hack (?); Expose operations in subinterfaces instead?
      override def statement: TransactionStatement =
        failIfClosed { ops.currentStatement(transaction) }

      override def release(): Unit =
        runAndClose { ops.nextStatement(transaction) }
  }

  def acquireWriteAccess(): TransactionWriteAccess = {
    new AbstractTransactionAccess(OpenForWriting) with TransactionWriteAccess {
      // TODO: Temporary Hack (?); Expose operations in subinterfaces instead?
      override def statement: TransactionStatement =
        failIfClosed { ops.currentStatement(transaction) }
    }
  }

  private abstract class AbstractTransactionAccess(initialState: InternalTransactionState)
    extends TransactionAccess with CloseableOnce {

    protected val transaction = acquireTransaction(initialState)

    override def insideTopLevelTransaction: Boolean =
      failIfClosed { ops.isTopLevelTransaction(transaction) }

    override def discard(): Unit =
      runAndClose { releaseTransaction() }

    override def abort(): Unit =
      runAndClose { try { ops.abort(transaction) }  finally { releaseTransaction() } }

    override def commit(): Unit =
      runAndClose { try { ops.commit(transaction) } finally { releaseTransaction() } }
  }

  private def acquireTransaction(newState: InternalTransactionState) = {
    updateState(newState)

    cachedTransaction match {
      case Some(existingTransaction) => existingTransaction
      case None =>
        val newTransaction = ops.beginTransaction()
        cachedTransaction = Some(newTransaction)
        newTransaction
    }
  }

  private def releaseTransaction() = {
    updateState(Closed)

    cachedTransaction = None
  }

  private def updateState(newState: InternalTransactionState) = {
    state = state.updatedWith(newState)
  }

  // TODO: Temporary Hack
  override def currentGraphTransaction: Option[graphdb.Transaction] =
    cachedTransaction.map(ops.graphTransaction)
}
