/*
 * semanticcms-file-servlet - Files nested within SemanticCMS pages and elements in a Servlet environment.
 * Copyright (C) 2013, 2014, 2015, 2016, 2017, 2020  AO Industries, Inc.
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
 * along with semanticcms-file-servlet.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.semanticcms.file.servlet;

import com.aoindustries.html.servlet.HtmlEE;
import com.aoindustries.io.buffer.BufferResult;
import com.aoindustries.io.buffer.BufferWriter;
import com.aoindustries.lang.Strings;
import com.aoindustries.taglib.AutoEncodingBufferedTag;
import com.semanticcms.core.model.ElementContext;
import com.semanticcms.core.servlet.CaptureLevel;
import com.semanticcms.core.servlet.Element;
import com.semanticcms.core.servlet.PageContext;
import com.semanticcms.core.servlet.PageRefResolver;
import com.semanticcms.file.servlet.impl.FileImpl;
import java.io.IOException;
import java.io.Writer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.SkipPageException;

public class File extends Element<com.semanticcms.file.model.File> {

	private final String path;

	private String book;

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
		String book,
		String path
	) {
		this(servletContext, request, response, element, path);
		this.book = Strings.nullIfEmpty(book);
	}

	public File(
		ServletContext servletContext,
		HttpServletRequest request,
		HttpServletResponse response,
		String book,
		String path
	) {
		this(servletContext, request, response, path);
		this.book = Strings.nullIfEmpty(book);
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
		String book,
		String path
	) {
		this(element, path);
		this.book = Strings.nullIfEmpty(book);
	}

	/**
	 * Creates a new file in the current page context.
	 *
	 * @see  PageContext
	 */
	public File(String book, String path) {
		this(path);
		this.book = Strings.nullIfEmpty(book);
	}

	@Override
	public File id(String id) {
		super.id(id);
		return this;
	}

	public File book(String book) {
		this.book = Strings.nullIfEmpty(book);
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
		element.setPageRef(
			PageRefResolver.getPageRef(
				servletContext,
				request,
				book,
				path
			)
		);
		super.doBody(captureLevel, body);
		BufferWriter capturedOut;
		if(captureLevel == CaptureLevel.BODY) {
			capturedOut = AutoEncodingBufferedTag.newBufferWriter(request);
		} else {
			capturedOut = null;
		}
		try {
			FileImpl.writeFileImpl(
				servletContext,
				request,
				response,
				(capturedOut == null) ? null : HtmlEE.get(servletContext, request, response, capturedOut),
				element
			);
		} finally {
			if(capturedOut != null) capturedOut.close();
		}
		writeMe = capturedOut==null ? null : capturedOut.getResult();
	}

	@Override
	public void writeTo(Writer out, ElementContext context) throws IOException {
		if(writeMe != null) writeMe.writeTo(out);
	}
}
