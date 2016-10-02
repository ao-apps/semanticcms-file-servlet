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
package com.semanticcms.file.servlet.impl;

import static com.aoindustries.encoding.JavaScriptInXhtmlAttributeEncoder.encodeJavaScriptInXhtmlAttribute;
import com.aoindustries.encoding.NewEncodingUtils;
import static com.aoindustries.encoding.TextInXhtmlAttributeEncoder.encodeTextInXhtmlAttribute;
import static com.aoindustries.encoding.TextInXhtmlEncoder.encodeTextInXhtml;
import com.aoindustries.io.buffer.BufferResult;
import com.aoindustries.net.UrlUtils;
import com.aoindustries.servlet.http.LastModifiedServlet;
import com.aoindustries.util.StringUtility;
import com.semanticcms.core.model.NodeBodyWriter;
import com.semanticcms.core.model.PageRef;
import com.semanticcms.core.servlet.Headers;
import com.semanticcms.core.servlet.PageIndex;
import com.semanticcms.core.servlet.SemanticCMS;
import com.semanticcms.core.servlet.ServletElementContext;
import com.semanticcms.core.servlet.impl.LinkImpl;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Logger;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.SkipPageException;

final public class FileImpl {

	private static final Logger logger = Logger.getLogger(FileImpl.class.getName());

	public static interface FileImplBody<E extends Throwable> {
		void doBody(boolean discard) throws E, IOException, SkipPageException;
	}

	private static class IsAllowedLock {}
	private static final IsAllowedLock isAllowedLock = new IsAllowedLock();
	private static boolean openFileNotFound;

	/**
	 * Using reflection to avoid hard dependency on semanticcms-openfile-servlet.
	 */
	private static boolean isAllowed(ServletContext servletContext, ServletRequest request) throws ServletException {
		synchronized(isAllowedLock) {
			// If failed once, fail quickly the second time
			if(openFileNotFound) return false;
			try {
				Class<?> openFileClass = Class.forName("com.semanticcms.openfile.servlet.OpenFile");
				Method isAllowedMethod = openFileClass.getMethod("isAllowed", ServletContext.class, ServletRequest.class);
				return (Boolean)isAllowedMethod.invoke(null, servletContext, request);
			} catch(ClassNotFoundException e) {
				logger.warning("Unable to open local files, if desktop integration is desired, add the semanticcms-openfile-servlet package.");
				openFileNotFound = true;
				return false;
			} catch(NoSuchMethodException e) {
				throw new ServletException(e);
			} catch(IllegalAccessException e) {
				throw new ServletException(e);
			} catch(InvocationTargetException e) {
				throw new ServletException(e);
			}
		}
	}

	/**
	 * @param out Optional, when null meta data is verified but no output is generated
	 */
	public static void writeFileImpl(
		ServletContext servletContext,
		HttpServletRequest request,
		HttpServletResponse response,
		Writer out,
		com.semanticcms.file.model.File element
	) throws ServletException, IOException, SkipPageException {
		PageRef pageRef = element.getPageRef();
		// Find the local file, assuming relative to CVSWORK directory
		File resourceFile = pageRef.getResourceFile(false, true);
		// Check if is directory and filename matches required pattern for directory
		boolean isDirectory;
		if(resourceFile == null) {
			// In other book and not available, assume directory when ends in path separator
			isDirectory = pageRef.getPath().endsWith(com.semanticcms.file.model.File.SEPARATOR_STRING);
		} else {
			// In accessible book, use attributes
			isDirectory = resourceFile.isDirectory();
			// When is a directory, must end in slash
			if(
				isDirectory
				&& !pageRef.getPath().endsWith(com.semanticcms.file.model.File.SEPARATOR_STRING)
			) {
				throw new IllegalArgumentException(
					"References to directories must end in slash ("
					+ com.semanticcms.file.model.File.SEPARATOR_CHAR
					+ "): "
					+ pageRef
				);
			}
		}
		if(out != null) {
			BufferResult body = element.getBody();
			boolean hasBody = body.getLength() != 0;
			// Determine if local file opening is allowed
			final boolean isAllowed = isAllowed(servletContext, request);
			final boolean isExporting = Headers.isExporting(request);

			String elemId = element.getId();
			out.write("<a");
			if(elemId != null) {
				out.write(" id=\"");
				encodeTextInXhtmlAttribute(
					PageIndex.getRefIdInPage(servletContext, request, element.getPage(), elemId),
					out
				);
				out.append('"');
			}
			if(!hasBody) {
				// TODO: Class like p:link, where providing empty class disables automatic class selection here
				SemanticCMS semanticCMS = SemanticCMS.getInstance(servletContext);
				String linkCssClass = semanticCMS.getLinkCssClass(element);
				if(linkCssClass != null) {
					out.write(" class=\"");
					encodeTextInXhtmlAttribute(linkCssClass, out);
					out.write('"');
				}
			}
			out.write(" href=\"");
			if(
				isAllowed
				&& resourceFile != null
				&& !isExporting
			) {
				encodeTextInXhtmlAttribute(resourceFile.toURI().toString(), out);
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
					response.encodeURL(
						UrlUtils.encodeUrlPath(
							urlPath,
							response.getCharacterEncoding()
						)
					),
					out
				);
			}
			out.write('"');
			if(
				isAllowed
				&& resourceFile != null
				&& !isExporting
			) {
				out.write(" onclick=\"");
				encodeJavaScriptInXhtmlAttribute("semanticcms_openfile_servlet.openFile(\"", out);
				NewEncodingUtils.encodeTextInJavaScriptInXhtmlAttribute(pageRef.getBook().getName(), out);
				encodeJavaScriptInXhtmlAttribute("\", \"", out);
				NewEncodingUtils.encodeTextInJavaScriptInXhtmlAttribute(pageRef.getPath(), out);
				encodeJavaScriptInXhtmlAttribute("\"); return false;", out);
				out.write('"');
			}
			out.write('>');
			if(!hasBody) {
				if(resourceFile == null) {
					LinkImpl.writeBrokenPathInXhtml(pageRef, out);
				} else {
					encodeTextInXhtml(resourceFile.getName(), out);
					if(isDirectory) encodeTextInXhtml(com.semanticcms.file.model.File.SEPARATOR_CHAR, out);
				}
			} else {
				body.writeTo(new NodeBodyWriter(element, out, new ServletElementContext(servletContext, request, response)));
			}
			out.write("</a>");
			if(!hasBody && resourceFile != null && !isDirectory) {
				out.write(" (");
				encodeTextInXhtml(StringUtility.getApproximateSize(resourceFile.length()), out);
				out.write(')');
			}
		}
	}

	/**
	 * Make no instances.
	 */
	private FileImpl() {
	}
}
