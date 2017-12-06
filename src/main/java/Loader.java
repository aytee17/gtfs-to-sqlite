import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @author Andrew Tang
 */
public class Loader {

    private List<File> mTextFiles;
    private JSONObject mSpecification;

    public Loader (File gtfsPath, Connection connection) {
        mSpecification = getSpecification();
        mTextFiles = getFiles(gtfsPath);
        DatabaseBuilder builder = new DatabaseBuilder(mSpecification, connection);
        for (File texFile : mTextFiles) {
            builder.addTable(texFile);
        }

        builder.buildDatabase();
    }

    private JSONObject getSpecification() {
        try {
            Main.print("Reading specification.");
            InputStream jsonStream = this.getClass().getResource("GTFS_Specification.json").openStream();
            BufferedReader jsonReader = new BufferedReader(new InputStreamReader(jsonStream));
            StringBuilder jsonStringBuilder = new StringBuilder();

            String line;
            while ((line = jsonReader.readLine()) != null) {
                jsonStringBuilder.append(line);
            }
            jsonReader.close();

            String jsonString = jsonStringBuilder.toString();
            Main.print("Specification loaded.");
            return new JSONObject(jsonString);
        }
        catch (IOException ioe) {
            ioe.printStackTrace();
        }
        catch (JSONException je) {
            je.printStackTrace();
        }
        return null;
    }

    private List<File> getFiles(File gtfsPath) {
        List<File> textFiles = new ArrayList<>();
        if (gtfsPath.isDirectory()) {
            File[] files = gtfsPath.listFiles();
            for (int i = 0; i < files.length; i++) {
                textFiles.add(files[i]);
                Main.print("Loaded " + files[i].getName());
            }
        } else {
            try {
                ZipFile zipFile = new ZipFile(gtfsPath);

                File gtfsFolder = new File(gtfsPath.getParent() + System.getProperty("file.separator") + "GTFS");
                gtfsFolder.mkdir();

                Enumeration<? extends ZipEntry> entries = zipFile.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    InputStream inputStream = zipFile.getInputStream(entry);
                    File entryFile = new File(gtfsFolder.getPath(), entry.getName());

                    IO.writeInputToFile(inputStream, entryFile);
                    textFiles.add(entryFile);
                    Main.print("Loaded " + entryFile.getName());
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Main.print("\n");
        return textFiles;
    }
}
