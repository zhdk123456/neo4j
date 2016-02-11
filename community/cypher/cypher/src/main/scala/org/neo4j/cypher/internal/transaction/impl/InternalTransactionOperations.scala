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

import org.neo4j.cypher.internal.transaction.api.{TransactionOperations, TransactionStatement}
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.kernel.api.Statement
import org.neo4j.kernel.impl.core.ThreadToStatementContextBridge

class InternalTransactionOperations(graph: GraphDatabaseService,
                                    txBridge: ThreadToStatementContextBridge)
  extends TransactionOperations {

  type Transaction = InternalTransaction

  override def isTopLevelTransaction(transaction: InternalTransaction): Boolean =
    transaction.isTopLevel

  override def beginTransaction(): InternalTransaction = {
    val isTopLevel = !txBridge.hasTransaction
    val tx = graph.beginTx()
    val statement = new InternalTransactionStatement(txBridge.get())
    new InternalTransaction(isTopLevel, tx, statement)
  }

  override def graphTransaction(transaction: InternalTransaction) =
    transaction.graphTransaction

  override def currentStatement(transaction: InternalTransaction): TransactionStatement =
    transaction.statement

  override def nextStatement(transaction: InternalTransaction): TransactionStatement =
    transaction.nextStatement(txBridge.get)

  override def abort(transaction: InternalTransaction): Unit =
    transaction.abort()

  override def commit(transaction: InternalTransaction): Unit =
    transaction.commit()
}

