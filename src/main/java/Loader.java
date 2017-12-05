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
        mTextFiles = getFiles(gtfsPath);
        mSpecification = getSpecification();
        DatabaseBuilder builder = new DatabaseBuilder(mSpecification, connection);
        for (File texFile : mTextFiles) {
            builder.addTable(texFile);
        }

        builder.buildDatabase();
        Main.print("Done.");
    }

    private JSONObject getSpecification() {
        try {
            InputStream jsonStream = this.getClass().getResource("GTFS_Specification.json").openStream();
            BufferedReader jsonReader = new BufferedReader(new InputStreamReader(jsonStream));
            StringBuilder jsonStringBuilder = new StringBuilder();

            String line;
            while ((line = jsonReader.readLine()) != null) {
                jsonStringBuilder.append(line);
            }
            jsonReader.close();

            String jsonString = jsonStringBuilder.toString();
            Main.print("spec loaded");
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
            }
        } else {
            try {
                ZipFile zipFile = new ZipFile(gtfsPath);

                File newFolder = new File("./GTFS");
                newFolder.mkdir();

                Enumeration<? extends ZipEntry> entries = zipFile.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    InputStream inputStream = zipFile.getInputStream(entry);
                    File entryFile = new File(newFolder.getPath(), entry.getName());

                    IO.writeInputToFile(inputStream, entryFile);
                    Main.print("got " + entryFile.getName());
                    textFiles.add(entryFile);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return textFiles;
    }
}
