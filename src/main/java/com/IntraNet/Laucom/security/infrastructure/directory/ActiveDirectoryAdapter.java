package com.IntraNet.Laucom.security.infrastructure.directory;

import com.IntraNet.Laucom.security.domain.port.IdentityDirectoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.naming.AuthenticationException;
import javax.naming.Context;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Adapter de {@link IdentityDirectoryPort} contra Active Directory vía LDAP simple bind
 * (ADR-002). Opcional: solo se activa con {@code ad.enabled=true} (un proyecto sin AD no lo
 * configura). Ver ADR-004 (objectGUID), ADR-017 (fail closed ante fallo de grupos).
 *
 * <p><b>Limitación de verificación conocida (igual naturaleza que
 * {@code UserAuthorizationPersistenceIT} con Testcontainers/MySQL):</b> este adapter no puede
 * verificarse mediante una prueba automatizada en este entorno por no existir aquí un servidor
 * Active Directory (ni una imagen Docker estándar y confiable que lo reproduzca fielmente,
 * a diferencia de MySQL). La lógica pura que sí es determinística y verificable sin directorio
 * real —conversión de {@code objectGUID}, extracción de identificador de grupo desde un DN,
 * escape de filtro LDAP— se extrajo a {@link ActiveDirectoryAttributeCodec} y se testea allí.
 * Este adapter debe validarse contra un Active Directory real (de prueba o staging) antes de
 * su primer despliegue — no se declara verificado por esta implementación.</p>
 */
@Component
@ConditionalOnProperty(name = "ad.enabled", havingValue = "true")
public class ActiveDirectoryAdapter implements IdentityDirectoryPort {

    private static final Logger log = LoggerFactory.getLogger(ActiveDirectoryAdapter.class);
    private static final String[] RETURNING_ATTRIBUTES = {"objectGUID", "memberOf", "displayName", "mail"};

    private final String ldapUrl;
    private final String baseDn;
    private final String domain;
    private final String serviceAccountPrincipal;
    private final String serviceAccountPassword;

    public ActiveDirectoryAdapter(@Value("${ad.url}") String ldapUrl,
                                   @Value("${ad.base-dn}") String baseDn,
                                   @Value("${ad.domain}") String domain,
                                   @Value("${ad.service-account.principal}") String serviceAccountPrincipal,
                                   @Value("${ad.service-account.password}") String serviceAccountPassword) {
        this.ldapUrl = ldapUrl;
        this.baseDn = baseDn;
        this.domain = domain;
        this.serviceAccountPrincipal = serviceAccountPrincipal;
        this.serviceAccountPassword = serviceAccountPassword;
    }

    @Override
    public DirectoryAuthenticationResult authenticate(String username, char[] password) {
        String userPrincipal = username.contains("@") ? username : username + "@" + domain;

        DirContext userBind;
        try {
            userBind = bind(userPrincipal, password);
        } catch (AuthenticationException e) {
            return new CredentialsRejected();
        } catch (NamingException e) {
            log.warn("Active Directory inalcanzable durante autenticación (AD_CONNECTION_FAILURE)", e);
            return new DirectoryUnavailable();
        }
        closeQuietly(userBind); // el bind del usuario solo sirve para verificar la contraseña.

        try {
            return lookupIdentity(username);
        } catch (NamingException e) {
            log.warn("Fallo al obtener/evaluar grupos AD tras credenciales válidas (AD_LOOKUP_FAILURE)", e);
            return new GroupLookupFailed(username);
        }
    }

    private DirectoryAuthenticationResult lookupIdentity(String username) throws NamingException {
        DirContext serviceCtx = bind(serviceAccountPrincipal, serviceAccountPassword.toCharArray());
        try {
            SearchControls controls = new SearchControls();
            controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            controls.setReturningAttributes(RETURNING_ATTRIBUTES);
            String filter = "(&(objectClass=user)(sAMAccountName="
                    + ActiveDirectoryAttributeCodec.escapeForFilter(username) + "))";

            NamingEnumeration<SearchResult> results = serviceCtx.search(baseDn, filter, controls);
            if (!results.hasMore()) {
                throw new NameNotFoundException("Usuario no encontrado en AD tras bind exitoso: " + username);
            }
            Attributes attrs = results.next().getAttributes();

            byte[] rawGuid = (byte[]) attrs.get("objectGUID").get();
            String externalId = ActiveDirectoryAttributeCodec.toStableString(rawGuid);
            String displayName = singleStringAttribute(attrs, "displayName");
            String email = singleStringAttribute(attrs, "mail");
            Set<String> groups = extractGroupIdentifiers(attrs.get("memberOf"));

            return new Authenticated(externalId, displayName, email, groups);
        } finally {
            closeQuietly(serviceCtx);
        }
    }

    private Set<String> extractGroupIdentifiers(Attribute memberOf) throws NamingException {
        Set<String> groups = new LinkedHashSet<>();
        if (memberOf == null) {
            return groups;
        }
        NamingEnumeration<?> values = memberOf.getAll();
        while (values.hasMore()) {
            String dn = (String) values.next();
            groups.add(ActiveDirectoryAttributeCodec.extractGroupIdentifier(dn));
        }
        return groups;
    }

    private String singleStringAttribute(Attributes attrs, String name) throws NamingException {
        Attribute attribute = attrs.get(name);
        return attribute == null ? null : (String) attribute.get();
    }

    private DirContext bind(String principal, char[] password) throws NamingException {
        Hashtable<String, Object> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, ldapUrl);
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.SECURITY_PRINCIPAL, principal);
        env.put(Context.SECURITY_CREDENTIALS, new String(password));
        return new InitialDirContext(env);
    }

    private void closeQuietly(DirContext ctx) {
        try {
            ctx.close();
        } catch (NamingException e) {
            log.debug("Fallo no crítico al cerrar el contexto LDAP", e);
        }
    }
}
