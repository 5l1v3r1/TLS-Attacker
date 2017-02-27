/**
 * TLS-Attacker - A Modular Penetration Testing Framework for TLS
 *
 * Copyright 2014-2016 Ruhr University Bochum / Hackmanit GmbH
 *
 * Licensed under Apache License 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package de.rub.nds.tlsattacker.tls.protocol.serializer;

import de.rub.nds.tlsattacker.tls.constants.CipherSuite;
import de.rub.nds.tlsattacker.tls.constants.CompressionMethod;
import de.rub.nds.tlsattacker.tls.constants.HandshakeMessageType;
import de.rub.nds.tlsattacker.tls.constants.ProtocolVersion;
import de.rub.nds.tlsattacker.tls.protocol.message.ServerHelloMessage;
import de.rub.nds.tlsattacker.util.ArrayConverter;
import java.util.Arrays;
import java.util.Collection;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 *
 * @author Robert Merget - robert.merget@rub.de
 */
@RunWith(Parameterized.class)
public class ServerHelloMessageSerializerTest {

    @Parameterized.Parameters
    public static Collection<Object[]> generateData() {
        return Arrays
                .asList(new Object[][] {
                        {
                                ArrayConverter
                                        .hexStringToByteArray("020000480303378f93cbcafda4c9ba43dafb49ab847ba1ae86a29d2679e7b9aac8e25c207e01200919fe8a189912807ee0621a45f4e6440a297f13574d2229fdbc96427b0e2d10002f000000"),
                                HandshakeMessageType.SERVER_HELLO.getValue(),
                                72,
                                ProtocolVersion.TLS12.getValue(),
                                new byte[] { (byte) 0x37, (byte) 0x8f, (byte) 0x93, (byte) 0xcb },
                                ArrayConverter
                                        .hexStringToByteArray("cafda4c9ba43dafb49ab847ba1ae86a29d2679e7b9aac8e25c207e01"),
                                32,
                                ArrayConverter
                                        .hexStringToByteArray("0919fe8a189912807ee0621a45f4e6440a297f13574d2229fdbc96427b0e2d10"),
                                CipherSuite.TLS_RSA_WITH_AES_128_CBC_SHA.getByteValue(),
                                CompressionMethod.NULL.getValue(), 0 },
                        {
                                ArrayConverter
                                        .hexStringToByteArray("020000460303378f93cbcafda4c9ba43dafb49ab847ba1ae86a29d2679e7b9aac8e25c207e01200919fe8a189912807ee0621a45f4e6440a297f13574d2229fdbc96427b0e2d10002f00"),
                                HandshakeMessageType.SERVER_HELLO.getValue(),
                                70,
                                ProtocolVersion.TLS12.getValue(),
                                new byte[] { (byte) 0x37, (byte) 0x8f, (byte) 0x93, (byte) 0xcb },
                                ArrayConverter
                                        .hexStringToByteArray("cafda4c9ba43dafb49ab847ba1ae86a29d2679e7b9aac8e25c207e01"),
                                32,
                                ArrayConverter
                                        .hexStringToByteArray("0919fe8a189912807ee0621a45f4e6440a297f13574d2229fdbc96427b0e2d10"),
                                CipherSuite.TLS_RSA_WITH_AES_128_CBC_SHA.getByteValue(),
                                CompressionMethod.NULL.getValue(), null } });
    }

    private ServerHelloMessage helloMessage;
    private byte[] message;

    public ServerHelloMessageSerializerTest(byte[] message, byte messageType, int messageLength,
            byte[] protocolVersion, byte[] unixTime, byte[] random, int sessionIdLength, byte[] sessionID,
            byte[] selectedCiphersuite, byte selectedCompression, Integer extensionLength) {
        this.message = message;
        helloMessage = new ServerHelloMessage();
        helloMessage.setType(messageType);
        helloMessage.setLength(messageLength);
        helloMessage.setProtocolVersion(protocolVersion);
        helloMessage.setUnixTime(unixTime);
        helloMessage.setRandom(random);
        helloMessage.setSessionIdLength(sessionIdLength);
        helloMessage.setSessionId(sessionID);
        helloMessage.setSelectedCipherSuite(selectedCiphersuite);
        helloMessage.setSelectedCompressionMethod(selectedCompression);
        if (extensionLength != null) {
            helloMessage.setExtensionsLength(extensionLength);
        }
    }

    @Before
    public void setUp() {
    }

    /**
     * Test of parse method, of class ServerHelloMessageParser.
     */
    @Test
    public void serialize() {
        ServerHelloMessageSerializer serializer = new ServerHelloMessageSerializer(helloMessage);
        byte[] serialised = serializer.serialize();
        assertArrayEquals(serialised, message);
    }

}