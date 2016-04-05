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
package org.neo4j.test.bootclasspathrunner;

import org.junit.runners.model.Statement;
import org.junit.runners.model.TestTimedOutException;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.neo4j.test.ThreadTestUtils;

public class VerboseFailOnTimeout extends Statement
{
    private final Statement originalStatement;
    private final TimeUnit timeUnit;
    private final long timeout;

    public VerboseFailOnTimeout( Statement statement, long timeout, TimeUnit timeUnit )
    {
        this.originalStatement = statement;
        this.timeout = timeout;
        this.timeUnit = timeUnit;
    }

    @Override
    public void evaluate() throws Throwable
    {
        CallableStatement callable = new CallableStatement();
        FutureTask<Throwable> task = new FutureTask<>( callable );
        Thread thread = new Thread( task, "Time-limited test" );
        thread.setDaemon( true );
        thread.start();
        callable.awaitStarted();
        Throwable throwable = getResult( task, thread );
        if ( throwable != null )
        {
            throw throwable;
        }
    }

    private Throwable getResult( FutureTask<Throwable> task, Thread thread ) throws Throwable
    {
        try
        {
            if ( timeout > 0 )
            {
                return task.get( timeout, timeUnit );
            }
            else
            {
                return task.get();
            }
        }
        catch ( ExecutionException e )
        {
            ThreadTestUtils.dumpAllStackTraces();
            return e.getCause();
        }
        catch ( TimeoutException e )
        {
            System.err.println( "=== Thread dump ===" );
            ThreadTestUtils.dumpAllStackTraces();
            return buildTimeoutException( thread );
        }
    }

    private Throwable buildTimeoutException( Thread thread ) throws TestTimedOutException
    {
        StackTraceElement[] stackTrace = thread.getStackTrace();
        TestTimedOutException timedOutException = new TestTimedOutException( timeout, timeUnit );
        timedOutException.setStackTrace( stackTrace );
        return timedOutException;
    }

    private class CallableStatement implements Callable<Throwable>
    {
        private final CountDownLatch startLatch = new CountDownLatch( 1 );

        public Throwable call() throws Exception
        {
            try
            {
                startLatch.countDown();
                originalStatement.evaluate();
            }
            catch ( Throwable e )
            {
                return e;
            }
            return null;
        }

        public void awaitStarted() throws InterruptedException
        {
            startLatch.await();
        }
    }
}
