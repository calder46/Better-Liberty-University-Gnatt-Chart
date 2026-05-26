package Dashboard;

import java.io.*;
import java.util.*;

/**
 * Handles serialization and deserialization of the course layout.
 * Saves which courses are in which semester column to a .dat file.
 * 
 * @author Jacob Wingate
 */
public class SaveData implements Serializable {

    private static final long serialVersionUID = 1L;

    //Maps column name (e.g. "year1Fall") to list of [courseName, credits] pairs
    private Map<String, List<String[]>> layout;

    public SaveData(Map<String, List<String[]>> layout) {
        this.layout = layout;
    }

    public Map<String, List<String[]>> getLayout() {
        return layout;
    }

    /**
     * Serializes the SaveData object to a file chosen by the user.
     * @param data - the SaveData object to save
     * @param file - the destination file
     */
    public static void save(SaveData data, File file) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            oos.writeObject(data);
            System.out.println("Layout saved to: " + file.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Save failed: " + e.getMessage());
        }
    }

    /**
     * Deserializes a SaveData object from a file chosen by the user.
     * @param file - the source file
     * @return SaveData object, or null if load failed
     */
    public static SaveData load(File file) {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            return (SaveData) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Load failed: " + e.getMessage());
            return null;
        }
    }
}
