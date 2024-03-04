package org.scynet;

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

import org.cytoscape.application.CyUserLog;
import org.apache.log4j.Logger;

public class ToggleShowOnlyCfNodesTask extends AbstractNetworkViewTask {

	private final Logger logger;
	private CyApplicationManager cyApplicationManager;

	public ToggleShowOnlyCfNodesTask(CyNetworkView view, CyApplicationManager cyApplicationManager){
		super(view);
		this.logger = Logger.getLogger(CyUserLog.NAME);
		this.cyApplicationManager = cyApplicationManager;
	}
	
	@Override
	public void run(final TaskMonitor taskMonitor) {
		if (cyApplicationManager.getCurrentNetwork() == null){
			logger.warn("No network selected. Nothing to do.");
			return;
		}

		if(view == null){
			logger.warn("No network view available for selected network. Nothing to do.");
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
				logger.info("Making all non-cross-feeding nodes visible.");
				for (CyNode node : nonCrossFedNodes) {
					View<CyNode> nodeView = view.getNodeView(node);
					if (nodeView == null) {
						continue;
					}
					nodeView.setLockedValue(BasicVisualLexicon.NODE_VISIBLE, true);
				}
			}
			else {
				logger.info("Hiding all non-cross-feeding nodes.");
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
		else {
			logger.error("The selected network is not in ScyNet format.");
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
