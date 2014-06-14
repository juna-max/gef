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
 * Note: Parts of this interface have been transferred from org.eclipse.gef.EditPart
 * 
 *******************************************************************************/
package org.eclipse.gef4.mvc.parts;

import java.util.List;

import org.eclipse.gef4.mvc.IActivatable;
import org.eclipse.gef4.mvc.IPropertyChangeSupport;
import org.eclipse.gef4.mvc.bindings.IAdaptable;

/**
 * 
 * @author nyssen
 *
 * @param <VR>
 *            The visual root node of the UI toolkit this {@link IVisualPart} is
 *            used in, e.g. javafx.scene.Node in case of JavaFX.
 */
public interface IVisualPart<VR> extends IActivatable, IAdaptable,
		IPropertyChangeSupport {

	// TODO: add others
	public static final String PARENT_PROPERTY = "parent";
	public static final String ANCHORAGES_PROPERTY = "anchorage";

	/**
	 * Returns the {@link IRootPart}. This method should only be called
	 * internally or by helpers such as edit policies. The root can be used to
	 * get the viewer.
	 * 
	 * @return <code>null</code> or the {@link IRootPart}
	 */
	public IRootPart<VR> getRoot();

	public abstract VR getVisual();

	public boolean isRefreshVisual();

	/**
	 * While interacting with IVisualParts the visual representation should not
	 * be updated to correspond with its model. To suppress
	 * {@link #refreshVisual()} from doing so, you can set this flag to
	 * <code>false</code>.
	 * 
	 * @param refreshVisual
	 */
	public void setRefreshVisual(boolean refreshVisual);

	public void refreshVisual();

	public void setParent(IVisualPart<VR> parent);

	public IVisualPart<VR> getParent();

	public List<IVisualPart<VR>> getChildren();

	public void removeChild(IVisualPart<VR> child);

	public void removeChildren(List<? extends IVisualPart<VR>> children);

	public void addChild(IVisualPart<VR> child, int index);

	public void addChild(IVisualPart<VR> child);

	public void addChildren(List<? extends IVisualPart<VR>> children);

	public void reorderChild(IVisualPart<VR> child, int index);

	// public void addVisualToParentVisual(IVisualPart<VR> parent, V
	// parentVisual);
	//
	// public void removeVisualFromParentVisual(IVisualPart<VR> parent, V
	// parentVisual);

	public void addAnchored(IVisualPart<VR> anchored);

	// TODO: add by index and reordering of anchored

	public void addAnchoreds(List<? extends IVisualPart<VR>> anchoreds);

	public void removeAnchored(IVisualPart<VR> anchored);

	public void removeAnchoreds(List<? extends IVisualPart<VR>> anchoreds);

	public List<IVisualPart<VR>> getAnchoreds();

	public void addAnchorage(IVisualPart<VR> anchorage);

	public void removeAnchorage(IVisualPart<VR> anchorage);

	public List<IVisualPart<VR>> getAnchorages();

	// anchorage visual may not be the visual of the anchorage itself!
	public void attachVisualToAnchorageVisual(IVisualPart<VR> anchorage,
			VR anchorageVisual);

	public void detachVisualFromAnchorageVisual(IVisualPart<VR> anchorage,
			VR anchorageVisual);
}