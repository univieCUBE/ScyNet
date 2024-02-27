package org.scynet;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.*;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.subnetwork.CySubNetwork;
import org.cytoscape.session.CyNetworkNaming;
import org.cytoscape.task.AbstractNetworkViewTask;
import org.cytoscape.view.layout.CyLayoutAlgorithmManager;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.view.presentation.property.ArrowShapeVisualProperty;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.work.Task;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

import static java.lang.Math.abs;

public class ContextualizeWithFluxDataTask extends AbstractNetworkViewTask {

	private CyApplicationManager cyApplicationManager;
	/**
	 * TSV-map created from the TSV-file if it was added
	 */
	private final HashMap<String, Double> tsvMap;
	/**
	 * The boolean defining if the submitted flux map is fva or fba
	 */
	private boolean isFva;
	/**
	 * The manager for networks in Cytoscape
	 */
	private final CyNetworkManager networkManager;
	/**
	 * The naming service for networks in Cytoscape
	 */
	private final CyNetworkNaming cyNetworkNaming;
	/**
	 * A color palette for the network visualisations. Colors are used in the following order:
	 * 1. Node member
	 * 2. Node metabolite
	 * 3. Arrow influx
	 * 4. Arrow efflux
	 * 5. Arrow bidirectional
	 * 6. Arrow 0 flux
	 */
	private final ArrayList<Color> palette;
	/**
	 * The layout algorithm manager in Cytoscape
	 */
	private final CyLayoutAlgorithmManager cyLayoutAlgorithmManager;


	public ContextualizeWithFluxDataTask(CyNetworkView view, CyApplicationManager cyApplicationManager, HashMap<String, Double> tsvMap, Boolean isFva, CyNetworkManager networkManager, CyNetworkNaming cyNetworkNaming, CyLayoutAlgorithmManager cyLayoutAlgorithmManager){
		super(view);
		this.cyApplicationManager = cyApplicationManager;
		this.tsvMap = tsvMap;
		this.isFva = isFva;
		this.networkManager = networkManager;
		this.cyNetworkNaming = cyNetworkNaming;
		this.cyLayoutAlgorithmManager = cyLayoutAlgorithmManager;
		this.palette = new ArrayList<>();
		Color compNodeColor = new Color(145,191,219, 175);
		Color exchgNodeColor = new Color(223,194,125, 175);
		Color influxArrowColor = new Color(128,205,193, 175);
		Color effluxArrowColor = new Color(253,174,97, 175);
		Color bidirectionalArrowColor = new Color(194,165,207, 175);
		Color zeroFluxArrowColor = new Color(100,100,100,175);
		Color defaultArrowColor = new Color(50,50,50,175);
		this.palette.add(compNodeColor);
		this.palette.add(exchgNodeColor);
		this.palette.add(influxArrowColor);
		this.palette.add(effluxArrowColor);
		this.palette.add(bidirectionalArrowColor);
		this.palette.add(zeroFluxArrowColor);
		this.palette.add(defaultArrowColor);
	}
	
	@Override
	public void run(final TaskMonitor taskMonitor) {
		if (cyApplicationManager.getCurrentNetwork() == null){			
			return;
		}

		if(view == null){
			return;
		}

		//Check if type and cross-fed columns exist
		CyNetwork currentNetwork = cyApplicationManager.getCurrentNetwork();
		Set<String> columnNames = CyTableUtil.getColumnNames(currentNetwork.getDefaultEdgeTable());

		if (columnNames.contains("sbml id") && columnNames.contains("flux")) {
			if (tsvMap.isEmpty()) {
				return;
			}

			// Add flux to edge
			for (CyEdge edge : currentNetwork.getEdgeList()) {
				String fluxKey = currentNetwork.getDefaultEdgeTable().getRow(edge.getSUID()).get("name", String.class);
				Double fluxValue;

				if (isFva) {
					Double minFluxValue = getFlux(fluxKey, true);
					Double maxFluxValue = getFlux(fluxKey, false);
					currentNetwork.getDefaultEdgeTable().getRow(edge.getSUID()).set("min flux", minFluxValue);
					currentNetwork.getDefaultEdgeTable().getRow(edge.getSUID()).set("max flux", maxFluxValue);
					fluxValue = Math.max(Math.abs(minFluxValue), Math.abs(maxFluxValue));
				}
				else {
					fluxValue = getFlux(fluxKey, false);
				}
				currentNetwork.getDefaultEdgeTable().getRow(edge.getSUID()).set("flux", fluxValue);
			}

			if (isFva) {
				setCrossFeedingNodeStatusFva(currentNetwork);
			}
			else {
				setCrossFeedingNodeStatus(currentNetwork);
			}

			// Add styling to edge
			paintEdges(currentNetwork);

			// Apply the scynet layout
			ApplyScynetLayoutTaskFactory scynetLayoutTF = new ApplyScynetLayoutTaskFactory(cyLayoutAlgorithmManager);
			TaskIterator tItr = scynetLayoutTF.createTaskIterator(view);
			Task nextTask = tItr.next();
			try {
				nextTask.run(taskMonitor);
			} catch (Exception e) {
				throw new RuntimeException("Could not finish layout", e);
			}

		} else {
			// Display a warning message that the network is not in the correct format
			JFrame frame = new JFrame();
			JOptionPane pane = new JOptionPane(
					"The selected network is not in ScyNet format. " +
							"Please select a network created by ScyNet.",
					JOptionPane.WARNING_MESSAGE
			);
			pane.setComponentOrientation(JOptionPane.getRootFrame().getComponentOrientation());
			JDialog dialog = pane.createDialog(frame, "Error: Wrong Network Format");

			dialog.setModal(false);
			dialog.setVisible(true);
		}

//
	}

	private Double getFlux(String key, Boolean isReverse) {
		if (!isFva && tsvMap.get(key) == null) {
			return 0.0d;
		}
		if (Objects.equals(key, "")) {
			return null;
		} else if (isFva) {
			if (isReverse && tsvMap.get(key + "_min") != null) {
				return tsvMap.get(key + "_min");
			}
			else if (!isReverse && tsvMap.get(key + "_max") != null) {
				return tsvMap.get(key + "_max");
			}
			else {
				return 0.0d;
			}
		} else {
			return tsvMap.get(key);
		}
	}

	private void paintEdges (CyNetwork newNetwork) {
		// Here we change the appearance of the Edges
		for (CyEdge newEdge : newNetwork.getEdgeList()) {
			String edgeSourceName = newNetwork.getDefaultNodeTable().getRow(newEdge.getSource().getSUID()).get("shared name", String.class);
			String edgeTargetName = newNetwork.getDefaultNodeTable().getRow(newEdge.getTarget().getSUID()).get("shared name", String.class);
			Double edgeFlux = newNetwork.getDefaultEdgeTable().getRow(newEdge.getSUID()).get("flux", Double.class);
			Double edgeMinFlux = newNetwork.getDefaultEdgeTable().getRow(newEdge.getSUID()).get("min flux", Double.class);
			Double edgeMaxFlux = newNetwork.getDefaultEdgeTable().getRow(newEdge.getSUID()).get("max flux", Double.class);
			View<CyEdge> edgeView = view.getEdgeView(newEdge);
			// If Fluxes were added or not
			if (isFva) {
				Double edgeWidth = 10.0d;
				edgeView.setLockedValue(BasicVisualLexicon.EDGE_WIDTH, edgeWidth);
				if (edgeMinFlux != null && edgeMaxFlux != null) {
					// Color of the Edges is selected based on Fluxes
					Paint edgeColor;
					if (edgeMinFlux < 0.0d && edgeMaxFlux > 0.0d) {
						edgeColor = this.palette.get(4);
						edgeView.setLockedValue(BasicVisualLexicon.EDGE_TARGET_ARROW_SHAPE, ArrowShapeVisualProperty.DELTA);
						edgeView.setLockedValue(BasicVisualLexicon.EDGE_SOURCE_ARROW_SHAPE, ArrowShapeVisualProperty.DELTA);
					}
					else if (edgeMaxFlux > 0.0d) {
						edgeColor = this.palette.get(3);
						edgeView.setLockedValue(BasicVisualLexicon.EDGE_TARGET_ARROW_SHAPE, ArrowShapeVisualProperty.DELTA);
					} else if (edgeMinFlux < 0.0d) {
						edgeColor = this.palette.get(2);
						edgeView.setLockedValue(BasicVisualLexicon.EDGE_SOURCE_ARROW_SHAPE, ArrowShapeVisualProperty.DELTA);
					}
					else {
						edgeColor = this.palette.get(5);
					}
					edgeView.setLockedValue(BasicVisualLexicon.EDGE_PAINT, edgeColor);

					// Width of the Edges is also based on Fluxes
					if (edgeMinFlux == 0.0d && edgeMaxFlux == 0.0d) {
						edgeView.setLockedValue(BasicVisualLexicon.EDGE_VISIBLE, false);
					}
					else {
						edgeView.setLockedValue(BasicVisualLexicon.EDGE_VISIBLE, true);

						// Make connected nodes visible as well
						View<CyNode> sourceView = view.getNodeView(newEdge.getSource());
						View<CyNode> targetView = view.getNodeView(newEdge.getTarget());
						if (sourceView != null) {
							sourceView.setLockedValue(BasicVisualLexicon.NODE_VISIBLE, true);
						}
						if (targetView != null) {
							targetView.setLockedValue(BasicVisualLexicon.NODE_VISIBLE, true);
						}
					}
				} else {
					// Otherwise we just chose the Color with a default color
					Paint edgeColor = this.palette.get(6);
					edgeView.setLockedValue(BasicVisualLexicon.EDGE_PAINT, edgeColor);
				}
			}
			else {
				if (edgeFlux != null) {
					// Color of the Edges is selected based on Fluxes
					Paint edgeColor = this.palette.get(5);
					if (edgeFlux > 0.0d) {
						edgeColor = this.palette.get(3);
						edgeView.setLockedValue(BasicVisualLexicon.EDGE_TARGET_ARROW_SHAPE, ArrowShapeVisualProperty.DELTA);
					} else if (edgeFlux < 0.0d) {
						edgeColor = this.palette.get(2);
						edgeView.setLockedValue(BasicVisualLexicon.EDGE_SOURCE_ARROW_SHAPE, ArrowShapeVisualProperty.DELTA);
					}
					edgeView.setLockedValue(BasicVisualLexicon.EDGE_PAINT, edgeColor);

					// Width of the Edges is also based on Fluxes
					if (edgeFlux != 0.0d) {
						Double edgeWidth = 10.0d;
						edgeView.setLockedValue(BasicVisualLexicon.EDGE_WIDTH, edgeWidth);

						edgeView.setLockedValue(BasicVisualLexicon.EDGE_VISIBLE, true);

						// Make connected nodes visible as well
						View<CyNode> sourceView = view.getNodeView(newEdge.getSource());
						View<CyNode> targetView = view.getNodeView(newEdge.getTarget());
						if (sourceView != null) {
							sourceView.setLockedValue(BasicVisualLexicon.NODE_VISIBLE, true);
						}
						if (targetView != null) {
							targetView.setLockedValue(BasicVisualLexicon.NODE_VISIBLE, true);
						}
					} else {
						double edgeWidth = 10.0d;
						edgeView.setLockedValue(BasicVisualLexicon.EDGE_WIDTH, edgeWidth);
						edgeView.setLockedValue(BasicVisualLexicon.EDGE_VISIBLE, false);
					}
				} else {
					Double edgeWidth = 10.0d;
					edgeView.setLockedValue(BasicVisualLexicon.EDGE_WIDTH, edgeWidth);
					// Otherwise we just chose the Color with a default color
					Paint edgeColor = this.palette.get(6);
					edgeView.setLockedValue(BasicVisualLexicon.EDGE_PAINT, edgeColor);
				}
			}
		}

		hideSingletons(newNetwork);
	}

	/**
	 * Sets the cross-fed column value for all exchange nodes
	 */
	private void setCrossFeedingNodeStatus(CyNetwork newNetwork) {
		for (CyNode node : newNetwork.getNodeList()) {
			String nodeType = newNetwork.getDefaultNodeTable().getRow(node.getSUID()).get("type", String.class);
			if (!Objects.equals(nodeType, "exchange metabolite")) {
				continue;
			}
			List<CyEdge> adjacentEdgeList = newNetwork.getAdjacentEdgeList(node, CyEdge.Type.ANY);
			boolean positive = false;
			boolean negative = false;
			for (CyEdge currentEdge : adjacentEdgeList) {
				Double currentFlux = newNetwork.getDefaultEdgeTable().getRow(currentEdge.getSUID()).get("flux", Double.class);
				if (currentFlux == null) {
					continue;
				}
				if (currentFlux < 0) {
					negative = true;
				}
				if (currentFlux > 0) {
					positive = true;
				}
			}
			if (!isFva && !(positive && negative)) {
				newNetwork.getDefaultNodeTable().getRow(node.getSUID()).set("cross-fed", false);
				// newView.getNodeView(newNode).setVisualProperty(BasicVisualLexicon.NODE_TRANSPARENCY, 0);
			}
			else {
				newNetwork.getDefaultNodeTable().getRow(node.getSUID()).set("cross-fed", true);
			}
		}
	}
	/**
	 * Sets the cross-fed column value for all exchange nodes
	 */
	private void setCrossFeedingNodeStatusFva(CyNetwork newNetwork) {
		for (CyNode node : newNetwork.getNodeList()) {
			String nodeType = newNetwork.getDefaultNodeTable().getRow(node.getSUID()).get("type", String.class);
			if (!Objects.equals(nodeType, "exchange metabolite")) {
				continue;
			}
			Set<String> negativeSet = new HashSet<>();
			Set<String> positiveSet = new HashSet<>();
			List<CyEdge> adjacentEdgeList = newNetwork.getAdjacentEdgeList(node, CyEdge.Type.ANY);
			for (CyEdge currentEdge : adjacentEdgeList) {
				Double minFlux = newNetwork.getDefaultEdgeTable().getRow(currentEdge.getSUID()).get("min flux", Double.class);
				Double maxFlux = newNetwork.getDefaultEdgeTable().getRow(currentEdge.getSUID()).get("max flux", Double.class);

				if (minFlux == null || maxFlux == null) {
					continue;
				}

				String target = newNetwork.getDefaultEdgeTable().getRow(currentEdge.getSUID()).get("target", String.class);
				String source = newNetwork.getDefaultEdgeTable().getRow(currentEdge.getSUID()).get("source", String.class);
				String comp;
				if (getOrganisms(newNetwork).contains(target)) {
					comp = target;
				} else if (getOrganisms(newNetwork).contains(source)) {
					comp = source;
				}
				else {
					continue;
				}

				if (minFlux < 0) {
					negativeSet.add(comp);
				}
				if (maxFlux > 0) {
					positiveSet.add(comp);
				}
			}
			Iterator<String> posCompIterator = positiveSet.iterator();

			Boolean isCrossFed = false;
			while (posCompIterator.hasNext()){
				String posComp = posCompIterator.next();
				if (negativeSet.size() > 1 && negativeSet.contains(posComp)) {
					isCrossFed = true;
					break;
				} else if (negativeSet.size() > 0 && !negativeSet.contains(posComp)) {
					isCrossFed = true;
					break;
				}
			}

			if (isCrossFed) {
				newNetwork.getDefaultNodeTable().getRow(node.getSUID()).set("cross-fed", true);
				// newView.getNodeView(newNode).setVisualProperty(BasicVisualLexicon.NODE_TRANSPARENCY, 0);
			}
			else {
				newNetwork.getDefaultNodeTable().getRow(node.getSUID()).set("cross-fed", false);
			}
		}
	}

	private Set<String> getOrganisms(CyNetwork newNetwork) {
		Set<String> organisms = new HashSet<>();
		for (CyNode node : newNetwork.getNodeList()) {
			String nodeType = newNetwork.getDefaultNodeTable().getRow(node.getSUID()).get("type", String.class);
			String nodeName = newNetwork.getDefaultNodeTable().getRow(node.getSUID()).get("shared name", String.class);
			if (Objects.equals(nodeType, "community member")) {
				organisms.add(nodeName);
			}
		}
		return organisms;
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
