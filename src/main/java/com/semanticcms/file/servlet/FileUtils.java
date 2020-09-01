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
package com.semanticcms.file.servlet;

import com.semanticcms.core.model.Element;
import com.semanticcms.core.model.Page;
import com.semanticcms.core.model.PageRef;
import com.semanticcms.core.servlet.CaptureLevel;
import com.semanticcms.core.servlet.CapturePage;
import com.semanticcms.file.model.File;
import com.semanticcms.openfile.servlet.OpenFile;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Logger;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

final public class FileUtils {

	private static final Logger logger = Logger.getLogger(FileUtils.class.getName());

	private static class IsOpenFileAllowedLock {}
	private static final IsOpenFileAllowedLock isOpenFileAllowedLock = new IsOpenFileAllowedLock();
	private static boolean openFileNotFound;

	/**
	 * Determines if local file opening is allowed.
	 *
	 * Uses reflection to avoid hard dependency on semanticcms-openfile-servlet.
	 *
	 * @see  OpenFile#isAllowed(javax.servlet.ServletContext, javax.servlet.ServletRequest)
	 */
	public static boolean isOpenFileAllowed(ServletContext servletContext, ServletRequest request) throws ServletException {
		synchronized(isOpenFileAllowedLock) {
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
			} catch(NoSuchMethodException | IllegalAccessException | InvocationTargetException e) { // TODO: ReflectiveOperationException
				throw new ServletException(e);
			}
		}
	}

	public static boolean hasFile(
		ServletContext servletContext,
		HttpServletRequest request,
		HttpServletResponse response,
		Page page,
		final boolean recursive
	) throws ServletException, IOException {
		return CapturePage.traversePagesAnyOrder(
			servletContext,
			request,
			response,
			page,
			CaptureLevel.META,
			(Page p) -> {
				for(Element e : p.getElements()) {
					if((e instanceof File) && !((File)e).isHidden()) {
						return true;
					}
				}
				return null;
			},
			(Page p) -> recursive ? p.getChildRefs() : null,
			// Child not in missing book
			(PageRef childPage) -> childPage.getBook() != null
		) != null;
	}

	/**
	 * Make no instances.
	 */
	private FileUtils() {
	}
}
