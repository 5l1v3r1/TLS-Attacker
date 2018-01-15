/**
 * TLS-Attacker - A Modular Penetration Testing Framework for TLS
 *
 * Copyright 2014-2017 Ruhr University Bochum / Hackmanit GmbH
 *
 * Licensed under Apache License 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package de.rub.nds.tlsattacker.core.workflow.action.executor;

import de.rub.nds.modifiablevariable.util.ArrayConverter;
import de.rub.nds.tlsattacker.core.constants.AlertLevel;
import de.rub.nds.tlsattacker.core.constants.HandshakeByteLength;
import de.rub.nds.tlsattacker.core.constants.HandshakeMessageType;
import de.rub.nds.tlsattacker.core.constants.ProtocolMessageType;
import de.rub.nds.tlsattacker.core.exceptions.AdjustmentException;
import de.rub.nds.tlsattacker.core.exceptions.ParserException;
import de.rub.nds.tlsattacker.core.https.HttpsRequestHandler;
import de.rub.nds.tlsattacker.core.https.HttpsResponseHandler;
import de.rub.nds.tlsattacker.core.protocol.handler.DtlsHandshakeMessageFragmentHandler;
import de.rub.nds.tlsattacker.core.protocol.handler.ParserResult;
import de.rub.nds.tlsattacker.core.protocol.handler.ProtocolMessageHandler;
import de.rub.nds.tlsattacker.core.protocol.handler.SSL2ServerHelloHandler;
import de.rub.nds.tlsattacker.core.protocol.handler.factory.HandlerFactory;
import de.rub.nds.tlsattacker.core.protocol.message.AlertMessage;
import de.rub.nds.tlsattacker.core.protocol.message.DtlsHandshakeMessageFragment;
import de.rub.nds.tlsattacker.core.protocol.message.ProtocolMessage;
import de.rub.nds.tlsattacker.core.record.AbstractRecord;
import de.rub.nds.tlsattacker.core.state.TlsContext;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ReceiveMessageHelper {

    protected static final Logger LOGGER = LogManager.getLogger(ReceiveMessageHelper.class.getName());

    public ReceiveMessageHelper() {
    }

    public MessageActionResult receiveMessages(TlsContext context) {
        return receiveMessages(new LinkedList<ProtocolMessage>(), context);
    }

    /**
     * Receives messages, and tries to receive the messages specified in
     * messages
     *
     * @param expectedMessages
     *            Messages which should be received
     * @param context
     *            The context on which Messages should be received
     * @return Actually received Messages
     */
    public MessageActionResult receiveMessages(List<ProtocolMessage> expectedMessages, TlsContext context) {
        context.setTalkingConnectionEndType(context.getChooser().getMyConnectionPeer());
        List<AbstractRecord> realRecords = new LinkedList<>();
        List<ProtocolMessage> messages = new LinkedList<>();
        List<ProtocolMessage> fragmentedMessages = new LinkedList<>();
        try {
            byte[] receivedBytes;
            boolean shouldContinue = true;
            do {
                receivedBytes = receiveByteArray(context);
                if (receivedBytes.length != 0) {
                    List<AbstractRecord> tempRecords = parseRecords(receivedBytes, context);
                    List<List<AbstractRecord>> recordGroups = getRecordGroups(tempRecords);
                    for (List<AbstractRecord> recordGroup : recordGroups) {
                        if (context.getChooser().getSelectedProtocolVersion().isDTLS()) {
                            decryptRecords(recordGroup, context);
                            fragmentedMessages.addAll(collectMessageFragments(recordGroup, recordGroup.get(0)
                                    .getContentMessageType(), context));
                            messages.addAll(processFragmentGroup(fragmentedMessages, context));
                        } else {
                            messages.addAll(processRecordGroup(recordGroup, context));
                        }
                    }
                    if (context.getConfig().isQuickReceive() && !expectedMessages.isEmpty()) {
                        shouldContinue = shouldContinue(expectedMessages, messages, context);

                    }
                    realRecords.addAll(tempRecords);
                }
            } while (receivedBytes.length != 0 && shouldContinue);

        } catch (IOException ex) {
            LOGGER.warn("Received " + ex.getLocalizedMessage() + " while recieving for Messages.");
            LOGGER.debug(ex);
            context.setReceivedTransportHandlerException(true);
        }
        if (fragmentedMessages.size() == 0) {
            fragmentedMessages = null;
        }
        return new MessageActionResult(realRecords, messages, fragmentedMessages);
    }

    public List<AbstractRecord> receiveRecords(TlsContext context) {
        context.setTalkingConnectionEndType(context.getChooser().getMyConnectionPeer());
        List<AbstractRecord> realRecords = new LinkedList<>();
        try {
            byte[] receivedBytes;
            do {
                receivedBytes = receiveByteArray(context);
                if (receivedBytes.length != 0) {
                    List<AbstractRecord> tempRecords = parseRecords(receivedBytes, context);
                    realRecords.addAll(tempRecords);
                }
            } while (receivedBytes.length != 0);

        } catch (IOException ex) {
            LOGGER.warn("Received " + ex.getLocalizedMessage() + " while recieving for Messages.");
            LOGGER.debug(ex);
            context.setReceivedTransportHandlerException(true);
        }
        return realRecords;
    }

    private boolean receivedFatalAlert(List<ProtocolMessage> messages) {
        for (ProtocolMessage message : messages) {
            if (message instanceof AlertMessage) {
                AlertMessage alert = (AlertMessage) message;
                if (alert.getLevel().getValue() == AlertLevel.FATAL.getValue()) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean receivedAllExpectedMessage(List<ProtocolMessage> expectedMessages,
            List<ProtocolMessage> actualMessages, boolean earlyStop) {
        if (actualMessages.size() != expectedMessages.size() && !earlyStop) {
            return false;
        } else {
            for (int i = 0; i < expectedMessages.size(); i++) {
                if (i >= actualMessages.size()) {
                    return false;
                }
                if (!expectedMessages.get(i).getClass().equals(actualMessages.get(i).getClass())) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean shouldContinue(List<ProtocolMessage> expectedMessages, List<ProtocolMessage> receivedMessages,
            TlsContext context) {

        boolean receivedFatalAlert = receivedFatalAlert(receivedMessages);
        if (receivedFatalAlert) {
            return false;
        }
        boolean receivedAllExpectedMessages = receivedAllExpectedMessage(expectedMessages, receivedMessages, context
                .getConfig().isEarlyStop());
        if (receivedAllExpectedMessages) {
            return false;
        }
        return true;
    }

    private List<ProtocolMessage> processRecordGroup(List<AbstractRecord> recordGroup, TlsContext context) {
        adjustContext(recordGroup, context);
        decryptRecords(recordGroup, context);
        return parseMessages(recordGroup, context);
    }

    private byte[] receiveByteArray(TlsContext context) throws IOException {
        byte[] received = context.getTransportHandler().fetchData();
        return received;
    }

    private List<AbstractRecord> parseRecords(byte[] recordBytes, TlsContext context) {
        try {
            return context.getRecordLayer().parseRecords(recordBytes);
        } catch (ParserException ex) {
            LOGGER.debug(ex);
            LOGGER.debug("Could not parse provided Bytes into records. Waiting for more Packets");
            byte[] extraBytes = new byte[0];
            try {
                extraBytes = receiveByteArray(context);
            } catch (IOException ex2) {
                LOGGER.warn("Could not receive more Bytes", ex2);
                context.setReceivedTransportHandlerException(true);
            }
            if (extraBytes != null && extraBytes.length >= 0) {
                return parseRecords(ArrayConverter.concatenate(recordBytes, extraBytes), context);
            }
            LOGGER.debug("Did not receive more Bytes. Parsing records softly");
            return context.getRecordLayer().parseRecordsSoftly(recordBytes);
        }
    }

    public List<ProtocolMessage> parseMessages(List<AbstractRecord> records, TlsContext context) {
        byte[] cleanProtocolMessageBytes = getCleanBytes(records);
        return handleCleanBytes(cleanProtocolMessageBytes, getProtocolMessageType(records), context);
    }

    private List<ProtocolMessage> handleCleanBytes(byte[] cleanProtocolMessageBytes,
            ProtocolMessageType typeFromRecord, TlsContext context) {
        int dataPointer = 0;
        List<ProtocolMessage> receivedMessages = new LinkedList<>();
        while (dataPointer < cleanProtocolMessageBytes.length) {
            ParserResult result = null;
            try {
                if (typeFromRecord != null) {
                    if (typeFromRecord == ProtocolMessageType.APPLICATION_DATA
                            && context.getConfig().isHttpsParsingEnabled()) {
                        try {
                            result = tryHandleAsHttpsMessage(cleanProtocolMessageBytes, dataPointer, context);
                        } catch (ParserException | AdjustmentException E) {
                            result = tryHandleAsCorrectMessage(cleanProtocolMessageBytes, dataPointer, typeFromRecord,
                                    context);
                        }
                    } else {
                        result = tryHandleAsCorrectMessage(cleanProtocolMessageBytes, dataPointer, typeFromRecord,
                                context);

                    }
                } else {
                    result = tryHandleAsSslMessage(cleanProtocolMessageBytes, dataPointer, context);
                }
            } catch (ParserException | AdjustmentException exCorrectMsg) {
                LOGGER.warn("Could not parse Message as a CorrectMessage");
                LOGGER.debug(exCorrectMsg);
                try {
                    if (typeFromRecord == ProtocolMessageType.HANDSHAKE) {
                        LOGGER.warn("Trying to parse Message as UnknownHandshakeMessage");
                        result = tryHandleAsUnknownHandshakeMessage(cleanProtocolMessageBytes, dataPointer,
                                typeFromRecord, context);
                    } else {
                        try {
                            result = tryHandleAsUnknownMessage(cleanProtocolMessageBytes, dataPointer, context);
                        } catch (ParserException | AdjustmentException exUnknownHMsg) {
                            LOGGER.warn("Could not parse Message as UnknownMessage");
                            LOGGER.debug(exUnknownHMsg);
                            break;
                        }
                    }
                } catch (ParserException exUnknownHandshakeMsg) {
                    LOGGER.warn("Could not parse Message as UnknownHandshakeMessage");
                    LOGGER.debug(exUnknownHandshakeMsg);

                    try {
                        result = tryHandleAsUnknownMessage(cleanProtocolMessageBytes, dataPointer, context);
                    } catch (ParserException | AdjustmentException exUnknownHMsg) {
                        LOGGER.warn("Could not parse Message as UnknownMessage");
                        LOGGER.debug(exUnknownHMsg);
                        break;
                    }
                }
            }
            if (result != null) {
                if (dataPointer == result.getParserPosition()) {
                    throw new ParserException("Ran into an infinite loop while parsing ProtocolMessages");
                }
                dataPointer = result.getParserPosition();
                LOGGER.debug("The following message was parsed: {}", result.getMessage().toString());
                receivedMessages.add(result.getMessage());
            }
        }
        return receivedMessages;
    }

    private List<ProtocolMessage> handleFragments(byte[] cleanProtocolMessageBytes, ProtocolMessageType typeFromRecord,
            TlsContext context) {
        int dataPointer = 0;
        List<ProtocolMessage> receivedFragmentMessages = new LinkedList<>();
        while (dataPointer < cleanProtocolMessageBytes.length) {
            ParserResult result = null;
            try {
                if (typeFromRecord == ProtocolMessageType.HANDSHAKE) {
                    result = tryParseAsDtlsMessageFragment(cleanProtocolMessageBytes, dataPointer, typeFromRecord,
                            context);
                } else {
                    result = tryHandleAsCorrectMessage(cleanProtocolMessageBytes, dataPointer, typeFromRecord, context);
                }
            } catch (ParserException | AdjustmentException exCorrectMsg) {
                LOGGER.warn("Could not parse Message as a DtlsMessageFragment/NonHsMessage");
                LOGGER.debug(exCorrectMsg);
                try {
                    result = tryHandleAsUnknownMessage(cleanProtocolMessageBytes, dataPointer, context);
                } catch (ParserException exUnknownMsg) {
                    LOGGER.warn("Could not parse Message as UnknownMessage");
                    LOGGER.debug(exUnknownMsg);
                    break;
                }
            }
            if (result != null) {
                if (dataPointer == result.getParserPosition()) {
                    throw new ParserException("Ran into an infinite loop while parsing ProtocolMessages");
                }
                dataPointer = result.getParserPosition();
                LOGGER.debug("The following message was parsed: {}", result.getMessage().toString());
                receivedFragmentMessages.add(result.getMessage());
            }
        }
        return receivedFragmentMessages;
    }

    private ParserResult tryHandleAsHttpsMessage(byte[] protocolMessageBytes, int pointer, TlsContext context)
            throws ParserException, AdjustmentException {
        try {
            HttpsRequestHandler handler = new HttpsRequestHandler(context);
            return handler.parseMessage(protocolMessageBytes, pointer, false);
        } catch (ParserException E) {
            try {
                HttpsResponseHandler handler = new HttpsResponseHandler(context);
                return handler.parseMessage(protocolMessageBytes, pointer, false);
            } catch (ParserException E2) {
                throw new ParserException("Could not parse ApplicationData as HTTPS", E2);
            }
        }
    }

    private ParserResult tryHandleAsCorrectMessage(byte[] protocolMessageBytes, int pointer,
            ProtocolMessageType typeFromRecord, TlsContext context) throws ParserException, AdjustmentException {
        HandshakeMessageType handshakeMessageType = HandshakeMessageType.getMessageType(protocolMessageBytes[pointer]);
        ProtocolMessageHandler pmh = HandlerFactory.getHandler(context, typeFromRecord, handshakeMessageType);
        return pmh.parseMessage(protocolMessageBytes, pointer, false);
    }

    private ParserResult tryParseAsCorrectMessage(byte[] protocolMessageBytes, int pointer,
            ProtocolMessageType typeFromRecord, TlsContext context) throws ParserException, AdjustmentException {
        HandshakeMessageType handshakeMessageType = HandshakeMessageType.getMessageType(protocolMessageBytes[pointer]);
        ProtocolMessageHandler pmh = HandlerFactory.getHandler(context, typeFromRecord, handshakeMessageType);
        return pmh.parseMessage(protocolMessageBytes, pointer, true);
    }

    private ParserResult tryParseAsDtlsMessageFragment(byte[] protocolMessageBytes, int pointer,
            ProtocolMessageType typeFromRecord, TlsContext context) throws ParserException, AdjustmentException {
        DtlsHandshakeMessageFragmentHandler fragmentHandler = new DtlsHandshakeMessageFragmentHandler(context);
        return fragmentHandler.parseMessage(protocolMessageBytes, pointer, true);
    }

    private ParserResult tryHandleAsSslMessage(byte[] cleanProtocolMessageBytes, int dataPointer, TlsContext context) {
        ProtocolMessageHandler pmh = new SSL2ServerHelloHandler(context);
        return pmh.parseMessage(cleanProtocolMessageBytes, dataPointer, false);
    }

    private ParserResult tryHandleAsUnknownHandshakeMessage(byte[] protocolMessageBytes, int pointer,
            ProtocolMessageType typeFromRecord, TlsContext context) throws ParserException, AdjustmentException {
        ProtocolMessageHandler pmh = HandlerFactory.getHandler(context, typeFromRecord, HandshakeMessageType.UNKNOWN);
        return pmh.parseMessage(protocolMessageBytes, pointer, false);
    }

    private ParserResult tryHandleAsUnknownMessage(byte[] protocolMessageBytes, int pointer, TlsContext context)
            throws ParserException, AdjustmentException {
        ProtocolMessageHandler pmh = HandlerFactory.getHandler(context, ProtocolMessageType.UNKNOWN, null);
        return pmh.parseMessage(protocolMessageBytes, pointer, false);
    }

    private byte[] getCleanBytes(List<AbstractRecord> recordSubGroup) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        for (AbstractRecord record : recordSubGroup) {
            try {
                stream.write(record.getCleanProtocolMessageBytes().getValue());
            } catch (IOException ex) {
                LOGGER.warn("Could not write CleanProtocolMessage bytes to Array");
                LOGGER.debug(ex);
            }
        }
        return stream.toByteArray();
    }

    private List<List<AbstractRecord>> getRecordGroups(List<AbstractRecord> records) {
        List<List<AbstractRecord>> returnList = new LinkedList<>();
        if (records.isEmpty()) {
            return returnList;
        }
        List<AbstractRecord> subGroup = new LinkedList<>();
        ProtocolMessageType currentSearchType = records.get(0).getContentMessageType();
        for (AbstractRecord record : records) {
            if (record.getContentMessageType() == currentSearchType) {
                subGroup.add(record);
            } else {
                returnList.add(subGroup);
                subGroup = new LinkedList<>();
                currentSearchType = record.getContentMessageType();
                subGroup.add(record);
            }
        }
        returnList.add(subGroup);
        return returnList;

    }

    private ProtocolMessageType getProtocolMessageType(List<AbstractRecord> recordSubGroup) {
        ProtocolMessageType type = null;
        for (AbstractRecord record : recordSubGroup) {
            if (type == null) {
                type = record.getContentMessageType();
            } else {
                ProtocolMessageType tempType = ProtocolMessageType.getContentType(record.getContentMessageType()
                        .getValue());
                if (tempType != type) {
                    LOGGER.error("Mixed Subgroup detected");
                }
            }

        }
        return type;
    }

    private void decryptRecords(List<AbstractRecord> records, TlsContext context) {
        for (AbstractRecord record : records) {
            context.getRecordLayer().decryptRecord(record);
        }
    }

    private void adjustContext(List<AbstractRecord> recordGroup, TlsContext context) {
        for (AbstractRecord record : recordGroup) {
            record.adjustContext(context);
        }
    }

    private List<ProtocolMessage> collectMessageFragments(List<AbstractRecord> recordGroup,
            ProtocolMessageType typeFromRecord, TlsContext context) {
        byte[] cleanBytes = getCleanBytes(recordGroup);
        return handleFragments(cleanBytes, typeFromRecord, context);
    }

    private List<ProtocolMessage> processFragmentGroup(List<ProtocolMessage> fragmentedMessages, TlsContext context) {
        List<ProtocolMessage> realMessages = new LinkedList<>();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        ProtocolMessageType lastRecordType = null;
        for (ProtocolMessage message : fragmentedMessages) {
            if (lastRecordType == null) {
                lastRecordType = message.getProtocolMessageType();
            }
            if (message instanceof DtlsHandshakeMessageFragment) {
                DtlsHandshakeMessageFragment fragment = (DtlsHandshakeMessageFragment) message;
                if (fragment.getFragmentOffset().getValue() == 0) {
                    stream.write(fragment.getType().getValue());
                    try {
                        stream.write(ArrayConverter.intToBytes(fragment.getLength().getValue(),
                                HandshakeByteLength.MESSAGE_LENGTH_FIELD));
                    } catch (IOException ex) {
                        LOGGER.warn("Could not write fragment to stream.", ex);
                    }
                }
                try {
                    stream.write(fragment.getContent().getValue());
                } catch (IOException ex) {
                    LOGGER.warn("Could not write fragment to stream.", ex);
                }
            } else {
                message.getHandler(context).prepareAfterParse(message);
                message.getHandler(context).adjustTLSContext(message);
                realMessages.add(message);
            }
            lastRecordType = message.getProtocolMessageType();
        }
        if (stream.size() > 0) {
            realMessages.addAll(handleCleanBytes(stream.toByteArray(), lastRecordType, context));
            for (ProtocolMessage realMessage : realMessages) {
                // This cannot work in the general case.....
                realMessage.getHandler(context).prepareAfterParse(realMessage);
                realMessage.getHandler(context).adjustTLSContext(realMessage);

            }
        }
        return realMessages;
    }
}
