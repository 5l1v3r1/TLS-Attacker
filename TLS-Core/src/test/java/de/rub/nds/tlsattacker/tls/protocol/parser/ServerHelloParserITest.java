/**
 * TLS-Attacker - A Modular Penetration Testing Framework for TLS
 *
 * Copyright 2014-2016 Ruhr University Bochum / Hackmanit GmbH
 *
 * Licensed under Apache License 2.0 http://www.apache.org/licenses/LICENSE-2.0
 */
package de.rub.nds.tlsattacker.tls.protocol.parser;

import de.rub.nds.tlsattacker.tests.IntegrationTest;
import de.rub.nds.tlsattacker.tls.exceptions.ParserException;
import de.rub.nds.tlsattacker.tls.protocol.handshake.ServerHelloMessage;
import java.util.Random;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.experimental.categories.Category;

/**
 *
 * @author Robert Merget - robert.merget@rub.de
 */
public class ServerHelloParserITest {
    
    private static final Logger LOGGER = LogManager.getLogger(ServerHelloParserITest.class);

    
    public ServerHelloParserITest() {
    }
    
    @Before
    public void setUp() {
    }
    
    /**
     * Try to parse alot of byte arrays into ServerHelloMessages and check that
     * nothing else but ParserExceptions are thrown
     */
    @Test
    @Category(IntegrationTest.class)
    public void testParser() {
        int counter = 0;
        Random r = new Random(0);
        for (int i = 0; i < 10000; i++) {

            try {
                int length = r.nextInt(10000);
                byte[] bytesToParse = new byte[length];
                r.nextBytes(bytesToParse);
                ServerHelloMessageParser parser = new ServerHelloMessageParser(r.nextInt(100), bytesToParse);
                ServerHelloMessage helloMessage = parser.parse();
                if (helloMessage != null) {
                    counter++;
                }
            } catch (ParserException E) {
            }
        }
        LOGGER.debug("Could parse " + counter + " random bytes into ServerHelloMessages");

    }

}