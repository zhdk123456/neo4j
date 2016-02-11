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

import org.neo4j.kernel.api.Statement

class InternalTransaction(private[impl] val isTopLevel: Boolean,
                          private[impl] val graphTransaction: org.neo4j.graphdb.Transaction,
                          private[impl] var statement: InternalTransactionStatement) {


  private[impl] def nextStatement(newKernelStatement: => Statement): InternalTransactionStatement = {
    statement.close()
    statement = new InternalTransactionStatement(newKernelStatement)
    statement
  }

  private[impl] def abort() = {
    statement.close()
    graphTransaction.failure()
    graphTransaction.close()
  }

  private[impl] def commit() = {
    statement.close()
    graphTransaction.success()
    graphTransaction.close()
  }

  override def toString = {
    val name: String = if (isTopLevel) "GraphBasedTopLevelTx" else "GraphBasedTx"
    s"$name(tx=@${hashString(graphTransaction)}, statement=@${hashString(statement)})"
  }

  private def hashString(thing: Any): String =
    Integer.toHexString(System.identityHashCode(thing))
}


