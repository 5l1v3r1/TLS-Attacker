/**
 * TLS-Attacker - A Modular Penetration Testing Framework for TLS
 *
 * Copyright 2014-2017 Ruhr University Bochum / Hackmanit GmbH
 *
 * Licensed under Apache License 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package de.rub.nds.tlsattacker.core.config;

import de.rub.nds.modifiablevariable.util.ArrayConverter;
import de.rub.nds.modifiablevariable.util.ByteArrayAdapter;
import de.rub.nds.tlsattacker.core.connection.InboundConnection;
import de.rub.nds.tlsattacker.core.connection.OutboundConnection;
import de.rub.nds.tlsattacker.core.constants.AuthzDataFormat;
import de.rub.nds.tlsattacker.core.constants.CertificateStatusRequestType;
import de.rub.nds.tlsattacker.core.constants.CertificateType;
import de.rub.nds.tlsattacker.core.constants.ChooserType;
import de.rub.nds.tlsattacker.core.constants.CipherSuite;
import de.rub.nds.tlsattacker.core.constants.ClientAuthenticationType;
import de.rub.nds.tlsattacker.core.constants.ClientCertificateType;
import de.rub.nds.tlsattacker.core.constants.CompressionMethod;
import de.rub.nds.tlsattacker.core.constants.ECPointFormat;
import de.rub.nds.tlsattacker.core.constants.HashAlgorithm;
import de.rub.nds.tlsattacker.core.constants.HeartbeatMode;
import de.rub.nds.tlsattacker.core.constants.MaxFragmentLength;
import de.rub.nds.tlsattacker.core.constants.NameType;
import de.rub.nds.tlsattacker.core.constants.NamedCurve;
import de.rub.nds.tlsattacker.core.constants.PRFAlgorithm;
import de.rub.nds.tlsattacker.core.constants.ProtocolVersion;
import de.rub.nds.tlsattacker.core.constants.RunningModeType;
import de.rub.nds.tlsattacker.core.constants.PskKeyExchangeMode;
import de.rub.nds.tlsattacker.core.constants.SignatureAlgorithm;
import de.rub.nds.tlsattacker.core.constants.SignatureAndHashAlgorithm;
import de.rub.nds.tlsattacker.core.constants.SrtpProtectionProfiles;
import de.rub.nds.tlsattacker.core.constants.TokenBindingKeyParameters;
import de.rub.nds.tlsattacker.core.constants.TokenBindingType;
import de.rub.nds.tlsattacker.core.constants.TokenBindingVersion;
import de.rub.nds.tlsattacker.core.constants.UserMappingExtensionHintType;
import de.rub.nds.tlsattacker.core.crypto.ec.CustomECPoint;
import de.rub.nds.tlsattacker.core.protocol.message.extension.KS.KSEntry;
import de.rub.nds.tlsattacker.core.protocol.message.extension.PSK.PskSet;
import de.rub.nds.tlsattacker.core.protocol.message.extension.SNI.SNIEntry;
import de.rub.nds.tlsattacker.core.protocol.message.extension.cachedinfo.CachedObject;
import de.rub.nds.tlsattacker.core.protocol.message.extension.certificatestatusrequestitemv2.RequestItemV2;
import de.rub.nds.tlsattacker.core.protocol.message.extension.trustedauthority.TrustedAuthority;
import de.rub.nds.tlsattacker.core.record.layer.RecordLayerType;
import de.rub.nds.tlsattacker.core.workflow.action.executor.WorkflowExecutorType;
import de.rub.nds.tlsattacker.core.workflow.factory.WorkflowTraceType;
import de.rub.nds.tlsattacker.core.workflow.filter.FilterType;
import java.io.*;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class Config implements Serializable {

    protected static final Logger LOGGER = LogManager.getLogger(Config.class);

    /**
     * The default Config file to load.
     */
    private static final String DEFAULT_CONFIG_FILE = "/default_config.xml";

    public static Config createConfig() {
        InputStream stream = Config.class.getResourceAsStream(DEFAULT_CONFIG_FILE);
        return ConfigIO.read(stream);

    }

    public static Config createConfig(File f) {
        return ConfigIO.read(f);
    }

    public static Config createConfig(InputStream stream) {
        return ConfigIO.read(stream);
    }

    /**
     * List of filters to apply on workflow traces before serialization.
     */
    private List<FilterType> outputFilters;

    /**
     * Whether filters return a copy of the input workflow trace or overwrite it
     * in place. While copying would be preferred in general, overwriting might
     * be desired in some scenarios for better performance.
     */
    private Boolean applyFiltersInPlace = true;

    /**
     * Whether to keep explicit user settings in the workflow trace when
     * applying filters or not. Filters might override explicit user definitions
     * in the filtered workflow trace. For example, the DefaultFilter removes
     * explicitly overwritten default connections. If this flag is true, the
     * user defined connections would be restored afterwards.
     */
    private Boolean filtersKeepUserSettings = true;

    /**
     * Default value for ProtocolVerionFields
     */
    private ProtocolVersion highestProtocolVersion = ProtocolVersion.TLS12;

    /**
     * The default connection parameters to use when running TLS-Client.
     */
    private OutboundConnection defaultClientConnection;

    /**
     * The default connection parameters to use when running TLS-Server.
     */
    private InboundConnection defaultServerConnection;

    private RunningModeType defaultRunningMode = RunningModeType.CLIENT;

    /**
     * If default generated WorkflowTraces should contain client Authentication
     */
    private Boolean clientAuthentication = false;

    /**
     * Which Signature and Hash algorithms we support
     */
    private List<SignatureAndHashAlgorithm> supportedSignatureAndHashAlgorithms;

    /**
     * Which Ciphersuites we support by default
     */
    private List<CipherSuite> defaultClientSupportedCiphersuites;

    /**
     * Which Ciphersuites we support by default
     */
    private List<CipherSuite> defaultServerSupportedCiphersuites;

    /**
     * If we are a dynamic workflow //TODO implement
     */
    private Boolean dynamicWorkflow = false;

    /**
     * Supported namedCurves by default
     */
    private List<NamedCurve> namedCurves;

    /**
     * Default clientSupportedNamed Curves
     */
    private List<NamedCurve> defaultClientNamedCurves;

    /**
     * Supported ProtocolVersions by default
     */
    private List<ProtocolVersion> supportedVersions;

    /**
     * Which heartBeat mode we are in
     */
    private HeartbeatMode heartbeatMode = HeartbeatMode.PEER_ALLOWED_TO_SEND;

    /**
     * Padding length for TLS 1.3 messages
     */
    private Integer paddingLength = 0;

    /**
     * Public key for KeyShareExtension
     */
    @XmlJavaTypeAdapter(ByteArrayAdapter.class)
    private byte[] keySharePublic = ArrayConverter
            .hexStringToByteArray("2a981db6cdd02a06c1763102c9e741365ac4e6f72b3176a6bd6a3523d3ec0f4c");

    /**
     * Key type for KeyShareExtension
     */
    private NamedCurve keyShareType = NamedCurve.ECDH_X25519;

    /**
     * Private key for KeyShareExtension
     */
    @XmlJavaTypeAdapter(ByteArrayAdapter.class)
    private byte[] keySharePrivate = ArrayConverter
            .hexStringToByteArray("03bd8bca70c19f657e897e366dbe21a466e4924af6082dbdf573827bcdde5def");

    /**
     * Hostname in SNI Extension
     */
    private String sniHostname = "localhost";

    /**
     * SNI HostnameType
     */
    private NameType sniType = NameType.HOST_NAME;

    /**
     * Should we terminate the connection on a wrong SNI ?
     */
    private Boolean sniHostnameFatal = false;
    
    /**
     * If set to true retransmits wont appear in the workflowtrace
     */
    private Boolean ignoreDtlsRetransmits = true;

    /**
     * MaxFragmentLength in MaxFragmentLengthExtension
     */
    private MaxFragmentLength maxFragmentLength = MaxFragmentLength.TWO_9;

    /**
     * SessionTLSTicket for the SessionTLSTicketExtension. It's an empty session
     * ticket since we initiate a new connection.
     */
    @XmlJavaTypeAdapter(ByteArrayAdapter.class)
    private byte[] tlsSessionTicket = new byte[0];

    /**
     * Renegotiation info for the RenegotiationInfo extension for the Client.
     * It's an empty info since we initiate a new connection.
     */
    @XmlJavaTypeAdapter(ByteArrayAdapter.class)
    private byte[] defaultClientRenegotiationInfo = new byte[0];

    /**
     * Renegotiation info for the RenegotiationInfo extension for the Client.
     * It's an empty info since we initiate a new connection.
     */
    @XmlJavaTypeAdapter(ByteArrayAdapter.class)
    private byte[] defaultServerRenegotiationInfo = new byte[0];

    /**
     * SignedCertificateTimestamp for the SignedCertificateTimestampExtension.
     * It's an emty timestamp, since the server sends it.
     */
    @XmlJavaTypeAdapter(ByteArrayAdapter.class)
    private byte[] defaultSignedCertificateTimestamp = new byte[0];

    /**
     * TokenBinding default version. To be defined later.
     */
    private TokenBindingVersion defaultTokenBindingVersion = TokenBindingVersion.DRAFT_13;

    /**
     * Default TokenBinding Key Parameters.
     */
    private List<TokenBindingKeyParameters> defaultTokenBindingKeyParameters;

    /**
     * This is the request type of the CertificateStatusRequest extension
     */
    private CertificateStatusRequestType certificateStatusRequestExtensionRequestType = CertificateStatusRequestType.OCSP;

    /**
     * This is the responder ID list of the CertificateStatusRequest extension
     */
    @XmlJavaTypeAdapter(ByteArrayAdapter.class)
    private byte[] certificateStatusRequestExtensionResponderIDList = new byte[0];

    /**
     * This is the request extension of the CertificateStatusRequest extension
     */
    @XmlJavaTypeAdapter(ByteArrayAdapter.class)
    private byte[] certificateStatusRequestExtensionRequestExtension = new byte[0];

    /**
     * Default ALPN announced protocols
     */
    private String[] alpnAnnouncedProtocols = new String[] { "h2" };

    @XmlJavaTypeAdapter(ByteArrayAdapter.class)
    private byte[] sessionId = new byte[0];

    /**
     * Default SRP Identifier
     */
    @XmlJavaTypeAdapter(ByteArrayAdapter.class)
    private byte[] secureRemotePasswordExtensionIdentifier = "UserName".getBytes(Charset.forName("UTF-8"));

    /**
     * Default SRTP extension protection profiles The list contains every
     * protection profile as in RFC 5764
     */
    private List<SrtpProtectionProfiles> secureRealTimeTransportProtocolProtectionProfiles;

    /**
     * Default SRTP extension master key identifier
     */
    @XmlJavaTypeAdapter(ByteArrayAdapter.class)
    private byte[] secureRealTimeTransportProtocolMasterKeyIdentifier = new byte[] {};

    /**
     * Default user mapping extension hint type
     */
    private UserMappingExtensionHintType userMappingExtensionHintType = UserMappingExtensionHintType.UPN_DOMAIN_HINT;

    /**
     * Default certificate type extension desired types
     */
    private List<CertificateType> certificateTypeDesiredTypes;

    /**
     * Default client certificate type extension desired types
     */
    private List<CertificateType> clientCertificateTypeDesiredTypes;

    /**
     * Default server certificate type extension desired types
     */
    private List<CertificateType> serverCertificateTypeDesiredTypes;

    /**
     * Default client authz extension data format list
     */
    private List<AuthzDataFormat> clientAuthzExtensionDataFormat;

    /**
     * Default state for the certificate type extension message. State "client"
     */
    private Boolean certificateTypeExtensionMessageState = true;

    /**
     * Default sever authz extension data format list.
     */
    private List<AuthzDataFormat> serverAuthzExtensionDataFormat;

    /**
     * Default trusted ca indication extension trusted CAs.
     */
    private List<TrustedAuthority> trustedCaIndicationExtensionAuthorties;

    /**
     * Default state for the client certificate type extension message (state
     * "client").
     */
    private Boolean clientCertificateTypeExtensionMessageState = true;

    /**
     * Default state for the cached info extension message (state "client").
     */
    private Boolean cachedInfoExtensionIsClientState = true;

    /**
     * Default cached objects for the cached info extension.
     */
    private List<CachedObject> cachedObjectList;

    /**
     * Default certificate status request v2 extension request list.
     */
    private List<RequestItemV2> statusRequestV2RequestList;

    /**
     * If we should use a workflow trace specified in File
     */
    private String workflowInput = null;

    /**
     * If set, save the workflow trace to this file after trace execution.
     */
    private String workflowOutput = null;

    /**
     * If set, save the actually used config to this file after trace execution.
     */
    private String configOutput = null;

    /**
     * The Type of workflow trace that should be generated
     */
    private WorkflowTraceType workflowTraceType = null;

    /**
     * If the Default generated workflowtrace should contain Application data
     * send by servers
     */
    private Boolean serverSendsApplicationData = false;

    /**
     * If we generate ClientHello with the ECPointFormat extension
     */
    private Boolean addECPointFormatExtension = true;

    /**
     * If we generate ClientHello with the EllipticCurve extension
     */
    private Boolean addEllipticCurveExtension = true;

    /**
     * If we generate ClientHello with the Heartbeat extension
     */
    private Boolean addHeartbeatExtension = false;

    /**
     * If we generate ClientHello with the MaxFragmentLength extension
     */
    private Boolean addMaxFragmentLengthExtenstion = false;

    /**
     * If we generate ClientHello with the ServerNameIndication extension
     */
    private Boolean addServerNameIndicationExtension = false;

    /**
     * If we generate ClientHello with the SignatureAndHashAlgorithm extension
     */
    private Boolean addSignatureAndHashAlgrorithmsExtension = false;

    /**
     * If we generate ClientHello with the SupportedVersion extension
     */
    private Boolean addSupportedVersionsExtension = false;

    /**
     * If we generate ClientHello with the KeyShare extension
     */
    private Boolean addKeyShareExtension = false;
    /**
     * If we generate ClientHello with the EarlyData extension
     */
    private Boolean addEarlyDataExtension = false;

    /**
     * If we generate ClientHello with the PSKKeyExchangeModes extension
     */
    private Boolean addPSKKeyExchangeModesExtension = false;

    /**
     * If we generate ClientHello with the PreSharedKey extension
     */
    private Boolean addPreSharedKeyExtension = false;
    /**
     * If we generate ClientHello with the Padding extension
     */
    private Boolean addPaddingExtension = false;

    /**
     * If we generate ClientHello with the ExtendedMasterSecret extension
     */
    private Boolean addExtendedMasterSecretExtension = false;

    /**
     * If we generate ClientHello with the SessionTicketTLS extension
     */
    private Boolean addSessionTicketTLSExtension = false;

    /**
     * If we generate ClientHello with SignedCertificateTimestamp extension
     */
    private Boolean addSignedCertificateTimestampExtension = false;

    /**
     * If we generate ClientHello with RenegotiationInfo extension
     */
    private Boolean addRenegotiationInfoExtension = true;

    /**
     * If we generate ClientHello with TokenBinding extension.
     */
    private Boolean addTokenBindingExtension = false;

    /**
     * Whether HTTPS request should contain a cookie header field or not.
     */
    private Boolean addHttpsCookie = false;

    /**
     * Default cookie value to use if addHttpsCookie is true.
     */
    private String defaultHttpsCookieName = "tls-attacker";

    /**
     * Default cookie value to use if addHttpsCookie is true.
     */
    private String defaultHttpsCookieValue = "42130912812";

    /**
     * If we generate ClientHello with CertificateStatusRequest extension
     */
    private Boolean addCertificateStatusRequestExtension = false;

    /**
     * If we generate ClientHello with ALPN extension
     */
    private Boolean addAlpnExtension = false;

    /**
     * If we generate ClientHello with SRP extension
     */
    private Boolean addSRPExtension = false;

    /**
     * If we generate ClientHello with SRTP extension
     */
    private Boolean addSRTPExtension = false;

    /**
     * If we generate ClientHello with truncated hmac extension
     */
    private Boolean addTruncatedHmacExtension = false;

    /**
     * If we generate ClientHello with user mapping extension
     */
    private Boolean addUserMappingExtension = false;

    /**
     * If we generate ClientHello with certificate type extension
     */
    private Boolean addCertificateTypeExtension = false;

    /**
     * If we generate ClientHello with client authz extension
     */
    private Boolean addClientAuthzExtension = false;

    /**
     * If we generate ClientHello with server authz extension
     */
    private Boolean addServerAuthzExtension = false;

    /**
     * If we generate ClientHello with client certificate type extension
     */
    private Boolean addClientCertificateTypeExtension = false;

    /**
     * If we generate ClientHello with server certificate type extension
     */
    private Boolean addServerCertificateTypeExtension = false;

    /**
     * If we generate ClientHello with encrypt then mac extension
     */
    private Boolean addEncryptThenMacExtension = false;

    /**
     * If we generate ClientHello with cached info extension
     */
    private Boolean addCachedInfoExtension = false;

    /**
     * If we generate ClientHello with client certificate url extension
     */
    private Boolean addClientCertificateUrlExtension = false;

    /**
     * If we generate ClientHello with trusted ca indication extension
     */
    private Boolean addTrustedCaIndicationExtension = false;

    /**
     * If we generate ClientHello with status request v2 extension
     */
    private Boolean addCertificateStatusRequestV2Extension = false;

    /**
     * If set to true, timestamps will be updated upon execution of a
     * workflowTrace
     */
    private Boolean updateTimestamps = true;

    /**
     * PSKKeyExchangeModes to be used in 0-RTT (or TLS 1.3 resumption)
     */
    List<PskKeyExchangeMode> pskKeyExchangeModes;

    /**
     * The PSK to use.
     */
    @XmlJavaTypeAdapter(ByteArrayAdapter.class)
    private byte[] psk = new byte[0];

    /**
     * The client's early traffic secret.
     */
    @XmlJavaTypeAdapter(ByteArrayAdapter.class)
    private byte[] clientEarlyTrafficSecret = new byte[128];

    /**
     * The early secret of the session.
     */
    @XmlJavaTypeAdapter(ByteArrayAdapter.class)
    private byte[] earlySecret = new byte[256];

    /**
     * The cipher suite used for early data.
     */
    private CipherSuite earlyDataCipherSuite = CipherSuite.TLS_AES_128_GCM_SHA256;

    /**
     * The psk used for early data (!= earlySecret or earlyTrafficSecret).
     */
    @XmlJavaTypeAdapter(ByteArrayAdapter.class)
    private byte[] earlyDataPsk = new byte[256];

    /**
     * Contains all values related to TLS 1.3 PSKs.
     */
    private List<PskSet> PskSets = new LinkedList<>();

    /**
     * Do we use a psk for our secrets?
     */
    private Boolean usePsk = false;

    /**
     * Early data to be sent.
     */
    @XmlJavaTypeAdapter(ByteArrayAdapter.class)
    private byte[] earlyData = ArrayConverter.hexStringToByteArray("544c532d41747461636b65720a");

    /**
     * The Certificate we initialize CertificateMessages with
     */
    @XmlJavaTypeAdapter(ByteArrayAdapter.class)
    private byte[] defaultRsaCertificate = ArrayConverter
            .hexStringToByteArray("0003970003943082039030820278A003020102020900A650C00794049FCD300D06092A864886F70D01010B0500305C310B30090603550406130241553113301106035504080C0A536F6D652D53746174653121301F060355040A0C18496E7465726E6574205769646769747320507479204C74643115301306035504030C0C544C532D41747461636B65723020170D3137303731333132353331385A180F32313137303631393132353331385A305C310B30090603550406130241553113301106035504080C0A536F6D652D53746174653121301F060355040A0C18496E7465726E6574205769646769747320507479204C74643115301306035504030C0C544C532D41747461636B657230820122300D06092A864886F70D01010105000382010F003082010A0282010100C8820D6C3CE84C8430F6835ABFC7D7A912E1664F44578751F376501A8C68476C3072D919C5D39BD0DBE080E71DB83BD4AB2F2F9BDE3DFFB0080F510A5F6929C196551F2B3C369BE051054C877573195558FD282035934DC86EDAB8D4B1B7F555E5B2FEE7275384A756EF86CB86793B5D1333F0973203CB96966766E655CD2CCCAE1940E4494B8E9FB5279593B75AFD0B378243E51A88F6EB88DEF522A8CD5C6C082286A04269A2879760FCBA45005D7F2672DD228809D47274F0FE0EA5531C2BD95366C05BF69EDC0F3C3189866EDCA0C57ADCCA93250AE78D9EACA0393A95FF9952FC47FB7679DD3803E6A7A6FA771861E3D99E4B551A4084668B111B7EEF7D0203010001A3533051301D0603551D0E04160414E7A92FE5543AEE2FF7592F800AC6E66541E3268B301F0603551D23041830168014E7A92FE5543AEE2FF7592F800AC6E66541E3268B300F0603551D130101FF040530030101FF300D06092A864886F70D01010B050003820101000D5C11E28CF19D1BC17E4FF543695168570AA7DB85B3ECB85405392A0EDAFE4F097EE4685B7285E3D9B869D23257161CA65E20B5E6A585D33DA5CD653AF81243318132C9F64A476EC08BA80486B3E439F765635A7EA8A969B3ABD8650036D74C5FC4A04589E9AC8DC3BE2708743A6CFE3B451E3740F735F156D6DC7FFC8A2C852CD4E397B942461C2FCA884C7AFB7EBEF7918D6AAEF1F0D257E959754C4665779FA0E3253EF2BEDBBD5BE5DA600A0A68E51D2D1C125C4E198669A6BC715E8F3884E9C3EFF39D40838ADA4B1F38313F6286AA395DC6DEA9DAF49396CF12EC47EFA7A0D3882F8B84D9AEEFFB252C6B81A566609605FBFD3F0D17E5B12401492A1A");

    @XmlJavaTypeAdapter(ByteArrayAdapter.class)
    private byte[] defaultDsaCertificate = ArrayConverter
            .hexStringToByteArray("0003540003513082034D3082030AA0030201020209008371F01046D40E48300B0609608648016503040302305C310B30090603550406130244453113301106035504080C0A536F6D652D53746174653121301F060355040A0C18496E7465726E6574205769646769747320507479204C74643115301306035504030C0C544C532D41747461636B65723020170D3137303731333132303831375A180F32313137303631393132303831375A305C310B30090603550406130244453113301106035504080C0A536F6D652D53746174653121301F060355040A0C18496E7465726E6574205769646769747320507479204C74643115301306035504030C0C544C532D41747461636B6572308201B63082012B06072A8648CE3804013082011E02818100A6B0EAF2CCE3B4370D66CD94AA68E425DF90B68936924D7A2B19173D5FDDC3A9E569E914CB5C028E6DD31DE7127CE1452708E78A8883FA86659F0E4773DDCB6D529206CAB19C1F66FB9D3A11E336A8AA28A24B2D64B0E5096E5860C2D5F889958133A149A8256ADC7A2EF7F61F545B04352834C0EE455D256AA6FB888CB87FD5021500FF03353AB857DDA61F2823EE734253E8D4D35C3D028180170B66A05C3644899197FE9E3FF26116B907B3E8E90FA3CFE64D2E7EB43D219CEE46EF342E0C03461176FAF144D609B95201FEEF462027B932815375B511ABF8E0048886D9E20FADC5D8EF9AB5CAEFCB3FF667CA953A53F82E0FF301D923CAC922EE3735B231D40177EC9AD827998018C9039BE63B067E9AF06C9B7D5011CA82038184000281804A3726DCC3299945FCF932C12701101C948926560F3E33B8C6708908B5A88C0BDDDBA2F24EC672BA61F6F49680FB900F99F01C3A08E00D48F85FC239CF14F6EEE3FDB0DB6C88BC89B98FC122793AF8F1D9265870C00EEF42D1EE1ACB5FB3874A6CAFF4E44F822E2EB365461C0AF384B9925FFB561453C5BE5554C86F20CEC0DCA3533051301D0603551D0E041604149B1C1B884AE8690571A0FABC67B445E77779EC0D301F0603551D230418301680149B1C1B884AE8690571A0FABC67B445E77779EC0D300F0603551D130101FF040530030101FF300B0609608648016503040302033000302D021412B619CE0DCCAEF09F8BB0ACBFD146300C0C1B00021500BDE6CB6CF90058B533D050542E24BA1F64860226");

    @XmlJavaTypeAdapter(ByteArrayAdapter.class)
    private byte[] defaultEcCertificate = ArrayConverter
            .hexStringToByteArray("0001BD0001BA308201B63082016CA003020102020900B9FB5B9B7B19C211300A06082A8648CE3D0403023045310B30090603550406130244453113301106035504080C0A536F6D652D53746174653121301F060355040A0C18496E7465726E6574205769646769747320507479204C74643020170D3137303731333132353530375A180F32313137303631393132353530375A3045310B30090603550406130244453113301106035504080C0A536F6D652D53746174653121301F060355040A0C18496E7465726E6574205769646769747320507479204C74643049301306072A8648CE3D020106082A8648CE3D03010103320004DF647234F375CB38137C6775B04A40950C932E180620717F802B21FE868479987D990383D908E19B683F412ECDF397E1A3533051301D0603551D0E04160414ACF90511E691018C1B69177AF743321486EE09D5301F0603551D23041830168014ACF90511E691018C1B69177AF743321486EE09D5300F0603551D130101FF040530030101FF300A06082A8648CE3D04030203380030350219009E8F2E5C4D6C4179B60E12B46B7AD19F7AF39F11731A359702180CDC387E4A12F6BBEE702A05B548C5F5FC2DE3842B6366A0");

    @XmlJavaTypeAdapter(ByteArrayAdapter.class)
    private byte[] distinguishedNames = new byte[0];

    private Boolean enforceSettings = false;

    /**
     * Stop as soon as all expected messages are received and dont wait for more
     */
    private Boolean earlyStop = false;

    private Boolean doDTLSRetransmits = false;

    private BigInteger defaultServerDhGenerator = new BigInteger("2");

    private BigInteger defaultServerDhModulus = new BigInteger(
            "15458150092069033378601573800816703249401189342134115050806105600042321586262936062413786779796157671421516779431947968642017250021834283152850968840396649272235097918348324");

    private BigInteger defaultClientDhGenerator = new BigInteger("2");

    private BigInteger defaultClientDhModulus = new BigInteger(
            "15458150092069033378601573800816703249401189342134115050806105600042321586262936062413786779796157671421516779431947968642017250021834283152850968840396649272235097918348324");

    private BigInteger defaultServerDhPrivateKey = new BigInteger(
            "1234567891234567889123546712839632542648746452354265471");

    private BigInteger defaultClientDhPrivateKey = new BigInteger(
            "1234567891234567889123546712839632542648746452354265471");

    private BigInteger defaultServerDhPublicKey = new BigInteger(
            "14480301636124364131011109953533209419584138262785800536726427889263750026424833537662211230987987661789535497502943331312908532241011314347509704298395798883527739408059572");

    private BigInteger defaultClientDhPublicKey = new BigInteger(
            "14480301636124364131011109953533209419584138262785800536726427889263750026424833537662211230987987661789535497502943331312908532241011314347509704298395798883527739408059572");

    private BigInteger defaultServerDsaPrivateKey;

    private String defaultApplicationMessageData = "Test";

    /**
     * If this is set TLS-Attacker only waits for the expected messages in the
     * ReceiveActions This is interesting for DTLS since this prevents the
     * server from retransmitting
     */
    private Boolean waitOnlyForExpectedDTLS = true;

    private List<ClientCertificateType> clientCertificateTypes;

    /**
     * max payload length used in our application (not set by the spec)
     */
    private Integer heartbeatPayloadLength = 256;

    private Integer heartbeatPaddingLength = 256;

    /**
     * How long should our DTLSCookies be by default
     */
    private Integer defaultDTLSCookieLength = 6;

    /**
     * How much data we should put into a record by default
     */
    private Integer defaultMaxRecordData = 1048576;

    /**
     * How much padding bytes should be send by default
     */
    @XmlJavaTypeAdapter(ByteArrayAdapter.class)
    private byte[] defaultPaddingExtensionBytes = new byte[] { 0, 0, 0, 0, 0, 0 };

    private WorkflowExecutorType workflowExecutorType = WorkflowExecutorType.DEFAULT;

    /**
     * Does not mix messages with different message types in a single record
     */
    private Boolean flushOnMessageTypeChange = true;

    /**
     * If there is not enough space in the defined records, new records are
     * dynamically added if not set, protocolmessage bytes that wont fit are
     * discarded
     */
    private Boolean createRecordsDynamically = true;
    /**
     * When "Null" records are defined to be send, every message will be sent in
     * atleast one individual record
     */
    private Boolean createIndividualRecords = true;

    /**
     * Which recordLayer should be used
     */
    private RecordLayerType recordLayerType = RecordLayerType.RECORD;

    /**
     * If this value is set the default workflowExecutor will remove all runtime
     * values from the workflow trace and will only keep the relevant
     * information
     */
    private Boolean resetWorkflowtracesBeforeSaving = false;

    /**
     * TLS-Attacker will not try to receive additional messages after the
     * configured number of messages has been received
     */
    private Boolean quickReceive = true;

    /**
     * If the WorkflowExecutor should take care of the connection opening
     */
    private Boolean workflowExecutorShouldOpen = true;

    /**
     * If the WorkflowExecutor should take care of the connection closing
     */
    private Boolean workflowExecutorShouldClose = true;

    private Boolean stopRecievingAfterFatal = false;

    private Boolean stopActionsAfterFatal = false;
    /**
     * This CipherSuite will be used if no cipherSuite has been negotiated yet
     */
    private CipherSuite defaultSelectedCipherSuite = CipherSuite.TLS_RSA_WITH_AES_128_CBC_SHA;

    private List<ECPointFormat> defaultServerSupportedPointFormats;

    private List<ECPointFormat> defaultClientSupportedPointFormats;

    private List<SignatureAndHashAlgorithm> defaultClientSupportedSignatureAndHashAlgorithms;

    private List<SignatureAndHashAlgorithm> defaultServerSupportedSignatureAndHashAlgorithms;

    private SignatureAndHashAlgorithm defaultSelectedSignatureAndHashAlgorithm = new SignatureAndHashAlgorithm(
            SignatureAlgorithm.RSA, HashAlgorithm.SHA1);

    private List<SNIEntry> defaultClientSNIEntryList;

    private ProtocolVersion defaultLastRecordProtocolVersion = ProtocolVersion.TLS10;

    private ProtocolVersion defaultSelectedProtocolVersion = ProtocolVersion.TLS12;

    private ProtocolVersion defaultHighestClientProtocolVersion = ProtocolVersion.TLS12;

    private MaxFragmentLength defaultMaxFragmentLength = MaxFragmentLength.TWO_12;

    private HeartbeatMode defaultHeartbeatMode = HeartbeatMode.PEER_ALLOWED_TO_SEND;

    private List<CompressionMethod> defaultClientSupportedCompressionMethods;

    private List<CompressionMethod> defaultServerSupportedCompressionMethods;

    @XmlJavaTypeAdapter(ByteArrayAdapter.class)
    private byte[] defaultMasterSecret = new byte[0];

    @XmlJavaTypeAdapter(ByteArrayAdapter.class)
    private byte[] defaultPreMasterSecret = new byte[0];

    @XmlJavaTypeAdapter(ByteArrayAdapter.class)
    private byte[] defaultClientRandom = new byte[0];

    @XmlJavaTypeAdapter(ByteArrayAdapter.class)
    private byte[] defaultServerRandom = new byte[0];

    @XmlJavaTypeAdapter(ByteArrayAdapter.class)
    private byte[] defaultClientSessionId = new byte[0];

    @XmlJavaTypeAdapter(ByteArrayAdapter.class)
    private byte[] defaultServerSessionId = new byte[0];

    private CompressionMethod defaultSelectedCompressionMethod = CompressionMethod.NULL;

    @XmlJavaTypeAdapter(ByteArrayAdapter.class)
    private byte[] defaultDtlsCookie = new byte[0];

    @XmlJavaTypeAdapter(ByteArrayAdapter.class)
    private byte[] defaultCertificateRequestContext = new byte[0];

    private PRFAlgorithm defaultPRFAlgorithm = PRFAlgorithm.TLS_PRF_LEGACY;

    private Byte defaultAlertDescription = 0;

    private Byte defaultAlertLevel = 0;

    private NamedCurve defaultSelectedCurve = NamedCurve.SECP192R1;

    private NamedCurve defaultEcCertificateCurve = NamedCurve.SECP192R1;

    private CustomECPoint defaultClientEcPublicKey;

    private CustomECPoint defaultServerEcPublicKey;

    private BigInteger defaultServerEcPrivateKey = new BigInteger(
            "191991257030464195512760799659436374116556484140110877679395918219072292938297573720808302564562486757422301181089761");

    private BigInteger defaultClientEcPrivateKey = new BigInteger(
            "191991257030464195512760799659436374116556484140110877679395918219072292938297573720808302564562486757422301181089761");

    private BigInteger defaultServerRSAModulus = new BigInteger(
            1,
            ArrayConverter
                    .hexStringToByteArray("00c8820d6c3ce84c8430f6835abfc7d7a912e1664f44578751f376501a8c68476c3072d919c5d39bd0dbe080e71db83bd4ab2f2f9bde3dffb0080f510a5f6929c196551f2b3c369be051054c877573195558fd282035934dc86edab8d4b1b7f555e5b2fee7275384a756ef86cb86793b5d1333f0973203cb96966766e655cd2cccae1940e4494b8e9fb5279593b75afd0b378243e51a88f6eb88def522a8cd5c6c082286a04269a2879760fcba45005d7f2672dd228809d47274f0fe0ea5531c2bd95366c05bf69edc0f3c3189866edca0c57adcca93250ae78d9eaca0393a95ff9952fc47fb7679dd3803e6a7a6fa771861e3d99e4b551a4084668b111b7eef7d"));// TODO

    private BigInteger defaultClientRSAModulus = new BigInteger(
            1,
            ArrayConverter
                    .hexStringToByteArray("00c8820d6c3ce84c8430f6835abfc7d7a912e1664f44578751f376501a8c68476c3072d919c5d39bd0dbe080e71db83bd4ab2f2f9bde3dffb0080f510a5f6929c196551f2b3c369be051054c877573195558fd282035934dc86edab8d4b1b7f555e5b2fee7275384a756ef86cb86793b5d1333f0973203cb96966766e655cd2cccae1940e4494b8e9fb5279593b75afd0b378243e51a88f6eb88def522a8cd5c6c082286a04269a2879760fcba45005d7f2672dd228809d47274f0fe0ea5531c2bd95366c05bf69edc0f3c3189866edca0c57adcca93250ae78d9eaca0393a95ff9952fc47fb7679dd3803e6a7a6fa771861e3d99e4b551a4084668b111b7eef7d"));// TODO

    private BigInteger defaultServerRSAPublicKey = new BigInteger("65537");

    private BigInteger defaultClientRSAPublicKey = new BigInteger("65537");

    private BigInteger defaultServerRSAPrivateKey = new BigInteger(
            1,
            ArrayConverter
                    .hexStringToByteArray("7dc0cb485a3edb56811aeab12cdcda8e48b023298dd453a37b4d75d9e0bbba27c98f0e4852c16fd52341ffb673f64b580b7111abf14bf323e53a2dfa92727364ddb34f541f74a478a077f15277c013606aea839307e6f5fec23fdd72506feea7cbe362697949b145fe8945823a39a898ac6583fc5fbaefa1e77cbc95b3b475e66106e92b906bdbb214b87bcc94020f317fc1c056c834e9cee0ad21951fbdca088274c4ef9d8c2004c6294f49b370fb249c1e2431fb80ce5d3dc9e342914501ef4c162e54e1ee4fed9369b82afc00821a29f4979a647e60935420d44184d98f9cb75122fb604642c6d1ff2b3a51dc32eefdc57d9a9407ad6a06d10e83e2965481"));// TODO

    private BigInteger defaultClientRSAPrivateKey = new BigInteger(
            1,
            ArrayConverter
                    .hexStringToByteArray("7dc0cb485a3edb56811aeab12cdcda8e48b023298dd453a37b4d75d9e0bbba27c98f0e4852c16fd52341ffb673f64b580b7111abf14bf323e53a2dfa92727364ddb34f541f74a478a077f15277c013606aea839307e6f5fec23fdd72506feea7cbe362697949b145fe8945823a39a898ac6583fc5fbaefa1e77cbc95b3b475e66106e92b906bdbb214b87bcc94020f317fc1c056c834e9cee0ad21951fbdca088274c4ef9d8c2004c6294f49b370fb249c1e2431fb80ce5d3dc9e342914501ef4c162e54e1ee4fed9369b82afc00821a29f4979a647e60935420d44184d98f9cb75122fb604642c6d1ff2b3a51dc32eefdc57d9a9407ad6a06d10e83e2965481"));// TODO

    @XmlJavaTypeAdapter(ByteArrayAdapter.class)
    private byte[] defaultPSKKey = ArrayConverter.hexStringToByteArray("1a2b3c4d");

    @XmlJavaTypeAdapter(ByteArrayAdapter.class)
    private byte[] defaultPSKIdentity = "Client_Identity".getBytes(Charset.forName("UTF-8"));

    @XmlJavaTypeAdapter(ByteArrayAdapter.class)
    private byte[] defaultPSKIdentityHint = new byte[0];

    private BigInteger defaultSRPModulus = new BigInteger(
            1,
            ArrayConverter
                    .hexStringToByteArray("EEAF0AB9ADB38DD69C33F80AFA8FC5E86072618775FF3C0B9EA2314C9C256576D674DF7496EA81D3383B4813D692C6E0E0D5D8E250B98BE48E495C1D6089DAD15DC7D7B46154D6B6CE8EF4AD69B15D4982559B297BCF1885C529F566660E57EC68EDBC3C05726CC02FD4CBF4976EAA9AFD5138FE8376435B9FC61D2FC0EB06E3"));

    private BigInteger defaultPSKModulus = new BigInteger(
            1,
            ArrayConverter
                    .hexStringToByteArray("FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD129024E088A67CC74020BBEA63B139B22514A08798E3404DDEF9519B3CD3A431B302B0A6DF25F14374FE1356D6D51C245E485B576625E7EC6F44C42E9A637ED6B0BFF5CB6F406B7EDEE386BFB5A899FA5AE9F24117C4B1FE649286651ECE45B3DC2007CB8A163BF0598DA48361C55D39A69163FA8FD24CF5F83655D23DCA3AD961C62F356208552BB9ED529077096966D670C354E4ABC9804F1746C08CA18217C32905E462E36CE3BE39E772C180E86039B2783A2EC07A28FB5C55DF06F4C52C9DE2BCBF6955817183995497CEA956AE515D2261898FA051015728E5A8AAAC42DAD33170D04507A33A85521ABDF1CBA64ECFB850458DBEF0A8AEA71575D060C7DB3970F85A6E1E4C7ABF5AE8CDB0933D71E8C94E04A25619DCEE3D2261AD2EE6BF12FFA06D98A0864D87602733EC86A64521F2B18177B200CBBE117577A615D6C770988C0BAD946E208E24FA074E5AB3143DB5BFCE0FD108E4B82D120A93AD2CAFFFFFFFFFFFFFFFF"));

    private BigInteger defaultPSKGenerator = new BigInteger("2");

    private BigInteger defaultPskDhServerPrivateKey = new BigInteger(1,
            ArrayConverter.hexStringToByteArray("440051d6f0b55ea967ab31c68a8b5e37d910dae0e2d459a486459caadf367516"));

    private BigInteger defaultPskDhServerPublicKey = new BigInteger(
            1,
            ArrayConverter
                    .hexStringToByteArray("5a0d3d4e049faa939ffa6a375b9c3c16a4c39753d19ff7da36bc391ea72fc0f68c929bdb400552ed84e0900c7a44c3222fd54d7148256862886bfb4016bd2d03c4c4cf476567c291770e47bd59d0aa5323cfddfc5596e0d6558c480ee8b0c62599834d4581a796a01981468789164504afbd29ce9936e86a290c5f00f8ba986b48010f3e5c079c7f351ddca2ee1fd50846b37bf7463c2b0f3d001b1317ac3069cd89e2e4927ed3d40875a6049af649d2dc349db5995a7525d70a3a1c9b673f5482f83343bd90d45e9c3962dc4a4bf2b4adb37e9166b2ddb31ccf11c5b9e6c98e0a9a3377abba56b0f4283b2eaa69f5368bc107e1c22599f88dd1924d0899c5f153462c911a8293078aefee9fb2389a7854833fcea61cfecbb49f828c361a981a5fedecf13796ae36e36c15a16670af96996c3c45a30e900e18c858f6232b5f7072bdd9e47d7fc61246ef5d19765739f38509284379bc319d9409e8fe236bd29b0335a5bc5bb0424ee44de8a19f864a159fda907d6f5a30ebc0a17e3628e490e5"));

    private BigInteger defaultSRPGenerator = new BigInteger("2");

    private BigInteger defaultSRPServerPrivateKey = new BigInteger("3");

    private BigInteger defaultSRPClientPrivateKey = new BigInteger("5");

    private BigInteger defaultSRPServerPublicKey = new BigInteger(
            1,
            ArrayConverter
                    .hexStringToByteArray("AC47983DEB1698D9A9029E8F7B39092F441DDD72C56D3A63F236E1CF6CEE839937AB5FD69F8CEBBA64C210170A59B2526ED34B9DD83EF86DF7899DF68297844B15E6F2D1BD2448640D32A48220E6343875976A268F28D25174C37D8DC19F2BA5A35301CEED689206FA91CE7A172D908B821DF8C760918E6A5D1C0CFA76AF503B"));

    private BigInteger defaultSRPClientPublicKey = new BigInteger(1, ArrayConverter.hexStringToByteArray("25C843"));

    @XmlJavaTypeAdapter(ByteArrayAdapter.class)
    private byte[] defaultSRPServerSalt = ArrayConverter.hexStringToByteArray("AABBCCDD");

    @XmlJavaTypeAdapter(ByteArrayAdapter.class)
    private byte[] defaultSRPIdentity = "UserName".getBytes(Charset.forName("UTF-8"));

    @XmlJavaTypeAdapter(ByteArrayAdapter.class)
    private byte[] defaultSRPPassword = "Password".getBytes(Charset.forName("UTF-8"));

    @XmlJavaTypeAdapter(ByteArrayAdapter.class)
    private byte[] defaultClientHandshakeTrafficSecret = new byte[0];

    @XmlJavaTypeAdapter(ByteArrayAdapter.class)
    private byte[] defaultServerHandshakeTrafficSecret = new byte[0];

    @XmlJavaTypeAdapter(ByteArrayAdapter.class)
    private byte[] defaultClientApplicationTrafficSecret = new byte[0];

    @XmlJavaTypeAdapter(ByteArrayAdapter.class)
    private byte[] defaultServerApplicationTrafficSecret = new byte[0];

    private TokenBindingType defaultTokenBindingType = TokenBindingType.PROVIDED_TOKEN_BINDING;

    private CustomECPoint defaultTokenBindingECPublicKey = null;

    private BigInteger defaultTokenBindingRsaPublicKey = new BigInteger("65537");

    private BigInteger defaultTokenBindingRsaPrivateKey = new BigInteger(
            "89489425009274444368228545921773093919669586065884257445497854456487674839629818390934941973262879616797970608917283679875499331574161113854088813275488110588247193077582527278437906504015680623423550067240042466665654232383502922215493623289472138866445818789127946123407807725702626644091036502372545139713");

    private BigInteger defaultTokenBindingEcPrivateKey = new BigInteger("3");

    private BigInteger defaultTokenBindingRsaModulus = new BigInteger(
            "145906768007583323230186939349070635292401872375357164399581871019873438799005358938369571402670149802121818086292467422828157022922076746906543401224889672472407926969987100581290103199317858753663710862357656510507883714297115637342788911463535102712032765166518411726859837988672111837205085526346618740053");

    private Boolean useRandomUnixTime = true;

    private ChooserType chooserType = ChooserType.DEFAULT;

    private Boolean useAllProvidedRecords = false;

    private Boolean httpsParsingEnabled = false;

    /**
     * The Ticket Lifetime Hint, Ticket Key and Ticket Key Name used in the
     * Extension defined in RFC5077, followed by additional TLS 1.3 draft 21
     * NewSessionTicket parameters.
     */
    private Long sessionTicketLifetimeHint = 0l;

    @XmlJavaTypeAdapter(ByteArrayAdapter.class)
    private byte[] sessionTicketKeyAES = ArrayConverter.hexStringToByteArray("536563757265535469636b65744b6579"); // SecureSTicketKey

    @XmlJavaTypeAdapter(ByteArrayAdapter.class)
    private byte[] sessionTicketKeyHMAC = ArrayConverter
            .hexStringToByteArray("536563757265535469636b65744b6579536563757265535469636b65744b6579"); // SecureSTicketKeySecureSTicketKey

    @XmlJavaTypeAdapter(ByteArrayAdapter.class)
    private byte[] sessionTicketKeyName = ArrayConverter.hexStringToByteArray("544c532d41747461636b6572204b6579"); // TLS-Attacker

    @XmlJavaTypeAdapter(ByteArrayAdapter.class)
    private byte[] defaultSessionTicketAgeAdd = ArrayConverter.hexStringToByteArray("cb8dbe8e");

    @XmlJavaTypeAdapter(ByteArrayAdapter.class)
    private byte[] defaultSessionTicketNonce = ArrayConverter.hexStringToByteArray("00");

    @XmlJavaTypeAdapter(ByteArrayAdapter.class)
    private byte[] defaultSessionTicketIdentity = ArrayConverter
            .hexStringToByteArray("5266d21abe0f5156106eb1f0ec54a48a90fbc136de990a8881192211cc83aa7992ceb67d7a40b3f304fdea87e4ca61042c19641fd7493975ec69a3ec3f5fb6404aa4ac5acd5efbea15d454d89888a46fc4e6c6b9a3e0ee08ea21538372ced8d0aca453ceae44ce372a5388ab4cef67c5eae8cc1c72735d2646c19b2c50a4ee9bc97e70c6b57cab276a11a59fc5cbe0f5d2519e164fbf9f07a9dd053bcfc08939b475c7a2e76f04ef2a06cc9672bd4034");

    /**
     * ClientAuthtication Type, not fully implemented yet
     */
    private ClientAuthenticationType clientAuthenticationType = ClientAuthenticationType.ANONYMOUS;
    private NamedCurve[] defaultEcdheNamedCurves = new NamedCurve[] { NamedCurve.SECP192R1 };

    private ECPointFormat[] defaultEcPointFormats = new ECPointFormat[] { ECPointFormat.UNCOMPRESSED };

    private Config() {
        defaultClientConnection = new OutboundConnection("client", 443, "localhost");
        defaultServerConnection = new InboundConnection("server", 443);
        workflowTraceType = WorkflowTraceType.HANDSHAKE;

        supportedSignatureAndHashAlgorithms = new LinkedList<>();
        supportedSignatureAndHashAlgorithms.add(new SignatureAndHashAlgorithm(SignatureAlgorithm.RSA,
                HashAlgorithm.SHA512));
        supportedSignatureAndHashAlgorithms.add(new SignatureAndHashAlgorithm(SignatureAlgorithm.RSA,
                HashAlgorithm.SHA384));
        supportedSignatureAndHashAlgorithms.add(new SignatureAndHashAlgorithm(SignatureAlgorithm.RSA,
                HashAlgorithm.SHA256));
        supportedSignatureAndHashAlgorithms.add(new SignatureAndHashAlgorithm(SignatureAlgorithm.RSA,
                HashAlgorithm.SHA224));
        supportedSignatureAndHashAlgorithms.add(new SignatureAndHashAlgorithm(SignatureAlgorithm.RSA,
                HashAlgorithm.SHA1));
        supportedSignatureAndHashAlgorithms
                .add(new SignatureAndHashAlgorithm(SignatureAlgorithm.RSA, HashAlgorithm.MD5));
        defaultClientSupportedCompressionMethods = new LinkedList<>();
        defaultClientSupportedCompressionMethods.add(CompressionMethod.NULL);
        defaultServerSupportedCompressionMethods = new LinkedList<>();
        defaultServerSupportedCompressionMethods.add(CompressionMethod.NULL);
        defaultClientSupportedCiphersuites = new LinkedList<>();
        defaultClientSupportedCiphersuites.addAll(CipherSuite.getImplemented());
        defaultServerSupportedCiphersuites = new LinkedList<>();
        defaultServerSupportedCiphersuites.addAll(CipherSuite.getImplemented());
        namedCurves = new LinkedList<>();
        namedCurves.add(NamedCurve.SECP192R1);
        namedCurves.add(NamedCurve.SECP256R1);
        namedCurves.add(NamedCurve.SECP384R1);
        namedCurves.add(NamedCurve.SECP521R1);
        defaultClientNamedCurves = new LinkedList<>();
        defaultClientNamedCurves.add(NamedCurve.SECP192R1);
        defaultClientNamedCurves.add(NamedCurve.SECP256R1);
        defaultClientNamedCurves.add(NamedCurve.SECP384R1);
        defaultClientNamedCurves.add(NamedCurve.SECP521R1);
        clientCertificateTypes = new LinkedList<>();
        clientCertificateTypes.add(ClientCertificateType.RSA_SIGN);
        supportedVersions = new LinkedList<>();
        supportedVersions.add(ProtocolVersion.TLS13);
        defaultTokenBindingKeyParameters = new LinkedList<>();
        defaultTokenBindingKeyParameters.add(TokenBindingKeyParameters.ECDSAP256);
        defaultTokenBindingKeyParameters.add(TokenBindingKeyParameters.RSA2048_PKCS1_5);
        defaultTokenBindingKeyParameters.add(TokenBindingKeyParameters.RSA2048_PSS);
        defaultServerSupportedSignatureAndHashAlgorithms = new LinkedList<>();
        defaultServerSupportedSignatureAndHashAlgorithms.add(new SignatureAndHashAlgorithm(SignatureAlgorithm.RSA,
                HashAlgorithm.SHA1));
        defaultServerSupportedPointFormats = new LinkedList<>();
        defaultClientSupportedPointFormats = new LinkedList<>();
        defaultServerSupportedPointFormats.add(ECPointFormat.UNCOMPRESSED);
        defaultClientSupportedPointFormats.add(ECPointFormat.UNCOMPRESSED);
        defaultClientEcPublicKey = new CustomECPoint(new BigInteger(
                "5477564916791683905639217522063413790465252514105158300031"), new BigInteger(
                "3142682168214624565874993023364886040439474355932713162721"));
        defaultServerEcPublicKey = new CustomECPoint(new BigInteger(
                "5477564916791683905639217522063413790465252514105158300031"), new BigInteger(
                "3142682168214624565874993023364886040439474355932713162721"));
        secureRealTimeTransportProtocolProtectionProfiles = new LinkedList<>();
        secureRealTimeTransportProtocolProtectionProfiles.add(SrtpProtectionProfiles.SRTP_AES128_CM_HMAC_SHA1_80);
        secureRealTimeTransportProtocolProtectionProfiles.add(SrtpProtectionProfiles.SRTP_AES128_CM_HMAC_SHA1_32);
        secureRealTimeTransportProtocolProtectionProfiles.add(SrtpProtectionProfiles.SRTP_NULL_HMAC_SHA1_80);
        secureRealTimeTransportProtocolProtectionProfiles.add(SrtpProtectionProfiles.SRTP_NULL_HMAC_SHA1_32);
        certificateTypeDesiredTypes = new LinkedList<>();
        certificateTypeDesiredTypes.add(CertificateType.OPEN_PGP);
        certificateTypeDesiredTypes.add(CertificateType.X509);
        clientAuthzExtensionDataFormat = new LinkedList<>();
        clientAuthzExtensionDataFormat.add(AuthzDataFormat.X509_ATTR_CERT);
        clientAuthzExtensionDataFormat.add(AuthzDataFormat.SAML_ASSERTION);
        clientAuthzExtensionDataFormat.add(AuthzDataFormat.X509_ATTR_CERT_URL);
        clientAuthzExtensionDataFormat.add(AuthzDataFormat.SAML_ASSERTION_URL);
        serverAuthzExtensionDataFormat = new LinkedList<>();
        serverAuthzExtensionDataFormat.add(AuthzDataFormat.X509_ATTR_CERT);
        serverAuthzExtensionDataFormat.add(AuthzDataFormat.SAML_ASSERTION);
        serverAuthzExtensionDataFormat.add(AuthzDataFormat.X509_ATTR_CERT_URL);
        serverAuthzExtensionDataFormat.add(AuthzDataFormat.SAML_ASSERTION_URL);
        clientCertificateTypeDesiredTypes = new LinkedList<>();
        clientCertificateTypeDesiredTypes.add(CertificateType.OPEN_PGP);
        clientCertificateTypeDesiredTypes.add(CertificateType.X509);
        clientCertificateTypeDesiredTypes.add(CertificateType.RAW_PUBLIC_KEY);
        serverCertificateTypeDesiredTypes = new LinkedList<>();
        serverCertificateTypeDesiredTypes.add(CertificateType.OPEN_PGP);
        serverCertificateTypeDesiredTypes.add(CertificateType.X509);
        serverCertificateTypeDesiredTypes.add(CertificateType.RAW_PUBLIC_KEY);
        cachedObjectList = new LinkedList<>();
        trustedCaIndicationExtensionAuthorties = new LinkedList<>();
        statusRequestV2RequestList = new LinkedList<>();
        outputFilters = new ArrayList<>();
        outputFilters.add(FilterType.DEFAULT);
        applyFiltersInPlace = false;
        filtersKeepUserSettings = true;
    }

    public long getSessionTicketLifetimeHint() {
        return sessionTicketLifetimeHint;
    }

    public void setSessionTicketLifetimeHint(long sessionTicketLifetimeHint) {
        this.sessionTicketLifetimeHint = sessionTicketLifetimeHint;
    }

    public byte[] getSessionTicketKeyAES() {
        return sessionTicketKeyAES;
    }

    public void setSessionTicketKeyAES(byte[] sessionTicketKeyAES) {
        this.sessionTicketKeyAES = sessionTicketKeyAES;
    }

    public byte[] getSessionTicketKeyHMAC() {
        return sessionTicketKeyHMAC;
    }

    public void setSessionTicketKeyHMAC(byte[] sessionTicketKeyHMAC) {
        this.sessionTicketKeyHMAC = sessionTicketKeyHMAC;
    }

    public byte[] getSessionTicketKeyName() {
        return sessionTicketKeyName;
    }

    public void setSessionTicketKeyName(byte[] sessionTicketKeyName) {
        this.sessionTicketKeyName = sessionTicketKeyName;
    }

    public ClientAuthenticationType getClientAuthenticationType() {
        return clientAuthenticationType;
    }

    public void setClientAuthenticationType(ClientAuthenticationType clientAuthenticationType) {
        this.clientAuthenticationType = clientAuthenticationType;
    }

    public ECPointFormat[] getDefaultEcPointFormats() {
        return defaultEcPointFormats;
    }

    public void setDefaultEcPointFormats(ECPointFormat[] defaultEcPointFormats) {
        this.defaultEcPointFormats = defaultEcPointFormats;
    }

    public NamedCurve[] getDefaultEcdheNamedCurves() {
        return defaultEcdheNamedCurves;
    }

    public void setDefaultEcdheNamedCurves(NamedCurve[] defaultEcdheNamedCurves) {
        this.defaultEcdheNamedCurves = defaultEcdheNamedCurves;
    }

    public Boolean isHttpsParsingEnabled() {
        return httpsParsingEnabled;
    }

    public void setHttpsParsingEnabled(Boolean httpsParsingEnabled) {
        this.httpsParsingEnabled = httpsParsingEnabled;
    }

    public Boolean isUseRandomUnixTime() {
        return useRandomUnixTime;
    }

    public void setUseRandomUnixTime(Boolean useRandomUnixTime) {
        this.useRandomUnixTime = useRandomUnixTime;
    }

    public Boolean isUseAllProvidedRecords() {
        return useAllProvidedRecords;
    }

    public void setUseAllProvidedRecords(Boolean useAllProvidedRecords) {
        this.useAllProvidedRecords = useAllProvidedRecords;
    }

    public byte[] getDefaultServerRenegotiationInfo() {
        return defaultServerRenegotiationInfo;
    }

    public void setDefaultServerRenegotiationInfo(byte[] defaultServerRenegotiationInfo) {
        this.defaultServerRenegotiationInfo = defaultServerRenegotiationInfo;
    }

    public ChooserType getChooserType() {
        return chooserType;
    }

    public void setChooserType(ChooserType chooserType) {
        this.chooserType = chooserType;
    }

    public Boolean isEarlyStop() {
        return earlyStop;
    }

    public void setEarlyStop(Boolean earlyStop) {
        this.earlyStop = earlyStop;
    }

    public CustomECPoint getDefaultTokenBindingECPublicKey() {
        return defaultTokenBindingECPublicKey;
    }

    public void setDefaultTokenBindingECPublicKey(CustomECPoint defaultTokenBindingECPublicKey) {
        this.defaultTokenBindingECPublicKey = defaultTokenBindingECPublicKey;
    }

    public BigInteger getDefaultTokenBindingRsaPublicKey() {
        return defaultTokenBindingRsaPublicKey;
    }

    public void setDefaultTokenBindingRsaPublicKey(BigInteger defaultTokenBindingRsaPublicKey) {
        this.defaultTokenBindingRsaPublicKey = defaultTokenBindingRsaPublicKey;
    }

    public BigInteger getDefaultTokenBindingRsaPrivateKey() {
        return defaultTokenBindingRsaPrivateKey;
    }

    public void setDefaultTokenBindingRsaPrivateKey(BigInteger defaultTokenBindingRsaPrivateKey) {
        this.defaultTokenBindingRsaPrivateKey = defaultTokenBindingRsaPrivateKey;
    }

    public BigInteger getDefaultTokenBindingEcPrivateKey() {
        return defaultTokenBindingEcPrivateKey;
    }

    public void setDefaultTokenBindingEcPrivateKey(BigInteger defaultTokenBindingEcPrivateKey) {
        this.defaultTokenBindingEcPrivateKey = defaultTokenBindingEcPrivateKey;
    }

    public BigInteger getDefaultTokenBindingRsaModulus() {
        return defaultTokenBindingRsaModulus;
    }

    public void setDefaultTokenBindingRsaModulus(BigInteger defaultTokenBindingRsaModulus) {
        this.defaultTokenBindingRsaModulus = defaultTokenBindingRsaModulus;
    }

    public TokenBindingType getDefaultTokenBindingType() {
        return defaultTokenBindingType;
    }

    public void setDefaultTokenBindingType(TokenBindingType defaultTokenBindingType) {
        this.defaultTokenBindingType = defaultTokenBindingType;
    }

    public byte[] getDefaultRsaCertificate() {
        return defaultRsaCertificate;
    }

    public void setDefaultRsaCertificate(byte[] defaultRsaCertificate) {
        this.defaultRsaCertificate = defaultRsaCertificate;
    }

    public byte[] getDefaultDsaCertificate() {
        return defaultDsaCertificate;
    }

    public void setDefaultDsaCertificate(byte[] defaultDsaCertificate) {
        this.defaultDsaCertificate = defaultDsaCertificate;
    }

    public byte[] getDefaultEcCertificate() {
        return defaultEcCertificate;
    }

    public void setDefaultEcCertificate(byte[] defaultEcCertificate) {
        this.defaultEcCertificate = defaultEcCertificate;
    }

    public byte[] getDefaultClientHandshakeTrafficSecret() {
        return defaultClientHandshakeTrafficSecret;
    }

    public void setDefaultClientHandshakeTrafficSecret(byte[] defaultClientHandshakeTrafficSecret) {
        this.defaultClientHandshakeTrafficSecret = defaultClientHandshakeTrafficSecret;
    }

    public byte[] getDefaultServerHandshakeTrafficSecret() {
        return defaultServerHandshakeTrafficSecret;
    }

    public void setDefaultServerHandshakeTrafficSecret(byte[] defaultServerHandshakeTrafficSecret) {
        this.defaultServerHandshakeTrafficSecret = defaultServerHandshakeTrafficSecret;
    }

    public byte[] getKeySharePublic() {
        return keySharePublic;
    }

    public void setKeySharePublic(byte[] keySharePublic) {
        this.keySharePublic = keySharePublic;
    }

    public byte[] getDefaultCertificateRequestContext() {
        return defaultCertificateRequestContext;
    }

    public void setDefaultCertificateRequestContext(byte[] defaultCertificateRequestContext) {
        this.defaultCertificateRequestContext = defaultCertificateRequestContext;
    }

    public Boolean isWorkflowExecutorShouldOpen() {
        return workflowExecutorShouldOpen;
    }

    public void setWorkflowExecutorShouldOpen(Boolean workflowExecutorShouldOpen) {
        this.workflowExecutorShouldOpen = workflowExecutorShouldOpen;
    }

    public Boolean isWorkflowExecutorShouldClose() {
        return workflowExecutorShouldClose;
    }

    public void setWorkflowExecutorShouldClose(Boolean workflowExecutorShouldClose) {
        this.workflowExecutorShouldClose = workflowExecutorShouldClose;
    }

    public Boolean isStopRecievingAfterFatal() {
        return stopRecievingAfterFatal;
    }

    public void setStopRecievingAfterFatal(Boolean stopRecievingAfterFatal) {
        this.stopRecievingAfterFatal = stopRecievingAfterFatal;
    }

    public byte[] getDefaultPSKKey() {
        return defaultPSKKey;
    }

    public void setDefaultPSKKey(byte[] defaultPSKKey) {
        this.defaultPSKKey = defaultPSKKey;
    }

    public byte[] getDefaultPSKIdentity() {
        return defaultPSKIdentity;
    }

    public void setDefaultPSKIdentity(byte[] defaultPSKIdentity) {
        this.defaultPSKIdentity = defaultPSKIdentity;
    }

    public byte[] getDefaultPSKIdentityHint() {
        return defaultPSKIdentityHint;
    }

    public void setDefaultPSKIdentityHint(byte[] defaultPSKIdentityHint) {
        this.defaultPSKIdentityHint = defaultPSKIdentityHint;
    }

    public BigInteger getDefaultSRPModulus() {
        return defaultSRPModulus;
    }

    public void setDefaultSRPModulus(BigInteger defaultSRPModulus) {
        this.defaultSRPModulus = defaultSRPModulus;
    }

    public BigInteger getDefaultPSKModulus() {
        return defaultPSKModulus;
    }

    public void setDefaultPSKModulus(BigInteger defaultPSKModulus) {
        this.defaultPSKModulus = defaultPSKModulus;
    }

    public BigInteger getDefaultPSKServerPrivateKey() {
        return defaultPskDhServerPrivateKey;
    }

    public void setDefaultPSKServerPrivateKey(BigInteger defaultPskDhServerPrivateKey) {
        this.defaultPskDhServerPrivateKey = defaultPskDhServerPrivateKey;
    }

    public BigInteger getDefaultPSKServerPublicKey() {
        return defaultPskDhServerPublicKey;
    }

    public void setDefaultPSKServerPublicKey(BigInteger defaultPskDhServerPublicKey) {
        this.defaultPskDhServerPublicKey = defaultPskDhServerPublicKey;
    }

    public BigInteger getDefaultPSKGenerator() {
        return defaultPSKGenerator;
    }

    public void setDefaultPSKGenerator(BigInteger defaultPSKGenerator) {
        this.defaultPSKGenerator = defaultPSKGenerator;
    }

    public BigInteger getDefaultSRPServerPrivateKey() {
        return defaultSRPServerPrivateKey;
    }

    public void setDefaultSRPServerPrivateKey(BigInteger defaultSRPServerPrivateKey) {
        this.defaultSRPServerPrivateKey = defaultSRPServerPrivateKey;
    }

    public BigInteger getDefaultSRPServerPublicKey() {
        return defaultSRPServerPublicKey;
    }

    public void setDefaultSRPServerPublicKey(BigInteger defaultSRPServerPublicKey) {
        this.defaultSRPServerPublicKey = defaultSRPServerPublicKey;
    }

    public BigInteger getDefaultSRPClientPrivateKey() {
        return defaultSRPClientPrivateKey;
    }

    public void setDefaultSRPClientPrivateKey(BigInteger defaultSRPClientPrivateKey) {
        this.defaultSRPClientPrivateKey = defaultSRPClientPrivateKey;
    }

    public BigInteger getDefaultSRPClientPublicKey() {
        return defaultSRPClientPublicKey;
    }

    public void setDefaultSRPClientPublicKey(BigInteger defaultSRPClientPublicKey) {
        this.defaultSRPClientPublicKey = defaultSRPClientPublicKey;
    }

    public BigInteger getDefaultSRPGenerator() {
        return defaultSRPGenerator;
    }

    public void setDefaultSRPGenerator(BigInteger defaultSRPGenerator) {
        this.defaultSRPGenerator = defaultSRPGenerator;
    }

    public byte[] getDefaultSRPServerSalt() {
        return defaultSRPServerSalt;
    }

    public void setDefaultSRPServerSalt(byte[] defaultSRPServerSalt) {
        this.defaultSRPServerSalt = defaultSRPServerSalt;
    }

    public byte[] getDefaultSRPIdentity() {
        return defaultSRPIdentity;
    }

    public void setDefaultSRPIdentity(byte[] defaultSRPIdentity) {
        this.defaultSRPIdentity = defaultSRPIdentity;
    }

    public byte[] getDefaultSRPPassword() {
        return defaultSRPPassword;
    }

    public void setDefaultSRPPassword(byte[] defaultSRPPassword) {
        this.defaultSRPPassword = defaultSRPPassword;
    }

    public BigInteger getDefaultClientRSAPrivateKey() {
        return defaultClientRSAPrivateKey;
    }

    public void setDefaultClientRSAPrivateKey(BigInteger defaultClientRSAPrivateKey) {
        this.defaultClientRSAPrivateKey = defaultClientRSAPrivateKey;
    }

    public BigInteger getDefaultServerRSAPrivateKey() {
        return defaultServerRSAPrivateKey;
    }

    public void setDefaultServerRSAPrivateKey(BigInteger defaultServerRSAPrivateKey) {
        this.defaultServerRSAPrivateKey = defaultServerRSAPrivateKey;
    }

    public BigInteger getDefaultServerRSAModulus() {
        return defaultServerRSAModulus;
    }

    public void setDefaultServerRSAModulus(BigInteger defaultServerRSAModulus) {
        if (defaultServerRSAModulus.signum() == 1) {
            this.defaultServerRSAModulus = defaultServerRSAModulus;
        } else {
            throw new IllegalArgumentException("Modulus cannot be negative or zero"
                    + defaultServerRSAModulus.toString());
        }
    }

    public BigInteger getDefaultServerRSAPublicKey() {
        return defaultServerRSAPublicKey;
    }

    public void setDefaultServerRSAPublicKey(BigInteger defaultServerRSAPublicKey) {
        this.defaultServerRSAPublicKey = defaultServerRSAPublicKey;
    }

    public BigInteger getDefaultClientRSAPublicKey() {
        return defaultClientRSAPublicKey;
    }

    public void setDefaultClientRSAPublicKey(BigInteger defaultClientRSAPublicKey) {
        this.defaultClientRSAPublicKey = defaultClientRSAPublicKey;
    }

    public BigInteger getDefaultServerEcPrivateKey() {
        return defaultServerEcPrivateKey;
    }

    public void setDefaultServerEcPrivateKey(BigInteger defaultServerEcPrivateKey) {
        this.defaultServerEcPrivateKey = defaultServerEcPrivateKey;
    }

    public BigInteger getDefaultClientEcPrivateKey() {
        return defaultClientEcPrivateKey;
    }

    public void setDefaultClientEcPrivateKey(BigInteger defaultClientEcPrivateKey) {
        this.defaultClientEcPrivateKey = defaultClientEcPrivateKey;
    }

    public NamedCurve getDefaultSelectedCurve() {
        return defaultSelectedCurve;
    }

    public void setDefaultSelectedCurve(NamedCurve defaultSelectedCurve) {
        this.defaultSelectedCurve = defaultSelectedCurve;
    }

    public CustomECPoint getDefaultClientEcPublicKey() {
        return defaultClientEcPublicKey;
    }

    public void setDefaultClientEcPublicKey(CustomECPoint defaultClientEcPublicKey) {
        this.defaultClientEcPublicKey = defaultClientEcPublicKey;
    }

    public CustomECPoint getDefaultServerEcPublicKey() {
        return defaultServerEcPublicKey;
    }

    public void setDefaultServerEcPublicKey(CustomECPoint defaultServerEcPublicKey) {
        this.defaultServerEcPublicKey = defaultServerEcPublicKey;
    }

    public byte getDefaultAlertDescription() {
        return defaultAlertDescription;
    }

    public void setDefaultAlertDescription(byte defaultAlertDescription) {
        this.defaultAlertDescription = defaultAlertDescription;
    }

    public byte getDefaultAlertLevel() {
        return defaultAlertLevel;
    }

    public void setDefaultAlertLevel(byte defaultAlertLevel) {
        this.defaultAlertLevel = defaultAlertLevel;
    }

    public BigInteger getDefaultServerDhPublicKey() {
        return defaultServerDhPublicKey;
    }

    public void setDefaultServerDhPublicKey(BigInteger defaultServerDhPublicKey) {
        this.defaultServerDhPublicKey = defaultServerDhPublicKey;
    }

    public BigInteger getDefaultClientDhPublicKey() {
        return defaultClientDhPublicKey;
    }

    public void setDefaultClientDhPublicKey(BigInteger defaultClientDhPublicKey) {
        this.defaultClientDhPublicKey = defaultClientDhPublicKey;
    }

    public BigInteger getDefaultServerDhPrivateKey() {
        return defaultServerDhPrivateKey;
    }

    public void setDefaultServerDhPrivateKey(BigInteger defaultServerDhPrivateKey) {
        this.defaultServerDhPrivateKey = defaultServerDhPrivateKey;
    }

    public BigInteger getDefaultServerDsaPrivateKey() {
        return defaultServerDsaPrivateKey;
    }

    public void setDefaultServerDsaPrivateKey(BigInteger defaultServerDsaPrivateKey) {
        this.defaultServerDsaPrivateKey = defaultServerDsaPrivateKey;
    }

    public PRFAlgorithm getDefaultPRFAlgorithm() {
        return defaultPRFAlgorithm;
    }

    public void setDefaultPRFAlgorithm(PRFAlgorithm defaultPRFAlgorithm) {
        this.defaultPRFAlgorithm = defaultPRFAlgorithm;
    }

    public byte[] getDefaultDtlsCookie() {
        return defaultDtlsCookie;
    }

    public void setDefaultDtlsCookie(byte[] defaultDtlsCookie) {
        this.defaultDtlsCookie = defaultDtlsCookie;
    }

    public byte[] getDefaultClientSessionId() {
        return defaultClientSessionId;
    }

    public void setDefaultClientSessionId(byte[] defaultClientSessionId) {
        this.defaultClientSessionId = defaultClientSessionId;
    }

    public byte[] getDefaultServerSessionId() {
        return defaultServerSessionId;
    }

    public void setDefaultServerSessionId(byte[] defaultServerSessionId) {
        this.defaultServerSessionId = defaultServerSessionId;
    }

    public CompressionMethod getDefaultSelectedCompressionMethod() {
        return defaultSelectedCompressionMethod;
    }

    public void setDefaultSelectedCompressionMethod(CompressionMethod defaultSelectedCompressionMethod) {
        this.defaultSelectedCompressionMethod = defaultSelectedCompressionMethod;
    }

    public byte[] getDefaultServerRandom() {
        return defaultServerRandom;
    }

    public void setDefaultServerRandom(byte[] defaultServerRandom) {
        this.defaultServerRandom = defaultServerRandom;
    }

    public byte[] getDefaultClientRandom() {
        return defaultClientRandom;
    }

    public void setDefaultClientRandom(byte[] defaultClientRandom) {
        this.defaultClientRandom = defaultClientRandom;
    }

    public byte[] getDefaultPreMasterSecret() {
        return defaultPreMasterSecret;
    }

    public void setDefaultPreMasterSecret(byte[] defaultPreMasterSecret) {
        this.defaultPreMasterSecret = defaultPreMasterSecret;
    }

    public byte[] getDefaultMasterSecret() {
        return defaultMasterSecret;
    }

    public void setDefaultMasterSecret(byte[] defaultMasterSecret) {
        this.defaultMasterSecret = defaultMasterSecret;
    }

    public ProtocolVersion getDefaultHighestClientProtocolVersion() {
        return defaultHighestClientProtocolVersion;
    }

    public void setDefaultHighestClientProtocolVersion(ProtocolVersion defaultHighestClientProtocolVersion) {
        this.defaultHighestClientProtocolVersion = defaultHighestClientProtocolVersion;
    }

    public ProtocolVersion getDefaultSelectedProtocolVersion() {
        return defaultSelectedProtocolVersion;
    }

    public void setDefaultSelectedProtocolVersion(ProtocolVersion defaultSelectedProtocolVersion) {
        this.defaultSelectedProtocolVersion = defaultSelectedProtocolVersion;
    }

    public List<SignatureAndHashAlgorithm> getDefaultServerSupportedSignatureAndHashAlgorithms() {
        return defaultServerSupportedSignatureAndHashAlgorithms;
    }

    public void setDefaultServerSupportedSignatureAndHashAlgorithms(
            List<SignatureAndHashAlgorithm> defaultServerSupportedSignatureAndHashAlgorithms) {
        this.defaultServerSupportedSignatureAndHashAlgorithms = defaultServerSupportedSignatureAndHashAlgorithms;
    }

    public void setDefaultServerSupportedSignatureAndHashAlgorithms(
            SignatureAndHashAlgorithm... defaultServerSupportedSignatureAndHashAlgorithms) {
        this.defaultServerSupportedSignatureAndHashAlgorithms = Arrays
                .asList(defaultServerSupportedSignatureAndHashAlgorithms);
    }

    public List<CipherSuite> getDefaultServerSupportedCiphersuites() {
        return defaultServerSupportedCiphersuites;
    }

    public void setDefaultServerSupportedCiphersuites(List<CipherSuite> defaultServerSupportedCiphersuites) {
        this.defaultServerSupportedCiphersuites = defaultServerSupportedCiphersuites;
    }

    public final void setDefaultServerSupportedCiphersuites(CipherSuite... defaultServerSupportedCiphersuites) {
        this.defaultServerSupportedCiphersuites = new ArrayList(Arrays.asList(defaultServerSupportedCiphersuites));
    }

    public List<CompressionMethod> getDefaultClientSupportedCompressionMethods() {
        return defaultClientSupportedCompressionMethods;
    }

    public void setDefaultClientSupportedCompressionMethods(
            List<CompressionMethod> defaultClientSupportedCompressionMethods) {
        this.defaultClientSupportedCompressionMethods = defaultClientSupportedCompressionMethods;
    }

    public final void setDefaultClientSupportedCompressionMethods(
            CompressionMethod... defaultClientSupportedCompressionMethods) {
        this.defaultClientSupportedCompressionMethods = new ArrayList(
                Arrays.asList(defaultClientSupportedCompressionMethods));
    }

    public HeartbeatMode getDefaultHeartbeatMode() {
        return defaultHeartbeatMode;
    }

    public void setDefaultHeartbeatMode(HeartbeatMode defaultHeartbeatMode) {
        this.defaultHeartbeatMode = defaultHeartbeatMode;
    }

    public MaxFragmentLength getDefaultMaxFragmentLength() {
        return defaultMaxFragmentLength;
    }

    public void setDefaultMaxFragmentLength(MaxFragmentLength defaultMaxFragmentLength) {
        this.defaultMaxFragmentLength = defaultMaxFragmentLength;
    }

    public SignatureAndHashAlgorithm getDefaultSelectedSignatureAndHashAlgorithm() {
        return defaultSelectedSignatureAndHashAlgorithm;
    }

    public void setDefaultSelectedSignatureAndHashAlgorithm(
            SignatureAndHashAlgorithm defaultSelectedSignatureAndHashAlgorithm) {
        this.defaultSelectedSignatureAndHashAlgorithm = defaultSelectedSignatureAndHashAlgorithm;
    }

    public List<ECPointFormat> getDefaultClientSupportedPointFormats() {
        return defaultClientSupportedPointFormats;
    }

    public void setDefaultClientSupportedPointFormats(List<ECPointFormat> defaultClientSupportedPointFormats) {
        this.defaultClientSupportedPointFormats = defaultClientSupportedPointFormats;
    }

    public final void setDefaultClientSupportedPointFormats(ECPointFormat... defaultClientSupportedPointFormats) {
        this.defaultClientSupportedPointFormats = new ArrayList(Arrays.asList(defaultClientSupportedPointFormats));
    }

    public ProtocolVersion getDefaultLastRecordProtocolVersion() {
        return defaultLastRecordProtocolVersion;
    }

    public void setDefaultLastRecordProtocolVersion(ProtocolVersion defaultLastRecordProtocolVersion) {
        this.defaultLastRecordProtocolVersion = defaultLastRecordProtocolVersion;
    }

    public List<SNIEntry> getDefaultClientSNIEntryList() {
        return defaultClientSNIEntryList;
    }

    public void setDefaultClientSNIEntryList(List<SNIEntry> defaultClientSNIEntryList) {
        this.defaultClientSNIEntryList = defaultClientSNIEntryList;
    }

    public final void setDefaultClientSNIEntries(SNIEntry... defaultClientSNIEntryList) {
        this.defaultClientSNIEntryList = new ArrayList(Arrays.asList(defaultClientSNIEntryList));
    }

    public List<SignatureAndHashAlgorithm> getDefaultClientSupportedSignatureAndHashAlgorithms() {
        return defaultClientSupportedSignatureAndHashAlgorithms;
    }

    public void setDefaultClientSupportedSignatureAndHashAlgorithms(
            List<SignatureAndHashAlgorithm> defaultClientSupportedSignatureAndHashAlgorithms) {
        this.defaultClientSupportedSignatureAndHashAlgorithms = defaultClientSupportedSignatureAndHashAlgorithms;
    }

    public final void setDefaultClientSupportedSignatureAndHashAlgorithms(
            SignatureAndHashAlgorithm... defaultClientSupportedSignatureAndHashAlgorithms) {
        this.defaultClientSupportedSignatureAndHashAlgorithms = Arrays
                .asList(defaultClientSupportedSignatureAndHashAlgorithms);
    }

    public List<ECPointFormat> getDefaultServerSupportedPointFormats() {
        return defaultServerSupportedPointFormats;
    }

    public void setDefaultServerSupportedPointFormats(List<ECPointFormat> defaultServerSupportedPointFormats) {
        this.defaultServerSupportedPointFormats = defaultServerSupportedPointFormats;
    }

    public final void setDefaultServerSupportedPointFormats(ECPointFormat... defaultServerSupportedPointFormats) {
        this.defaultServerSupportedPointFormats = new ArrayList(Arrays.asList(defaultServerSupportedPointFormats));
    }

    public List<NamedCurve> getDefaultClientNamedCurves() {
        return defaultClientNamedCurves;
    }

    public void setDefaultClientNamedCurves(List<NamedCurve> defaultClientNamedCurves) {
        this.defaultClientNamedCurves = defaultClientNamedCurves;
    }

    public final void setDefaultClientNamedCurves(NamedCurve... defaultClientNamedCurves) {
        this.defaultClientNamedCurves = new ArrayList(Arrays.asList(defaultClientNamedCurves));
    }

    public CipherSuite getDefaultSelectedCipherSuite() {
        return defaultSelectedCipherSuite;
    }

    public void setDefaultSelectedCipherSuite(CipherSuite defaultSelectedCipherSuite) {
        this.defaultSelectedCipherSuite = defaultSelectedCipherSuite;
    }

    public Boolean isQuickReceive() {
        return quickReceive;
    }

    public void setQuickReceive(Boolean quickReceive) {
        this.quickReceive = quickReceive;
    }

    public Boolean isResetWorkflowtracesBeforeSaving() {
        return resetWorkflowtracesBeforeSaving;
    }

    public void setResetWorkflowtracesBeforeSaving(Boolean resetWorkflowtracesBeforeSaving) {
        this.resetWorkflowtracesBeforeSaving = resetWorkflowtracesBeforeSaving;
    }

    public RecordLayerType getRecordLayerType() {
        return recordLayerType;
    }

    public void setRecordLayerType(RecordLayerType recordLayerType) {
        this.recordLayerType = recordLayerType;
    }

    public Boolean isFlushOnMessageTypeChange() {
        return flushOnMessageTypeChange;
    }

    public void setFlushOnMessageTypeChange(Boolean flushOnMessageTypeChange) {
        this.flushOnMessageTypeChange = flushOnMessageTypeChange;
    }

    public Boolean isCreateRecordsDynamically() {
        return createRecordsDynamically;
    }

    public void setCreateRecordsDynamically(Boolean createRecordsDynamically) {
        this.createRecordsDynamically = createRecordsDynamically;
    }

    public Boolean isCreateIndividualRecords() {
        return createIndividualRecords;
    }

    public void setCreateIndividualRecords(Boolean createIndividualRecords) {
        this.createIndividualRecords = createIndividualRecords;
    }

    public int getDefaultMaxRecordData() {
        return defaultMaxRecordData;
    }

    public void setDefaultMaxRecordData(int defaultMaxRecordData) {
        if (defaultMaxRecordData == 0) {
            LOGGER.warn("defaultMaxRecordData is being set to 0");
        }
        this.defaultMaxRecordData = defaultMaxRecordData;
    }

    public WorkflowExecutorType getWorkflowExecutorType() {
        return workflowExecutorType;
    }

    public void setWorkflowExecutorType(WorkflowExecutorType workflowExecutorType) {
        this.workflowExecutorType = workflowExecutorType;
    }

    public NameType getSniType() {
        return sniType;
    }

    public void setSniType(NameType sniType) {
        this.sniType = sniType;
    }

    public int getHeartbeatPayloadLength() {
        return heartbeatPayloadLength;
    }

    public void setHeartbeatPayloadLength(int heartbeatPayloadLength) {
        this.heartbeatPayloadLength = heartbeatPayloadLength;
    }

    public int getHeartbeatPaddingLength() {
        return heartbeatPaddingLength;
    }

    public void setHeartbeatPaddingLength(int heartbeatPaddingLength) {
        this.heartbeatPaddingLength = heartbeatPaddingLength;
    }

    public Boolean isAddPaddingExtension() {
        return addPaddingExtension;
    }

    public void setAddPaddingExtension(Boolean addPaddingExtension) {
        this.addPaddingExtension = addPaddingExtension;
    }

    public Boolean isAddExtendedMasterSecretExtension() {
        return addExtendedMasterSecretExtension;
    }

    public void setAddExtendedMasterSecretExtension(Boolean addExtendedMasterSecretExtension) {
        this.addExtendedMasterSecretExtension = addExtendedMasterSecretExtension;
    }

    public Boolean isAddSessionTicketTLSExtension() {
        return addSessionTicketTLSExtension;
    }

    public void setAddSessionTicketTLSExtension(Boolean addSessionTicketTLSExtension) {
        this.addSessionTicketTLSExtension = addSessionTicketTLSExtension;
    }

    public byte[] getDefaultPaddingExtensionBytes() {
        return defaultPaddingExtensionBytes;
    }

    public void setDefaultPaddingExtensionBytes(byte[] defaultPaddingExtensionBytes) {
        this.defaultPaddingExtensionBytes = defaultPaddingExtensionBytes;
    }

    public List<ClientCertificateType> getClientCertificateTypes() {
        return clientCertificateTypes;
    }

    public void setClientCertificateTypes(List<ClientCertificateType> clientCertificateTypes) {
        this.clientCertificateTypes = clientCertificateTypes;
    }

    public final void setClientCertificateTypes(ClientCertificateType... clientCertificateTypes) {
        this.clientCertificateTypes = new ArrayList(Arrays.asList(clientCertificateTypes));
    }

    public Boolean isWaitOnlyForExpectedDTLS() {
        return waitOnlyForExpectedDTLS;
    }

    public void setWaitOnlyForExpectedDTLS(Boolean waitOnlyForExpectedDTLS) {
        this.waitOnlyForExpectedDTLS = waitOnlyForExpectedDTLS;
    }

    public String getDefaultApplicationMessageData() {
        return defaultApplicationMessageData;
    }

    public Boolean isDoDTLSRetransmits() {
        return doDTLSRetransmits;
    }

    public void setDoDTLSRetransmits(Boolean doDTLSRetransmits) {
        this.doDTLSRetransmits = doDTLSRetransmits;
    }

    public void setDefaultApplicationMessageData(String defaultApplicationMessageData) {
        this.defaultApplicationMessageData = defaultApplicationMessageData;
    }

    public Boolean isEnforceSettings() {
        return enforceSettings;
    }

    public void setEnforceSettings(Boolean enforceSettings) {
        this.enforceSettings = enforceSettings;
    }

    public BigInteger getDefaultServerDhGenerator() {
        return defaultServerDhGenerator;
    }

    public void setDefaultServerDhGenerator(BigInteger defaultServerDhGenerator) {
        this.defaultServerDhGenerator = defaultServerDhGenerator;
    }

    public BigInteger getDefaultServerDhModulus() {
        return defaultServerDhModulus;
    }

    public void setDefaultServerDhModulus(BigInteger defaultServerDhModulus) {
        if (defaultServerDhModulus.signum() == 1) {
            this.defaultServerDhModulus = defaultServerDhModulus;
        } else {
            throw new IllegalArgumentException("Modulus cannot be negative or zero:"
                    + defaultServerDhModulus.toString());
        }
    }

    public BigInteger getDefaultClientDhPrivateKey() {
        return defaultClientDhPrivateKey;
    }

    public void setDefaultClientDhPrivateKey(BigInteger defaultClientDhPrivateKey) {
        this.defaultClientDhPrivateKey = defaultClientDhPrivateKey;
    }

    public byte[] getDistinguishedNames() {
        return distinguishedNames;
    }

    public void setDistinguishedNames(byte[] distinguishedNames) {
        this.distinguishedNames = distinguishedNames;
    }

    public ProtocolVersion getHighestProtocolVersion() {
        return highestProtocolVersion;
    }

    public void setHighestProtocolVersion(ProtocolVersion highestProtocolVersion) {
        this.highestProtocolVersion = highestProtocolVersion;
    }

    public Boolean isUpdateTimestamps() {
        return updateTimestamps;
    }

    public void setUpdateTimestamps(Boolean updateTimestamps) {
        this.updateTimestamps = updateTimestamps;
    }

    public Boolean isServerSendsApplicationData() {
        return serverSendsApplicationData;
    }

    public void setServerSendsApplicationData(Boolean serverSendsApplicationData) {
        this.serverSendsApplicationData = serverSendsApplicationData;
    }

    public WorkflowTraceType getWorkflowTraceType() {
        return workflowTraceType;
    }

    public void setWorkflowTraceType(WorkflowTraceType workflowTraceType) {
        this.workflowTraceType = workflowTraceType;
    }

    public String getWorkflowOutput() {
        return workflowOutput;
    }

    public void setWorkflowOutput(String workflowOutput) {
        this.workflowOutput = workflowOutput;
    }

    public String getConfigOutput() {
        return configOutput;
    }

    public void setConfigOutput(String configOutput) {
        this.configOutput = configOutput;
    }

    public String getWorkflowInput() {
        return workflowInput;
    }

    public void setWorkflowInput(String workflowInput) {
        this.workflowInput = workflowInput;
    }

    public Boolean isSniHostnameFatal() {
        return sniHostnameFatal;
    }

    public void setSniHostnameFatal(Boolean sniHostnameFatal) {
        this.sniHostnameFatal = sniHostnameFatal;
    }

    public MaxFragmentLength getMaxFragmentLength() {
        return maxFragmentLength;
    }

    public void setMaxFragmentLength(MaxFragmentLength maxFragmentLengthConfig) {
        this.maxFragmentLength = maxFragmentLengthConfig;
    }

    public String getSniHostname() {
        return sniHostname;
    }

    public void setSniHostname(String SniHostname) {
        this.sniHostname = SniHostname;
    }

    public NamedCurve getKeyShareType() {
        return keyShareType;
    }

    public void setKeyShareType(NamedCurve keyShareType) {
        this.keyShareType = keyShareType;
    }

    public Boolean isDynamicWorkflow() {
        throw new UnsupportedOperationException("DynamicWorkflow is currently not supported.");
    }

    public void setDynamicWorkflow(Boolean dynamicWorkflow) {
        throw new UnsupportedOperationException("DynamicWorkflow is currently not supported.");
    }

    public List<CipherSuite> getDefaultClientSupportedCiphersuites() {
        return defaultClientSupportedCiphersuites;
    }

    public void setDefaultClientSupportedCiphersuites(List<CipherSuite> defaultClientSupportedCiphersuites) {
        this.defaultClientSupportedCiphersuites = defaultClientSupportedCiphersuites;
    }

    public final void setDefaultClientSupportedCiphersuites(CipherSuite... defaultClientSupportedCiphersuites) {
    }

    public Boolean isClientAuthentication() {
        return clientAuthentication;
    }

    public void setClientAuthentication(Boolean clientAuthentication) {
        this.clientAuthentication = clientAuthentication;
    }

    public List<SignatureAndHashAlgorithm> getSupportedSignatureAndHashAlgorithms() {
        return supportedSignatureAndHashAlgorithms;
    }

    public void setSupportedSignatureAndHashAlgorithms(
            List<SignatureAndHashAlgorithm> supportedSignatureAndHashAlgorithms) {
        this.supportedSignatureAndHashAlgorithms = supportedSignatureAndHashAlgorithms;
    }

    public final void setSupportedSignatureAndHashAlgorithms(
            SignatureAndHashAlgorithm... supportedSignatureAndHashAlgorithms) {
        this.supportedSignatureAndHashAlgorithms = new ArrayList(Arrays.asList(supportedSignatureAndHashAlgorithms));
    }

    public List<NamedCurve> getNamedCurves() {
        return namedCurves;
    }

    public void setNamedCurves(List<NamedCurve> namedCurves) {
        this.namedCurves = namedCurves;
    }

    public final void setNamedCurves(NamedCurve... namedCurves) {
        this.namedCurves = new ArrayList(Arrays.asList(namedCurves));
    }

    public List<ProtocolVersion> getSupportedVersions() {
        return supportedVersions;
    }

    public void setSupportedVersions(List<ProtocolVersion> supportedVersions) {
        this.supportedVersions = supportedVersions;
    }

    public final void setSupportedVersions(ProtocolVersion... supportedVersions) {
        this.supportedVersions = new ArrayList(Arrays.asList(supportedVersions));
    }

    public HeartbeatMode getHeartbeatMode() {
        return heartbeatMode;
    }

    public void setHeartbeatMode(HeartbeatMode heartbeatMode) {
        this.heartbeatMode = heartbeatMode;
    }

    public Boolean isAddECPointFormatExtension() {
        return addECPointFormatExtension;
    }

    public void setAddECPointFormatExtension(Boolean addECPointFormatExtension) {
        this.addECPointFormatExtension = addECPointFormatExtension;
    }

    public Boolean isAddEllipticCurveExtension() {
        return addEllipticCurveExtension;
    }

    public void setAddEllipticCurveExtension(Boolean addEllipticCurveExtension) {
        this.addEllipticCurveExtension = addEllipticCurveExtension;
    }

    public Boolean isAddHeartbeatExtension() {
        return addHeartbeatExtension;
    }

    public void setAddHeartbeatExtension(Boolean addHeartbeatExtension) {
        this.addHeartbeatExtension = addHeartbeatExtension;
    }

    public Boolean isAddMaxFragmentLengthExtenstion() {
        return addMaxFragmentLengthExtenstion;
    }

    public void setAddMaxFragmentLengthExtenstion(Boolean addMaxFragmentLengthExtenstion) {
        this.addMaxFragmentLengthExtenstion = addMaxFragmentLengthExtenstion;
    }

    public Boolean isAddServerNameIndicationExtension() {
        return addServerNameIndicationExtension;
    }

    public void setAddServerNameIndicationExtension(Boolean addServerNameIndicationExtension) {
        this.addServerNameIndicationExtension = addServerNameIndicationExtension;
    }

    public Boolean isAddSignatureAndHashAlgrorithmsExtension() {
        return addSignatureAndHashAlgrorithmsExtension;
    }

    public void setAddSignatureAndHashAlgrorithmsExtension(Boolean addSignatureAndHashAlgrorithmsExtension) {
        this.addSignatureAndHashAlgrorithmsExtension = addSignatureAndHashAlgrorithmsExtension;
    }

    public Boolean isAddSupportedVersionsExtension() {
        return addSupportedVersionsExtension;
    }

    public void setAddSupportedVersionsExtension(Boolean addSupportedVersionsExtension) {
        this.addSupportedVersionsExtension = addSupportedVersionsExtension;
    }

    public Boolean isAddKeyShareExtension() {
        return addKeyShareExtension;
    }

    public void setAddKeyShareExtension(Boolean addKeyShareExtension) {
        this.addKeyShareExtension = addKeyShareExtension;
    }

    public Boolean isAddEarlyDataExtension() {
        return addEarlyDataExtension;
    }

    public void setAddEarlyDataExtension(Boolean addEarlyDataExtension) {
        this.addEarlyDataExtension = addEarlyDataExtension;
    }

    public Boolean isAddPSKKeyExchangeModesExtension() {
        return addPSKKeyExchangeModesExtension;
    }

    public void setAddPSKKeyExchangeModesExtension(Boolean addPSKKeyExchangeModesExtension) {
        this.addPSKKeyExchangeModesExtension = addPSKKeyExchangeModesExtension;
    }

    public Boolean isAddPreSharedKeyExtension() {
        return addPreSharedKeyExtension;
    }

    public void setAddPreSharedKeyExtension(Boolean addPreSharedKeyExtension) {
        this.addPreSharedKeyExtension = addPreSharedKeyExtension;
    }

    public void setPSKKeyExchangeModes(List<PskKeyExchangeMode> pskKeyExchangeModes) {
        this.pskKeyExchangeModes = pskKeyExchangeModes;
    }

    public List<PskKeyExchangeMode> getPSKKeyExchangeModes() {
        return pskKeyExchangeModes;
    }

    public Integer getDefaultDTLSCookieLength() {
        return defaultDTLSCookieLength;
    }

    public void setDefaultDTLSCookieLength(Integer defaultDTLSCookieLength) {
        this.defaultDTLSCookieLength = defaultDTLSCookieLength;
    }

    public Integer getPaddingLength() {
        return paddingLength;
    }

    public void setPaddingLength(Integer paddingLength) {
        this.paddingLength = paddingLength;
    }

    public byte[] getKeySharePrivate() {
        return keySharePrivate;
    }

    public void setKeySharePrivate(byte[] keySharePrivate) {
        this.keySharePrivate = keySharePrivate;
    }

    public byte[] getTlsSessionTicket() {
        return tlsSessionTicket;
    }

    public void setTlsSessionTicket(byte[] tlsSessionTicket) {
        this.tlsSessionTicket = tlsSessionTicket;
    }

    public byte[] getDefaultSignedCertificateTimestamp() {
        return defaultSignedCertificateTimestamp;
    }

    public void setDefaultSignedCertificateTimestamp(byte[] defaultSignedCertificateTimestamp) {
        this.defaultSignedCertificateTimestamp = defaultSignedCertificateTimestamp;
    }

    public Boolean isAddSignedCertificateTimestampExtension() {
        return addSignedCertificateTimestampExtension;
    }

    public void setAddSignedCertificateTimestampExtension(Boolean addSignedCertificateTimestampExtension) {
        this.addSignedCertificateTimestampExtension = addSignedCertificateTimestampExtension;
    }

    public byte[] getDefaultClientRenegotiationInfo() {
        return defaultClientRenegotiationInfo;
    }

    public void setDefaultClientRenegotiationInfo(byte[] defaultClientRenegotiationInfo) {
        this.defaultClientRenegotiationInfo = defaultClientRenegotiationInfo;
    }

    public Boolean isAddRenegotiationInfoExtension() {
        return addRenegotiationInfoExtension;
    }

    public void setAddRenegotiationInfoExtension(Boolean addRenegotiationInfoExtension) {
        this.addRenegotiationInfoExtension = addRenegotiationInfoExtension;
    }

    public TokenBindingVersion getDefaultTokenBindingVersion() {
        return defaultTokenBindingVersion;
    }

    public void setDefaultTokenBindingVersion(TokenBindingVersion defaultTokenBindingVersion) {
        this.defaultTokenBindingVersion = defaultTokenBindingVersion;
    }

    public List<TokenBindingKeyParameters> getDefaultTokenBindingKeyParameters() {
        return defaultTokenBindingKeyParameters;
    }

    public void setDefaultTokenBindingKeyParameters(List<TokenBindingKeyParameters> defaultTokenBindingKeyParameters) {
        this.defaultTokenBindingKeyParameters = defaultTokenBindingKeyParameters;
    }

    public final void setDefaultTokenBindingKeyParameters(TokenBindingKeyParameters... defaultTokenBindingKeyParameters) {
        this.defaultTokenBindingKeyParameters = new ArrayList(Arrays.asList(defaultTokenBindingKeyParameters));
    }

    public Boolean isAddTokenBindingExtension() {
        return addTokenBindingExtension;
    }

    public void setAddTokenBindingExtension(Boolean addTokenBindingExtension) {
        this.addTokenBindingExtension = addTokenBindingExtension;
    }

    public Boolean isAddHttpsCookie() {
        return addHttpsCookie;
    }

    public void setAddHttpsCookie(Boolean addHttpsCookie) {
        this.addHttpsCookie = addHttpsCookie;
    }

    public String getDefaultHttpsCookieName() {
        return defaultHttpsCookieName;
    }

    public void setDefaultHttpsCookieName(String defaultHttpsCookieName) {
        this.defaultHttpsCookieName = defaultHttpsCookieName;
    }

    public String getDefaultHttpsCookieValue() {
        return defaultHttpsCookieValue;
    }

    public void setDefaultHttpsCookieValue(String defaultHttpsCookieValue) {
        this.defaultHttpsCookieValue = defaultHttpsCookieValue;
    }

    public KSEntry getDefaultServerKSEntry() {
        return new KSEntry(keyShareType, keySharePublic);
    }

    public CertificateStatusRequestType getCertificateStatusRequestExtensionRequestType() {
        return certificateStatusRequestExtensionRequestType;
    }

    public void setCertificateStatusRequestExtensionRequestType(
            CertificateStatusRequestType certificateStatusRequestExtensionRequestType) {
        this.certificateStatusRequestExtensionRequestType = certificateStatusRequestExtensionRequestType;
    }

    public byte[] getCertificateStatusRequestExtensionResponderIDList() {
        return certificateStatusRequestExtensionResponderIDList;
    }

    public void setCertificateStatusRequestExtensionResponderIDList(
            byte[] certificateStatusRequestExtensionResponderIDList) {
        this.certificateStatusRequestExtensionResponderIDList = certificateStatusRequestExtensionResponderIDList;
    }

    public byte[] getCertificateStatusRequestExtensionRequestExtension() {
        return certificateStatusRequestExtensionRequestExtension;
    }

    public void setCertificateStatusRequestExtensionRequestExtension(
            byte[] certificateStatusRequestExtensionRequestExtension) {
        this.certificateStatusRequestExtensionRequestExtension = certificateStatusRequestExtensionRequestExtension;
    }

    public byte[] getSessionId() {
        return sessionId;
    }

    public void setSessionId(byte[] sessionId) {
        this.sessionId = sessionId;
    }

    public byte[] getSecureRemotePasswordExtensionIdentifier() {
        return secureRemotePasswordExtensionIdentifier;
    }

    public void setSecureRemotePasswordExtensionIdentifier(byte[] secureRemotePasswordExtensionIdentifier) {
        this.secureRemotePasswordExtensionIdentifier = secureRemotePasswordExtensionIdentifier;
    }

    public List<SrtpProtectionProfiles> getSecureRealTimeTransportProtocolProtectionProfiles() {
        return secureRealTimeTransportProtocolProtectionProfiles;
    }

    public void setSecureRealTimeTransportProtocolProtectionProfiles(
            List<SrtpProtectionProfiles> secureRealTimeTransportProtocolProtectionProfiles) {
        this.secureRealTimeTransportProtocolProtectionProfiles = secureRealTimeTransportProtocolProtectionProfiles;
    }

    public byte[] getSecureRealTimeTransportProtocolMasterKeyIdentifier() {
        return secureRealTimeTransportProtocolMasterKeyIdentifier;
    }

    public void setSecureRealTimeTransportProtocolMasterKeyIdentifier(
            byte[] secureRealTimeTransportProtocolMasterKeyIdentifier) {
        this.secureRealTimeTransportProtocolMasterKeyIdentifier = secureRealTimeTransportProtocolMasterKeyIdentifier;
    }

    public UserMappingExtensionHintType getUserMappingExtensionHintType() {
        return userMappingExtensionHintType;
    }

    public void setUserMappingExtensionHintType(UserMappingExtensionHintType userMappingExtensionHintType) {
        this.userMappingExtensionHintType = userMappingExtensionHintType;
    }

    public List<CertificateType> getCertificateTypeDesiredTypes() {
        return certificateTypeDesiredTypes;
    }

    public void setCertificateTypeDesiredTypes(List<CertificateType> certificateTypeDesiredTypes) {
        this.certificateTypeDesiredTypes = certificateTypeDesiredTypes;
    }

    public List<CertificateType> getClientCertificateTypeDesiredTypes() {
        return clientCertificateTypeDesiredTypes;
    }

    public void setClientCertificateTypeDesiredTypes(List<CertificateType> clientCertificateTypeDesiredTypes) {
        this.clientCertificateTypeDesiredTypes = clientCertificateTypeDesiredTypes;
    }

    public List<CertificateType> getServerCertificateTypeDesiredTypes() {
        return serverCertificateTypeDesiredTypes;
    }

    public void setServerCertificateTypeDesiredTypes(List<CertificateType> serverCertificateTypeDesiredTypes) {
        this.serverCertificateTypeDesiredTypes = serverCertificateTypeDesiredTypes;
    }

    public List<AuthzDataFormat> getClientAuthzExtensionDataFormat() {
        return clientAuthzExtensionDataFormat;
    }

    public void setClientAuthzExtensionDataFormat(List<AuthzDataFormat> clientAuthzExtensionDataFormat) {
        this.clientAuthzExtensionDataFormat = clientAuthzExtensionDataFormat;
    }

    public Boolean isCertificateTypeExtensionMessageState() {
        return certificateTypeExtensionMessageState;
    }

    public void setCertificateTypeExtensionMessageState(Boolean certificateTypeExtensionMessageState) {
        this.certificateTypeExtensionMessageState = certificateTypeExtensionMessageState;
    }

    public List<AuthzDataFormat> getServerAuthzExtensionDataFormat() {
        return serverAuthzExtensionDataFormat;
    }

    public void setServerAuthzExtensionDataFormat(List<AuthzDataFormat> serverAuthzExtensionDataFormat) {
        this.serverAuthzExtensionDataFormat = serverAuthzExtensionDataFormat;
    }

    public List<TrustedAuthority> getTrustedCaIndicationExtensionAuthorties() {
        return trustedCaIndicationExtensionAuthorties;
    }

    public void setTrustedCaIndicationExtensionAuthorties(List<TrustedAuthority> trustedCaIndicationExtensionAuthorties) {
        this.trustedCaIndicationExtensionAuthorties = trustedCaIndicationExtensionAuthorties;
    }

    public Boolean isClientCertificateTypeExtensionMessageState() {
        return clientCertificateTypeExtensionMessageState;
    }

    public void setClientCertificateTypeExtensionMessageState(Boolean clientCertificateTypeExtensionMessageState) {
        this.clientCertificateTypeExtensionMessageState = clientCertificateTypeExtensionMessageState;
    }

    public Boolean isCachedInfoExtensionIsClientState() {
        return cachedInfoExtensionIsClientState;
    }

    public void setCachedInfoExtensionIsClientState(Boolean cachedInfoExtensionIsClientState) {
        this.cachedInfoExtensionIsClientState = cachedInfoExtensionIsClientState;
    }

    public List<CachedObject> getCachedObjectList() {
        return cachedObjectList;
    }

    public void setCachedObjectList(List<CachedObject> cachedObjectList) {
        this.cachedObjectList = cachedObjectList;
    }

    public List<RequestItemV2> getStatusRequestV2RequestList() {
        return statusRequestV2RequestList;
    }

    public void setStatusRequestV2RequestList(List<RequestItemV2> statusRequestV2RequestList) {
        this.statusRequestV2RequestList = statusRequestV2RequestList;
    }

    public Boolean isAddCertificateStatusRequestExtension() {
        return addCertificateStatusRequestExtension;
    }

    public void setAddCertificateStatusRequestExtension(Boolean addCertificateStatusRequestExtension) {
        this.addCertificateStatusRequestExtension = addCertificateStatusRequestExtension;
    }

    public Boolean isAddAlpnExtension() {
        return addAlpnExtension;
    }

    public void setAddAlpnExtension(Boolean addAlpnExtension) {
        this.addAlpnExtension = addAlpnExtension;
    }

    public Boolean isAddSRPExtension() {
        return addSRPExtension;
    }

    public void setAddSRPExtension(Boolean addSRPExtension) {
        this.addSRPExtension = addSRPExtension;
    }

    public Boolean isAddSRTPExtension() {
        return addSRTPExtension;
    }

    public void setAddSRTPExtension(Boolean addSRTPExtension) {
        this.addSRTPExtension = addSRTPExtension;
    }

    public Boolean isAddTruncatedHmacExtension() {
        return addTruncatedHmacExtension;
    }

    public void setAddTruncatedHmacExtension(Boolean addTruncatedHmacExtension) {
        this.addTruncatedHmacExtension = addTruncatedHmacExtension;
    }

    public Boolean isAddUserMappingExtension() {
        return addUserMappingExtension;
    }

    public void setAddUserMappingExtension(Boolean addUserMappingExtension) {
        this.addUserMappingExtension = addUserMappingExtension;
    }

    public Boolean isAddCertificateTypeExtension() {
        return addCertificateTypeExtension;
    }

    public void setAddCertificateTypeExtension(Boolean addCertificateTypeExtension) {
        this.addCertificateTypeExtension = addCertificateTypeExtension;
    }

    public Boolean isAddClientAuthzExtension() {
        return addClientAuthzExtension;
    }

    public void setAddClientAuthzExtension(Boolean addClientAuthzExtension) {
        this.addClientAuthzExtension = addClientAuthzExtension;
    }

    public Boolean isAddServerAuthzExtension() {
        return addServerAuthzExtension;
    }

    public void setAddServerAuthzExtension(Boolean addServerAuthzExtension) {
        this.addServerAuthzExtension = addServerAuthzExtension;
    }

    public Boolean isAddClientCertificateTypeExtension() {
        return addClientCertificateTypeExtension;
    }

    public void setAddClientCertificateTypeExtension(Boolean addClientCertificateTypeExtension) {
        this.addClientCertificateTypeExtension = addClientCertificateTypeExtension;
    }

    public Boolean isAddServerCertificateTypeExtension() {
        return addServerCertificateTypeExtension;
    }

    public void setAddServerCertificateTypeExtension(Boolean addServerCertificateTypeExtension) {
        this.addServerCertificateTypeExtension = addServerCertificateTypeExtension;
    }

    public Boolean isAddEncryptThenMacExtension() {
        return addEncryptThenMacExtension;
    }

    public void setAddEncryptThenMacExtension(Boolean addEncryptThenMacExtension) {
        this.addEncryptThenMacExtension = addEncryptThenMacExtension;
    }

    public Boolean isAddCachedInfoExtension() {
        return addCachedInfoExtension;
    }

    public void setAddCachedInfoExtension(Boolean addCachedInfoExtension) {
        this.addCachedInfoExtension = addCachedInfoExtension;
    }

    public Boolean isAddClientCertificateUrlExtension() {
        return addClientCertificateUrlExtension;
    }

    public void setAddClientCertificateUrlExtension(Boolean addClientCertificateUrlExtension) {
        this.addClientCertificateUrlExtension = addClientCertificateUrlExtension;
    }

    public Boolean isAddTrustedCaIndicationExtension() {
        return addTrustedCaIndicationExtension;
    }

    public void setAddTrustedCaIndicationExtension(Boolean addTrustedCaIndicationExtension) {
        this.addTrustedCaIndicationExtension = addTrustedCaIndicationExtension;
    }

    public Boolean isAddCertificateStatusRequestV2Extension() {
        return addCertificateStatusRequestV2Extension;
    }

    public void setAddCertificateStatusRequestV2Extension(Boolean addCertificateStatusRequestV2Extension) {
        this.addCertificateStatusRequestV2Extension = addCertificateStatusRequestV2Extension;
    }

    public List<CompressionMethod> getDefaultServerSupportedCompressionMethods() {
        return defaultServerSupportedCompressionMethods;
    }

    public void setDefaultServerSupportedCompressionMethods(
            List<CompressionMethod> defaultServerSupportedCompressionMethods) {
        this.defaultServerSupportedCompressionMethods = defaultServerSupportedCompressionMethods;
    }

    public void setDefaultServerSupportedCompressionMethods(
            CompressionMethod... defaultServerSupportedCompressionMethods) {
        this.defaultServerSupportedCompressionMethods = new ArrayList(
                Arrays.asList(defaultServerSupportedCompressionMethods));
    }

    public OutboundConnection getDefaultClientConnection() {
        return defaultClientConnection;
    }

    public void setDefaultClientConnection(OutboundConnection defaultClientConnection) {
        this.defaultClientConnection = defaultClientConnection;
    }

    public InboundConnection getDefaultServerConnection() {
        return defaultServerConnection;
    }

    public void setDefaultServerConnection(InboundConnection defaultServerConnection) {
        this.defaultServerConnection = defaultServerConnection;
    }

    public RunningModeType getDefaulRunningMode() {
        return defaultRunningMode;
    }

    public void setDefaulRunningMode(RunningModeType defaulRunningMode) {
        this.defaultRunningMode = defaulRunningMode;
    }

    public Boolean isStopActionsAfterFatal() {
        return stopActionsAfterFatal;
    }

    public void setStopActionsAfterFatal(Boolean stopActionsAfterFatal) {
        this.stopActionsAfterFatal = stopActionsAfterFatal;
    }

    public List<FilterType> getOutputFilters() {
        return outputFilters;
    }

    public void setOutputFilters(List<FilterType> outputFilters) {
        this.outputFilters = outputFilters;
    }

    public Boolean isApplyFiltersInPlace() {
        return applyFiltersInPlace;
    }

    public void setApplyFiltersInPlace(Boolean applyFiltersInPlace) {
        this.applyFiltersInPlace = applyFiltersInPlace;
    }

    public Boolean isFiltersKeepUserSettings() {
        return filtersKeepUserSettings;
    }

    public void setFiltersKeepUserSettings(Boolean filtersKeepUserSettings) {
        this.filtersKeepUserSettings = filtersKeepUserSettings;
    }

    public byte[] getDefaultClientApplicationTrafficSecret() {
        return defaultClientApplicationTrafficSecret;
    }

    public void setDefaultClientApplicationTrafficSecret(byte[] defaultClientApplicationTrafficSecret) {
        this.defaultClientApplicationTrafficSecret = defaultClientApplicationTrafficSecret;
    }

    public byte[] getDefaultServerApplicationTrafficSecret() {
        return defaultServerApplicationTrafficSecret;
    }

    public void setDefaultServerApplicationTrafficSecret(byte[] defaultServerApplicationTrafficSecret) {
        this.defaultServerApplicationTrafficSecret = defaultServerApplicationTrafficSecret;
    }

    /**
     * @return the earlyData
     */
    public byte[] getEarlyData() {
        return earlyData;
    }

    /**
     * @param earlyData
     *            the earlyData to set
     */
    public void setEarlyData(byte[] earlyData) {
        this.earlyData = earlyData;
    }

    /**
     * @return the PskSets
     */
    public List<PskSet> getPskSets() {
        return PskSets;
    }

    /**
     * @param PskSets
     *            the PskSets to set
     */
    public void setPskSets(List<PskSet> PskSets) {
        this.PskSets = PskSets;
    }

    /**
     * @return the psk
     */
    public byte[] getPsk() {
        return psk;
    }

    /**
     * @param psk
     *            the psk to set
     */
    public void setPsk(byte[] psk) {
        this.psk = psk;
    }

    /**
     * @return the defaultSessionTicketAgeAdd
     */
    public byte[] getDefaultSessionTicketAgeAdd() {
        return defaultSessionTicketAgeAdd;
    }

    /**
     * @param defaultSessionTicketAgeAdd
     *            the defaultSessionTicketAgeAdd to set
     */
    public void setDefaultSessionTicketAgeAdd(byte[] defaultSessionTicketAgeAdd) {
        this.defaultSessionTicketAgeAdd = defaultSessionTicketAgeAdd;
    }

    /**
     * @return the defaultSessionTicketNonce
     */
    public byte[] getDefaultSessionTicketNonce() {
        return defaultSessionTicketNonce;
    }

    /**
     * @param defaultSessionTicketNonce
     *            the defaultSessionTicketNonce to set
     */
    public void setDefaultSessionTicketNonce(byte[] defaultSessionTicketNonce) {
        this.defaultSessionTicketNonce = defaultSessionTicketNonce;
    }

    /**
     * @return the defaultSessionTicketIdentity
     */
    public byte[] getDefaultSessionTicketIdentity() {
        return defaultSessionTicketIdentity;
    }

    /**
     * @param defaultSessionTicketIdentity
     *            the defaultSessionTicketIdentity to set
     */
    public void setDefaultSessionTicketIdentity(byte[] defaultSessionTicketIdentity) {
        this.defaultSessionTicketIdentity = defaultSessionTicketIdentity;
    }

    /**
     * @return the clientEarlyTrafficSecret
     */
    public byte[] getClientEarlyTrafficSecret() {
        return clientEarlyTrafficSecret;
    }

    /**
     * @param clientEarlyTrafficSecret
     *            the clientEarlyTrafficSecret to set
     */
    public void setClientEarlyTrafficSecret(byte[] clientEarlyTrafficSecret) {
        this.clientEarlyTrafficSecret = clientEarlyTrafficSecret;
    }

    /**
     * @return the earlySecret
     */
    public byte[] getEarlySecret() {
        return earlySecret;
    }

    /**
     * @param earlySecret
     *            the earlySecret to set
     */
    public void setEarlySecret(byte[] earlySecret) {
        this.earlySecret = earlySecret;
    }

    /**
     * @return the earlyDataCipherSuite
     */
    public CipherSuite getEarlyDataCipherSuite() {
        return earlyDataCipherSuite;
    }

    /**
     * @param earlyDataCipherSuite
     *            the earlyDataCipherSuite to set
     */
    public void setEarlyDataCipherSuite(CipherSuite earlyDataCipherSuite) {
        this.earlyDataCipherSuite = earlyDataCipherSuite;
    }

    /**
     * @return the earlyDataPsk
     */
    public byte[] getEarlyDataPsk() {
        return earlyDataPsk;
    }

    /**
     * @param earlyDataPsk
     *            the earlyDataPsk to set
     */
    public void setEarlyDataPsk(byte[] earlyDataPsk) {
        this.earlyDataPsk = earlyDataPsk;
    }

    /**
     * @return the usePsk
     */
    public Boolean isUsePsk() {
        return usePsk;
    }

    /**
     * @param usePsk
     *            the usePsk to set
     */
    public void setUsePsk(Boolean usePsk) {
        this.usePsk = usePsk;
    }

    public String[] getAlpnAnnouncedProtocols() {
        return alpnAnnouncedProtocols;
    }

    public void setAlpnAnnouncedProtocols(String[] alpnAnnouncedProtocols) {
        this.alpnAnnouncedProtocols = alpnAnnouncedProtocols;
    }

    public NamedCurve getDefaultEcCertificateCurve() {
        return defaultEcCertificateCurve;
    }

    public void setDefaultEcCertificateCurve(NamedCurve defaultEcCertificateCurve) {
        this.defaultEcCertificateCurve = defaultEcCertificateCurve;
    }

    public BigInteger getDefaultClientRSAModulus() {
        return defaultClientRSAModulus;
    }

    public void setDefaultClientRSAModulus(BigInteger defaultClientRSAModulus) {
        this.defaultClientRSAModulus = defaultClientRSAModulus;
    }

    public BigInteger getDefaultClientDhGenerator() {
        return defaultClientDhGenerator;
    }

    public void setDefaultClientDhGenerator(BigInteger defaultClientDhGenerator) {
        this.defaultClientDhGenerator = defaultClientDhGenerator;
    }

    public BigInteger getDefaultClientDhModulus() {
        return defaultClientDhModulus;
    }

    public void setDefaultClientDhModulus(BigInteger defaultClientDhModulus) {
        this.defaultClientDhModulus = defaultClientDhModulus;
    }

    public Boolean getIgnoreDtlsRetransmits() {
        return ignoreDtlsRetransmits;
    }

    public void setIgnoreDtlsRetransmits(Boolean ignoreDtlsRetransmits) {
        this.ignoreDtlsRetransmits = ignoreDtlsRetransmits;
    }
}
