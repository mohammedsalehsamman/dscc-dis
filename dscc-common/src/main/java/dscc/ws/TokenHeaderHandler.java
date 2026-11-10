package dscc.ws;

import javax.xml.namespace.QName;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.handler.Handler;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Client-side SOAP handler: adds the login token to the header of every outgoing SOAP message:
 * <pre>&lt;soap:Header&gt;&lt;auth:Token xmlns:auth="http://auth.dscc/"&gt;...&lt;/auth:Token&gt;&lt;/soap:Header&gt;</pre>
 * The server-side handler reads it, so the business operations keep their original signatures.
 */
public class TokenHeaderHandler implements SOAPHandler<SOAPMessageContext> {

    public static final String NAMESPACE = "http://auth.dscc/";
    public static final QName TOKEN = new QName(NAMESPACE, "Token", "auth");

    private final Supplier<String> tokenSupplier;

    public TokenHeaderHandler(Supplier<String> tokenSupplier) {
        this.tokenSupplier = tokenSupplier;
    }

    /** Installs this handler on a JAX-WS port (proxy). */
    @SuppressWarnings("rawtypes")
    public static void install(Object port, Supplier<String> tokenSupplier) {
        BindingProvider bp = (BindingProvider) port;
        List<Handler> chain = bp.getBinding().getHandlerChain();
        chain.add(new TokenHeaderHandler(tokenSupplier));
        bp.getBinding().setHandlerChain(chain);
    }

    /** Sets connect/read timeouts (milliseconds) on a JAX-WS port. */
    public static void timeouts(Object port, int connectMs, int readMs) {
        BindingProvider bp = (BindingProvider) port;
        bp.getRequestContext().put("com.sun.xml.ws.connect.timeout", connectMs);
        bp.getRequestContext().put("com.sun.xml.ws.request.timeout", readMs);
    }

    @Override
    public boolean handleMessage(SOAPMessageContext context) {
        boolean outbound = (Boolean) context.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);
        String token = tokenSupplier.get();
        if (outbound && token != null && !token.isEmpty()) {
            try {
                SOAPMessage msg = context.getMessage();
                SOAPEnvelope env = msg.getSOAPPart().getEnvelope();
                SOAPHeader header = env.getHeader() != null ? env.getHeader() : env.addHeader();
                header.addHeaderElement(TOKEN).addTextNode(token);
                msg.saveChanges();
            } catch (SOAPException e) {
                throw new RuntimeException("Cannot add token header", e);
            }
        }
        return true;
    }

    @Override
    public boolean handleFault(SOAPMessageContext context) {
        return true;
    }

    @Override
    public void close(MessageContext context) {
    }

    @Override
    public Set<QName> getHeaders() {
        return Collections.singleton(TOKEN);
    }
}
