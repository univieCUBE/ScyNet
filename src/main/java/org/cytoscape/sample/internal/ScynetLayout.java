package org.cytoscape.sample.internal;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTableUtil;
import org.cytoscape.view.layout.AbstractLayoutAlgorithm;
import org.cytoscape.view.layout.AbstractLayoutTask;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.view.model.VisualProperty;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.work.Task;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.undo.UndoSupport;
import org.cytoscape.util.swing.MessageDialogs;
import javax.swing.JDialog;

import javax.swing.*;
import java.util.*;

public class ScynetLayout extends AbstractLayoutAlgorithm {
	/**
	 * Creates a new MyLayout object.
	 */

	CyApplicationManager cyApplicationManager;
	public ScynetLayout(UndoSupport undo, CyApplicationManager cyApplicationManager) {
		super("scynetLayout","ScyNet Layout", undo);
		this.cyApplicationManager = cyApplicationManager;
	}
	
	public TaskIterator createTaskIterator(CyNetworkView networkView, Object context, Set<View<CyNode>> nodesToLayOut, String attrName) {
		Task task = new AbstractLayoutTask(toString(), networkView, nodesToLayOut, attrName, undoSupport) {
			@Override
			protected void doLayout(TaskMonitor taskMonitor) {
				/*
				Steps for the layout algorithm
				1. Check if network is a scynet generated network
				*/
				CyNetwork currentNetwork = cyApplicationManager.getCurrentNetwork();
				Set<String> columnNames = CyTableUtil.getColumnNames(currentNetwork.getDefaultNodeTable());

				if (!(columnNames.contains("type") && columnNames.contains("cross-fed"))) {
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
				} else {

					/*
					2. Find and count all community member nodes
					3. Find and count all metabolite nodes
					3.a) Partition metabolites into: disconnected, single connection, double connection, multi connection
					*/
					Set<CyNode> memberNodes = new HashSet<>();
					HashMap<String, CyNode> memberNamesToNodes = new HashMap<>();
					HashMap<CyNode, String> memberNodesToNames = new HashMap<>();
					Set<CyNode> singleNodes = new HashSet<>();
					Set<CyNode> doubleNodes = new HashSet<>();
					Set<CyNode> multiNodes = new HashSet<>();
					for (CyNode node : currentNetwork.getNodeList()) {
						View<CyNode> nodeView = networkView.getNodeView(node);
						if (!nodeView.getVisualProperty(BasicVisualLexicon.NODE_VISIBLE)) {
							continue;
						} else if (Objects.equals("community member", currentNetwork.getDefaultNodeTable().getRow(node.getSUID()).get("type", String.class))) {
							memberNodes.add(node);
							String memberName = currentNetwork.getDefaultNodeTable().getRow(node.getSUID()).get("shared name", String.class);
							memberNamesToNodes.put(memberName, node);
							memberNodesToNames.put(node, memberName);
						} else {
							// Partition metabolites into: disconnected, single connection, double connection, multi connection
							List<CyEdge> edges = currentNetwork.getAdjacentEdgeList(node, CyEdge.Type.ANY);
							List<CyEdge> visibleEdges = new ArrayList<>();

							for (CyEdge edge : edges) {
								View<CyEdge> edgeView = networkView.getEdgeView(edge);
								if (edgeView.getVisualProperty(BasicVisualLexicon.EDGE_VISIBLE)) {
									visibleEdges.add(edge);
								}
							}

							if (visibleEdges.isEmpty()) {
								nodeView.setLockedValue(BasicVisualLexicon.NODE_VISIBLE, false);
							} else if (visibleEdges.size() == 1) {
								singleNodes.add(node);
							} else if (visibleEdges.size() == 2) {
								doubleNodes.add(node);
							} else {
								multiNodes.add(node);
							}
						}
					}

					HashMap<CyNode, String> doubleNodeMembers = new HashMap<>();
					HashMap<String, Integer> memberPairCount = new HashMap<>();
					if (!doubleNodes.isEmpty()) {
						for (CyNode node : doubleNodes) {
							String memberPairName = "";
							String firstNeighbor = null;
							List<CyEdge> edges = currentNetwork.getAdjacentEdgeList(node, CyEdge.Type.ANY);
							for (CyEdge edge : edges) {
								View<CyEdge> edgeView = networkView.getEdgeView(edge);
								if (!edgeView.getVisualProperty(BasicVisualLexicon.EDGE_VISIBLE)) {
									continue;
								}
								CyNode neighbor;
								neighbor = edge.getSource();
								if (Objects.equals(neighbor, node)) {
									neighbor = edge.getTarget();
								}
								if (firstNeighbor == null) {
									firstNeighbor = memberNodesToNames.get(neighbor);
								} else {
									String secondNeighbor = memberNodesToNames.get(neighbor);

									if (firstNeighbor.compareTo(secondNeighbor) < 0) {
										memberPairName = firstNeighbor + " " + secondNeighbor;
									} else {
										memberPairName = secondNeighbor + " " + firstNeighbor;
									}
								}

							}
							doubleNodeMembers.put(node, memberPairName);
							memberPairCount.putIfAbsent(memberPairName, 0);
							memberPairCount.put(memberPairName, memberPairCount.get(memberPairName) + 1);
						}
					}

					// Order the member nodes
					List<CyNode> orderedMembers = new ArrayList<>();
					if (!memberPairCount.isEmpty()) {

						HashMap<String, Integer> tmpMemberPairCount = (HashMap<String, Integer>) memberPairCount.clone();
						int maxPairCount = Collections.max(tmpMemberPairCount.values());
						for (Map.Entry<String, Integer> entry : tmpMemberPairCount.entrySet()) {
							if (entry.getValue() == maxPairCount) {
								String memberPair = entry.getKey();
								orderedMembers.add(memberNamesToNodes.get(memberPair.split(" ")[0]));
								orderedMembers.add(memberNamesToNodes.get(memberPair.split(" ")[1]));
								tmpMemberPairCount.remove(memberPair);
								break;
							}
						}
						for (int i = 2; i < memberNodes.size(); i++) {
							int max = 0;
							CyNode bestNeighbor = null;
							String lastNode = memberNodesToNames.get(orderedMembers.get(i - 1));
							for (Map.Entry<String, Integer> entry : tmpMemberPairCount.entrySet()) {
								String memberPair = entry.getKey();
								Integer value = entry.getValue();
								if (value <= max) {
									continue;
								}
								if (memberPair.contains(lastNode)) {
									String neighbor = memberPair.split(" ")[1];
									if (Objects.equals(neighbor, lastNode)) {
										neighbor = memberPair.split(" ")[0];
									}
									if (orderedMembers.contains(memberNamesToNodes.get(neighbor))) {
										continue;
									}
									max = value;
									bestNeighbor = memberNamesToNodes.get(neighbor);
								}

							}
							if (bestNeighbor == null) {
								for (CyNode nextNode : memberNodes) {
									if (!orderedMembers.contains(nextNode)) {
										bestNeighbor = nextNode;
										break;
									}
								}
							}
							orderedMembers.add(bestNeighbor);

						}
					} else {
						orderedMembers.addAll(memberNodes);
					}
					/*
					3.b) Calculate number and size of the circles
					*/
					int numMembers = memberNodes.size();
					int numDouble = doubleNodes.size();
					int numMulti = multiNodes.size();
					int numSingle = singleNodes.size();

					int sizeMemberNode = 150;
					int sizeMetaboliteNode = 32;

					double radiusMulti = (double) Math.ceil(numMulti / Math.PI) * sizeMetaboliteNode + 2 * sizeMetaboliteNode;
					double radiusDouble = (double) Math.ceil(numDouble / Math.PI) + 4 * sizeMetaboliteNode;
					if (radiusDouble < radiusMulti + 3 * sizeMetaboliteNode) {
						radiusDouble = radiusMulti + 3 * sizeMetaboliteNode;
					}
					double radiusMembers = (double) Math.ceil(numMembers / Math.PI) * sizeMemberNode + 2 * sizeMemberNode;
					if (radiusMembers < radiusDouble + 2 * sizeMemberNode) {
						radiusMembers = radiusDouble + 2 * sizeMemberNode;
					}
					double radiusSingle = (double) Math.ceil(numSingle / Math.PI) * sizeMetaboliteNode + 4 * sizeMetaboliteNode;
					if (radiusSingle < radiusMembers + 4 * sizeMetaboliteNode) {
						radiusSingle = radiusMembers + 4 * sizeMetaboliteNode;
					}
					/*
					3.c) Make a list of favorable neighbors based on the double connections.
					*/

					/*
					4. Layout 3rd circle with organisms (this is the reference location). Maximize the number of favorable neighbors.
					5. Layout inner (multi connection) circle nodes
					6. Layout 2nd circle with double connections
					7. Layout single connections

					 */

					final VisualProperty<Double> xLoc = BasicVisualLexicon.NODE_X_LOCATION;
					final VisualProperty<Double> yLoc = BasicVisualLexicon.NODE_Y_LOCATION;

					// Set visual property.

					for (View<CyNode> nodeView : nodesToLayOut) {
						nodeView.setVisualProperty(xLoc, 10.0d);
						nodeView.setVisualProperty(yLoc, 100.0d);
					}


					double multiNodeIndex = 0;
					for (CyNode multiNode : multiNodes) {
						double radians = Math.PI * 2 * multiNodeIndex / numMulti;
						View<CyNode> nodeView = networkView.getNodeView(multiNode);

						nodeView.setVisualProperty(xLoc, getXCoordinateFromPolar(radiusMulti, radians));
						nodeView.setVisualProperty(yLoc, getYCoordinateFromPolar(radiusMulti, radians));
						multiNodeIndex += 1;
					}

					double memberNodeIndex = 0;
					for (CyNode memberNode : orderedMembers) {
						double radians = Math.PI * 2 * memberNodeIndex / numMembers;
						View<CyNode> nodeView = networkView.getNodeView(memberNode);

						nodeView.setVisualProperty(xLoc, getXCoordinateFromPolar(radiusMembers, radians));
						nodeView.setVisualProperty(yLoc, getYCoordinateFromPolar(radiusMembers, radians));
						memberNodeIndex += 1;
					}

					double doubleNodeIndex = 0;
					HashMap<String, Integer> numberPlacedDouble = new HashMap<>();
					for (CyNode doubleNode : doubleNodes) {
						String connectedPair = doubleNodeMembers.get(doubleNode);
						boolean connectedAreAdjacent = false;

						CyNode neighbor1 = memberNamesToNodes.get(connectedPair.split(" ")[0]);
						CyNode neighbor2 = memberNamesToNodes.get(connectedPair.split(" ")[1]);

						int indexNeighbor1 = orderedMembers.indexOf(neighbor1);
						int indexNeighbor2 = orderedMembers.indexOf(neighbor2);

						if (indexNeighbor1 + 1 == indexNeighbor2 || indexNeighbor1 - 1 == indexNeighbor2) {
							connectedAreAdjacent = true;
						} else if ((indexNeighbor1 == 0 && indexNeighbor2 == numMembers - 1) || (indexNeighbor2 == 0 && indexNeighbor1 == numMembers - 1)) {
							connectedAreAdjacent = true;
							indexNeighbor1 = numMembers - 1;
							indexNeighbor2 = numMembers;
						}

						double radians;
						View<CyNode> nodeView = networkView.getNodeView(doubleNode);
						if (!connectedAreAdjacent) {
							// Place the node in the double node circle
							radians = Math.PI * 2 * doubleNodeIndex / numDouble;

							nodeView.setVisualProperty(xLoc, getXCoordinateFromPolar(radiusDouble, radians));
							nodeView.setVisualProperty(yLoc, getYCoordinateFromPolar(radiusDouble, radians));
						} else {
							// Place the node between the member neighbors
							radians = Math.PI * 2 * (indexNeighbor1 + indexNeighbor2) / (2 * numMembers);
							double radiusOffset = 4 * sizeMetaboliteNode;
							if (numberPlacedDouble.get(connectedPair) != null) {
								radiusOffset = radiusOffset + sizeMetaboliteNode * 1.5 * numberPlacedDouble.get(connectedPair);
							}

							nodeView.setVisualProperty(xLoc, getXCoordinateFromPolar(radiusDouble + radiusOffset, radians));
							nodeView.setVisualProperty(yLoc, getYCoordinateFromPolar(radiusDouble + radiusOffset, radians));

							numberPlacedDouble.putIfAbsent(connectedPair, 0);
							numberPlacedDouble.put(connectedPair, numberPlacedDouble.get(connectedPair) + 1);
						}
						doubleNodeIndex += 1;
					}

					HashMap<CyNode, Integer> numberPlacedSingle = new HashMap<>();
					double minRadiansOffset = 2 * sizeMetaboliteNode / radiusSingle;
					for (CyNode singleNode : singleNodes) {
						CyNode neighbor = null;
						List<CyEdge> edges = currentNetwork.getAdjacentEdgeList(singleNode, CyEdge.Type.ANY);
						for (CyEdge edge : edges) {
							View<CyEdge> edgeView = networkView.getEdgeView(edge);
							if (!edgeView.getVisualProperty(BasicVisualLexicon.EDGE_VISIBLE)) {
								continue;
							}
							neighbor = edge.getSource();
							if (Objects.equals(neighbor, singleNode)) {
								neighbor = edge.getTarget();
							}
						}
						numberPlacedSingle.putIfAbsent(neighbor, 0);

						memberNodeIndex = orderedMembers.indexOf(neighbor);
						double radians = Math.PI * 2 * memberNodeIndex / numMembers;
						double radiansOffset = Math.ceil(numberPlacedSingle.get(neighbor) / 2.0d);
						if (numberPlacedSingle.get(neighbor) % 2 == 0) {
							radiansOffset = -1 * radiansOffset;
						}

						View<CyNode> nodeView = networkView.getNodeView(singleNode);

						nodeView.setVisualProperty(xLoc, getXCoordinateFromPolar(radiusSingle, radians + minRadiansOffset * radiansOffset));
						nodeView.setVisualProperty(yLoc, getYCoordinateFromPolar(radiusSingle, radians + minRadiansOffset * radiansOffset));
						numberPlacedSingle.put(neighbor, numberPlacedSingle.get(neighbor) + 1);
					}


				}
			}
		};
		return new TaskIterator(task);
	}

	/*
	public Object createLayoutContext() {
		return new ScynetLayoutContext();
	}
	 */

	private double getXCoordinateFromPolar(double radius, double radians) {
		return (double) Math.round(radius * Math.cos(radians));
	}

	private double getYCoordinateFromPolar(double radius, double radians) {
		return (double) Math.round(radius * Math.sin(radians));
	}
}
