import org.apache.commons.cli.*;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * @author Andrew Tang
 */
public class Main {
    public static final String GTFS_FLAG = "g";
    public static final String GTFS_DESCRIPTION = "Path to the GTFS data";

    public static final String URL_FLAG = "u";
    public static final String URL_DESCRIPTION = "URL to the GTFS data";

    public static final String DATABASE_FLAG = "d";
    public static final String DATABASE_DESCRIPTION = "Path to the database file";

    public static void main(String[] args) {
        Connection connection = null;
        try {
            Options options = new Options();
            options.addOption(GTFS_FLAG, true , GTFS_DESCRIPTION);
            options.addOption(DATABASE_FLAG, true, DATABASE_DESCRIPTION);
            options.addOption(URL_FLAG, true, URL_DESCRIPTION);

            CommandLineParser parser = new DefaultParser();
            CommandLine line = parser.parse(options, args);

            if (line.hasOption(GTFS_FLAG) == false && line.hasOption(URL_FLAG) == false) {
                System.out.println("Need to set URL or file path to gtfs");
                System.exit(-1);
            }
            if (!line.hasOption(DATABASE_FLAG)) {
                System.exit(-1);
            }

            String gtfsPath;
            File gtfsFile;

            if (line.hasOption(URL_FLAG)) {
                String gtfsURL = line.getOptionValue(URL_FLAG);
                print("Downloading feed from: " + gtfsURL);
                gtfsFile = IO.getFileFromURL("./GTFS.zip", gtfsURL);
                print("Feed downloaded.");
            } else {
                gtfsPath = line.getOptionValue(GTFS_FLAG);
                gtfsFile = new File(gtfsPath);
            }

            String databasePath = line.getOptionValue(DATABASE_FLAG);
            connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath);
            connection.setAutoCommit(false);

            new Loader(gtfsFile, connection);

        } catch (ParseException parseException) {
            parseException.printStackTrace();
       } catch (SQLException sqlException) {
            sqlException.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    // Global helper method for printing
    public static void print(String message) {
        System.out.println(message);
    }
}
