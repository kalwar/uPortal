/**
 * Licensed to Jasig under one or more contributor license
 * agreements. See the NOTICE file distributed with this work
 * for additional information regarding copyright ownership.
 * Jasig licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a
 * copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jasig.portal.portlet.container.services;

import java.util.Collection;

import javax.portlet.PortletMode;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.pluto.container.PortletContainer;
import org.apache.pluto.container.PortletRenderResponseContext;
import org.apache.pluto.container.PortletURLProvider;
import org.apache.pluto.container.PortletWindow;
import org.apache.pluto.container.PortletURLProvider.TYPE;
import org.apache.pluto.driver.AttributeKeys;
import org.jasig.portal.portlet.registry.IPortletEntityRegistry;
import org.jasig.portal.portlet.registry.IPortletWindowRegistry;
import org.jasig.portal.portlet.url.IPortletUrlSyntaxProvider;

/**
 * @author Nicholas Blair, npblair@wisc.edu
 * @version $Revision$
 */
class PortletRenderResponseContextImpl extends PortletMimeResponseContextImpl implements
PortletRenderResponseContext
{
	public PortletRenderResponseContextImpl(PortletContainer container, HttpServletRequest containerRequest,
			HttpServletResponse containerResponse, PortletWindow window, IPortletUrlSyntaxProvider portletUrlSyntaxProvider,
			IPortletWindowRegistry portletWindowRegistry, IPortletEntityRegistry portletEntityRegistry) {
		super(container, containerRequest, containerResponse, window, portletUrlSyntaxProvider, portletWindowRegistry, portletEntityRegistry);
	}

	public void setNextPossiblePortletModes(Collection<PortletMode> portletModes)
	{
		// not supported
	}

	public void setTitle(String title)
	{
		if (!isClosed())
		{
			getServletRequest().setAttribute(AttributeKeys.PORTLET_TITLE, title);
		}
	}

	@Override
	public PortletURLProvider getPortletURLProvider(TYPE type) {
		// TODO Auto-generated method stub
		return null;
	}
}
