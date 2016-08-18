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

import com.semanticcms.core.model.Element;
import com.semanticcms.core.model.Page;
import com.semanticcms.core.model.PageRef;
import com.semanticcms.core.servlet.CaptureLevel;
import com.semanticcms.core.servlet.CapturePage;
import com.semanticcms.file.model.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

final public class FileUtils {

	public static boolean hasFile(
		ServletContext servletContext,
		HttpServletRequest request,
		HttpServletResponse response,
		Page page,
		boolean recursive
	) throws ServletException, IOException {
		return hasFileRecursive(
			servletContext,
			request,
			response,
			page,
			recursive,
			recursive ? new HashSet<PageRef>() : null
		);
	}

	private static boolean hasFileRecursive(
		ServletContext servletContext,
		HttpServletRequest request,
		HttpServletResponse response,
		Page page,
		boolean recursive,
		Set<PageRef> seenPages
	) throws ServletException, IOException {
		for(Element e : page.getElements()) {
			if((e instanceof File) && !((File)e).isHidden()) {
				return true;
			}
		}
		if(recursive) {
			seenPages.add(page.getPageRef());
			for(PageRef childRef : page.getChildPages()) {
				if(
					// Child not in missing book
					childRef.getBook() != null
					// Not already seen
					&& !seenPages.contains(childRef)
				) {
					if(
						hasFileRecursive(
							servletContext,
							request,
							response,
							CapturePage.capturePage(servletContext, request, response, childRef, CaptureLevel.META),
							recursive,
							seenPages
						)
					) {
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * Make no instances.
	 */
	private FileUtils() {
	}
}
