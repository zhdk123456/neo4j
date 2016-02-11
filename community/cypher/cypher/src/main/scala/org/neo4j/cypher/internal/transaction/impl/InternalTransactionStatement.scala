package org.neo4j.cypher.internal.transaction.impl

import org.neo4j.cypher.internal.transaction.api.TransactionStatement
import org.neo4j.kernel.api._

class InternalTransactionStatement(val kernelStatement: Statement)
  extends TransactionStatement {

  self =>

  def readOperations = kernelStatement.readOperations()
  def dataWriteOperations= kernelStatement.dataWriteOperations()
  def schemaWriteOperations = kernelStatement.schemaWriteOperations()
  def tokenWriteOperations = kernelStatement.tokenWriteOperations()

  // TODO: Temporary; should be private
  def close(): Unit = {
    kernelStatement.close()
  }
}
