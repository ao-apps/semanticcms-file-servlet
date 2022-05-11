/*
 * semanticcms-file-servlet - Files nested within SemanticCMS pages and elements in a Servlet environment.
 * Copyright (C) 2013, 2014, 2015, 2016, 2017, 2020, 2021, 2022  AO Industries, Inc.
 *     support@aoindustries.com
 *     7262 Bull Pen Cir
 *     Mobile, AL 36695
 *
 * This file is part of semanticcms-file-servlet.
 *
 * semanticcms-file-servlet is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * semanticcms-file-servlet is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with semanticcms-file-servlet.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.semanticcms.file.servlet;

import com.aoapps.encoding.taglib.EncodingBufferedTag;
import com.aoapps.html.servlet.DocumentEE;
import com.aoapps.io.buffer.BufferResult;
import com.aoapps.io.buffer.BufferWriter;
import com.aoapps.lang.Strings;
import com.aoapps.net.DomainName;
import com.aoapps.net.Path;
import com.semanticcms.core.controller.ResourceRefResolver;
import com.semanticcms.core.controller.SemanticCMS;
import com.semanticcms.core.model.ElementContext;
import com.semanticcms.core.model.ResourceRef;
import com.semanticcms.core.pages.CaptureLevel;
import com.semanticcms.core.pages.local.PageContext;
import com.semanticcms.core.servlet.Element;
import com.semanticcms.file.renderer.html.FileHtmlRenderer;
import java.io.IOException;
import java.io.Writer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.SkipPageException;

/**
 * A file nested within SemanticCMS pages and elements in a Servlet environment.
 */
public class File extends Element<com.semanticcms.file.model.File> {

  private final String path;

  private DomainName domain;
  private Path book;

  /**
   * Creates a new file.
   */
  public File(
      ServletContext servletContext,
      HttpServletRequest request,
      HttpServletResponse response,
      com.semanticcms.file.model.File element,
      String path
  ) {
    super(
        servletContext,
        request,
        response,
        element
    );
    this.path = Strings.nullIfEmpty(path);
  }

  /**
   * Creates a new file.
   */
  public File(
      ServletContext servletContext,
      HttpServletRequest request,
      HttpServletResponse response,
      String path
  ) {
    this(
        servletContext,
        request,
        response,
        new com.semanticcms.file.model.File(),
        path
    );
  }

  public File(
      ServletContext servletContext,
      HttpServletRequest request,
      HttpServletResponse response,
      com.semanticcms.file.model.File element,
      Path book,
      String path
  ) {
    this(servletContext, request, response, element, path);
    this.book = book;
  }

  public File(
      ServletContext servletContext,
      HttpServletRequest request,
      HttpServletResponse response,
      Path book,
      String path
  ) {
    this(servletContext, request, response, path);
    this.book = book;
  }

  public File(
      ServletContext servletContext,
      HttpServletRequest request,
      HttpServletResponse response,
      com.semanticcms.file.model.File element,
      DomainName domain,
      Path book,
      String path
  ) {
    this(servletContext, request, response, element, book, path);
    this.domain = domain;
  }

  public File(
      ServletContext servletContext,
      HttpServletRequest request,
      HttpServletResponse response,
      DomainName domain,
      Path book,
      String path
  ) {
    this(servletContext, request, response, book, path);
    this.domain = domain;
  }

  /**
   * Creates a new file in the current page context.
   *
   * @see  PageContext
   */
  public File(
      com.semanticcms.file.model.File element,
      String path
  ) {
    this(
        PageContext.getServletContext(),
        PageContext.getRequest(),
        PageContext.getResponse(),
        element,
        path
    );
  }

  /**
   * Creates a new file in the current page context.
   *
   * @see  PageContext
   */
  public File(String path) {
    this(
        PageContext.getServletContext(),
        PageContext.getRequest(),
        PageContext.getResponse(),
        path
    );
  }

  /**
   * Creates a new file in the current page context.
   *
   * @see  PageContext
   */
  public File(
      com.semanticcms.file.model.File element,
      Path book,
      String path
  ) {
    this(element, path);
    this.book = book;
  }

  /**
   * Creates a new file in the current page context.
   *
   * @see  PageContext
   */
  public File(Path book, String path) {
    this(path);
    this.book = book;
  }

  /**
   * Creates a new file in the current page context.
   *
   * @see  PageContext
   */
  public File(
      com.semanticcms.file.model.File element,
      DomainName domain,
      Path book,
      String path
  ) {
    this(element, book, path);
    this.domain = domain;
  }

  /**
   * Creates a new file in the current page context.
   *
   * @see  PageContext
   */
  public File(DomainName domain, Path book, String path) {
    this(book, path);
    this.domain = domain;
  }

  @Override
  public File id(String id) {
    super.id(id);
    return this;
  }

  public File domain(DomainName domain) {
    this.domain = domain;
    return this;
  }

  public File book(Path book) {
    this.book = book;
    return this;
  }

  public File hidden(boolean hidden) {
    element.setHidden(hidden);
    return this;
  }

  private BufferResult writeMe;

  @Override
  protected void doBody(CaptureLevel captureLevel, Body<? super com.semanticcms.file.model.File> body) throws ServletException, IOException, SkipPageException {
    // Resolve file now to catch problems earlier even in meta mode
    ResourceRef resourceRef = ResourceRefResolver.getResourceRef(
        servletContext,
        request,
        domain,
        book,
        path
    );
    element.setResource(
        SemanticCMS.getInstance(servletContext).getBook(resourceRef.getBookRef()).getResources(),
        resourceRef
    );
    super.doBody(captureLevel, body);
    BufferWriter capturedOut;
    if (captureLevel == CaptureLevel.BODY) {
      capturedOut = EncodingBufferedTag.newBufferWriter(request);
    } else {
      capturedOut = null;
    }
    try {
      FileHtmlRenderer.writeFileImpl(
          servletContext,
          request,
          response,
          (capturedOut == null) ? null : new DocumentEE(servletContext, request, response, capturedOut),
          element
      );
    } finally {
      if (capturedOut != null) {
        capturedOut.close();
      }
    }
    writeMe = capturedOut == null ? null : capturedOut.getResult();
  }

  @Override
  public void writeTo(Writer out, ElementContext context) throws IOException {
    if (writeMe != null) {
      writeMe.writeTo(out);
    }
  }
}
