/**
 * TLS-Attacker - A Modular Penetration Testing Framework for TLS
 *
 * Copyright 2014-2017 Ruhr University Bochum / Hackmanit GmbH
 *
 * Licensed under Apache License 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package de.rub.nds.tlsattacker.core.workflow;

import de.rub.nds.tlsattacker.core.state.State;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 *
 */
public class ParallelExecutor {

    private static final Logger LOGGER = LogManager.getLogger();

    private final ExecutorService executorService;

    private final int size;

    private final int reexecutions;

    public ParallelExecutor(int size, int reexecutions) {
        executorService = new ThreadPoolExecutor(size, size, 10, TimeUnit.DAYS, new LinkedBlockingDeque<Runnable>());
        this.reexecutions = reexecutions;
        this.size = size;
        if (reexecutions < 0) {
            throw new IllegalArgumentException("Reexecutions is below zero");
        }
    }

    public ParallelExecutor(int size, int reexecutions, ThreadFactory factory) {
        executorService = new ThreadPoolExecutor(size, size, 10, TimeUnit.DAYS, new LinkedBlockingDeque<Runnable>(),
                factory);
        this.reexecutions = reexecutions;
        this.size = size;
        if (reexecutions < 0) {
            throw new IllegalArgumentException("Reexecutions is below zero");
        }
    }

    public Future addTask(State state) {
        if (executorService.isShutdown()) {
            throw new RuntimeException("Cannot add Tasks to already shutdown executor");
        }
        Future<?> submit = executorService.submit(new StateThreadExecutor(state, reexecutions));
        return submit;
    }

    public void bulkExecute(List<State> stateList) {
        if (executorService.isShutdown()) {
            throw new RuntimeException("Cannot add Tasks to already shutdown executor");
        }
        List<Future> futureList = new LinkedList<>();
        for (State state : stateList) {
            futureList.add(addTask(state));
        }
        for (Future future : futureList) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException ex) {
                throw new RuntimeException("Failed to execute tasks!");
            }
        }
        return;
    }

    public void bulkExecute(State... states) {
        this.bulkExecute(new ArrayList<>(Arrays.asList(states)));
    }

    public int getSize() {
        return size;
    }

    public void shutdown() {
        executorService.shutdown();
    }

    private class StateThreadExecutor implements Runnable {

        private final State state;

        private boolean hasError = false;

        public StateThreadExecutor(State state, int reexecutions) {
            this.state = state;
        }

        @Override
        public void run() {
            Exception exception = null;
            long sleepTime = 0;
            for (int i = 0; i < reexecutions + 1; i++) {
                WorkflowExecutor executor = new DefaultWorkflowExecutor(state);
                try {
                    if (sleepTime > 0) {
                        Thread.sleep(sleepTime);
                    }
                    executor.executeWorkflow();
                    break;
                } catch (Exception E) {
                    LOGGER.debug("Encountered an exception during the execution", E);
                    hasError = true;
                    sleepTime += 1000;
                    exception = E;
                }
            }
            if (hasError) {
                LOGGER.error("Could not executre Workflow.", exception);
                throw new RuntimeException("Could not execute State even after " + reexecutions + " reexecutions",
                        exception);
            }
        }

    }
}
