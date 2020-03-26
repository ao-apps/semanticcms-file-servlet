/*
 * semanticcms-file-servlet - Files nested within SemanticCMS pages and elements in a Servlet environment.
 * Copyright (C) 2013, 2014, 2015, 2016, 2017, 2019, 2020  AO Industries, Inc.
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
package com.semanticcms.file.servlet.impl;

import static com.aoindustries.encoding.JavaScriptInXhtmlAttributeEncoder.javaScriptInXhtmlAttributeEncoder;
import com.aoindustries.encoding.MediaWriter;
import static com.aoindustries.encoding.TextInXhtmlAttributeEncoder.encodeTextInXhtmlAttribute;
import com.aoindustries.html.Html;
import com.aoindustries.io.NoCloseWriter;
import com.aoindustries.io.buffer.BufferResult;
import com.aoindustries.lang.Strings;
import com.aoindustries.net.Path;
import com.aoindustries.net.URIEncoder;
import com.aoindustries.servlet.lastmodified.LastModifiedServlet;
import com.semanticcms.core.model.NodeBodyWriter;
import com.semanticcms.core.model.PageRef;
import com.semanticcms.core.servlet.Headers;
import com.semanticcms.core.servlet.PageIndex;
import com.semanticcms.core.servlet.SemanticCMS;
import com.semanticcms.core.servlet.ServletElementContext;
import com.semanticcms.core.servlet.impl.LinkImpl;
import com.semanticcms.file.servlet.FileUtils;
import java.io.File;
import java.io.IOException;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.SkipPageException;

final public class FileImpl {

	@FunctionalInterface
	public static interface FileImplBody<E extends Throwable> {
		void doBody(boolean discard) throws E, IOException, SkipPageException;
	}

	/**
	 * @param html Optional, when null meta data is verified but no output is generated
	 */
	public static void writeFileImpl(
		ServletContext servletContext,
		HttpServletRequest request,
		HttpServletResponse response,
		Html html,
		com.semanticcms.file.model.File element
	) throws ServletException, IOException, SkipPageException {
		PageRef pageRef = element.getPageRef();
		// Find the local file, assuming relative to CVSWORK directory
		File resourceFile = pageRef.getResourceFile(false, true);
		// Check if is directory and filename matches required pattern for directory
		boolean isDirectory;
		if(resourceFile == null) {
			// In other book and not available, assume directory when ends in path separator
			isDirectory = pageRef.getPath().endsWith(Path.SEPARATOR_STRING);
		} else {
			// In accessible book, use attributes
			isDirectory = resourceFile.isDirectory();
			// When is a directory, must end in slash
			if(
				isDirectory
				&& !pageRef.getPath().endsWith(Path.SEPARATOR_STRING)
			) {
				throw new IllegalArgumentException(
					"References to directories must end in slash ("
					+ Path.SEPARATOR_CHAR
					+ "): "
					+ pageRef
				);
			}
		}
		if(html != null) {
			BufferResult body = element.getBody();
			boolean hasBody = body.getLength() != 0;
			// Determine if local file opening is allowed
			final boolean isOpenFileAllowed = FileUtils.isOpenFileAllowed(servletContext, request);
			final boolean isExporting = Headers.isExporting(request);

			String elemId = element.getId();
			html.out.write("<a");
			if(elemId != null) {
				html.out.write(" id=\"");
				encodeTextInXhtmlAttribute(
					// TODO: To appendIdInPage, review other uses, too
					PageIndex.getRefIdInPage(request, element.getPage(), elemId),
					html.out
				);
				html.out.append('"');
			}
			if(!hasBody) {
				// TODO: Class like core:link, where providing empty class disables automatic class selection here
				SemanticCMS semanticCMS = SemanticCMS.getInstance(servletContext);
				String linkCssClass = semanticCMS.getLinkCssClass(element);
				if(linkCssClass != null) {
					html.out.write(" class=\"");
					encodeTextInXhtmlAttribute(linkCssClass, html.out);
					html.out.write('"');
				}
			}
			html.out.write(" href=\"");
			if(
				isOpenFileAllowed
				&& resourceFile != null
				&& !isExporting
			) {
				encodeTextInXhtmlAttribute(
					response.encodeURL(resourceFile.toURI().toASCIIString()),
					html.out
				);
			} else {
				final String urlPath;
				if(
					resourceFile != null
					&& !isDirectory
					// Check for header disabling auto last modified
					&& !"false".equalsIgnoreCase(request.getHeader(LastModifiedServlet.LAST_MODIFIED_HEADER_NAME))
				) {
					// Include last modified on file
					urlPath = request.getContextPath()
						+ pageRef.getServletPath()
						+ "?" + LastModifiedServlet.LAST_MODIFIED_PARAMETER_NAME
						+ "=" + LastModifiedServlet.encodeLastModified(resourceFile.lastModified())
					;
				} else {
					urlPath = request.getContextPath()
						+ pageRef.getServletPath()
					;
				}
				encodeTextInXhtmlAttribute(
					response.encodeURL(URIEncoder.encodeURI(urlPath)),
					html.out
				);
			}
			html.out.write('"');
			if(
				isOpenFileAllowed
				&& resourceFile != null
				&& !isExporting
			) {
				html.out.write(" onclick=\"");
				try (MediaWriter onclick = new MediaWriter(html.encodingContext, javaScriptInXhtmlAttributeEncoder, new NoCloseWriter(html.out))) {
					onclick.append("semanticcms_openfile_servlet.openFile(").text(pageRef.getBook().getName()).append(", ").text(pageRef.getPath()).append("); return false;");
				}
				html.out.write('"');
			}
			html.out.write('>');
			if(!hasBody) {
				if(resourceFile == null) {
					LinkImpl.writeBrokenPathInXhtml(pageRef, html.out);
				} else {
					html.text(resourceFile.getName());
					if(isDirectory) html.text(Path.SEPARATOR_CHAR);
				}
			} else {
				body.writeTo(new NodeBodyWriter(element, html.out, new ServletElementContext(servletContext, request, response)));
			}
			html.out.write("</a>");
			if(!hasBody && resourceFile != null && !isDirectory) {
				html.out.write(" (");
				html.text(Strings.getApproximateSize(resourceFile.length()));
				html.out.write(')');
			}
		}
	}

	/**
	 * Make no instances.
	 */
	private FileImpl() {
	}
}
