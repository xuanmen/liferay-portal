/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.portal.backgroundtask;

import com.liferay.portal.kernel.cache.Lifecycle;
import com.liferay.portal.kernel.cache.ThreadLocalCacheManager;
import com.liferay.portal.kernel.cluster.ClusterInvokeThreadLocal;
import com.liferay.portal.kernel.test.rule.DeleteAfterTestRun;
import com.liferay.portal.kernel.test.util.TestPropsValues;
import com.liferay.portal.kernel.test.util.UserTestUtil;
import com.liferay.portal.kernel.util.CentralizedThreadLocal;
import com.liferay.portal.kernel.util.GroupThreadLocal;
import com.liferay.portal.kernel.util.LocaleThreadLocal;
import com.liferay.portal.kernel.util.MapUtil;
import com.liferay.portal.model.User;
import com.liferay.portal.security.auth.CompanyThreadLocal;
import com.liferay.portal.security.auth.PrincipalThreadLocal;
import com.liferay.portal.security.permission.PermissionChecker;
import com.liferay.portal.security.permission.PermissionCheckerFactory;

import java.io.Serializable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;

import org.mockito.Mockito;

/**
 * @author Michael C. Han
 */
public abstract class BaseBackgroundTaskTestCase {

	@Before
	public void setUp() throws Exception {
		_backgroundTaskThreadLocalManagerImpl =
			new BackgroundTaskThreadLocalManagerImpl();

		PermissionCheckerFactory permissionCheckerFactory = Mockito.mock(
			PermissionCheckerFactory.class);

		PermissionChecker permissionChecker = Mockito.mock(
			PermissionChecker.class);

		User user = Mockito.mock(User.class);

		Mockito.when(
			permissionCheckerFactory.create(user)
		).thenReturn(
			permissionChecker
		);

		_backgroundTaskThreadLocalManagerImpl.setPermissionCheckerFactory(
			permissionCheckerFactory);

		_companyId = TestPropsValues.getCompanyId();
		_clusterInvokeEnabled = true;
		_defaultLocale = Locale.US;
		_groupId = TestPropsValues.getGroupId();
		_siteDefaultLocale = Locale.CANADA;
		_themeDisplayLocale = Locale.FRANCE;
		_user = UserTestUtil.addUser();

		_principalName = String.valueOf(_user.getUserId());
	}

	@After
	public void tearDown() throws Exception {
		resetThreadLocals();
	}

	protected void assertThreadLocalValues() {
		Assert.assertEquals(
			Long.valueOf(_companyId), CompanyThreadLocal.getCompanyId());
		Assert.assertEquals(
			_clusterInvokeEnabled, ClusterInvokeThreadLocal.isEnabled());
		Assert.assertEquals(
			_defaultLocale, LocaleThreadLocal.getDefaultLocale());
		Assert.assertEquals(
			Long.valueOf(_groupId), GroupThreadLocal.getGroupId());
		Assert.assertEquals(_principalName, PrincipalThreadLocal.getName());
		Assert.assertEquals(
			_siteDefaultLocale, LocaleThreadLocal.getSiteDefaultLocale());
		Assert.assertEquals(
			_themeDisplayLocale, LocaleThreadLocal.getThemeDisplayLocale());
		Assert.assertNull(PrincipalThreadLocal.getPassword());
	}

	protected void assertThreadLocalValues(
		Map<String, Serializable> threadLocalValues) {

		Assert.assertTrue(MapUtil.isNotEmpty(threadLocalValues));
		Assert.assertEquals(7, threadLocalValues.size());
		Assert.assertEquals(_companyId, threadLocalValues.get("companyId"));
		Assert.assertEquals(
			_clusterInvokeEnabled, threadLocalValues.get("clusterInvoke"));
		Assert.assertEquals(
			_defaultLocale, threadLocalValues.get("defaultLocale"));
		Assert.assertEquals(_groupId, threadLocalValues.get("groupId"));
		Assert.assertEquals(
			_principalName, threadLocalValues.get("principalName"));
		Assert.assertNull(threadLocalValues.get("principalPassword"));
		Assert.assertEquals(
			_siteDefaultLocale, threadLocalValues.get("siteDefaultLocale"));
		Assert.assertEquals(
			_themeDisplayLocale, threadLocalValues.get("themeDisplayLocale"));
	}

	protected void initalizeThreadLocals() {
		CompanyThreadLocal.setCompanyId(_companyId);
		ClusterInvokeThreadLocal.setEnabled(true);
		GroupThreadLocal.setGroupId(_groupId);
		LocaleThreadLocal.setDefaultLocale(_defaultLocale);
		LocaleThreadLocal.setSiteDefaultLocale(_siteDefaultLocale);
		LocaleThreadLocal.setThemeDisplayLocale(_themeDisplayLocale);
		PrincipalThreadLocal.setName(_principalName);
	}

	protected HashMap<String, Serializable> initializeThreadLocalValues() {
		HashMap<String, Serializable> threadLocalValues = new HashMap<>();

		threadLocalValues.put("companyId", _companyId);
		threadLocalValues.put("clusterInvoke", _clusterInvokeEnabled);
		threadLocalValues.put("defaultLocale", _defaultLocale);
		threadLocalValues.put("groupId", _groupId);
		threadLocalValues.put("principalName", _principalName);
		threadLocalValues.put("siteDefaultLocale", _siteDefaultLocale);
		threadLocalValues.put("themeDisplayLocale", _themeDisplayLocale);

		return threadLocalValues;
	}

	protected void resetThreadLocals() {
		ThreadLocalCacheManager.clearAll(Lifecycle.REQUEST);

		CentralizedThreadLocal.clearShortLivedThreadLocals();
	}

	protected BackgroundTaskThreadLocalManagerImpl
		_backgroundTaskThreadLocalManagerImpl;

	private boolean _clusterInvokeEnabled;
	private long _companyId;
	private Locale _defaultLocale;
	private long _groupId;
	private String _principalName;
	private Locale _siteDefaultLocale;
	private Locale _themeDisplayLocale;

	@DeleteAfterTestRun
	private User _user;

}