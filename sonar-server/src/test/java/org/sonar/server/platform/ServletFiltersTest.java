/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.server.platform;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InOrder;
import org.sonar.api.web.ServletFilter;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class ServletFiltersTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void initAndDestroyFilters() throws Exception {
    ServletFilter filter = mock(ServletFilter.class);
    FilterConfig config = mock(FilterConfig.class);
    ServletFilters filters = new ServletFilters();
    filters.init(config, Arrays.asList(filter));

    assertThat(filters.getFilters()).containsOnly(filter);
    verify(filter).init(config);

    filters.destroy();
    verify(filter).destroy();
  }

  @Test
  public void initFilters_propagate_failure() throws Exception {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("foo");

    ServletFilter filter = mock(ServletFilter.class);
    doThrow(new IllegalStateException("foo")).when(filter).init(any(FilterConfig.class));

    FilterConfig config = mock(FilterConfig.class);
    ServletFilters filters = new ServletFilters();
    filters.init(config, Arrays.asList(filter));
  }

  @Test
  public void doFilter_no_filters() throws Exception {
    FilterConfig config = mock(FilterConfig.class);
    ServletFilters filters = new ServletFilters();
    filters.init(config, Collections.<ServletFilter>emptyList());

    ServletRequest request = mock(HttpServletRequest.class);
    ServletResponse response = mock(HttpServletResponse.class);
    FilterChain chain = mock(FilterChain.class);
    filters.doFilter(request, response, chain);

    verify(chain).doFilter(request, response);
  }

  @Test
  public void doFilter_keep_same_order() throws Exception {
    TrueFilter filter1 = new TrueFilter();
    TrueFilter filter2 = new TrueFilter();

    ServletFilters filters = new ServletFilters();
    filters.init(mock(FilterConfig.class), Arrays.<ServletFilter>asList(filter1, filter2));

    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getRequestURI()).thenReturn("/foo/bar");
    when(request.getContextPath()).thenReturn("");
    ServletResponse response = mock(HttpServletResponse.class);
    FilterChain chain = mock(FilterChain.class);
    filters.doFilter(request, response, chain);

    assertThat(filter1.count).isEqualTo(1);
    assertThat(filter2.count).isEqualTo(2);
  }

  private static final class TrueFilter extends ServletFilter {
    private static int globalCount = 0;
    int count = 0;
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
      globalCount++;
      count = globalCount;
      filterChain.doFilter(servletRequest, servletResponse);
    }

    public void destroy() {
    }

  }
}
