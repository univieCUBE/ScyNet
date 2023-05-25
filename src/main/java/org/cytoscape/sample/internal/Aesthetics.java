package org.cytoscape.sample.internal;

import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.presentation.property.NodeShapeVisualProperty;
import org.cytoscape.view.presentation.property.ArrowShapeVisualProperty;

import javax.swing.plaf.ColorUIResource;
import java.awt.*;
import java.util.*;
import java.util.List;

import static java.lang.Math.abs;
/**
* Defines methods to modify the visual appearance of a Cytoscape network based on certain criteria.
* Takes the CreateNodes object created earlier, which is used to access information about the network's nodes, and a HashMap
* of flux values for each node. It also takes the network and network view to be modified, a boolean flag indicating whether
* only "crossfeeding" nodes should be displayed, and a HashMap of flux values for each node.
*/
public class Aesthetics {

    /**
     * The instance of CreateNodes created earlier
     */
    private final CreateNodes nodes;
    /**
     * The new network
     */
    private final CyNetwork newNetwork;
    /**
     * The new network view
     */
    private final CyNetworkView newView;
    /**
     * A List of all compartment names
     */
    private final Set<String> compList;
    /**
     * Flux-Map translated from the TSV-Map
     */
    private HashMap<CyNode, Double> fluxMap;
    /**
     * TSV-map created from the TSV-file if it was added
     */
    private HashMap<String, Double> tsvMap;
    /**
     * The boolean defining if the submitted flux map is fva or fba
     */
    private boolean isFva;
    /**
     * Constructs an Aesthetics object using the specified parameters.
     *
     * @param nodes             the CreateNodes object used to access information about the network's nodes
     * @param fluxMap           a HashMap of flux values for each node in the network
     * @param newNetwork        the CyNetwork object to be modified
     * @param newView           the CyNetworkView object to be modified
     * @param showOnlyCrossfeeding  a boolean flag indicating whether only crossfeeding nodes should be displayed
     * @param tsvMap            a HashMap of TSV values for each node in the network
     */

    public Aesthetics(CreateNodes nodes, HashMap<CyNode, Double> fluxMap, CyNetwork newNetwork, CyNetworkView newView, boolean showOnlyCrossfeeding, HashMap<String, Double> tsvMap, Boolean isFva) {
        this.nodes = nodes;
        this.newNetwork = newNetwork;
        this.newView = newView;
        this.compList = nodes.getOrganisms();
        this.fluxMap = fluxMap;
        this.tsvMap = tsvMap;
        this.isFva = isFva;
        compNodes();
        exchgNodes();
        edges();
        if (!tsvMap.isEmpty()) {
            if (!isFva) {
                setCrossFeedingNodeStatus();  // cross-feeding status depends on flux data
            }
            else {
                setCrossFeedingNodeStatusFva();
            }
        }
        newView.updateView();
        if (showOnlyCrossfeeding) {
            removeNonCFNodes();
        }
        removeSingletons();
    }
    /**
     * Modifies the appearance of the compartment nodes in the network view.
     */
    private void compNodes() {
        // Here we change the appearance of the Compartment Nodes
        Iterator<String> compListIterator = compList.iterator();
        Color[] equidistantColors = getEquidistantColors(compList.size());
        int idx = 0;
        while (compListIterator.hasNext()) {
            String compartment = compListIterator.next();
            idx++;
            // Here
            View<CyNode> compNodeView = newView.getNodeView(nodes.getCompNodeFromName(compartment));
            if (compNodeView == null) {
                continue;
            }

            String compNodeName = newNetwork.getDefaultNodeTable().getRow(nodes.getCompNodeFromName(compartment).getSUID()).get("shared name", String.class);
            double compNodeSize = compNodeView.getVisualProperty(BasicVisualLexicon.NODE_SIZE) + 100;
            Paint compNodeColor = new ColorUIResource(equidistantColors[idx - 1]);
            Integer size = 25;
            compNodeView.setLockedValue(BasicVisualLexicon.NODE_FILL_COLOR, compNodeColor);
            compNodeView.setLockedValue(BasicVisualLexicon.NODE_SIZE, compNodeSize);
            compNodeView.setLockedValue(BasicVisualLexicon.NODE_LABEL, compNodeName);
            compNodeView.setLockedValue(BasicVisualLexicon.NODE_LABEL_FONT_SIZE, size);
            compNodeView.setLockedValue(BasicVisualLexicon.NODE_SHAPE, NodeShapeVisualProperty.ELLIPSE);
        }
    }
    /**
     * Modifies the appearance of the external nodes in the network view and removes nodes with no flux.
     */
    private void exchgNodes() {
        // Here we change the appearance of the external Nodes
        List<CyNode> extNodesList = nodes.getExchgNodes();
        for (CyNode exchgNode : extNodesList) {
            CyNode newNode = nodes.getNewNode(exchgNode);
            View<CyNode> exchgNodeView = newView.getNodeView(newNode);
            if (exchgNodeView == null) {
                continue;
            }
            String exchgNodeName = newNetwork.getDefaultNodeTable().getRow(nodes.getNewNode(exchgNode).getSUID()).get("shared name", String.class);
            Paint exchgNodeColor = Color.getHSBColor(0.05f, 0.61f, 0.94f);
            double exchgNodeWidth = 120.0d;
            double exchgNodeHeight = 55.0d;
            int size = 22;

            exchgNodeView.setLockedValue(BasicVisualLexicon.NODE_FILL_COLOR, exchgNodeColor);
            exchgNodeView.setLockedValue(BasicVisualLexicon.NODE_WIDTH, exchgNodeWidth);
            exchgNodeView.setLockedValue(BasicVisualLexicon.NODE_HEIGHT, exchgNodeHeight);
            exchgNodeView.setLockedValue(BasicVisualLexicon.NODE_LABEL, exchgNodeName);
            exchgNodeView.setLockedValue(BasicVisualLexicon.NODE_LABEL_FONT_SIZE, size);
            exchgNodeView.setLockedValue(BasicVisualLexicon.NODE_SHAPE, NodeShapeVisualProperty.ELLIPSE);
            /*if (!tsvMap.isEmpty() && fluxMap.get(newNode).equals(0.0d)){
                newNetwork.removeNodes(Collections.singletonList(newNode));
                // exchgNodeView.setVisualProperty(BasicVisualLexicon.NODE_TRANSPARENCY, 0);
            }*/
        }
    }
    /**
     * Modifies the appearance of the edges in the network view.
     */
    private void edges () {
        // Here we change the appearance of the Edges
        for (CyEdge newEdge : newNetwork.getEdgeList()) {
            String edgeSourceName = newNetwork.getDefaultNodeTable().getRow(newEdge.getSource().getSUID()).get("shared name", String.class);
            String edgeTargetName = newNetwork.getDefaultNodeTable().getRow(newEdge.getTarget().getSUID()).get("shared name", String.class);
            Double edgeFlux = newNetwork.getDefaultEdgeTable().getRow(newEdge.getSUID()).get("flux", Double.class);
            View<CyEdge> edgeView = newView.getEdgeView(newEdge);
            // If Fluxes were added or not
            if (edgeFlux != null) {
                // Color of the Edges is selected based on Fluxes
                Paint edgeColor;
                if (edgeFlux >= 0.0d) {
                    edgeColor = Color.getHSBColor(220.0f/360.0f, 0.61f, 0.94f);
                    edgeView.setLockedValue(BasicVisualLexicon.EDGE_TARGET_ARROW_SHAPE, ArrowShapeVisualProperty.DELTA);
                } else {
                    edgeColor = Color.getHSBColor(90.0f/360.0f, 0.61f, 0.94f);
                    edgeView.setLockedValue(BasicVisualLexicon.EDGE_TARGET_ARROW_SHAPE, ArrowShapeVisualProperty.DELTA);
                }
                edgeView.setLockedValue(BasicVisualLexicon.EDGE_PAINT, edgeColor);

                // Width of the Edges is also based on Fluxes
                if (edgeFlux != 0.0d) {
                    Double edgeWidth = abs(edgeFlux) + 1;
                    if (edgeWidth > 50.0d) {
                        edgeWidth = 50.0d;
                    }
                    edgeView.setLockedValue(BasicVisualLexicon.EDGE_WIDTH, edgeWidth);
                } else {
                    edgeView.setLockedValue(BasicVisualLexicon.EDGE_TRANSPARENCY, 0);
                }
            } else {
                // Otherwise we just chose the Color based on their direction
                if (compList.contains(edgeSourceName)) {
                    Paint edgeColor = Color.getHSBColor(220.0f/360.0f, 0.61f, 0.94f);
                    edgeView.setLockedValue(BasicVisualLexicon.EDGE_PAINT, edgeColor);
                }
                if (compList.contains(edgeTargetName)) {
                    Paint edgeColor = Color.getHSBColor(90.0f/360.0f, 0.61f, 0.94f);
                    edgeView.setLockedValue(BasicVisualLexicon.EDGE_PAINT, edgeColor);
                }
            }
        }
    }

    /**
     * Identifies and removes all nodes that do not have crossfeeding.
     */
    private void removeSingletons() {

        List<CyNode> allNodes = newNetwork.getNodeList();
        for (CyNode node : allNodes) {
            List<CyEdge> adjacentEdgeList = newNetwork.getAdjacentEdgeList(node, CyEdge.Type.ANY);
            if (adjacentEdgeList.isEmpty()) {
                newNetwork.removeNodes(Collections.singletonList(node));
                // newView.getNodeView(newNode).setVisualProperty(BasicVisualLexicon.NODE_TRANSPARENCY, 0);
            }
        }
    }
    /**
     * Sets the cross-fed column value for all exchange nodes
     */
    private void setCrossFeedingNodeStatus() {

        List<CyNode> extNodesList = nodes.getExchgNodes();
        for (CyNode exchgNode : extNodesList) {
            CyNode newNode = nodes.getNewNode(exchgNode);
            List<CyEdge> adjacentEdgeList = newNetwork.getAdjacentEdgeList(newNode, CyEdge.Type.ANY);
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
                newNetwork.getDefaultNodeTable().getRow(newNode.getSUID()).set("cross-fed", false);
                // newView.getNodeView(newNode).setVisualProperty(BasicVisualLexicon.NODE_TRANSPARENCY, 0);
            }
            else {
                newNetwork.getDefaultNodeTable().getRow(newNode.getSUID()).set("cross-fed", true);
            }
        }
    }
    /**
     * Sets the cross-fed column value for all exchange nodes
     */
    private void setCrossFeedingNodeStatusFva() {

        List<CyNode> extNodesList = nodes.getExchgNodes();
        for (CyNode exchgNode : extNodesList) {
            Set<String> negativeSet = new HashSet<>();
            Set<String> positiveSet = new HashSet<>();
            CyNode newNode = nodes.getNewNode(exchgNode);
            List<CyEdge> adjacentEdgeList = newNetwork.getAdjacentEdgeList(newNode, CyEdge.Type.ANY);
            boolean positive = false;
            boolean negative = false;
            for (CyEdge currentEdge : adjacentEdgeList) {
                Double currentFlux = newNetwork.getDefaultEdgeTable().getRow(currentEdge.getSUID()).get("flux", Double.class);
                if (currentFlux == null) {
                    continue;
                }

                String target = newNetwork.getDefaultEdgeTable().getRow(currentEdge.getSUID()).get("target", String.class);
                String source = newNetwork.getDefaultEdgeTable().getRow(currentEdge.getSUID()).get("source", String.class);
                String comp;
                if (nodes.getOrganisms().contains(target)) {
                    comp = target;
                } else if (nodes.getOrganisms().contains(source)) {
                    comp = source;
                }
                else {
                    continue;
                }

                if (currentFlux < 0) {
                    negativeSet.add(comp);
                }
                if (currentFlux > 0) {
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
                newNetwork.getDefaultNodeTable().getRow(newNode.getSUID()).set("cross-fed", true);
                // newView.getNodeView(newNode).setVisualProperty(BasicVisualLexicon.NODE_TRANSPARENCY, 0);
            }
            else {
                newNetwork.getDefaultNodeTable().getRow(newNode.getSUID()).set("cross-fed", false);
            }
        }
    }

    /**
     * Identifies and removes all nodes that do not have crossfeeding.
     */
    private void removeNonCFNodes() {

        List<CyNode> extNodesList = nodes.getExchgNodes();
        for (CyNode exchgNode : extNodesList) {
            CyNode newNode = nodes.getNewNode(exchgNode);
            List<CyEdge> adjacentEdgeList = newNetwork.getAdjacentEdgeList(newNode, CyEdge.Type.ANY);
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
            if (!(positive && negative)) {
                newNetwork.removeNodes(Collections.singletonList(newNode));
                // newView.getNodeView(newNode).setVisualProperty(BasicVisualLexicon.NODE_TRANSPARENCY, 0);
            }
        }
    }

    /**
     * Calculates equidistant colors for the given number of compartment nodes in the network.
     *
     * @param n amount of colors which a needed to generate
     * @return the values for the 'n' colors
     */
    public static Color[] getEquidistantColors(int n) {
        Color[] colors = new Color[n];
        for (int i = 0; i < n; i++) {
            float hue = (float) i / n;
            colors[i] = Color.getHSBColor(hue, 0.61f, 0.94f);
        }
        return colors;
    }
}


