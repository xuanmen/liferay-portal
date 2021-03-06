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

package com.liferay.journal.verify;

import com.liferay.journal.model.JournalArticle;
import com.liferay.journal.model.JournalArticleConstants;
import com.liferay.journal.model.JournalArticleImage;
import com.liferay.journal.model.JournalArticleResource;
import com.liferay.journal.model.JournalContentSearch;
import com.liferay.journal.model.JournalFolder;
import com.liferay.journal.service.JournalArticleImageLocalService;
import com.liferay.journal.service.JournalArticleLocalService;
import com.liferay.journal.service.JournalArticleResourceLocalService;
import com.liferay.journal.service.JournalContentSearchLocalService;
import com.liferay.journal.service.JournalFolderLocalService;
import com.liferay.journal.service.configuration.configurator.JournalServiceConfigurator;
import com.liferay.journal.util.comparator.ArticleVersionComparator;
import com.liferay.portal.kernel.dao.db.DB;
import com.liferay.portal.kernel.dao.db.DBFactoryUtil;
import com.liferay.portal.kernel.dao.jdbc.DataAccess;
import com.liferay.portal.kernel.dao.orm.ActionableDynamicQuery;
import com.liferay.portal.kernel.dao.orm.DynamicQuery;
import com.liferay.portal.kernel.dao.orm.Property;
import com.liferay.portal.kernel.dao.orm.PropertyFactoryUtil;
import com.liferay.portal.kernel.dao.orm.QueryUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.util.CharPool;
import com.liferay.portal.kernel.util.FriendlyURLNormalizerUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.HtmlUtil;
import com.liferay.portal.kernel.util.HttpUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.workflow.WorkflowConstants;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.service.ResourceLocalService;
import com.liferay.portal.util.PortalInstances;
import com.liferay.portal.verify.VerifyLayout;
import com.liferay.portlet.PortletPreferencesFactoryUtil;
import com.liferay.portlet.asset.model.AssetEntry;
import com.liferay.portlet.asset.service.AssetEntryLocalService;
import com.liferay.portlet.documentlibrary.service.DLAppLocalService;
import com.liferay.portlet.dynamicdatamapping.NoSuchStructureException;
import com.liferay.portlet.dynamicdatamapping.util.DDMFieldsCounter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import javax.portlet.PortletPreferences;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Alexander Chow
 * @author Shinn Lok
 */
@Component(immediate = true, service = JournalServiceVerifyProcess.class)
public class JournalServiceVerifyProcess extends VerifyLayout {

	public static final long DEFAULT_GROUP_ID = 14;

	public static final int NUM_OF_ARTICLES = 5;

	@Activate
	@Override
	protected void doVerify() throws Exception {
		verifyArticleAssets();
		verifyArticleContents();
		verifyArticleLayouts();
		verifyArticleStructures();
		verifyContentSearch();
		verifyFolderAssets();
		verifyOracleNewLine();
		verifyPermissions();
		verifyTree();
		verifyURLTitle();
	}

	@Reference(unbind = "-")
	protected void setAssetEntryLocalService(
		AssetEntryLocalService assetEntryLocalService) {

		_assetEntryLocalService = assetEntryLocalService;
	}

	@Reference(unbind = "-")
	protected void setDLAppLocalService(DLAppLocalService dlAppLocalService) {
		_dlAppLocalService = dlAppLocalService;
	}

	@Reference(unbind = "-")
	protected void setJournalArticleImageLocalService(
		JournalArticleImageLocalService journalArticleImageLocalService) {

		_journalArticleImageLocalService = journalArticleImageLocalService;
	}

	@Reference(unbind = "-")
	protected void setJournalArticleLocalService(
		JournalArticleLocalService journalArticleLocalService) {

		_journalArticleLocalService = journalArticleLocalService;
	}

	@Reference(unbind = "-")
	protected void setJournalArticleResourceLocalService(
		JournalArticleResourceLocalService journalArticleResourceLocalService) {

		_journalArticleResourceLocalService =
			journalArticleResourceLocalService;
	}

	@Reference(unbind = "-")
	protected void setJournalContentSearchLocalService(
		JournalContentSearchLocalService journalContentSearchLocalService) {

		_journalContentSearchLocalService = journalContentSearchLocalService;
	}

	@Reference(unbind = "-")
	protected void setJournalFolderLocalService(
		JournalFolderLocalService journalFolderLocalService) {

		_journalFolderLocalService = journalFolderLocalService;
	}

	@Reference(unbind = "-")
	protected void setJournalServiceConfigurator(
		JournalServiceConfigurator JournalServiceConfigurator) {
	}

	@Reference(unbind = "-")
	protected void setResourceLocalService(
		ResourceLocalService resourceLocalService) {

		_resourceLocalService = resourceLocalService;
	}

	protected void updateContentSearch(long groupId, String portletId)
		throws Exception {

		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			con = DataAccess.getUpgradeOptimizedConnection();

			ps = con.prepareStatement(
				"select preferences from PortletPreferences inner join " +
					"Layout on PortletPreferences.plid = Layout.plid where " +
						"groupId = ? and portletId = ?");

			ps.setLong(1, groupId);
			ps.setString(2, portletId);

			rs = ps.executeQuery();

			while (rs.next()) {
				String xml = rs.getString("preferences");

				PortletPreferences portletPreferences =
					PortletPreferencesFactoryUtil.fromDefaultXML(xml);

				String articleId = portletPreferences.getValue(
					"articleId", null);

				List<JournalContentSearch> contentSearches =
					_journalContentSearchLocalService.getArticleContentSearches(
						groupId, articleId);

				if (contentSearches.isEmpty()) {
					continue;
				}

				JournalContentSearch contentSearch = contentSearches.get(0);

				_journalContentSearchLocalService.updateContentSearch(
					contentSearch.getGroupId(), contentSearch.isPrivateLayout(),
					contentSearch.getLayoutId(), contentSearch.getPortletId(),
					articleId, true);
			}
		}
		finally {
			DataAccess.cleanUp(con, ps, rs);
		}
	}

	protected void updateCreateAndModifiedDates() throws Exception {
		ActionableDynamicQuery actionableDynamicQuery =
			_journalArticleResourceLocalService.getActionableDynamicQuery();

		if (_log.isDebugEnabled()) {
			long count = actionableDynamicQuery.performCount();

			_log.debug(
				"Processing " + count +
					" article resources for create and modified dates");
		}

		actionableDynamicQuery.setPerformActionMethod(
			new ActionableDynamicQuery.PerformActionMethod() {

				@Override
				public void performAction(Object object) {
					JournalArticleResource articleResource =
						(JournalArticleResource)object;

					updateCreateDate(articleResource);
					updateModifiedDate(articleResource);
				}

			});

		actionableDynamicQuery.performActions();

		if (_log.isDebugEnabled()) {
			_log.debug("Create and modified dates verified for articles");
		}
	}

	protected void updateCreateDate(JournalArticleResource articleResource) {
		List<JournalArticle> articles = _journalArticleLocalService.getArticles(
			articleResource.getGroupId(), articleResource.getArticleId(),
			QueryUtil.ALL_POS, QueryUtil.ALL_POS,
			new ArticleVersionComparator(true));

		if (articles.size() <= 1) {
			return;
		}

		JournalArticle firstArticle = articles.get(0);

		Date createDate = firstArticle.getCreateDate();

		for (JournalArticle article : articles) {
			if (!createDate.equals(article.getCreateDate())) {
				article.setCreateDate(createDate);

				_journalArticleLocalService.updateJournalArticle(article);
			}
		}
	}

	protected void updateDocumentLibraryElements(Element element) {
		Element dynamicContentElement = element.element("dynamic-content");

		String path = dynamicContentElement.getStringValue();

		String[] pathArray = StringUtil.split(path, CharPool.SLASH);

		if (pathArray.length != 5) {
			return;
		}

		long groupId = GetterUtil.getLong(pathArray[2]);
		long folderId = GetterUtil.getLong(pathArray[3]);
		String title = HttpUtil.decodeURL(HtmlUtil.escape(pathArray[4]));

		try {
			FileEntry fileEntry = _dlAppLocalService.getFileEntry(
				groupId, folderId, title);

			Node node = dynamicContentElement.node(0);

			node.setText(path + StringPool.SLASH + fileEntry.getUuid());
		}
		catch (PortalException pe) {
		}
	}

	protected void updateDynamicElements(JournalArticle article)
		throws Exception {

		Document document = SAXReaderUtil.read(article.getContent());

		Element rootElement = document.getRootElement();

		updateDynamicElements(rootElement.elements("dynamic-element"));

		article.setContent(document.asXML());

		_journalArticleLocalService.updateJournalArticle(article);
	}

	protected void updateDynamicElements(List<Element> dynamicElements)
		throws PortalException {

		DDMFieldsCounter ddmFieldsCounter = new DDMFieldsCounter();

		for (Element dynamicElement : dynamicElements) {
			updateDynamicElements(dynamicElement.elements("dynamic-element"));

			String name = dynamicElement.attributeValue("name");

			int index = ddmFieldsCounter.get(name);

			dynamicElement.addAttribute("index", String.valueOf(index));

			String type = dynamicElement.attributeValue("type");

			if (type.equals("image")) {
				updateImageElement(dynamicElement, name, index);
			}

			ddmFieldsCounter.incrementKey(name);
		}
	}

	protected void updateElement(long groupId, Element element) {
		List<Element> dynamicElementElements = element.elements(
			"dynamic-element");

		for (Element dynamicElementElement : dynamicElementElements) {
			updateElement(groupId, dynamicElementElement);
		}

		String type = element.attributeValue("type");

		if (type.equals("document_library")) {
			updateDocumentLibraryElements(element);
		}
		else if (type.equals("link_to_layout")) {
			updateLinkToLayoutElements(groupId, element);
		}
	}

	protected void updateImageElement(Element element, String name, int index) {
		Element dynamicContentElement = element.element("dynamic-content");

		long articleImageId = GetterUtil.getLong(
			dynamicContentElement.attributeValue("id"));

		JournalArticleImage articleImage =
			_journalArticleImageLocalService.fetchJournalArticleImage(
				articleImageId);

		if (articleImage == null) {
			return;
		}

		articleImage.setElName(name + StringPool.UNDERLINE + index);

		_journalArticleImageLocalService.updateJournalArticleImage(
			articleImage);
	}

	protected void updateLinkToLayoutElements(long groupId, Element element) {
		Element dynamicContentElement = element.element("dynamic-content");

		Node node = dynamicContentElement.node(0);

		String text = node.getText();

		if (!text.isEmpty() && !text.endsWith(StringPool.AT + groupId)) {
			node.setText(
				dynamicContentElement.getStringValue() + StringPool.AT +
					groupId);
		}
	}

	protected void updateModifiedDate(JournalArticleResource articleResource) {
		JournalArticle article = _journalArticleLocalService.fetchLatestArticle(
			articleResource.getResourcePrimKey(),
			WorkflowConstants.STATUS_APPROVED, true);

		if (article == null) {
			return;
		}

		AssetEntry assetEntry = _assetEntryLocalService.fetchEntry(
			articleResource.getGroupId(), articleResource.getUuid());

		if (assetEntry == null) {
			return;
		}

		Date modifiedDate = article.getModifiedDate();

		if (modifiedDate.equals(assetEntry.getModifiedDate())) {
			return;
		}

		article.setModifiedDate(assetEntry.getModifiedDate());

		_journalArticleLocalService.updateJournalArticle(article);
	}

	protected void updateResourcePrimKey() throws PortalException {
		ActionableDynamicQuery actionableDynamicQuery =
			_journalArticleLocalService.getActionableDynamicQuery();

		actionableDynamicQuery.setAddCriteriaMethod(
			new ActionableDynamicQuery.AddCriteriaMethod() {

				@Override
				public void addCriteria(DynamicQuery dynamicQuery) {
					Property resourcePrimKey = PropertyFactoryUtil.forName(
						"resourcePrimKey");

					dynamicQuery.add(resourcePrimKey.le(0l));
				}

			}
		);

		if (_log.isDebugEnabled()) {
			long count = actionableDynamicQuery.performCount();

			_log.debug(
				"Processing " + count +
					" default article versions in draft mode");
		}

		actionableDynamicQuery.setPerformActionMethod(
			new ActionableDynamicQuery.PerformActionMethod() {

				@Override
				public void performAction(Object object)
					throws PortalException {

					JournalArticle article = (JournalArticle)object;

					long groupId = article.getGroupId();
					String articleId = article.getArticleId();
					double version = article.getVersion();

					_journalArticleLocalService.checkArticleResourcePrimKey(
						groupId, articleId, version);
				}

			});

		actionableDynamicQuery.performActions();
	}

	protected void updateURLTitle(
			long groupId, String articleId, String urlTitle)
		throws Exception {

		String normalizedURLTitle = FriendlyURLNormalizerUtil.normalize(
			urlTitle, _friendlyURLPattern);

		if (urlTitle.equals(normalizedURLTitle)) {
			return;
		}

		normalizedURLTitle = _journalArticleLocalService.getUniqueUrlTitle(
			groupId, articleId, normalizedURLTitle);

		Connection con = null;
		PreparedStatement ps = null;

		try {
			con = DataAccess.getUpgradeOptimizedConnection();

			ps = con.prepareStatement(
				"update JournalArticle set urlTitle = ? where urlTitle = ?");

			ps.setString(1, normalizedURLTitle);
			ps.setString(2, urlTitle);

			ps.executeUpdate();
		}
		finally {
			DataAccess.cleanUp(con, ps);
		}
	}

	protected void verifyArticleAssets() throws Exception {
		List<JournalArticle> journalArticles =
			_journalArticleLocalService.getNoAssetArticles();

		if (_log.isDebugEnabled()) {
			_log.debug(
				"Processing " + journalArticles.size() +
					" articles with no asset");
		}

		for (JournalArticle journalArticle : journalArticles) {
			try {
				_journalArticleLocalService.updateAsset(
					journalArticle.getUserId(), journalArticle, null, null,
					null);
			}
			catch (Exception e) {
				if (_log.isWarnEnabled()) {
					_log.warn(
						"Unable to update asset for article " +
							journalArticle.getId() + ": " + e.getMessage());
				}
			}
		}

		ActionableDynamicQuery actionableDynamicQuery =
			_journalArticleLocalService.getActionableDynamicQuery();

		actionableDynamicQuery.setAddCriteriaMethod(
			new ActionableDynamicQuery.AddCriteriaMethod() {

				@Override
				public void addCriteria(DynamicQuery dynamicQuery) {
					Property versionProperty = PropertyFactoryUtil.forName(
						"version");

					dynamicQuery.add(
						versionProperty.eq(
							JournalArticleConstants.VERSION_DEFAULT));

					Property statusProperty = PropertyFactoryUtil.forName(
						"status");

					dynamicQuery.add(
						statusProperty.eq(WorkflowConstants.STATUS_DRAFT));
				}

			});

		if (_log.isDebugEnabled()) {
			long count = actionableDynamicQuery.performCount();

			_log.debug(
				"Processing " + count +
					" default article versions in draft mode");
		}

		actionableDynamicQuery.setPerformActionMethod(
			new ActionableDynamicQuery.PerformActionMethod() {

				@Override
				public void performAction(Object object)
					throws PortalException {

					JournalArticle article = (JournalArticle)object;

					AssetEntry assetEntry = _assetEntryLocalService.fetchEntry(
						JournalArticle.class.getName(),
						article.getResourcePrimKey());

					_assetEntryLocalService.updateEntry(
						assetEntry.getClassName(), assetEntry.getClassPK(),
						null, assetEntry.isVisible());
				}

			});

		actionableDynamicQuery.performActions();

		if (_log.isDebugEnabled()) {
			_log.debug("Assets verified for articles");
		}

		updateCreateAndModifiedDates();
		updateResourcePrimKey();
	}

	protected void verifyArticleContents() throws Exception {
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			con = DataAccess.getUpgradeOptimizedConnection();

			ps = con.prepareStatement(
				"select id_ from JournalArticle where (content like " +
					"'%document_library%' or content like '%link_to_layout%')" +
						" and DDMStructureKey != ''");

			rs = ps.executeQuery();

			while (rs.next()) {
				long id = rs.getLong("id_");

				JournalArticle article = _journalArticleLocalService.getArticle(
					id);

				try {
					Document document = SAXReaderUtil.read(
						article.getContent());

					Element rootElement = document.getRootElement();

					for (Element element : rootElement.elements()) {
						updateElement(article.getGroupId(), element);
					}

					article.setContent(document.asXML());

					_journalArticleLocalService.updateJournalArticle(article);
				}
				catch (Exception e) {
					_log.error(
						"Unable to update content for article " +
							article.getId(),
						e);
				}
			}
		}
		finally {
			DataAccess.cleanUp(con, ps, rs);
		}
	}

	protected void verifyArticleLayouts() throws Exception {
		verifyUuid("JournalArticle");
	}

	protected void verifyArticleStructures() throws PortalException {
		ActionableDynamicQuery actionableDynamicQuery =
			_journalArticleLocalService.getActionableDynamicQuery();

		if (_log.isDebugEnabled()) {
			long count = actionableDynamicQuery.performCount();

			_log.debug(
				"Processing " + count + " articles for invalid structures " +
					"and dynamic elements");
		}

		actionableDynamicQuery.setPerformActionMethod(
			new ActionableDynamicQuery.PerformActionMethod() {

				@Override
				public void performAction(Object object) {
					JournalArticle article = (JournalArticle)object;

					try {
						_journalArticleLocalService.checkStructure(
							article.getGroupId(), article.getArticleId(),
							article.getVersion());
					}
					catch (NoSuchStructureException nsse) {
						if (_log.isWarnEnabled()) {
							_log.warn(
								"Removing reference to missing structure for " +
									"article " + article.getId());
						}

						article.setDDMStructureKey(StringPool.BLANK);
						article.setDDMTemplateKey(StringPool.BLANK);

						_journalArticleLocalService.updateJournalArticle(
							article);
					}
					catch (Exception e) {
						_log.error(
							"Unable to check the structure for article " +
								article.getId(),
							e);
					}

					try {
						updateDynamicElements(article);
					}
					catch (Exception e) {
						_log.error(
							"Unable to update content for article " +
								article.getId(),
							e);
					}
				}

			});

		actionableDynamicQuery.performActions();
	}

	protected void verifyContentSearch() throws Exception {
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			con = DataAccess.getUpgradeOptimizedConnection();

			ps = con.prepareStatement(
				"select groupId, portletId from JournalContentSearch group " +
					"by groupId, portletId having count(groupId) > 1 and " +
						"count(portletId) > 1");

			rs = ps.executeQuery();

			while (rs.next()) {
				long groupId = rs.getLong("groupId");
				String portletId = rs.getString("portletId");

				updateContentSearch(groupId, portletId);
			}
		}
		finally {
			DataAccess.cleanUp(con, ps, rs);
		}
	}

	protected void verifyFolderAssets() throws Exception {
		List<JournalFolder> folders =
			_journalFolderLocalService.getNoAssetFolders();

		if (_log.isDebugEnabled()) {
			_log.debug(
				"Processing " + folders.size() + " folders with no asset");
		}

		for (JournalFolder folder : folders) {
			try {
				_journalFolderLocalService.updateAsset(
					folder.getUserId(), folder, null, null, null);
			}
			catch (Exception e) {
				if (_log.isWarnEnabled()) {
					_log.warn(
						"Unable to update asset for folder " +
							folder.getFolderId() + ": " + e.getMessage());
				}
			}
		}

		if (_log.isDebugEnabled()) {
			_log.debug("Assets verified for folders");
		}
	}

	protected void verifyOracleNewLine() throws Exception {
		DB db = DBFactoryUtil.getDB();

		String dbType = db.getType();

		if (!dbType.equals(DB.TYPE_ORACLE)) {
			return;
		}

		// This is a workaround for a limitation in Oracle sqlldr's inability
		// insert new line characters for long varchar columns. See
		// http://forums.liferay.com/index.php?showtopic=2761&hl=oracle for more
		// information. Check several articles because some articles may not
		// have new lines.

		boolean checkNewLine = false;

		List<JournalArticle> articles = _journalArticleLocalService.getArticles(
			DEFAULT_GROUP_ID, 0, NUM_OF_ARTICLES);

		for (JournalArticle article : articles) {
			String content = article.getContent();

			if ((content != null) && content.contains("\\n")) {
				articles = _journalArticleLocalService.getArticles(
					DEFAULT_GROUP_ID);

				for (int j = 0; j < articles.size(); j++) {
					article = articles.get(j);

					_journalArticleLocalService.checkNewLine(
						article.getGroupId(), article.getArticleId(),
						article.getVersion());
				}

				checkNewLine = true;

				break;
			}
		}

		// Only process this once

		if (!checkNewLine) {
			if (_log.isInfoEnabled()) {
				_log.info("Do not fix oracle new line");
			}

			return;
		}
		else {
			if (_log.isInfoEnabled()) {
				_log.info("Fix oracle new line");
			}
		}
	}

	protected void verifyPermissions() throws PortalException {
		List<JournalArticle> articles =
			_journalArticleLocalService.getNoPermissionArticles();

		for (JournalArticle article : articles) {
			_resourceLocalService.addResources(
				article.getCompanyId(), 0, 0, JournalArticle.class.getName(),
				article.getResourcePrimKey(), false, false, false);
		}
	}

	protected void verifyTree() throws Exception {
		long[] companyIds = PortalInstances.getCompanyIdsBySQL();

		for (long companyId : companyIds) {
			_journalFolderLocalService.rebuildTree(companyId);
		}
	}

	protected void verifyURLTitle() throws Exception {
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			con = DataAccess.getUpgradeOptimizedConnection();

			ps = con.prepareStatement(
				"select distinct groupId, articleId, urlTitle from " +
					"JournalArticle");

			rs = ps.executeQuery();

			while (rs.next()) {
				long groupId = rs.getLong("groupId");
				String articleId = rs.getString("articleId");
				String urlTitle = GetterUtil.getString(
					rs.getString("urlTitle"));

				updateURLTitle(groupId, articleId, urlTitle);
			}
		}
		finally {
			DataAccess.cleanUp(con, ps, rs);
		}
	}

	private static final Log _log = LogFactoryUtil.getLog(
		JournalServiceVerifyProcess.class);

	private static final Pattern _friendlyURLPattern = Pattern.compile(
		"[^a-z0-9_-]");

	private AssetEntryLocalService _assetEntryLocalService;
	private DLAppLocalService _dlAppLocalService;
	private JournalArticleImageLocalService _journalArticleImageLocalService;
	private JournalArticleLocalService _journalArticleLocalService;
	private JournalArticleResourceLocalService
		_journalArticleResourceLocalService;
	private JournalContentSearchLocalService _journalContentSearchLocalService;
	private JournalFolderLocalService _journalFolderLocalService;
	private ResourceLocalService _resourceLocalService;

}