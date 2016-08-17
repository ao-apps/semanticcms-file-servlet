/*
 * semanticcms-file-servlet - Files nested within SemanticCMS pages and elements in a Servlet environment.
 * Copyright (C) 2013, 2014, 2015, 2016  AO Industries, Inc.
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

import com.aoindustries.io.TempFileList;
import com.aoindustries.io.buffer.AutoTempFileWriter;
import com.aoindustries.io.buffer.BufferResult;
import com.aoindustries.io.buffer.BufferWriter;
import com.aoindustries.io.buffer.SegmentedWriter;
import com.aoindustries.servlet.filter.TempFileContext;
import com.semanticcms.core.model.ElementContext;
import com.semanticcms.core.servlet.CaptureLevel;
import com.semanticcms.core.servlet.Element;
import com.semanticcms.core.servlet.PageContext;
import com.semanticcms.file.servlet.impl.FileImpl;
import java.io.IOException;
import java.io.Writer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.SkipPageException;

public class File extends Element<com.semanticcms.file.model.File> {

	public File(
		ServletContext servletContext,
		HttpServletRequest request,
		HttpServletResponse response,
		String path
	) {
		super(
			servletContext,
			request,
			response,
			new com.semanticcms.file.model.File()
		);
		element.setPath(path);
	}

	public File(
		ServletContext servletContext,
		HttpServletRequest request,
		HttpServletResponse response,
		String book,
		String path
	) {
		this(servletContext, request, response, path);
		element.setBook(book);
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
	public File(String book, String path) {
		this(path);
		element.setBook(book);
	}

	@Override
	public File id(String id) {
		super.id(id);
		return this;
	}

	public File book(String book) {
		element.setBook(book);
		return this;
	}

	public File hidden(boolean hidden) {
		element.setHidden(hidden);
		return this;
	}

	private BufferResult writeMe;
	@Override
	protected void doBody(CaptureLevel captureLevel, Body<? super com.semanticcms.file.model.File> body) throws ServletException, IOException, SkipPageException {
		super.doBody(captureLevel, body);
		BufferWriter capturedOut;
		if(captureLevel == CaptureLevel.BODY) {
			// Enable temp files if temp file context active
			capturedOut = TempFileContext.wrapTempFileList(
				new SegmentedWriter(),
				request,
				// Java 1.8: AutoTempFileWriter::new
				new TempFileContext.Wrapper<BufferWriter>() {
					@Override
					public BufferWriter call(BufferWriter original, TempFileList tempFileList) {
						return new AutoTempFileWriter(original, tempFileList);
					}
				}
			);
		} else {
			capturedOut = null;
		}
		try {
			FileImpl.writeFileImpl(
				servletContext,
				request,
				response,
				capturedOut,
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
