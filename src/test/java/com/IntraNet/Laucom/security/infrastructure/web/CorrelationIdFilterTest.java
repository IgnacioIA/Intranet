package com.IntraNet.Laucom.security.infrastructure.web;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/** REQ-AUTH-024. */
class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    void generatesANewCorrelationId_whenNoHeaderIsPresent() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        String correlationId = (String) request.getAttribute(CorrelationIdFilter.REQUEST_ATTRIBUTE_NAME);
        assertThat(correlationId).isNotBlank();
        assertThat(response.getHeader(CorrelationIdFilter.HEADER_NAME)).isEqualTo(correlationId);
        verify(chain).doFilter(request, response);
    }

    @Test
    void reusesTheIncomingHeader_whenItHasASafeFormat() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/login");
        request.addHeader(CorrelationIdFilter.HEADER_NAME, "client-supplied-id-123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, mock(FilterChain.class));

        assertThat(request.getAttribute(CorrelationIdFilter.REQUEST_ATTRIBUTE_NAME)).isEqualTo("client-supplied-id-123");
        assertThat(response.getHeader(CorrelationIdFilter.HEADER_NAME)).isEqualTo("client-supplied-id-123");
    }

    @Test
    void rejectsAnUnsafeIncomingHeader_andGeneratesANewOne() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/login");
        request.addHeader(CorrelationIdFilter.HEADER_NAME, "line1\nFAKE-LOG-LINE-INJECTED");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, mock(FilterChain.class));

        String correlationId = (String) request.getAttribute(CorrelationIdFilter.REQUEST_ATTRIBUTE_NAME);
        assertThat(correlationId).doesNotContain("\n").isNotEqualTo("line1\nFAKE-LOG-LINE-INJECTED");
    }

    @Test
    void rejectsAnOverlyLongIncomingHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/login");
        request.addHeader(CorrelationIdFilter.HEADER_NAME, "a".repeat(500));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, mock(FilterChain.class));

        String correlationId = (String) request.getAttribute(CorrelationIdFilter.REQUEST_ATTRIBUTE_NAME);
        assertThat(correlationId).hasSizeLessThan(100);
    }

    @Test
    void removesTheMdcEntry_afterTheChainCompletes() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, mock(FilterChain.class));

        assertThat(MDC.get("correlationId")).isNull();
    }
}
