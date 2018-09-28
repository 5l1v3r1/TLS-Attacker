/**
 * TLS-Attacker - A Modular Penetration Testing Framework for TLS
 *
 * Copyright 2014-2017 Ruhr University Bochum / Hackmanit GmbH
 *
 * Licensed under Apache License 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package de.rub.nds.tlsattacker.attacks;

import com.beust.jcommander.JCommander;
import de.rub.nds.tlsattacker.attacks.config.BleichenbacherCommandConfig;
import de.rub.nds.tlsattacker.attacks.config.Cve20162107CommandConfig;
import de.rub.nds.tlsattacker.attacks.config.DrownCommandConfig;
import de.rub.nds.tlsattacker.attacks.config.EarlyCCSCommandConfig;
import de.rub.nds.tlsattacker.attacks.config.HeartbleedCommandConfig;
import de.rub.nds.tlsattacker.attacks.config.InvalidCurveAttackConfig;
import de.rub.nds.tlsattacker.attacks.config.PaddingOracleCommandConfig;
import de.rub.nds.tlsattacker.attacks.config.PoodleCommandConfig;
import de.rub.nds.tlsattacker.attacks.config.PskBruteForcerAttackClientCommandConfig;
import de.rub.nds.tlsattacker.attacks.config.PskBruteForcerAttackServerCommandConfig;
import de.rub.nds.tlsattacker.attacks.config.SimpleMitmProxyCommandConfig;
import de.rub.nds.tlsattacker.attacks.config.TLSPoodleCommandConfig;
import de.rub.nds.tlsattacker.attacks.config.delegate.GeneralAttackDelegate;
import de.rub.nds.tlsattacker.attacks.impl.Attacker;
import de.rub.nds.tlsattacker.attacks.impl.BleichenbacherAttacker;
import de.rub.nds.tlsattacker.attacks.impl.Cve20162107Attacker;
import de.rub.nds.tlsattacker.attacks.impl.DrownAttacker;
import de.rub.nds.tlsattacker.attacks.impl.EarlyCCSAttacker;
import de.rub.nds.tlsattacker.attacks.impl.HeartbleedAttacker;
import de.rub.nds.tlsattacker.attacks.impl.InvalidCurveAttacker;
import de.rub.nds.tlsattacker.attacks.impl.PaddingOracleAttacker;
import de.rub.nds.tlsattacker.attacks.impl.PoodleAttacker;
import de.rub.nds.tlsattacker.attacks.impl.PskBruteForcerAttackClient;
import de.rub.nds.tlsattacker.attacks.impl.PskBruteForcerAttackServer;
import de.rub.nds.tlsattacker.attacks.impl.SimpleMitmProxy;
import de.rub.nds.tlsattacker.attacks.impl.TLSPoodleAttacker;
import de.rub.nds.tlsattacker.core.config.TLSDelegateConfig;
import de.rub.nds.tlsattacker.core.config.delegate.GeneralDelegate;
import de.rub.nds.tlsattacker.core.exceptions.ConfigurationException;
import static de.rub.nds.tlsattacker.util.ConsoleLogger.CONSOLE;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 */
public class Main {

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     *
     * @param args
     */
    public static void main(String[] args) {
        GeneralDelegate generalDelegate = new GeneralAttackDelegate();
        JCommander jc = new JCommander(generalDelegate);
        BleichenbacherCommandConfig bleichenbacherTest = new BleichenbacherCommandConfig(generalDelegate);
        jc.addCommand(BleichenbacherCommandConfig.ATTACK_COMMAND, bleichenbacherTest);

        PskBruteForcerAttackServerCommandConfig pskBruteForcerAttackServerTest = new PskBruteForcerAttackServerCommandConfig(
                generalDelegate);
        jc.addCommand(PskBruteForcerAttackServerCommandConfig.ATTACK_COMMAND, pskBruteForcerAttackServerTest);

        PskBruteForcerAttackClientCommandConfig pskBruteForcerAttackClientTest = new PskBruteForcerAttackClientCommandConfig(
                generalDelegate);
        jc.addCommand(PskBruteForcerAttackClientCommandConfig.ATTACK_COMMAND, pskBruteForcerAttackClientTest);
        InvalidCurveAttackConfig ellipticTest = new InvalidCurveAttackConfig(generalDelegate);
        jc.addCommand(InvalidCurveAttackConfig.ATTACK_COMMAND, ellipticTest);
        HeartbleedCommandConfig heartbleed = new HeartbleedCommandConfig(generalDelegate);
        jc.addCommand(HeartbleedCommandConfig.ATTACK_COMMAND, heartbleed);
        PaddingOracleCommandConfig paddingOracle = new PaddingOracleCommandConfig(generalDelegate);
        jc.addCommand(PaddingOracleCommandConfig.ATTACK_COMMAND, paddingOracle);
        TLSPoodleCommandConfig tlsPoodle = new TLSPoodleCommandConfig(generalDelegate);
        jc.addCommand(TLSPoodleCommandConfig.ATTACK_COMMAND, tlsPoodle);
        Cve20162107CommandConfig cve20162107 = new Cve20162107CommandConfig(generalDelegate);
        jc.addCommand(Cve20162107CommandConfig.ATTACK_COMMAND, cve20162107);
        EarlyCCSCommandConfig earlyCCS = new EarlyCCSCommandConfig(generalDelegate);
        jc.addCommand(EarlyCCSCommandConfig.ATTACK_COMMAND, earlyCCS);
        PoodleCommandConfig poodle = new PoodleCommandConfig(generalDelegate);
        jc.addCommand(PoodleCommandConfig.ATTACK_COMMAND, poodle);
        SimpleMitmProxyCommandConfig simpleMitmProxy = new SimpleMitmProxyCommandConfig(generalDelegate);
        jc.addCommand(SimpleMitmProxyCommandConfig.ATTACK_COMMAND, simpleMitmProxy);
        DrownCommandConfig drownConfig = new DrownCommandConfig(generalDelegate);
        jc.addCommand(DrownCommandConfig.COMMAND, drownConfig);
        jc.parse(args);
        if (generalDelegate.isHelp() || jc.getParsedCommand() == null) {
            if (jc.getParsedCommand() == null) {
                jc.usage();
            } else {
                jc.usage(jc.getParsedCommand());
            }
            return;
        }
        Attacker<? extends TLSDelegateConfig> attacker = null;
        switch (jc.getParsedCommand()) {
            case BleichenbacherCommandConfig.ATTACK_COMMAND:
                attacker = new BleichenbacherAttacker(bleichenbacherTest, bleichenbacherTest.createConfig());
                break;
            case InvalidCurveAttackConfig.ATTACK_COMMAND:
                attacker = new InvalidCurveAttacker(ellipticTest, ellipticTest.createConfig());
                break;
            case HeartbleedCommandConfig.ATTACK_COMMAND:
                attacker = new HeartbleedAttacker(heartbleed, heartbleed.createConfig());
                break;
            case TLSPoodleCommandConfig.ATTACK_COMMAND:
                attacker = new TLSPoodleAttacker(tlsPoodle, tlsPoodle.createConfig());
                break;
            case PaddingOracleCommandConfig.ATTACK_COMMAND:
                attacker = new PaddingOracleAttacker(paddingOracle, paddingOracle.createConfig());
                break;
            case Cve20162107CommandConfig.ATTACK_COMMAND:
                attacker = new Cve20162107Attacker(cve20162107, cve20162107.createConfig());
                break;
            case EarlyCCSCommandConfig.ATTACK_COMMAND:
                attacker = new EarlyCCSAttacker(earlyCCS, earlyCCS.createConfig());
                break;
            case PoodleCommandConfig.ATTACK_COMMAND:
                attacker = new PoodleAttacker(poodle, poodle.createConfig());
                break;
            case SimpleMitmProxyCommandConfig.ATTACK_COMMAND:
                attacker = new SimpleMitmProxy(simpleMitmProxy, simpleMitmProxy.createConfig());
                break;
            case PskBruteForcerAttackClientCommandConfig.ATTACK_COMMAND:
                attacker = new PskBruteForcerAttackClient(pskBruteForcerAttackClientTest,
                        pskBruteForcerAttackClientTest.createConfig());
                break;
            case PskBruteForcerAttackServerCommandConfig.ATTACK_COMMAND:
                attacker = new PskBruteForcerAttackServer(pskBruteForcerAttackServerTest,
                        pskBruteForcerAttackServerTest.createConfig());
                break;
            case DrownCommandConfig.COMMAND:
                attacker = new DrownAttacker(drownConfig, drownConfig.createConfig());
                break;
            default:
                throw new ConfigurationException("Command not found");
        }
        if (attacker == null) {
            throw new ConfigurationException("Attacker not found");
        }
        if (isPrintHelpForCommand(jc, attacker.getConfig())) {
            jc.usage(jc.getParsedCommand());
        } else {

            if (attacker.getConfig().isExecuteAttack()) {
                attacker.attack();
            } else {
                try {
                    Boolean result = attacker.checkVulnerability();
                    if (result == Boolean.TRUE) {
                        CONSOLE.error("Vulnerable:" + result.toString());
                    } else if (result == Boolean.FALSE) {
                        CONSOLE.info("Vulnerable:" + result.toString());
                    } else {
                        CONSOLE.warn("Vulnerable: Uncertain");
                    }
                } catch (UnsupportedOperationException E) {
                    LOGGER.info("The selected attacker is currently not implemented");
                }
            }
        }
    }

    /**
     *
     * @param jc
     * @param config
     * @return
     */
    public static boolean isPrintHelpForCommand(JCommander jc, TLSDelegateConfig config) {
        return config.getGeneralDelegate().isHelp();
    }
}
