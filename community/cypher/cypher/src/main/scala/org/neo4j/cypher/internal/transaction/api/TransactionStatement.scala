package org.neo4j.cypher.internal.transaction.api

import org.neo4j.kernel.api._

/**
 * Give access to underlying kernel statement without exposing close (eventually)
 */
trait TransactionStatement
  // TODO: Temporary hack
  extends Statement {

  def readOperations: ReadOperations
  def dataWriteOperations: DataWriteOperations
  def schemaWriteOperations: SchemaWriteOperations
  def tokenWriteOperations: TokenWriteOperations
}
