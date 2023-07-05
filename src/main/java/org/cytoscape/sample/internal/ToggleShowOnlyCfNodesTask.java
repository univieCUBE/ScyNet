package org.cytoscape.sample.internal;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTableUtil;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.task.AbstractNetworkViewTask;
import org.cytoscape.view.presentation.property.BooleanVisualProperty;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.view.model.View;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Set;

public class ToggleShowOnlyCfNodesTask extends AbstractNetworkViewTask {

	private CyApplicationManager cyApplicationManager;

	public ToggleShowOnlyCfNodesTask(CyNetworkView view, CyApplicationManager cyApplicationManager){
		/*super("Toggle show only cross-feeding nodes");
		setPreferredMenu("Apps.SCyNet");*/
		super(view);
		this.cyApplicationManager = cyApplicationManager;
	}
	
	@Override
	public void run(final TaskMonitor taskMonitor) {
		if (cyApplicationManager.getCurrentNetwork() == null){			
			return;
		}
		CyNetwork network = cyApplicationManager.getCurrentNetwork();

		if(view == null){
			return;
		}

		//Get the selected nodes

		//Check if type and cross-fed columns exist
		CyNetwork currentNetwork = cyApplicationManager.getCurrentNetwork();
		Set<String> columnNames = CyTableUtil.getColumnNames(currentNetwork.getDefaultNodeTable());

		if (columnNames.contains("type") && columnNames.contains("cross-fed")) {
			List<CyNode> nonCrossFedNodes = CyTableUtil.getNodesInState(cyApplicationManager.getCurrentNetwork(),"cross-fed",false);

			Boolean allHidden = true;
			for (CyNode node : nonCrossFedNodes) {
				View<CyNode> nodeView = view.getNodeView(node);
				if (nodeView == null) {
					continue;
				}
				if (nodeView.getVisualProperty(BasicVisualLexicon.NODE_VISIBLE)) {
					allHidden = false;
					break;
				}
			}


			if (allHidden) {
				for (CyNode node : nonCrossFedNodes) {
					View<CyNode> nodeView = view.getNodeView(node);
					if (nodeView == null) {
						continue;
					}
					nodeView.setLockedValue(BasicVisualLexicon.NODE_VISIBLE, true);
				}
			}
			else {
				for (CyNode node : nonCrossFedNodes) {
					View<CyNode> nodeView = view.getNodeView(node);
					if (nodeView == null) {
						continue;
					}
					nodeView.setLockedValue(BasicVisualLexicon.NODE_VISIBLE, false);
				}
			}

		}

//
	}
}
