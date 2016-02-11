package org.neo4j.cypher.internal.transaction.api

import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.kernel.api.Statement
import org.neo4j.kernel.impl.core.ThreadToStatementContextBridge

/**
  * Low-level API for working with transactions
  */
trait TransactionOperations {
  type Transaction

  def isTopLevelTransaction(transaction: Transaction): Boolean
  def beginTransaction(): Transaction

  def currentStatement(transaction: Transaction): Statement
  def finishStatement(transaction: Transaction): Unit

  def abort(transaction: Transaction)
  def commit(transaction: Transaction)
}

class GraphBasedTransactionOperations(graph: GraphDatabaseService,
                                      txBridge: ThreadToStatementContextBridge)
  extends TransactionOperations {

  type Transaction = GraphBasedTransaction

  override def isTopLevelTransaction(transaction: GraphBasedTransaction): Boolean =
    transaction.isTopLevel

  override def beginTransaction(): GraphBasedTransaction = {
    val isTopLevel = txBridge.hasTransaction
    val tx = graph.beginTx()
    val statement = txBridge.get()
    new GraphBasedTransaction(tx, isTopLevel, statement)
  }

  override def currentStatement(transaction: GraphBasedTransaction): Statement =
    transaction.statement

  override def finishStatement(transaction: GraphBasedTransaction): Unit = {
    transaction.statement.close()
    transaction.statement = txBridge.get()
  }

  override def abort(transaction: GraphBasedTransaction): Unit = {
    transaction.tx.failure()
    transaction.tx.close()
  }

  override def commit(transaction: GraphBasedTransaction): Unit = {
    transaction.tx.success()
    transaction.tx.close()
  }
}

class GraphBasedTransaction(val tx: org.neo4j.graphdb.Transaction,
                            val isTopLevel: Boolean,
                            var statement: Statement) {
  override def toString = {
    val name: String = if (isTopLevel) "GraphBasedTopLevelTx" else "GraphBasedTx"
    s"$name(tx=@${hashString(tx)}, statement=@${hashString(statement)})"
  }

  def hashString(thing: Any): String =
    Integer.toHexString(System.identityHashCode(thing))
}
