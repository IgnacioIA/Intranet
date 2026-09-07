package com.IntraNet.Laucom.security.infrastructure.web;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;

import static org.assertj.core.api.Assertions.assertThat;

/** SPEC-AUTH-005 §12, Fase 19 (correlationId en el cuerpo de error). */
class ProblemDetailAuthenticationEntryPointTest {

    private final ProblemDetailAuthenticationEntryPoint entryPoint = new ProblemDetailAuthenticationEntryPoint();

    @Test
    void writesA401ProblemJsonBody_includingTheCorrelationId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/auth/me");
        request.setAttribute(CorrelationIdFilter.REQUEST_ATTRIBUTE_NAME, "trace-123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, new BadCredentialsException("irrelevant"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).startsWith("application/problem+json");
        String body = response.getContentAsString();
        assertThat(body).contains("invalid-access-token").contains("\"correlationId\":\"trace-123\"");
    }

    @Test
    void doesNotFail_whenNoCorrelationIdWasSet() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/auth/me");
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, new BadCredentialsException("irrelevant"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("\"correlationId\":null");
    }
}
