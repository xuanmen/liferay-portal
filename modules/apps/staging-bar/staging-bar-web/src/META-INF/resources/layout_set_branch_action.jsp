<%--
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
--%>

<%@ include file="/init.jsp" %>

<%
ResultRow row = (ResultRow)request.getAttribute(WebKeys.SEARCH_CONTAINER_RESULT_ROW);

LayoutSetBranch layoutSetBranch = (LayoutSetBranch)row.getObject();

long currentLayoutSetBranchId = GetterUtil.getLong((String)request.getAttribute("view_layout_set_branches.jsp-currentLayoutSetBranchId"));
%>

<liferay-ui:icon-menu icon="<%= StringPool.BLANK %>" message="<%= StringPool.BLANK %>">
	<c:if test="<%= LayoutSetBranchPermissionUtil.contains(permissionChecker, layoutSetBranch, ActionKeys.UPDATE) %>">
		<portlet:renderURL var="editURL">
			<portlet:param name="mvcPath" value="/edit_layout_set_branch.jsp" />
			<portlet:param name="redirect" value="<%= currentURL %>" />
			<portlet:param name="groupId" value="<%= String.valueOf(layoutSetBranch.getGroupId()) %>" />
			<portlet:param name="layoutSetBranchId" value="<%= String.valueOf(layoutSetBranch.getLayoutSetBranchId()) %>" />
		</portlet:renderURL>

		<%
		String taglibURL = "javascript:Liferay.StagingBar.updateBranch({uri: '" + HtmlUtil.escapeJS(editURL) + "', dialogTitle: '" + HtmlUtil.escapeJS(LanguageUtil.get(request, "update-site-pages-variation")) + "'});";
		%>

		<liferay-ui:icon
			iconCssClass="icon-edit"
			message="edit"
			url="<%= taglibURL %>"
		/>
	</c:if>

	<c:if test="<%= LayoutSetBranchPermissionUtil.contains(permissionChecker, layoutSetBranch, ActionKeys.PERMISSIONS) %>">
		<liferay-security:permissionsURL
			modelResource="<%= LayoutSetBranch.class.getName() %>"
			modelResourceDescription="<%= layoutSetBranch.getName() %>"
			resourcePrimKey="<%= String.valueOf(layoutSetBranch.getLayoutSetBranchId()) %>"
			var="permissionsURL"
			windowState="<%= LiferayWindowState.POP_UP.toString() %>"
		/>

		<liferay-ui:icon
			iconCssClass="icon-lock"
			message="permissions"
			method="get"
			url="<%= permissionsURL %>"
			useDialog="<%= true %>"
		/>
	</c:if>

	<c:if test="<%= LayoutSetBranchPermissionUtil.contains(permissionChecker, layoutSetBranch, ActionKeys.MERGE) %>">
		<portlet:renderURL var="mergeURL">
			<portlet:param name="mvcPath" value="/merge_layout_set_branch.jsp" />
			<portlet:param name="redirect" value="<%= currentURL %>" />
			<portlet:param name="groupId" value="<%= String.valueOf(layoutSetBranch.getGroupId()) %>" />
			<portlet:param name="privateLayout" value="<%= String.valueOf(layoutSetBranch.isPrivateLayout()) %>" />
			<portlet:param name="layoutSetBranchId" value="<%= String.valueOf(layoutSetBranch.getLayoutSetBranchId()) %>" />
		</portlet:renderURL>

		<%
		String taglibURL = "javascript:Liferay.StagingBar.mergeBranch({uri: '" + HtmlUtil.escapeJS(mergeURL) + "', dialogTitle: '" + HtmlUtil.escapeJS(LanguageUtil.get(request, "merge-site-pages-variation")) + "'});";
		%>

		<liferay-ui:icon
			iconCssClass="icon-copy"
			message="merge"
			url="<%= taglibURL %>"
		/>
	</c:if>

	<c:if test="<%= !layoutSetBranch.isMaster() && LayoutSetBranchPermissionUtil.contains(permissionChecker, layoutSetBranch, ActionKeys.DELETE) %>">
		<portlet:actionURL name="deleteLayoutSetBranch" var="deleteURL">
			<portlet:param name="redirect" value="<%= currentURL %>" />
			<portlet:param name="groupId" value="<%= String.valueOf(layoutSetBranch.getGroupId()) %>" />
			<portlet:param name="layoutSetBranchId" value="<%= String.valueOf(layoutSetBranch.getLayoutSetBranchId()) %>" />
			<portlet:param name="currentLayoutSetBranchId" value="<%= String.valueOf(currentLayoutSetBranchId) %>" />
		</portlet:actionURL>

		<liferay-ui:icon-delete
			url="<%= deleteURL %>"
		/>
	</c:if>
</liferay-ui:icon-menu>