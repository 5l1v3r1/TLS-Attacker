/**
 * TLS-Attacker - A Modular Penetration Testing Framework for TLS
 *
 * Copyright 2014-2017 Ruhr University Bochum / Hackmanit GmbH
 *
 * Licensed under Apache License 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package de.rub.nds.tlsattacker.core.workflow.action.executor;

import de.rub.nds.tlsattacker.core.protocol.message.ProtocolMessage;
import de.rub.nds.tlsattacker.core.record.AbstractRecord;
import java.util.List;

public class MessageActionResult {

    private final List<AbstractRecord> recordList;

    private final List<ProtocolMessage> messageList;

    private final List<ProtocolMessage> messageFragmentList;

    public MessageActionResult(List<AbstractRecord> recordList, List<ProtocolMessage> messageList,
            List<ProtocolMessage> messageFragmentList) {
        this.recordList = recordList;
        this.messageList = messageList;
        this.messageFragmentList = messageFragmentList;
    }

    public List<AbstractRecord> getRecordList() {
        return recordList;
    }

    public List<ProtocolMessage> getMessageList() {
        return messageList;
    }

    public List<ProtocolMessage> getMessageFragmentList() {
        return messageFragmentList;
    }
}
