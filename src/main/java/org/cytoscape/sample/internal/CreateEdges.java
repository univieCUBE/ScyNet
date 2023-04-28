package org.cytoscape.sample.internal;

import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;

import java.util.*;

/**
 * This class is used to add edges to the newly created network.
 * The edges are added to the corresponding nodes and then their attributes are filled into the EdgeTable.
 */
public class CreateEdges {

    /**
     * All nodes from the old network
     */
    private final List<CyNode> cyNodeList;
    /**
     * The old network
     */
    private final CyNetwork oldNetwork;
    /**
     * The new network
     */
    private final CyNetwork newNetwork;
    /**
     * The CreateNodes object created earlier
     */
    private final CreateNodes createNodes;
    /**
     * Map of each node from the old network to its outgoing edges
     */
    private final HashMap<CyNode, List<CyEdge>> outgoingEdges;
    /**
     * Map of each node from the old network to its incoming edges
     */
    private final HashMap<CyNode, List<CyEdge>> incomingEdges;
    /**
     * List of ID's created for each to avoid duplicates
     */
    private final List<String> edgeIDs;
    /**
     * List of the old external nodes
     */
    private final List<CyNode> oldExternalNodes;
    /**
     * TSV-map created from the TSV-file with fluxes, if it was added
     */
    private final HashMap<String, Double> tsvMap;
    /**
     * boolean saying if the TSV-map was added
     */
    private boolean mapAdded = true;
    /**
     * Flux-map translated from the TSV-map
     */
    private final HashMap<CyNode, Double> nodeFluxes = new HashMap<>();

    /**
     * Adds all the corresponding edges and their attributes to the new network.
     * @param oldNetwork is the original network from which the simple network is created
     * @param newNetwork is the new network, which at this point consists only of nodes
     * @param createNodes is the CreateNodes object created earlier holding all the translations
     * @param tsvMap is the map with the flux-values, if one was loaded in
     */
    public CreateEdges(CyNetwork oldNetwork, CyNetwork newNetwork, CreateNodes createNodes, HashMap<String, Double> tsvMap) {
        this.edgeIDs = new ArrayList<>();
        if (tsvMap.isEmpty()) {this.mapAdded = false;}
        this.tsvMap = tsvMap;
        this.newNetwork = newNetwork;
        this.oldNetwork = oldNetwork;
        this.createNodes = createNodes;
        this.cyNodeList = oldNetwork.getNodeList();
        this.oldExternalNodes = createNodes.getExtNodes();
        this.outgoingEdges = mkMapOfOutEdges();
        this.incomingEdges = mkMapOfInEdges();
        makeFluxMap();
        makeAllEdges();
    }


    /**
     * The columns of the new EdgeTable are created, then all edges are created and their attributes
     * are added into the table.
     */
    private void makeAllEdges() {
        // here we add the columns needed in the edge-table and then we create all the edges
        newNetwork.getDefaultEdgeTable().createColumn("source", String.class, true);
        newNetwork.getDefaultEdgeTable().createColumn("target", String.class, true);
        newNetwork.getDefaultEdgeTable().createColumn("edgeID", String.class, true);
        newNetwork.getDefaultEdgeTable().createColumn("flux", Double.class, true);
        newNetwork.getDefaultEdgeTable().createColumn("stoichiometry", Double.class, true);
        makeEdgesToNode();
        makeEdgesFromNode();
    }

    /**
     * This method makes edges between external nodes and the sources of edges going to these nodes, or between external nodes and the internal compartment nodes
     * if the sources are located inside a compartment. It loops through all external nodes and their sources, using their corresponding
     * old and new nodes to create edges, and sets the edge attributes.
     */
    private void makeEdgesToNode() {
        // here we loop through all external Nodes and get their Sources, using these we make edges the external Nodes
        // or compartment Nodes if the Source is in a compartment

        for (CyNode oldExchgNode : createNodes.getExchgNodes()) {

            List<CyNode> oldSources = getAllNeighbors(oldExchgNode, "Sources");

            for (CyNode oSource : oldSources) {

                if (oldExternalNodes.contains(oSource)) {


                    CyEdge edge = makeEdge(createNodes.getNewNode(oSource), createNodes.getNewNode(oldExchgNode));
                    edgeTributes(edge, oSource, oldExchgNode);
                } else {

                    CyNode comp = createNodes.getIntCompNodeForAnyNode(oSource);
                    CyEdge edge = makeEdge(comp, createNodes.getNewNode(oldExchgNode));
                    edgeTributesComp(edge, oSource, oldExchgNode, true);

                }
            }
        }
    }

    /**
     * Loops through all external Nodes and gets their targets, using these we make edges from the external Nodes or
     * compartment Nodes if the target is in a compartment.
     */
    private void makeEdgesFromNode () {
        // here we loop through all external Nodes and get their Targets, using these we make edges the external Nodes
        // or compartment Nodes if the Target is in a compartment
        for (CyNode oldExchgNode : createNodes.getExchgNodes()) {
            List<CyNode> oldTargets = getAllNeighbors(oldExchgNode, "Targets");
            for (CyNode oTarget : oldTargets) {

                CyNode comp = createNodes.getIntCompNodeForAnyNode(oTarget);

                CyEdge edge = makeEdge(createNodes.getNewNode(oldExchgNode), comp);
                edgeTributesComp(edge, oldExchgNode, oTarget, false);
            }
        }
    }

    /**
     * Creates and returns a map that maps each node in the cyNodeList to a list of its outgoing edges.
     *
     * @return a HashMap that maps each node in the cyNodeList to a list of its outgoing edges
     */
    private HashMap<CyNode, List<CyEdge>> mkMapOfOutEdges () {
        // this method is used to create the Map which maps a Node to its outgoing Edges
        HashMap<CyNode, List<CyEdge>> outEdges = new HashMap<>();
        for (CyNode cyNode : cyNodeList) {
            outEdges.put(cyNode, oldNetwork.getAdjacentEdgeList(cyNode, CyEdge.Type.OUTGOING));
        }
        return outEdges;
    }

    /**
     * Creates and returns a map that maps each node in the cyNodeList to a list of its incoming edges.
     *
     * @return a HashMap that maps each node in the cyNodeList to a list of its incoming edges
     */
    private HashMap<CyNode, List<CyEdge>> mkMapOfInEdges () {
        // this method is used to create the Map which maps a Node to its incoming Edges
        HashMap<CyNode, List<CyEdge>> inEdges = new HashMap<>();
        for (CyNode cyNode : cyNodeList) {
            inEdges.put(cyNode, oldNetwork.getAdjacentEdgeList(cyNode, CyEdge.Type.INCOMING));
        }
        return inEdges;
    }

    /**
     * Returns a list of all neighbors of the given node in the given direction.
     *
     * @param oldExchgNode the node to get the neighbors of
     * @param direction the direction in which to get the neighbors ("Sources" or "Targets")
     * @return a list of all neighbors of the given node in the given direction
     */
    private List<CyNode> getAllNeighbors (CyNode oldExchgNode, String direction ){

        List<CyNode> oldNeighbors = new ArrayList<>();

        if (Objects.equals(direction, "Sources")) {
            List<CyEdge> edges = incomingEdges.get(oldExchgNode);
            for (CyEdge edge : edges) {
                oldNeighbors.add(edge.getSource());
            }
        } else {
            List<CyEdge> edges = outgoingEdges.get(oldExchgNode);
            for (CyEdge edge : edges) {
                oldNeighbors.add(edge.getTarget());
            }
        }

        return oldNeighbors;
    }

    /**
     * Creates and returns a new directed edge between the source and target nodes if it doesn't exist already,
     * otherwise returns the already created edge.
     *
     * @param source the source node for the edge
     * @param target the target node for the edge
     * @return a new directed edge between the source and target nodes if it doesn't exist already, otherwise returns the already created edge.
     */
    private CyEdge makeEdge (CyNode source, CyNode target){
        // here an Edge is created if it does not already exist, which is checked by its individual edgeID
        // otherwise the already created Edge is returned

        String edgeID = source.getSUID().toString().concat("-".concat(target.getSUID().toString()));
        if (!edgeIDs.contains(edgeID)) {

            edgeIDs.add(edgeID);
            CyEdge newEdge = newNetwork.addEdge(source, target, true);
            newNetwork.getDefaultEdgeTable().getRow(newEdge.getSUID()).set("Source", newNetwork.getDefaultNodeTable().getRow(source.getSUID()).get("shared Name", String.class));
            newNetwork.getDefaultEdgeTable().getRow(newEdge.getSUID()).set("Target", newNetwork.getDefaultNodeTable().getRow(target.getSUID()).get("shared Name", String.class));
            return newEdge;
        } else {
            // I AM NOT SURE WHY THIS IS NEEDED, BUT IT IS NEEDED
            // WHY ARE THERE MULTIPLE TIMES THE SAME EDGE (at least same edgeID) IN THE OLD NETWORK???
            return newNetwork.getConnectingEdgeList(source, target, CyEdge.Type.DIRECTED).get(0);
        }
    }

    /**
     * Adds attributes of an edge to its entry in the edge-table (external Node to external Node).
     *
     * @param currentEdge the current edge to add attributes to
     * @param oldSource the old source node of the current edge
     * @param oldTarget the old target node of the current edge
     */
    private void edgeTributes (CyEdge currentEdge, CyNode oldSource, CyNode oldTarget){
        // here all the attributes of an Edge are added to its entry in the edge-table (external Node to external Node)
        List<CyEdge> oldEdges = oldNetwork.getConnectingEdgeList(oldSource, oldTarget, CyEdge.Type.ANY);
        String sourceName = oldNetwork.getDefaultNodeTable().getRow(oldSource.getSUID()).get("shared name", String.class);
        String targetName = oldNetwork.getDefaultNodeTable().getRow(oldTarget.getSUID()).get("shared name", String.class);
        newNetwork.getDefaultEdgeTable().getRow(currentEdge.getSUID()).set("source", sourceName);
        newNetwork.getDefaultEdgeTable().getRow(currentEdge.getSUID()).set("target", targetName);

        Double stoichiometry = 0.0;
        for (CyEdge oldEdge : oldEdges) {
            Double stoich = oldNetwork.getDefaultEdgeTable().getRow(oldEdge.getSUID()).get("stoichiometry", Double.class);
            if (stoich != null) {
                stoichiometry += stoich;
            }
        }
        newNetwork.getDefaultEdgeTable().getRow(currentEdge.getSUID()).set("stoichiometry", stoichiometry);
    }

    /**
     * Adds attributes of an edge to its entry in the edge-table (external Node to comp Node).
     *
     * @param currentEdge The edge whose attributes are to be added.
     * @param oldSource The old source node of the edge.
     * @param oldTarget The old target node of the edge.
     * @param sourceIsComp A boolean indicating if the old source node is a compartment.
     */
    private void edgeTributesComp (CyEdge currentEdge, CyNode oldSource, CyNode oldTarget,boolean sourceIsComp){
        // here all the attributes of an Edge are added to its entry in the edge-table (external Node to comp Node)
        if (sourceIsComp) {
            String sourceName = createNodes.getCompNameFromNode(createNodes.getIntCompNodeForAnyNode(oldSource));
            String targetName = oldNetwork.getDefaultNodeTable().getRow(oldTarget.getSUID()).get("shared name", String.class);
            String sharedName = oldNetwork.getDefaultNodeTable().getRow(oldSource.getSUID()).get("shared name", String.class);
            String fluxKey = getFluxKey(oldSource);
            Double fluxValue = getFlux(fluxKey);
            if(mapAdded) {
                setFlux(oldTarget, fluxValue);
            }
            newNetwork.getDefaultEdgeTable().getRow(currentEdge.getSUID()).set("source", sourceName);
            newNetwork.getDefaultEdgeTable().getRow(currentEdge.getSUID()).set("target", targetName);
            newNetwork.getDefaultEdgeTable().getRow(currentEdge.getSUID()).set("shared name", sharedName);
            newNetwork.getDefaultEdgeTable().getRow(currentEdge.getSUID()).set("shared interaction", "EXPORT");
            newNetwork.getDefaultEdgeTable().getRow(currentEdge.getSUID()).set("flux", fluxValue);
            newNetwork.getDefaultEdgeTable().getRow(currentEdge.getSUID()).set("name", fluxKey);
        } else {
            String targetName = createNodes.getCompNameFromNode(createNodes.getIntCompNodeForAnyNode(oldTarget));
            String sourceName = oldNetwork.getDefaultNodeTable().getRow(oldSource.getSUID()).get("shared name", String.class);
            String sharedName = sourceName.concat(" - Import");
            String fluxKey = getFluxKey(oldTarget);
            Double fluxValue = getFlux(fluxKey);
            if(mapAdded) {
                setFlux(oldSource, fluxValue);
            }
            newNetwork.getDefaultEdgeTable().getRow(currentEdge.getSUID()).set("source", sourceName);
            newNetwork.getDefaultEdgeTable().getRow(currentEdge.getSUID()).set("target", targetName);
            newNetwork.getDefaultEdgeTable().getRow(currentEdge.getSUID()).set("shared name", sharedName);
            newNetwork.getDefaultEdgeTable().getRow(currentEdge.getSUID()).set("shared interaction", "IMPORT");
            newNetwork.getDefaultEdgeTable().getRow(currentEdge.getSUID()).set("flux", fluxValue);
            newNetwork.getDefaultEdgeTable().getRow(currentEdge.getSUID()).set("name", fluxKey);
        }

        List<CyEdge> oldEdges = oldNetwork.getConnectingEdgeList(oldSource, oldTarget, CyEdge.Type.ANY);
        Double stoichiometry = 0.0;
        for (CyEdge oldEdge : oldEdges) {
            Double current_stoichiometry = oldNetwork.getDefaultEdgeTable().getRow(oldEdge.getSUID()).get("stoichiometry", Double.class);
            if (current_stoichiometry != null) {
                stoichiometry += current_stoichiometry;
            }
        }
        newNetwork.getDefaultEdgeTable().getRow(currentEdge.getSUID()).set("stoichiometry", stoichiometry);
    }

    /**
     * Creates a hashmap to determine whether the nodes in the new network have flux or not, with a default value of 0.0.
     */
    private void makeFluxMap(){
        // create a hashmap to determine whether the nodes in the new network  have flux or not, set to 0 as default
        for (CyNode exchgNode: createNodes.getExchgNodes()){
            CyNode newExchgNode = createNodes.getNewNode(exchgNode);
            nodeFluxes.putIfAbsent(newExchgNode, 0.0d);
        }
    }

    /**
     * Sets the flux value for a given node in the nodeFluxes hashmap. If the flux value is not 0.0,
     * adds the absolute value of the flux to the current flux value for the node in the hashmap.
     *
     * @param oldNode the old node for which to set the flux value
     * @param fluxValue the new flux value to set for the node
     */
    private void setFlux(CyNode oldNode, Double fluxValue){
        CyNode newNode = createNodes.getNewNode(oldNode);
        if (!fluxValue.equals(0.0)){
            Double newFlux = nodeFluxes.get(newNode) + Math.abs(fluxValue);
            nodeFluxes.put(newNode,newFlux);
        }
    }

    /**
     * Returns the key for the flux value of a given node, obtained from its sbml id.
     * If the node does not represent a reaction, an empty string is returned.
     *
     * @param oldNode the node for which to obtain the flux key
     * @return the key for the flux value of the node, or an empty string if the node does not represent a reaction
     */
    private String getFluxKey(CyNode oldNode){
        //why do we use sbml type AND ID?
        String oldType = oldNetwork.getDefaultNodeTable().getRow(oldNode.getSUID()).get("sbml type", String.class);
        String oldID = oldNetwork.getDefaultNodeTable().getRow(oldNode.getSUID()).get("sbml id", String.class);
        String[] splitID = oldID.split("_", 0);
        if (Objects.equals(oldType, "reaction") && Objects.equals(splitID[0], "R")) {
            String key = "";
            if (splitID[2].length() == 2) {
                key = splitID[1].concat(splitID[3]);
            } else {
                key = splitID[1].concat(splitID[2]);
            }
            return key;
        }
        return "";
    }

    /**
     * Returns the flux value corresponding to the given key from the flux map if it exists. If the key
     * is not found in the flux map and the map has been added, it returns 0.0. If the key is empty or the map
     * has not been added, returns null.
     *
     * @param key the key to retrieve the flux value
     * @return the flux value corresponding to the given key if it exists, 0.0 if the key is not found and the map is added,
     * or null if the key is empty or the map has not been added.
     */
    private Double getFlux(String key) {
        if (mapAdded && tsvMap.get(key) == null) {
            return 0.0d;
        }
        if (!mapAdded || Objects.equals(key, "")) {
            return null;
        } else {
            return tsvMap.get(key);
        }
    }

    /**
     * Get-function for the Flux-Map created
     *
     * @return the Flux-Map
     */
    public HashMap<CyNode, Double> getFLuxMap(){
        return nodeFluxes;
    }
}





