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

trait TransactionAccessProvider {
  def acquireReadAccess(): TransactionReadAccess
  def acquireWriteAccess(): TransactionWriteAccess

  // TODO: Temporary Hack; should go away once refactoring is complete
  def currentGraphTransaction: Option[org.neo4j.graphdb.Transaction]
}

trait TransactionAccess {
  def insideTopLevelTransaction: Boolean

  // TODO: Temporary Hack (?); Expose operations in subinterfaces instead?
  def statement: KernelStatement

  // TODO: Temporary Hack; should go away once refactoring is complete
  def discard()

  def commit()
  def abort()
}

trait TransactionReadAccess extends TransactionAccess {
  def release()
}

trait TransactionWriteAccess extends TransactionAccess {
  def commitAndReOpen()
}
