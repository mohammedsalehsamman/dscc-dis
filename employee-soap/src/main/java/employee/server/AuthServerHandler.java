package employee.server;

import dscc.util.ServerLog;
import dscc.ws.TokenHeaderHandler;

import javax.xml.namespace.QName;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFactory;
import javax.xml.soap.SOAPFault;
import javax.xml.soap.SOAPHeader;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;
import javax.xml.ws.soap.SOAPFaultException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

/**
 * Server-side SOAP handler for EmployeeService (authentication).
 * For every incoming request it:
 * <ol>
 *   <li>reads the &lt;auth:Token&gt; element from the SOAP header,</li>
 *   <li>looks up the session in the database,</li>
 *   <li>rejects the request with a SOAP Fault if the token is missing/invalid,</li>
 *   <li>otherwise stores the user in the message context so the service can check the role.</li>
 * </ol>
 */
public class AuthServerHandler implements SOAPHandler<SOAPMessageContext> {

    public static final String AUTH_KEY = "dscc.auth";

    private final Database db;
    private final UserDao users;

    public AuthServerHandler(Database db) {
        this.db = db;
        this.users = new UserDao(db);
    }

    @Override
    public boolean handleMessage(SOAPMessageContext context) {
        boolean outbound = (Boolean) context.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);
        if (outbound) return true;
        ServerLog.request();
        try {
            String token = readToken(context.getMessage().getSOAPHeader());
            String operation = operationName(context.getMessage().getSOAPBody());
            AuthContext auth;
            synchronized (db) {
                auth = users.findSession(token);
            }
            if (auth == null) {
                ServerLog.info("REJECTED " + operation + " : " + (token == null ? "no token" : "invalid/expired token"));
                throw fault(token == null
                        ? "Authentication required: please login first (missing auth:Token header)"
                        : "Session expired or invalid token: please login again");
            }
            context.put(AUTH_KEY, auth);
            context.setScope(AUTH_KEY, MessageContext.Scope.APPLICATION);
            return true;
        } catch (SOAPException | java.sql.SQLException e) {
            throw fault("Server error while checking the token: " + e.getMessage());
        }
    }

    private static String readToken(SOAPHeader header) {
        if (header == null) return null;
        Iterator<?> it = header.getChildElements(TokenHeaderHandler.TOKEN);
        if (!it.hasNext()) return null;
        String value = ((javax.xml.soap.SOAPElement) it.next()).getValue();
        return value == null ? null : value.trim();
    }

    private static String operationName(SOAPBody body) {
        if (body == null) return "?";
        Iterator<?> it = body.getChildElements();
        while (it.hasNext()) {
            Object o = it.next();
            if (o instanceof javax.xml.soap.SOAPElement) return ((javax.xml.soap.SOAPElement) o).getLocalName();
        }
        return "?";
    }

    private static SOAPFaultException fault(String message) {
        try {
            SOAPFault f = SOAPFactory.newInstance().createFault(message,
                    new QName(SOAPConstants.URI_NS_SOAP_ENVELOPE, "Client"));
            return new SOAPFaultException(f);
        } catch (SOAPException e) {
            throw new RuntimeException(message, e);
        }
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
        return Collections.singleton(TokenHeaderHandler.TOKEN);
    }
}
