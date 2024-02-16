package org.cytoscape.sample.internal;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTableUtil;
import org.cytoscape.task.AbstractNetworkViewTask;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.work.TaskMonitor;

import java.util.*;

public class ToggleShowOnlyCfNodesTask extends AbstractNetworkViewTask {

	private CyApplicationManager cyApplicationManager;

	public ToggleShowOnlyCfNodesTask(CyNetworkView view, CyApplicationManager cyApplicationManager){
		super(view);
		this.cyApplicationManager = cyApplicationManager;
	}
	
	@Override
	public void run(final TaskMonitor taskMonitor) {
		if (cyApplicationManager.getCurrentNetwork() == null){			
			return;
		}

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
			hideSingletons(currentNetwork);

		}

//
	}

	private void hideSingletons(CyNetwork currentNetwork) {
		for (CyNode node : currentNetwork.getNodeList()) {
			View<CyNode> nodeView = view.getNodeView(node);
			if (!nodeView.getVisualProperty(BasicVisualLexicon.NODE_VISIBLE)) {
				continue;
			} else if (Objects.equals("exchange metabolite", currentNetwork.getDefaultNodeTable().getRow(node.getSUID()).get("type", String.class))) {
				// Check for visible edges
				List<CyEdge> edges = currentNetwork.getAdjacentEdgeList(node, CyEdge.Type.ANY);
				List<CyEdge> visibleEdges = new ArrayList<>();

				for (CyEdge edge : edges) {
					View<CyEdge> edgeView = view.getEdgeView(edge);
					if (edgeView.getVisualProperty(BasicVisualLexicon.EDGE_VISIBLE)) {
						visibleEdges.add(edge);
						break;
					}
				}

				// Hide nodes without visible edges
				if (visibleEdges.isEmpty()) {
					nodeView.setLockedValue(BasicVisualLexicon.NODE_VISIBLE, false);
				}
			}
		}
	}
}
