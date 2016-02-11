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
package org.neo4j.cypher.internal.transaction.api

import org.neo4j.graphdb.Transaction
import org.neo4j.kernel.api.Statement

trait TransactionAccessProvider {
  def acquireReadAccess(name: String): TransactionReadAccess
  def acquireWriteAccess(name: String): TransactionWriteAccess

  // Temporary Hack
  def currentTransaction: Transaction
}

trait TransactionAccess {
  def insideTopLevelTransaction: Boolean
  def isExclusive: Boolean

  def statement: Statement

  // Temporary Hack
  def discard()

  def commit()
  def abort()
}

trait TransactionReadAccess extends TransactionAccess {
  def commit(): Unit = {
    if (isExclusive) {
      markToCommit()
      release()
    } else {
      throw new IllegalStateException("Cannot commit directly from non-exclusive read access")
    }
  }

  def abort(): Unit = {
    if (isExclusive) {
      markToAbort()
      release()
    } else {
      throw new IllegalStateException("Cannot abort directly from non-exclusive read access")
    }
  }

  def markToCommit()
  def markToAbort()

  def release()
}

trait TransactionWriteAccess extends TransactionAccess {
  override final def isExclusive = true
}
