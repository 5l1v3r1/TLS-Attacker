/**
 * TLS-Attacker - A Modular Penetration Testing Framework for TLS
 *
 * Copyright 2014-2016 Ruhr University Bochum / Hackmanit GmbH
 *
 * Licensed under Apache License 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package de.rub.nds.tlsattacker.tls.record;

import de.rub.nds.tlsattacker.modifiablevariable.ModifiableVariableFactory;
import de.rub.nds.tlsattacker.modifiablevariable.ModifiableVariableProperty;
import de.rub.nds.tlsattacker.modifiablevariable.bytearray.ModifiableByteArray;
import de.rub.nds.tlsattacker.tls.constants.ProtocolMessageType;
import de.rub.nds.tlsattacker.tls.constants.ProtocolVersion;
import de.rub.nds.tlsattacker.tls.protocol.ModifiableVariableHolder;
import de.rub.nds.tlsattacker.tls.record.encryptor.Encryptor;
import de.rub.nds.tlsattacker.tls.record.parser.AbstractRecordParser;
import de.rub.nds.tlsattacker.tls.record.preparator.AbstractRecordPreparator;
import de.rub.nds.tlsattacker.tls.record.serializer.AbstractRecordSerializer;
import de.rub.nds.tlsattacker.tls.workflow.TlsConfig;
import de.rub.nds.tlsattacker.tls.workflow.TlsContext;

/**
 *
 * @author Robert Merget <robert.merget@rub.de>
 */
public abstract class AbstractRecord extends ModifiableVariableHolder {

    /**
     * protocol message bytes transported in the record as seen on the transport
     * layer if encrypption is active this is encrypted if not its plaintext
     */
    @ModifiableVariableProperty(type = ModifiableVariableProperty.Type.CIPHERTEXT)
    private ModifiableByteArray protocolMessageBytes;

    /**
     * The decrypted , unpadded, unmaced record bytes
     */
    @ModifiableVariableProperty(type = ModifiableVariableProperty.Type.PLAIN_PROTOCOL_MESSAGE)
    private ModifiableByteArray cleanProtocolMessageBytes;

    private ProtocolMessageType contentMessageType;
    /**
     * maximum length configuration for this record
     */
    private Integer maxRecordLengthConfig;

    public AbstractRecord() {
    }

    public AbstractRecord(TlsConfig config) {
        this.maxRecordLengthConfig = config.getDefaultMaxRecordData();
    }

    public abstract AbstractRecordPreparator getRecordPreparator(TlsContext context, Encryptor encryptor,
            ProtocolMessageType type);

    public abstract AbstractRecordParser getRecordParser(int startposition, byte[] array, ProtocolVersion version);

    public abstract AbstractRecordSerializer getRecordSerializer();

    public ProtocolMessageType getContentMessageType() {
        return contentMessageType;
    }

    public void setContentMessageType(ProtocolMessageType contentMessageType) {
        this.contentMessageType = contentMessageType;
    }

    public ModifiableByteArray getCleanProtocolMessageBytes() {
        return cleanProtocolMessageBytes;
    }

    public void setCleanProtocolMessageBytes(byte[] cleanProtocolMessageBytes) {
        this.cleanProtocolMessageBytes = ModifiableVariableFactory.safelySetValue(this.cleanProtocolMessageBytes,
                cleanProtocolMessageBytes);
    }

    public void setCleanProtocolMessageBytes(ModifiableByteArray cleanProtocolMessageBytes) {
        this.cleanProtocolMessageBytes = cleanProtocolMessageBytes;
    }

    public ModifiableByteArray getProtocolMessageBytes() {
        return protocolMessageBytes;
    }

    public void setProtocolMessageBytes(ModifiableByteArray protocolMessageBytes) {
        this.protocolMessageBytes = protocolMessageBytes;
    }

    public void setProtocolMessageBytes(byte[] bytes) {
        this.protocolMessageBytes = ModifiableVariableFactory.safelySetValue(this.protocolMessageBytes, bytes);
    }

    public Integer getMaxRecordLengthConfig() {
        return maxRecordLengthConfig;
    }

    public void setMaxRecordLengthConfig(Integer maxRecordLengthConfig) {
        this.maxRecordLengthConfig = maxRecordLengthConfig;
    }

}
