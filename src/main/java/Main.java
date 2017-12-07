import org.apache.commons.cli.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Scanner;

/**
 * @author Andrew Tang
 */
public class Main {

    public static final String GTFS_OPTION = "p";
    public static final String URL_OPTION = "u";
    public static final String DATABASE_OPTION = "d";
    public static final String CMD_NAME = "gtsql";
    public static final String FOOTER = "\nThis tool is used to generate an SQLite database from a GTFS feed. " +
            "\nPlease report issues at https://github.com/aytee17/gtfs-to-sqlite";

    public static void main(String[] args) {
        try {
            Options options = new Options();

            Option path = Option.builder(GTFS_OPTION)
                    .longOpt("path")
                    .hasArg()
                    .argName("gtfs_path")
                    .desc("Path to the GTFS data")
                    .required()
                    .build();

            Option url = Option.builder(URL_OPTION)
                    .longOpt("url")
                    .hasArg()
                    .argName("gtfs_url")
                    .desc("URL to the GTFS data")
                    .build();

            Option database = Option.builder(DATABASE_OPTION)
                    .longOpt("database")
                    .hasArg()
                    .argName("database_path")
                    .desc("Path to the database file")
                    .required()
                    .build();

            options.addOption(path)
                    .addOption(url)
                    .addOption(database);

            try {
                CommandLine line = new DefaultParser().parse(options, args);

                Path databasePath = Paths.get(line.getOptionValue(DATABASE_OPTION));
                if ( Files.exists(databasePath) ) {
                    print("A file at " + databasePath.toString() + " already exists.");
                    System.out.print("Would you like to replace it? (yes/no) ");
                    Scanner reader = new Scanner(System.in);
                    String response  = reader.nextLine();
                    if (response.equals("yes")) {
                        print("Deleting file...");
                        Files.delete(databasePath);
                        print("File deleted.");
                    } else if (response.equals("no")) {
                        System.exit(0);
                    } else {
                        throw new Exception("Unrecognised response.");
                    }
                    reader.close();
                }

                Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath.toAbsolutePath());
                connection.setAutoCommit(false);

                File gtfsFile;
                String gtfsPath = Paths
                        .get(line.getOptionValue(GTFS_OPTION))
                        .toAbsolutePath()
                        .toString();

                if (line.hasOption(URL_OPTION)) {
                    String gtfsURL = line.getOptionValue(URL_OPTION);
                    print("Downloading GTFS feed from: " + gtfsURL);
                    gtfsFile = IO.getFileFromURL(gtfsPath   + System.getProperty("file.separator") + "GTFS.zip", gtfsURL);
                    print("Feed downloaded.");
                } else {
                    gtfsFile = new File(gtfsPath);
                }

                new Loader(gtfsFile, connection);
                connection.close();
                print("Database created.");

            } catch (ParseException exception) {
                print(exception.getMessage() + "\n");
                new HelpFormatter().printHelp(CMD_NAME, "", options, FOOTER, true);
            }
        }  catch (Exception e) {
            e.printStackTrace();
            print(e.getMessage());
            print(FOOTER);
        }
    }

    public static void print(String message) {
        System.out.println(message);
    }
}
