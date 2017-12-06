import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Andrew Tang
 */

public class QueryCreator {
    private static final Pattern FOREIGN_KEY_PATTERN = Pattern.compile(".+, FOREIGN KEY\\(\\) REFERENCES ([a-z]+)\\(?.+\\)");
    private JSONObject mTextFileSpec;
    // The text file containing the table and it's values
    private File mTextFile;


    // The attributes defined in mTextFile
    private String[] mFileAttributes;
    /* The indicies of all valid attributes in mFileAttributes, where valid attributes are attributes that
    appear in the spec */
    private List<Integer> mValidAttributes;

    private String mTableName;

    private String mCreateQuery;

    private List<String[]> mDependencies;

    // Holds all attributes in a composite key ff this table has one
    private List<String> mCompositeKey;
    private List<String> mForeignKeys;

    public List<String> getIndexQueries() {
        return mIndexQueries;
    }

    private List<String> mIndexQueries;


    private BufferedReader mBufferedReader;

    public QueryCreator(JSONObject textFileSpec, File textFile) throws JSONException {
        mTextFileSpec = textFileSpec;
        mTextFile = textFile;

        mTableName = mTextFile.getName().replaceAll(".txt", "");
        mCompositeKey = new ArrayList<>();
        mForeignKeys = new ArrayList<>();

        mFileAttributes = getAttributes();
        mValidAttributes = new ArrayList<>();

        mDependencies = new ArrayList<>();

        // Initialize Dependencies and ValidAttributes
        for (int i = 0; i < mFileAttributes.length; i++) {
            String attribute = mFileAttributes[i];
            if (mTextFileSpec.has(attribute)) {
                mValidAttributes.add(new Integer(i));
                String attributeDefinition = mTextFileSpec.getString(attribute);
                Matcher match = FOREIGN_KEY_PATTERN.matcher(attributeDefinition);
                if (match.find()) {
                    String table = match.group(1);
                    mDependencies.add(new String[] {
                            attribute,
                            table
                    });
                }
            }
        }

        try {
            mCreateQuery = generateCreateTableQuery();
        } catch (IOException e) {
            e.printStackTrace();
        }

        mIndexQueries = generateCreateIndexQueries();
    }

    private String[] getAttributes() {
        try {
            //Read GTFS data text file
            FileReader fileReader = new FileReader(mTextFile);
            BufferedReader bufferedReader = new BufferedReader(fileReader);

            // Read the first line of the text file containing the column names of the table
            String firstLine = bufferedReader.readLine();
            String[] fileAttributes = firstLine.split(",");

            //bufferedReader.close();
            mBufferedReader = bufferedReader;

            return fileAttributes;

        } catch (IOException ioe) {
            return null;
        }
    }

    private String generateCreateTableQuery() throws IOException, JSONException {

        // Initialize the CREATE query

        StringBuilder createTableQuery = new StringBuilder(
                "CREATE TABLE IF NOT EXISTS " + mTableName + " (\n"
        );

        // Lookup the associated SQL type column from GTFS_Specification, for each attribute in the file
        for (int i = 0; i < mFileAttributes.length; i++) {
            String attribute = mFileAttributes[i];
            boolean endOfAttributes;
            if (mTextFileSpec.has(attribute)) {
                String attributeDefinition = mTextFileSpec.getString(attribute);
                attributeDefinition = processDefinition(attribute, attributeDefinition);

                createTableQuery.append("\t" + attribute + " " + attributeDefinition);

                endOfAttributes = (i + 1) >= mValidAttributes.size();

                // If there are more attributes add a comma and new line
                if (!endOfAttributes) {
                    createTableQuery.append(",\n");
                } else {

                    //add a primary key statement for composite keys at the end if there are any
                    if (!mCompositeKey.isEmpty()) {

                        createTableQuery.append(",\n");
                        StringBuilder privateKeyDeclaration = new StringBuilder("\tPRIMARY KEY(");
                        for (int j = 0; j < mCompositeKey.size(); j++) {
                            privateKeyDeclaration.append(mCompositeKey.get(j));
                            if (j != (mCompositeKey.size() - 1)) {
                                privateKeyDeclaration.append(", ");
                            }
                        }
                        privateKeyDeclaration.append(")");
                        createTableQuery.append(privateKeyDeclaration);
                    }

                    // add foreign key statements
                    if (!mForeignKeys.isEmpty()) {
                        createTableQuery.append(",\n");
                        for (int k = 0; k < mForeignKeys.size(); k++) {
                            createTableQuery.append(mForeignKeys.get(k));
                            if (k != (mForeignKeys.size() - 1)) {
                                createTableQuery.append(",\n");
                            }
                        }
                    }
                }
            }
        }
        createTableQuery.append("\n);");
        return createTableQuery.toString();
    }

    private List<String> generateCreateIndexQueries() {
        List<String> indexQueries = new ArrayList<>();

        String table = getTableName();

        for (String[] entry : mDependencies) {
            String referenceAttribute = entry[0];
            String referenceTable = entry[1];

            StringBuilder query = new StringBuilder("CREATE INDEX fk_" +
                    table + "_" + referenceTable + "_" + referenceAttribute +
                    " ON " + table + "(" + referenceAttribute + ");");
            indexQueries.add(query.toString());
        }

        return indexQueries;
    }

    private String processDefinition(String attribute, String definition) {
        String foreignKeyPattern = ".+, FOREIGN KEY\\(\\) REFERENCES .+\\(?.+\\)";
        String multiplePrimaryKeysPattern = ".+, PRIMARY KEY";

        if (definition.matches(foreignKeyPattern)) {
            definition = definition.replaceAll("\\(\\)", "(" + attribute + ")");
            String[] args = definition.split(", ");
            definition = args[0];
            StringBuilder foreignKey = new StringBuilder("\t" + args[1]);
            mForeignKeys.add(foreignKey.toString());
        } else if (definition.matches(multiplePrimaryKeysPattern)) {
            definition = definition.replaceAll(", PRIMARY KEY", "");
            mCompositeKey.add(attribute);
        }
        return definition;
    }


    /**
     * Queries are executed as soon as they are built to save from storing them in memory for CreationNode.
     * This violates the 'separation of concerns' principle and should be changed, especially if this software
     * is to be extended to support more databases.
     */
    public void executeInsertQueries(Connection connection) {
        try {
            CSVParser parser = new CSVParser(mBufferedReader, CSVFormat.EXCEL);
            Statement insertStatement = connection.createStatement();

            for (CSVRecord record : parser) {

                StringBuilder queryBuild = new StringBuilder("INSERT INTO " + getTableName());
                StringBuilder columns = new StringBuilder(" ( ");
                StringBuilder values = new StringBuilder("VALUES( ");

                for (int i = 0; i < mValidAttributes.size(); i++) {
                    Integer validIndex = mValidAttributes.get(i);

                    String attribute = mFileAttributes[validIndex];
                    columns.append(attribute);

                    String value = record.get(validIndex);
                    values.append("\"" + value + "\"");

                    boolean endOfAttributes = (i + 1) >= mValidAttributes.size();

                    if (!endOfAttributes) {
                        String seperator = ", ";
                        columns.append(seperator);
                        values.append(seperator);
                    } else {
                        columns.append(") ");
                        values.append(");");
                    }
                }
                queryBuild.append(columns).append(values);
                String query = queryBuild.toString();
                insertStatement.executeUpdate(query);
            }

            insertStatement.close();

        } catch (IOException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public String getTableName() {
        return mTableName;
    }

    public String getCreateQuery() {
        return mCreateQuery;
    }

    public List<String[]> getTableDependencies() {
        return mDependencies;
    }

}


























