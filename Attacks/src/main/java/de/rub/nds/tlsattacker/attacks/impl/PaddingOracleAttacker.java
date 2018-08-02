/**
 * TLS-Attacker - A Modular Penetration Testing Framework for TLS
 *
 * Copyright 2014-2017 Ruhr University Bochum / Hackmanit GmbH
 *
 * Licensed under Apache License 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package de.rub.nds.tlsattacker.attacks.impl;

import de.rub.nds.tlsattacker.attacks.config.PaddingOracleCommandConfig;
import de.rub.nds.tlsattacker.attacks.constants.PaddingRecordGeneratorType;
import de.rub.nds.tlsattacker.attacks.exception.AttackFailedException;
import de.rub.nds.tlsattacker.attacks.exception.PaddingOracleUnstableException;
import de.rub.nds.tlsattacker.attacks.padding.PaddingVectorGenerator;
import de.rub.nds.tlsattacker.attacks.padding.PaddingVectorGeneratorFactory;
import de.rub.nds.tlsattacker.attacks.util.response.EqualityError;
import de.rub.nds.tlsattacker.attacks.util.response.EqualityErrorTranslator;
import de.rub.nds.tlsattacker.attacks.util.response.FingerPrintChecker;
import de.rub.nds.tlsattacker.attacks.util.response.ResponseExtractor;
import de.rub.nds.tlsattacker.attacks.util.response.ResponseFingerprint;
import de.rub.nds.tlsattacker.core.config.Config;
import de.rub.nds.tlsattacker.core.constants.CipherSuite;
import de.rub.nds.tlsattacker.core.constants.ProtocolVersion;
import de.rub.nds.tlsattacker.core.exceptions.ConfigurationException;
import de.rub.nds.tlsattacker.core.exceptions.WorkflowExecutionException;
import de.rub.nds.tlsattacker.core.record.AbstractRecord;
import de.rub.nds.tlsattacker.core.record.Record;
import de.rub.nds.tlsattacker.core.state.State;
import de.rub.nds.tlsattacker.core.util.LogLevel;
import de.rub.nds.tlsattacker.core.workflow.WorkflowExecutor;
import de.rub.nds.tlsattacker.core.workflow.WorkflowExecutorFactory;
import de.rub.nds.tlsattacker.core.workflow.WorkflowTrace;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import org.apache.logging.log4j.Level;

/**
 * Executes a padding oracle attack check. It logs an error in case the tested
 * server is vulnerable to poodle.
 */
public class PaddingOracleAttacker extends Attacker<PaddingOracleCommandConfig> {

    private final Config tlsConfig;

    private boolean groupRecords = true;

    private HashMap<Integer, List<ResponseFingerprint>> responseMap;

    private CipherSuite testedSuite;

    private ProtocolVersion testedVersion;

    public PaddingOracleAttacker(PaddingOracleCommandConfig paddingOracleConfig, Config baseConfig) {
        super(paddingOracleConfig, baseConfig);
        tlsConfig = getTlsConfig();
    }

    @Override
    public void executeAttack() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public State executeTlsFlow(WorkflowTrace trace) {
        tlsConfig.setAddSignatureAndHashAlgorithmsExtension(true);
        tlsConfig.setEarlyStop(true);
        tlsConfig.setStopActionsAfterFatal(true);
        tlsConfig.setQuickReceive(true);
        tlsConfig.setWorkflowExecutorShouldClose(false);
        State state = new State(tlsConfig, trace);
        WorkflowExecutor workflowExecutor = WorkflowExecutorFactory.createWorkflowExecutor(
                tlsConfig.getWorkflowExecutorType(), state);
        workflowExecutor.executeWorkflow();
        return state;
    }

    @Override
    public Boolean isVulnerable() {
        if (config.getRecordGeneratorType() == PaddingRecordGeneratorType.VERY_SHORT) {
            groupRecords = false;
        }
        LOGGER.log(LogLevel.CONSOLE_OUTPUT,
                "A server is considered vulnerable to this attack if it responds differently to the test vectors.");
        LOGGER.log(LogLevel.CONSOLE_OUTPUT, "A server is considered secure if it always responds the same way.");
        EqualityError error;

        try {
            responseMap = createResponseMap();
            error = getEqualityError(responseMap);
            if (error == EqualityError.SOCKET_EXCEPTION || error == EqualityError.SOCKET_STATE) {
                LOGGER.log(LogLevel.CONSOLE_OUTPUT, "Found a candidate for a Socket difference performing rescan");
                HashMap<Integer, List<ResponseFingerprint>> responseMapTwo = createResponseMap();
                EqualityError errorTwo = getEqualityError(responseMapTwo);
                if (error == errorTwo && lookEqual(responseMap, responseMapTwo)) {
                    HashMap<Integer, List<ResponseFingerprint>> responseMapThree = createResponseMap();
                    EqualityError errorThree = getEqualityError(responseMapThree);
                    if (error == errorThree && lookEqual(responseMap, responseMapThree)) {
                        LOGGER.log(LogLevel.CONSOLE_OUTPUT,
                                "Found an equality Error in a SocketState, performed to rescans and it still presisted");
                        LOGGER.log(LogLevel.CONSOLE_OUTPUT, "The Server is very likely vulnerabble");
                    } else {
                        LOGGER.log(LogLevel.CONSOLE_OUTPUT, "Rescan revealed a false positive");
                        return false;
                    }
                } else {
                    LOGGER.log(LogLevel.CONSOLE_OUTPUT, "Rescan revealed a false positive");
                    return false;
                }
            }
        } catch (AttackFailedException E) {
            LOGGER.log(LogLevel.CONSOLE_OUTPUT, E.getMessage());
            return null;
        }
        LOGGER.log(LogLevel.CONSOLE_OUTPUT, EqualityErrorTranslator.translation(error, null, null));
        if (error != EqualityError.NONE || LOGGER.getLevel().isMoreSpecificThan(Level.INFO)) {
            for (List<ResponseFingerprint> fingerprintList : responseMap.values()) {
                LOGGER.debug("----------------Map-----------------");
                for (ResponseFingerprint fingerprint : fingerprintList) {
                    LOGGER.debug(fingerprint.toString());
                }
            }
        }
        return error != EqualityError.NONE;
    }

    public boolean lookEqual(HashMap<Integer, List<ResponseFingerprint>> responseMapOne,
            HashMap<Integer, List<ResponseFingerprint>> responseMapTwo) {
        for (Integer key : responseMapOne.keySet()) {
            List<ResponseFingerprint> listOne = responseMapOne.get(key);
            List<ResponseFingerprint> listTwo = responseMapTwo.get(key);
            if (listOne.size() != listTwo.size()) {
                throw new PaddingOracleUnstableException(
                        "The padding Oracle seems to be unstable - there is something going terrible wrong. We recommend manual analysis");
            }
            for (int i = 0; i < listOne.size(); i++) {
                if (FingerPrintChecker.checkEquality(listOne.get(i), listTwo.get(i), false) != EqualityError.NONE) {
                    return false;
                }
            }
        }
        return true;
    }

    public HashMap<Integer, List<ResponseFingerprint>> createResponseMap() {
        PaddingVectorGenerator generator = PaddingVectorGeneratorFactory.getPaddingVectorGenerator(config);
        List<WorkflowTrace> traceList = generator.getPaddingOracleVectors(tlsConfig);
        boolean first = true;
        HashMap<Integer, List<ResponseFingerprint>> responseMap = new HashMap<>();
        for (WorkflowTrace trace : traceList) {
            State state;
            try {
                state = executeTlsFlow(trace);
                testedSuite = state.getTlsContext().getSelectedCipherSuite();
                testedVersion = state.getTlsContext().getSelectedProtocolVersion();
            } catch (WorkflowExecutionException | ConfigurationException E) {
                LOGGER.warn(E);
                LOGGER.warn("TLS-Attacker failed execute a Handshake. Reexecuting");
                try {
                    trace.reset();
                    state = executeTlsFlow(trace);
                } catch (WorkflowExecutionException | ConfigurationException Ex) {
                    LOGGER.warn(Ex);
                    LOGGER.warn("Could not execute Handshake with the Server");
                    throw new AttackFailedException(
                            "Could not execute Handshake with the Server. Maybe it does not support CBC. You will probably need to debug this");
                }

            }
            if (state.getWorkflowTrace().allActionsExecuted()) {
                ResponseFingerprint fingerprint = ResponseExtractor.getFingerprint(state);
                clearConnections(state);
                int length = getLastRecordLength(state);
                if (!groupRecords) {
                    length = 0;
                }
                List<ResponseFingerprint> responseFingerprintList = responseMap.get(length);
                if (responseFingerprintList == null) {
                    responseFingerprintList = new LinkedList<>();
                    responseMap.put(length, responseFingerprintList);
                }
                responseFingerprintList.add(fingerprint);
            } else {
                LOGGER.warn("Could not execute Workflow. Something went wrong... Check the debug output for more information");
            }
        }
        return responseMap;
    }

    private int getLastRecordLength(State state) {
        AbstractRecord lastRecord = state.getWorkflowTrace().getLastSendingAction().getSendRecords()
                .get(state.getWorkflowTrace().getLastSendingAction().getSendRecords().size() - 1);
        return ((Record) lastRecord).getLength().getValue();
    }

    public EqualityError getEqualityError(HashMap<Integer, List<ResponseFingerprint>> responseMap) {
        for (List<ResponseFingerprint> list : responseMap.values()) {
            ResponseFingerprint fingerprint = list.get(0);
            for (int i = 1; i < list.size(); i++) {
                EqualityError error = FingerPrintChecker.checkEquality(fingerprint, list.get(i), true);
                if (error != EqualityError.NONE) {
                    LOGGER.log(LogLevel.CONSOLE_OUTPUT, "Found an equality Error: " + error);
                    LOGGER.debug("Fingerprint1: " + fingerprint.toString());
                    LOGGER.debug("Fingerprint2: " + list.get(i).toString());

                    return error;
                }
            }
        }
        return EqualityError.NONE;
    }

    private void clearConnections(State state) {
        try {
            state.getTlsContext().getTransportHandler().closeConnection();
        } catch (IOException ex) {
            LOGGER.debug(ex);
        }
    }

    public HashMap<Integer, List<ResponseFingerprint>> getResponseMap() {
        return responseMap;
    }

    public CipherSuite getTestedSuite() {
        return testedSuite;
    }

    public ProtocolVersion getTestedVersion() {
        return testedVersion;
    }
}
