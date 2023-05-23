package org.cytoscape.sample.internal;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Objects;

/**
 * A class used to select a file which is used to create a HashMap, which maps all reactions/edges to a Flux.
 */
public class FileChoosing {

    /**
     * The chosen file (should be TSV-format)
     */
    private File chosenFile;

    /**
     * This opens a JFileChooser where a TSV-file can be selected.
     */
    public FileChoosing()
    {
        // Here we use the JFileChooser to open a window where the user can select a TSV-file with the fluxes
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Choose the tab-delimited TSV-file or press CANCEL");

        int fileValue = chooser.showDialog(null, "Choose");
        // If there was no file selected here, later we will return the empty tsvMap with makeMap()
        if(fileValue == JFileChooser.APPROVE_OPTION)
        {
            this.chosenFile = chooser.getSelectedFile();
        }
    }

    /**
     * Using the previously added TSV-file a HashMap is created, which maps all the reactions/edges to a certain Flux.
     *
     * @return The Hashmap mapping all edges to a Flux. If no file was added an empty HashMap is returned.
     */
    public HashMap<String, Double> makeMap() {
        HashMap<String, Double> tsvMap = new HashMap<>();
        if (chosenFile == null) {
            return tsvMap;
        }
        String line = "";
        try {
            // parsing a TSV file into BufferedReader class constructor
            BufferedReader br = new BufferedReader(new FileReader(chosenFile));
            while ((line = br.readLine()) != null)
            {
                String[] values = line.split("\t", 0);
                if (!Objects.equals(values[0], "reaction_id")) {
                    String key = values[0];
                    //String[] splitValues = values[0].split("_",0);
                    //if (splitValues.length == 4) {
                    //    key = splitValues[0].concat(splitValues[2]);
                    //} else {
                    //    key = splitValues[0].concat(splitValues[1]);
                    //}
                    tsvMap.put(key, Double.parseDouble(values[1]));
                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return tsvMap;
    }
}
