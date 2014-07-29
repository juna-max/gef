/*******************************************************************************
 * Copyright (c) 2014 itemis AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Alexander Nyßen (itemis AG) - initial API and implementation
 *     
 * Note: Parts of this class have been transferred from org.eclipse.gef.editparts.AbstractEditPart.
 *     
 *******************************************************************************/
package org.eclipse.gef4.mvc.behaviors;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.gef4.mvc.models.IContentModel;
import org.eclipse.gef4.mvc.parts.IContentPart;
import org.eclipse.gef4.mvc.parts.IContentPartFactory;
import org.eclipse.gef4.mvc.parts.IRootPart;
import org.eclipse.gef4.mvc.parts.IVisualPart;
import org.eclipse.gef4.mvc.parts.PartUtils;

/**
 * A behavior that can be adapted to an {@link IRootPart} or an
 * {@link IContentPart} to synchronize the list of {@link IContentPart} children
 * and (only in case of an {@link IContentPart}) anchorages with the list of
 * content children and anchored.
 * 
 * @author anyssen
 * 
 * @param <VR>
 *            The visual root node of the UI toolkit this {@link IVisualPart} is
 *            used in, e.g. javafx.scene.Node in case of JavaFX.
 */
public class ContentBehavior<VR> extends AbstractBehavior<VR> implements
		PropertyChangeListener {

	// We need to ensure that when undoing model operations, the same content
	// parts are re-used when re-synchronizing; as such, we put content parts
	// into this pool within disposeIfObsolete() and relocate them within
	// findOrCreatePart()
	private Map<Object, IContentPart<VR>> contentPartPool;

	@Override
	public void activate() {
		super.activate();
		if (getHost() == getHost().getRoot()) {
			synchronizeContentChildren(getHost().getRoot().getViewer()
					.getContentModel().getContents());
			getHost().getRoot().getViewer().getContentModel()
					.addPropertyChangeListener(this);
		} else {
			synchronizeContentChildren(((IContentPart<VR>) getHost())
					.getContentChildren());
			synchronizeContentAnchorages(((IContentPart<VR>) getHost())
					.getContentAnchorages());
			getHost().addPropertyChangeListener(this);
		}
	}

	@Override
	public void deactivate() {
		if (getHost() == getHost().getRoot()) {
			getHost().getRoot().getViewer().getContentModel()
					.removePropertyChangeListener(this);
			synchronizeContentChildren(Collections.emptyList());
		} else {
			getHost().removePropertyChangeListener(this);
			synchronizeContentAnchorages(Collections.emptyList());
			synchronizeContentChildren(Collections.emptyList());
		}
		super.deactivate();
	}

	protected void disposeIfObsolete(IContentPart<VR> contentPart) {
		if (contentPart.getParent() == null
				&& contentPart.getAnchorages().isEmpty()) {
			// keep track of the removed content part, so we may relocate it
			// within findOrCreate() later
			if (contentPartPool == null) {
				contentPartPool = new HashMap<Object, IContentPart<VR>>();
			}
			contentPartPool.put(contentPart.getContent(), contentPart);
			contentPart.setContent(null);
		}
	}

	protected IContentPart<VR> findOrCreatePartFor(Object content) {
		Map<Object, IContentPart<VR>> contentPartMap = getHost().getRoot()
				.getViewer().getContentPartMap();
		if (contentPartMap.containsKey(content)) {
			return contentPartMap.get(content);
		} else {
			// 'Revive' a content part, if it was removed before
			IContentPart<VR> contentPart = null;
			if (contentPartPool != null) {
				contentPart = contentPartPool.remove(content);
				if (contentPartPool.isEmpty()) {
					contentPartPool = null;
				}
			}

			// If the part could not be revived, a new one is created
			if (contentPart == null) {
				IContentPartFactory<VR> contentPartFactory = getHost()
						.getRoot().getViewer().getContentPartFactory();
				contentPart = contentPartFactory.createContentPart(content,
						this, Collections.emptyMap());
			}

			// initialize part
			contentPart.setContent(content);
			return contentPart;
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void propertyChange(PropertyChangeEvent event) {
		if (IContentModel.CONTENTS_PROPERTY.equals(event.getPropertyName())) {
			synchronizeContentChildren((List<Object>) event.getNewValue());
			// TODO: flushing of models should be done somewhere more
			// appropriate
			getHost().getRoot().getViewer().getSelectionModel().deselectAll();
			getHost().getRoot().getViewer().getHoverModel().clearHover();
		} else if (IContentPart.CONTENT_PROPERTY
				.equals(event.getPropertyName())) {
			synchronizeContentChildren(((IContentPart<VR>) getHost())
					.getContentChildren());
			synchronizeContentAnchorages(((IContentPart<VR>) getHost())
					.getContentAnchorages());
		}
	}

	/**
	 * Updates the host {@link IVisualPart}'s {@link IContentPart} anchorages
	 * (see {@link IVisualPart#getAnchorages()}) so that it is in sync with the
	 * set of content anchorages that is passed in.
	 * 
	 * @param contentAnchorages
	 *            The list of content anchorages to be synchronized with the
	 *            list of {@link IContentPart} anchorages (
	 *            {@link IContentPart#getAnchorages()}).
	 * 
	 * @see IContentPart#getContentAnchorages()
	 * @see IContentPart#getAnchorages()
	 */
	@SuppressWarnings("unchecked")
	public void synchronizeContentAnchorages(
			List<? extends Object> contentAnchorages) {
		int i;

		List<IContentPart<VR>> anchorageContentParts = PartUtils.filterParts(
				getHost().getAnchorages(), IContentPart.class);
		int anchorageContentPartsSize = anchorageContentParts.size();
		int contentAnchoragesSize = contentAnchorages.size();

		IContentPart<VR> contentPart;
		Map<Object, IContentPart<VR>> contentToContentPartMap = Collections
				.emptyMap();
		if (anchorageContentPartsSize > 0) {
			contentToContentPartMap = new HashMap<Object, IContentPart<VR>>(
					anchorageContentPartsSize);
			for (i = 0; i < anchorageContentPartsSize; i++) {
				contentPart = anchorageContentParts.get(i);
				contentToContentPartMap.put(contentPart.getContent(),
						contentPart);
			}
		}

		Object content;
		for (i = 0; i < contentAnchoragesSize; i++) {
			content = contentAnchorages.get(i);

			// Do a quick check to see if editPart[i] == model[i]
			if (i < anchorageContentParts.size()
					&& anchorageContentParts.get(i).getContent() == content) {
				continue;
			}

			// Look to see if the EditPart is already around but in the
			// wrong location
			contentPart = contentToContentPartMap.get(content);

			if (contentPart != null) {
				// TODO: this is wrong, it has to take into consideration the
				// visual parts in between
				getHost().reorderAnchorage(contentPart, i);
			} else {
				// An EditPart for this model doesn't exist yet. Create and
				// insert one.
				contentPart = findOrCreatePartFor(content);
				// what if it does not exist??
				getHost().addAnchorage(contentPart);
			}
		}

		// remove the remaining EditParts
		anchorageContentParts = PartUtils.filterParts(
				getHost().getAnchorages(), IContentPart.class);
		anchorageContentPartsSize = anchorageContentParts.size();
		if (i < anchorageContentPartsSize) {
			List<IContentPart<VR>> trash = new ArrayList<IContentPart<VR>>(
					anchorageContentPartsSize - i);
			for (; i < anchorageContentPartsSize; i++) {
				trash.add(anchorageContentParts.get(i));
			}
			for (i = 0; i < trash.size(); i++) {
				IContentPart<VR> ep = trash.get(i);
				getHost().removeAnchorage(ep);
				disposeIfObsolete(ep);
			}
		}
	}

	/**
	 * Updates the host {@link IVisualPart}'s {@link IContentPart} children (see
	 * {@link IVisualPart#getChildren()}) so that it is in sync with the set of
	 * content children that is passed in.
	 * 
	 * @param contentChildren
	 *            The list of content children to be synchronized with the list
	 *            of {@link IContentPart} children (
	 *            {@link IContentPart#getChildren()}).
	 * 
	 * @see IContentPart#getContentChildren()
	 * @see IContentPart#getChildren()
	 */
	@SuppressWarnings("unchecked")
	public void synchronizeContentChildren(
			final List<? extends Object> contentChildren) {
		int i;

		// only synchronize IContentPart children
		List<IContentPart<VR>> childContentParts = PartUtils.filterParts(
				getHost().getChildren(), IContentPart.class);
		int contentChildrenSize = contentChildren.size();
		int childContentPartsSize = childContentParts.size();

		Map<Object, IContentPart<VR>> contentToContentPartMap = Collections
				.emptyMap();
		IContentPart<VR> contentPart;
		if (childContentPartsSize > 0) {
			contentToContentPartMap = new HashMap<Object, IContentPart<VR>>(
					childContentPartsSize);
			for (i = 0; i < childContentPartsSize; i++) {
				contentPart = childContentParts.get(i);
				contentToContentPartMap.put(contentPart.getContent(),
						contentPart);
			}
		}

		Object content;
		for (i = 0; i < contentChildrenSize; i++) {
			content = contentChildren.get(i);

			// Do a quick check to see if editPart[i] == model[i]
			if (i < childContentPartsSize
					&& childContentParts.get(i).getContent() == content) {
				continue;
			}

			// Look to see if the EditPart is already around but in the
			// wrong location
			contentPart = contentToContentPartMap.get(content);

			if (contentPart != null) {
				// TODO: this is wrong, it has to take into consideration the
				// visual parts in between
				getHost().reorderChild(contentPart, i);
			} else {
				// An EditPart for this model doesn't exist yet. Create and
				// insert one.
				contentPart = findOrCreatePartFor(content);
				getHost().addChild(contentPart, i);
			}
		}

		// remove the remaining EditParts
		childContentParts = PartUtils.filterParts(getHost().getChildren(),
				IContentPart.class);
		childContentPartsSize = childContentParts.size();

		if (i < childContentPartsSize) {
			List<IContentPart<VR>> trash = new ArrayList<IContentPart<VR>>(
					childContentPartsSize - i);
			for (; i < childContentPartsSize; i++) {
				trash.add(childContentParts.get(i));
			}
			for (i = 0; i < trash.size(); i++) {
				IContentPart<VR> ep = trash.get(i);
				getHost().removeChild(ep);
				disposeIfObsolete(ep);
			}
		}
	}

}
